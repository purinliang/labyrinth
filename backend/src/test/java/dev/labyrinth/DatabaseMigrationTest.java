package dev.labyrinth;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class DatabaseMigrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void appliesAndValidatesAllMigrationsOnFreshPostgres() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertEquals("PostgreSQL",
                    connection.getMetaData().getDatabaseProductName());
        }
        flyway.validate();
        assertEquals(0, flyway.info().pending().length);
        assertNull(jdbc.queryForObject(
                "SELECT to_regclass('public.experiments')::text",
                String.class));
        assertEquals(4, jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_name IN "
                        + "('teams', 'users', 'tickets', 'comments')",
                Integer.class));
        assertEquals(1,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM flyway_schema_history "
                                + "WHERE version = '3' AND success = true",
                        Integer.class));
    }

    @Test
    void upgradesVersionTwoWithoutLosingTicketData() {
        // Isolate the historical schema from the application's current schema.
        try (var postgres = new PostgreSQLContainer("postgres:17")) {
            postgres.start();
            var source = new DriverManagerDataSource(postgres.getJdbcUrl(),
                    postgres.getUsername(), postgres.getPassword());
            Flyway oldSchema =
                    Flyway.configure().dataSource(source).target("2").load();
            assertEquals(2, oldSchema.migrate().migrationsExecuted);
            var legacy = new JdbcTemplate(source);
            legacy.update("INSERT INTO experiments (name, status) "
                    + "VALUES ('Retired record', 'PENDING')");
            Long teamId = legacy.queryForObject(
                    "INSERT INTO teams (name) VALUES (?) RETURNING id",
                    Long.class, "Retained team");
            Long userId = legacy.queryForObject(
                    "INSERT INTO users (name, team_id) VALUES (?, ?) "
                            + "RETURNING id",
                    Long.class, "Retained user", teamId);
            Long ticketId = legacy.queryForObject(
                    "INSERT INTO tickets "
                            + "(title, content, status, current_team_id) "
                            + "VALUES (?, ?, 'OPEN', ?) RETURNING id",
                    Long.class, "Retained ticket", "Retained content", teamId);
            Long commentId = legacy.queryForObject(
                    "INSERT INTO comments (content, author_id, ticket_id) "
                            + "VALUES (?, ?, ?) RETURNING id",
                    Long.class, "Retained comment", userId, ticketId);

            Flyway upgrade =
                    Flyway.configure().dataSource(source).target("3").load();
            assertEquals(1, upgrade.migrate().migrationsExecuted);
            upgrade.validate();
            assertNull(legacy.queryForObject(
                    "SELECT to_regclass('public.experiments')::text",
                    String.class));
            assertRetainedData(legacy, teamId, userId, ticketId, commentId);

            assertEquals(0, upgrade.migrate().migrationsExecuted);
            assertRetainedData(legacy, teamId, userId, ticketId, commentId);
        }
    }

    private void assertRetainedData(JdbcTemplate database, long teamId,
            long userId, long ticketId, long commentId) {
        assertEquals(1, database.queryForObject("""
                SELECT COUNT(*)
                FROM tickets t
                JOIN teams g ON g.id = t.current_team_id
                JOIN comments c ON c.ticket_id = t.id
                JOIN users u ON u.id = c.author_id
                WHERE g.id = ? AND u.id = ? AND t.id = ? AND c.id = ?
                    AND g.name = 'Retained team'
                    AND u.name = 'Retained user' AND u.team_id = g.id
                    AND t.title = 'Retained ticket'
                    AND t.content = 'Retained content'
                    AND t.status = 'OPEN' AND t.version = 0
                    AND c.content = 'Retained comment'
                """, Integer.class, teamId, userId, ticketId, commentId));
    }
}
