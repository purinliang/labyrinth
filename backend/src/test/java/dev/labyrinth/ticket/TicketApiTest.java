package dev.labyrinth.ticket;

import java.net.URI;

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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class TicketApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TeamRepository teams;

    @Test
    void createsPersistsAndRetrievesTicketThroughLocation() throws Exception {
        long teamId = teams.save(new Team("Ticket API team")).getId();
        var created = mvc
                .perform(MockMvcRequestBuilders.post("/lab/api/tickets")
                        .contextPath("/lab").queryParam("ignored", "value")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Demo\",\"content\":\"Content\","
                                + "\"currentTeamId\":" + teamId + "}"))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.title")
                        .value("Demo"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content")
                        .value("Content"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.currentTeamId")
                        .value(teamId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status")
                        .value("OPEN"))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.version").value(0))
                .andExpect(MockMvcResultMatchers.jsonPath("$.createdAt")
                        .isString())
                .andReturn();

        String location = created.getResponse().getHeader("Location");
        assertNotNull(location);
        URI uri = URI.create(location);
        assertEquals("http", uri.getScheme());
        assertEquals("localhost", uri.getHost());
        assertNull(uri.getQuery());
        assertTrue(uri.getPath().startsWith("/lab/api/tickets/"));
        long id = Long.parseLong(
                uri.getPath().substring(uri.getPath().lastIndexOf('/') + 1));
        assertTrue(id > 0);
        assertEquals("Demo", jdbc.queryForObject(
                "SELECT title FROM tickets WHERE id = ?", String.class, id));
        assertEquals(teamId,
                jdbc.queryForObject(
                        "SELECT current_team_id FROM tickets WHERE id = ?",
                        Long.class, id));

        mvc.perform(
                MockMvcRequestBuilders.get(uri.getPath()).contextPath("/lab"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(id))
                .andExpect(MockMvcResultMatchers.content()
                        .json(created.getResponse().getContentAsString()));
    }

    @Test
    void returnsNotFoundForUnknownTicket() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/tickets/0"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void rejectsUnknownTeamWithoutCreatingTicket() throws Exception {
        Long before = ticketCount();
        mvc.perform(MockMvcRequestBuilders.post("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Demo\",\"content\":\"Content\","
                        + "\"currentTeamId\":9223372036854775807}"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
        assertEquals(before, ticketCount());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"content\":\"Content\",\"currentTeamId\":1}",
            "{\"title\":null,\"content\":\"Content\",\"currentTeamId\":1}",
            "{\"title\":\"\",\"content\":\"Content\",\"currentTeamId\":1}",
            "{\"title\":\"Demo\",\"currentTeamId\":1}",
            "{\"title\":\"Demo\",\"content\":null,\"currentTeamId\":1}",
            "{\"title\":\"Demo\",\"content\":\" \",\"currentTeamId\":1}",
            "{\"title\":\"Demo\",\"content\":\"Content\"}",
            "{\"title\":\"Demo\",\"content\":\"Content\","
                    + "\"currentTeamId\":null}",
            "{\"title\":\"Demo\",\"content\":\"Content\","
                    + "\"currentTeamId\":0}",
            "{\"title\":\"Demo\",\"content\":\"Content\","
                    + "\"currentTeamId\":-1}"})
    void rejectsInvalidRequestsWithoutCreatingTicket(String body)
            throws Exception {
        Long before = ticketCount();
        mvc.perform(MockMvcRequestBuilders.post("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.content()
                        .json("{\"message\":\"Invalid argument\"}"));
        assertEquals(before, ticketCount());
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-number", "9223372036854775808"})
    void rejectsInvalidIds(String id) throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/tickets/{id}", id))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    private Long ticketCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM tickets", Long.class);
    }
}
