package dev.labyrinth.ticket;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import dev.labyrinth.PostgresTestConfiguration;
import dev.labyrinth.team.Team;
import dev.labyrinth.team.TeamRepository;
import dev.labyrinth.user.User;
import dev.labyrinth.user.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class TicketTransferTransactionTest {

    @Autowired
    private TicketService service;

    @Autowired
    private TeamRepository teams;

    @Autowired
    private UserRepository users;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager manager;

    @Test
    void rollsBackTicketAndCommentWhenLaterWorkFails() {
        Fixture fixture = fixture();
        long id = fixture.ticket().getId();
        var transaction = new TransactionTemplate(transactionManager);
        assertThrows(IllegalStateException.class,
                () -> transaction.executeWithoutResult(status -> {
                    service.transferTicket(id, fixture.target().getId(),
                            fixture.author().getId(), "Transfer note", 0);
                    // Ensure both writes reach PostgreSQL before the failure.
                    manager.flush();
                    assertEquals(fixture.target().getId(), jdbc.queryForObject(
                            "SELECT current_team_id FROM tickets WHERE id = ?",
                            Long.class, id));
                    assertEquals(1L, commentCount(id));
                    throw new IllegalStateException("Later work failed");
                }));

        Ticket reloaded = service.getTicket(id);
        assertEquals(fixture.source().getId(),
                reloaded.getCurrentTeam().getId());
        assertEquals(0, reloaded.getVersion());
        assertEquals(TicketStatus.OPEN, reloaded.getStatus());
        assertEquals(0L, commentCount(id));
    }

    @ParameterizedTest
    @ValueSource(strings = {"transfer", "close"})
    void commitsOnlyOneConcurrentUpdateAndItsComment(String competingAction)
            throws Exception {
        Fixture fixture = fixture();
        long id = fixture.ticket().getId();
        CyclicBarrier barrier = new CyclicBarrier(2);
        boolean transferWon;
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> attemptUpdate(fixture,
                    fixture.target().getId(), "transfer", "First", barrier));
            var second = executor.submit(
                    () -> attemptUpdate(fixture, fixture.otherTarget().getId(),
                            competingAction, "Second", barrier));
            var firstFailure = first.get(30, TimeUnit.SECONDS);
            var secondFailure = second.get(30, TimeUnit.SECONDS);
            // Both passed the version check; exactly one commit must fail.
            assertNotEquals(firstFailure == null, secondFailure == null);
            transferWon = firstFailure == null;
        }

        Ticket reloaded = service.getTicket(id);
        assertEquals(1, reloaded.getVersion());
        assertEquals(fixture.ticket().getCreatedAt(), reloaded.getCreatedAt());
        if (transferWon || competingAction.equals("transfer")) {
            Team destination =
                    transferWon ? fixture.target() : fixture.otherTarget();
            assertEquals(destination.getId(),
                    reloaded.getCurrentTeam().getId());
            assertEquals(TicketStatus.OPEN, reloaded.getStatus());
            assertEquals(1L, commentCount(id));
            assertEquals(transferWon ? "First" : "Second",
                    jdbc.queryForObject(
                            "SELECT content FROM comments WHERE ticket_id = ?",
                            String.class, id));
            assertEquals(fixture.author().getId(), jdbc.queryForObject(
                    "SELECT author_id FROM comments WHERE ticket_id = ?",
                    Long.class, id));
        } else {
            assertEquals(fixture.source().getId(),
                    reloaded.getCurrentTeam().getId());
            assertEquals(TicketStatus.CLOSED, reloaded.getStatus());
            assertEquals(0L, commentCount(id));
        }
    }

    private OptimisticLockingFailureException attemptUpdate(Fixture fixture,
            long targetId, String action, String comment,
            CyclicBarrier barrier) {
        try {
            new TransactionTemplate(transactionManager)
                    .executeWithoutResult(status -> {
                        if (action.equals("transfer")) {
                            service.transferTicket(fixture.ticket().getId(),
                                    targetId, fixture.author().getId(),
                                    comment, 0);
                        } else {
                            service.closeTicket(fixture.ticket().getId(), 0);
                        }
                        // Hold both transactions before either can commit.
                        awaitBoth(barrier);
                    });
            return null;
        } catch (OptimisticLockingFailureException exception) {
            return exception;
        }
    }

    private void awaitBoth(CyclicBarrier barrier) {
        try {
            barrier.await(15, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        } catch (BrokenBarrierException | TimeoutException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private Fixture fixture() {
        Team source = teams.save(new Team("Transaction source"));
        Team target = teams.save(new Team("Transaction destination"));
        Team otherTarget = teams.save(new Team("Other destination"));
        User author = users.save(new User("Researcher", source));
        Ticket ticket =
                service.createTicket("Demo", "Content", source.getId());
        return new Fixture(ticket, source, target, otherTarget, author);
    }

    private Long commentCount(long ticketId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM comments WHERE ticket_id = ?",
                Long.class, ticketId);
    }

    private record Fixture(Ticket ticket, Team source, Team target,
            Team otherTarget, User author) {
    }
}
