package dev.labyrinth.experiment;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import dev.labyrinth.PostgresTestConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class ExperimentPersistenceTest {

    @Autowired
    private ExperimentRepository repository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    @Test
    void appliesFlywayMigrationToRealPostgres() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertEquals("PostgreSQL",
                    connection.getMetaData().getDatabaseProductName());
        }
        flyway.validate();
        assertEquals("1", flyway.info().current().getVersion().getVersion());
        assertEquals(1,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM flyway_schema_history "
                                + "WHERE version = '1' AND success = true",
                        Integer.class));
    }

    @Test
    void persistsExperimentBeyondItsOriginalPersistenceContext() {
        Experiment saved = repository
                .saveAndFlush(new Experiment("O'Reilly — Δ experiment"));
        assertNotNull(saved.getId());
        assertTrue(saved.getId() > 0);

        try (var manager = entityManagerFactory.createEntityManager()) {
            Experiment reloaded =
                    manager.find(Experiment.class, saved.getId());
            assertNotNull(reloaded);
            assertNotSame(saved, reloaded);
            assertEquals(saved.getId(), reloaded.getId());
            assertEquals("O'Reilly — Δ experiment", reloaded.getName());
            assertEquals(ExperimentStatus.PENDING, reloaded.getStatus());
        }
        assertEquals(saved.getName(),
                jdbc.queryForObject(
                        "SELECT name FROM experiments WHERE id = ?",
                        String.class, saved.getId()));
    }

    @Test
    void repeatedMigrationKeepsPersistedExperiments() {
        Experiment saved = repository.saveAndFlush(new Experiment("Retained"));
        assertEquals(0, flyway.migrate().migrationsExecuted);
        Experiment reloaded = repository.findById(saved.getId()).orElseThrow();
        assertEquals("Retained", reloaded.getName());
        assertEquals(ExperimentStatus.PENDING, reloaded.getStatus());
    }

    @Test
    void duplicateNamesHaveIndependentDatabaseGeneratedIds() {
        Experiment first =
                repository.saveAndFlush(new Experiment("Duplicate"));
        Experiment second =
                repository.saveAndFlush(new Experiment("Duplicate"));
        assertTrue(!first.getId().equals(second.getId()));
        assertTrue(repository.findById(first.getId()).isPresent());
        assertTrue(repository.findById(second.getId()).isPresent());
    }
}
