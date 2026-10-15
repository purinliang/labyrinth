package dev.labyrinth.ticket;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import dev.labyrinth.PostgresTestConfiguration;
import dev.labyrinth.team.Team;
import dev.labyrinth.team.TeamRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class TicketStatusApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TicketService service;

    @Autowired
    private TeamRepository teams;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void closesAndReopensTicketWithCommittedVersions() throws Exception {
        Ticket original = createTicket();
        long id = original.getId();
        String closed = mvc
                .perform(MockMvcRequestBuilders
                        .post("/api/tickets/{id}/close", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0}"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status")
                        .value("CLOSED"))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.version").value(1))
                .andReturn().getResponse().getContentAsString();
        mvc.perform(MockMvcRequestBuilders.get("/api/tickets/{id}", id))
                .andExpect(MockMvcResultMatchers.content().json(closed));
        assertEquals("CLOSED", jdbc.queryForObject(
                "SELECT status FROM tickets WHERE id = ?", String.class, id));
        assertEquals(1,
                jdbc.queryForObject("SELECT version FROM tickets WHERE id = ?",
                        Integer.class, id));

        String reopened = mvc
                .perform(MockMvcRequestBuilders
                        .post("/api/tickets/{id}/reopen", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":1}"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status")
                        .value("OPEN"))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.version").value(2))
                .andReturn().getResponse().getContentAsString();
        mvc.perform(MockMvcRequestBuilders.get("/api/tickets/{id}", id))
                .andExpect(MockMvcResultMatchers.content().json(reopened));
        Ticket reloaded = service.getTicket(id);
        assertEquals(Ticket.Status.OPEN, reloaded.getStatus());
        assertEquals(2, reloaded.getVersion());
        assertEquals(original.getTitle(), reloaded.getTitle());
        assertEquals(original.getContent(), reloaded.getContent());
        assertEquals(original.getCurrentTeam().getId(),
                reloaded.getCurrentTeam().getId());
        assertEquals(original.getCreatedAt(), reloaded.getCreatedAt());
    }

    @ParameterizedTest
    @ValueSource(strings = {"close", "reopen"})
    void rejectsStaleVersionsWithoutChangingTicket(String action)
            throws Exception {
        Ticket ticket = createTicket();
        int staleVersion = 1;
        if (action.equals("reopen")) {
            service.closeTicket(ticket.getId(), 0);
            staleVersion = 0;
        } else {
            service.closeTicket(ticket.getId(), 0);
            service.reopenTicket(ticket.getId(), 1);
        }
        String before = getTicketBody(ticket.getId());
        mvc.perform(MockMvcRequestBuilders
                .post("/api/tickets/{id}/{action}", ticket.getId(), action)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":" + staleVersion + "}"))
                .andExpect(MockMvcResultMatchers.status().isConflict());
        assertEquals(before, getTicketBody(ticket.getId()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"close", "reopen"})
    void rejectsRepeatedTransitionsWithoutChangingTicket(String action)
            throws Exception {
        Ticket ticket = createTicket();
        int version = 0;
        if (action.equals("close")) {
            service.closeTicket(ticket.getId(), 0);
            version = 1;
        }
        String before = getTicketBody(ticket.getId());
        mvc.perform(MockMvcRequestBuilders
                .post("/api/tickets/{id}/{action}", ticket.getId(), action)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":" + version + "}"))
                .andExpect(MockMvcResultMatchers.status().isConflict());
        assertEquals(before, getTicketBody(ticket.getId()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"close", "reopen"})
    void returnsNotFoundForUnknownTicket(String action) throws Exception {
        mvc.perform(
                MockMvcRequestBuilders.post("/api/tickets/0/{action}", action)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0}"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"expectedVersion\":null}",
            "{\"expectedVersion\":-1}"})
    void rejectsInvalidVersionsWithoutChangingTicket(String body)
            throws Exception {
        Ticket ticket = createTicket();
        String before = getTicketBody(ticket.getId());
        for (String action : new String[]{"close", "reopen"}) {
            mvc.perform(MockMvcRequestBuilders
                    .post("/api/tickets/{id}/{action}", ticket.getId(), action)
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.content()
                            .json("{\"message\":\"Invalid argument\"}"));
        }
        assertEquals(before, getTicketBody(ticket.getId()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-number", "9223372036854775808"})
    void rejectsInvalidIds(String id) throws Exception {
        for (String action : new String[]{"close", "reopen"}) {
            mvc.perform(MockMvcRequestBuilders
                    .post("/api/tickets/{id}/{action}", id, action)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"expectedVersion\":0}"))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }
    }

    @Test
    void rejectsMalformedJsonWithoutChangingTicket() throws Exception {
        Ticket ticket = createTicket();
        String before = getTicketBody(ticket.getId());
        for (String action : new String[]{"close", "reopen"}) {
            mvc.perform(MockMvcRequestBuilders
                    .post("/api/tickets/{id}/{action}", ticket.getId(), action)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"expectedVersion\":"))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }
        assertEquals(before, getTicketBody(ticket.getId()));
    }

    private Ticket createTicket() {
        long teamId = teams.save(new Team("Status API team")).getId();
        return service.createTicket("Demo", "Content", teamId);
    }

    private String getTicketBody(long id) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.get("/api/tickets/{id}", id))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn()
                .getResponse().getContentAsString();
    }
}
