package dev.labyrinth.ticket;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import dev.labyrinth.PostgresTestConfiguration;
import dev.labyrinth.comment.Comment;
import dev.labyrinth.team.Team;
import dev.labyrinth.user.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class TicketPersistenceTest {

    @Autowired
    private EntityManagerFactory factory;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void persistsTicketAndCommentWithTheirUserAndTeam() {
        Team team = new Team("Support");
        User user = new User("Alice", team);
        Ticket ticket = new Ticket("Demo", "Ticket content", team);
        Comment comment = new Comment("Comment content", user, ticket);

        try (var manager = factory.createEntityManager()) {
            manager.getTransaction().begin();
            manager.persist(team);
            manager.persist(user);
            manager.persist(ticket);
            manager.persist(comment);
            manager.getTransaction().commit();
        }

        assertNotNull(team.getId());
        assertNotNull(user.getId());
        assertNotNull(ticket.getId());
        assertNotNull(comment.getId());
        assertEquals(0, ticket.getVersion());
        assertNotNull(ticket.getCreatedAt());
        assertEquals(ticket.getCreatedAt(),
                jdbc.queryForObject(
                        "SELECT created_at FROM tickets WHERE id = ?",
                        (result, row) -> result.getTimestamp(1).toInstant(),
                        ticket.getId()));
        assertEquals("OPEN",
                jdbc.queryForObject("SELECT status FROM tickets WHERE id = ?",
                        String.class, ticket.getId()));
        assertNotNull(comment.getCreatedAt());
        assertEquals(comment.getCreatedAt(),
                jdbc.queryForObject(
                        "SELECT created_at FROM comments WHERE id = ?",
                        (result, row) -> result.getTimestamp(1).toInstant(),
                        comment.getId()));

        try (var manager = factory.createEntityManager()) {
            Ticket reloaded = manager.find(Ticket.class, ticket.getId());
            assertEquals("Demo", reloaded.getTitle());
            assertEquals("Ticket content", reloaded.getContent());
            assertEquals(Ticket.Status.OPEN, reloaded.getStatus());
            assertEquals(0, reloaded.getVersion());
            assertEquals(ticket.getCreatedAt(), reloaded.getCreatedAt());
            assertEquals(team.getId(), reloaded.getCurrentTeam().getId());
            assertEquals("Support", reloaded.getCurrentTeam().getName());

            User reloadedUser = manager.find(User.class, user.getId());
            assertEquals("Alice", reloadedUser.getName());
            assertEquals(team.getId(), reloadedUser.getTeam().getId());

            Comment reloadedComment =
                    manager.find(Comment.class, comment.getId());
            assertEquals("Comment content", reloadedComment.getContent());
            assertEquals(user.getId(), reloadedComment.getAuthor().getId());
            assertEquals(ticket.getId(), reloadedComment.getTicket().getId());
            assertEquals(comment.getCreatedAt(),
                    reloadedComment.getCreatedAt());
        }
    }

    @Test
    void rejectsAStaleTicketAfterItsVersionIsIncremented() {
        Ticket original = persistTicket();

        try (var manager = factory.createEntityManager()) {
            manager.getTransaction().begin();
            Ticket current = manager.find(Ticket.class, original.getId());
            manager.lock(current, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
            manager.getTransaction().commit();
        }

        try (var manager = factory.createEntityManager()) {
            Ticket current = manager.find(Ticket.class, original.getId());
            assertEquals(1, current.getVersion());
            assertEquals(original.getCreatedAt(), current.getCreatedAt());
            manager.clear();
            manager.getTransaction().begin();
            try {
                assertThrows(OptimisticLockException.class,
                        () -> manager.merge(original));
            } finally {
                manager.getTransaction().rollback();
            }
        }
    }

    private Ticket persistTicket() {
        Team team = new Team("Version test");
        Ticket ticket = new Ticket("Versioned", "Content", team);
        try (var manager = factory.createEntityManager()) {
            manager.getTransaction().begin();
            manager.persist(team);
            manager.persist(ticket);
            manager.getTransaction().commit();
        }
        return ticket;
    }
}
