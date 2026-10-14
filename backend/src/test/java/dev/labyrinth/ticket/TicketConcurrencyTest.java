package dev.labyrinth.ticket;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import dev.labyrinth.GlobalExceptionHandler;
import dev.labyrinth.PostgresTestConfiguration;
import dev.labyrinth.team.Team;
import dev.labyrinth.team.TeamRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class TicketConcurrencyTest {

    @Autowired
    private TicketService service;

    @Autowired
    private TeamRepository teams;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @ParameterizedTest
    @ValueSource(strings = {"close", "reopen"})
    void preventsLostUpdatesAndMapsCommitConflictToHttp409(String action)
            throws Exception {
        long teamId = teams.save(new Team("Concurrent status team")).getId();
        long id = service.createTicket("Demo", "Content", teamId).getId();
        if (action.equals("reopen")) {
            service.closeTicket(id, 0);
        }
        int expectedVersion = service.getTicket(id).getVersion();
        CyclicBarrier barrier = new CyclicBarrier(2);
        OptimisticLockingFailureException conflict;
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> attemptTransition(id,
                    expectedVersion, action, barrier));
            var second = executor.submit(() -> attemptTransition(id,
                    expectedVersion, action, barrier));
            var firstResult = first.get(30, TimeUnit.SECONDS);
            var secondResult = second.get(30, TimeUnit.SECONDS);
            if (firstResult == null) {
                assertNotNull(secondResult);
                conflict = secondResult;
            } else {
                assertNull(secondResult);
                conflict = firstResult;
            }
        }
        assertEquals(action.equals("close") ? "CLOSED" : "OPEN",
                jdbc.queryForObject("SELECT status FROM tickets WHERE id = ?",
                        String.class, id));
        assertEquals(expectedVersion + 1,
                jdbc.queryForObject("SELECT version FROM tickets WHERE id = ?",
                        Integer.class, id));

        // Send the actual database conflict through the HTTP exception
        // handler.
        TicketService failingService = mock(TicketService.class);
        if (action.equals("close")) {
            when(failingService.closeTicket(id, expectedVersion))
                    .thenThrow(conflict);
        } else {
            when(failingService.reopenTicket(id, expectedVersion))
                    .thenThrow(conflict);
        }
        var mvc = MockMvcBuilders
                .standaloneSetup(new TicketController(failingService))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(MockMvcRequestBuilders
                .post("/api/tickets/{id}/{action}", id, action)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":" + expectedVersion + "}"))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.content()
                        .json("{\"message\":\"Version conflict\"}"));
    }

    private OptimisticLockingFailureException attemptTransition(long id,
            int expectedVersion, String action, CyclicBarrier barrier) {
        try {
            new TransactionTemplate(transactionManager)
                    .executeWithoutResult(transaction -> {
                        if (action.equals("close")) {
                            service.closeTicket(id, expectedVersion);
                        } else {
                            service.reopenTicket(id, expectedVersion);
                        }
                        // Both transactions must read the same version
                        // before either commits.
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
}
