package dev.labyrinth.ticket;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import dev.labyrinth.PostgresTestConfiguration;
import dev.labyrinth.team.Team;
import dev.labyrinth.team.TeamRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class TicketServiceTest {

    @Autowired
    private TicketService service;

    @Autowired
    private TeamRepository teams;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void createsPersistsAndRetrievesTicket() {
        long teamId = teams.save(new Team("Ticket service team")).getId();
        Ticket created =
                service.createTicket("Demo", "Ticket content", teamId);

        assertNotNull(created);
        assertNotNull(created.getId());
        assertNotNull(created.getCreatedAt());
        assertEquals(Ticket.Status.OPEN, created.getStatus());
        assertEquals(0, created.getVersion());
        assertEquals(teamId,
                jdbc.queryForObject(
                        "SELECT current_team_id FROM tickets WHERE id = ?",
                        Long.class, created.getId()));
        assertEquals(created.getCreatedAt(),
                jdbc.queryForObject(
                        "SELECT created_at FROM tickets WHERE id = ?",
                        (result, row) -> result.getTimestamp(1).toInstant(),
                        created.getId()));

        Ticket reloaded = service.getTicket(created.getId());
        assertNotNull(reloaded);
        assertEquals(created.getId(), reloaded.getId());
        assertEquals("Demo", reloaded.getTitle());
        assertEquals("Ticket content", reloaded.getContent());
        assertEquals(teamId, reloaded.getCurrentTeam().getId());
        assertEquals(Ticket.Status.OPEN, reloaded.getStatus());
        assertEquals(0, reloaded.getVersion());
        assertEquals(created.getCreatedAt(), reloaded.getCreatedAt());
    }

    @Test
    void doesNotCreateTicketForUnknownTeam() {
        Long before = jdbc.queryForObject("SELECT COUNT(*) FROM tickets",
                Long.class);
        var exception = assertThrows(ResponseStatusException.class,
                () -> service.createTicket("Missing team", "Content",
                        Long.MAX_VALUE));
        assertEquals(404, exception.getStatusCode().value());
        assertEquals("Team not found", exception.getReason());
        assertEquals(before, jdbc
                .queryForObject("SELECT COUNT(*) FROM tickets", Long.class));
    }

    @Test
    void rejectsUnknownTicket() {
        var exception = assertThrows(ResponseStatusException.class,
                () -> service.getTicket(Long.MAX_VALUE));
        assertEquals(404, exception.getStatusCode().value());
        assertEquals("Ticket not found", exception.getReason());
    }
}
