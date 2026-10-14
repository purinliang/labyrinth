package dev.labyrinth.comment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import dev.labyrinth.PostgresTestConfiguration;
import dev.labyrinth.team.Team;
import dev.labyrinth.team.TeamRepository;
import dev.labyrinth.ticket.Ticket;
import dev.labyrinth.ticket.TicketRepository;
import dev.labyrinth.user.User;
import dev.labyrinth.user.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class CommentServiceTest {

    @Autowired
    private CommentService service;

    @Autowired
    private TeamRepository teams;

    @Autowired
    private UserRepository users;

    @Autowired
    private TicketRepository tickets;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void createsPersistsAndRetrievesComment() {
        Team team = teams.save(new Team("Comment service team"));
        User author = users.save(new User("Researcher", team));
        Ticket ticket = tickets.save(new Ticket("Demo", "Content", team));
        Comment created = service.createComment("Comment content",
                author.getId(), ticket.getId());

        assertNotNull(created);
        assertNotNull(created.getId());
        assertNotNull(created.getCreatedAt());
        assertEquals(author.getId(),
                jdbc.queryForObject(
                        "SELECT author_id FROM comments WHERE id = ?",
                        Long.class, created.getId()));
        assertEquals(ticket.getId(),
                jdbc.queryForObject(
                        "SELECT ticket_id FROM comments WHERE id = ?",
                        Long.class, created.getId()));
        assertEquals(created.getCreatedAt(),
                jdbc.queryForObject(
                        "SELECT created_at FROM comments WHERE id = ?",
                        (result, row) -> result.getTimestamp(1).toInstant(),
                        created.getId()));

        Comment reloaded = service.getComment(created.getId());
        assertNotNull(reloaded);
        assertEquals(created.getId(), reloaded.getId());
        assertEquals("Comment content", reloaded.getContent());
        assertEquals(author.getId(), reloaded.getAuthor().getId());
        assertEquals(ticket.getId(), reloaded.getTicket().getId());
        assertEquals(created.getCreatedAt(), reloaded.getCreatedAt());
    }

    @Test
    void doesNotCreateCommentForUnknownAuthor() {
        Team team = teams.save(new Team("Missing author team"));
        Ticket ticket = tickets.save(new Ticket("Demo", "Content", team));
        Long before = commentCount();
        assertNull(service.createComment("Content", Long.MAX_VALUE,
                ticket.getId()));
        assertEquals(before, commentCount());
    }

    @Test
    void doesNotCreateCommentForUnknownTicket() {
        Team team = teams.save(new Team("Missing ticket team"));
        User author = users.save(new User("Researcher", team));
        Long before = commentCount();
        assertNull(service.createComment("Content", author.getId(),
                Long.MAX_VALUE));
        assertEquals(before, commentCount());
    }

    @Test
    void returnsNullForUnknownComment() {
        assertNull(service.getComment(Long.MAX_VALUE));
    }

    private Long commentCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM comments",
                Long.class);
    }
}
