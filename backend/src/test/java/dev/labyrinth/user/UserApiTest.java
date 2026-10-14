package dev.labyrinth.user;

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
class UserApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TeamRepository teams;

    @Test
    void createsPersistsAndRetrievesUserThroughLocation() throws Exception {
        long teamId = teams.save(new Team("User API team")).getId();
        var created = mvc
                .perform(MockMvcRequestBuilders.post("/lab/api/users")
                        .contextPath("/lab").queryParam("ignored", "value")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Researcher\",\"teamId\":"
                                + teamId + "}"))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name")
                        .value("Researcher"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.teamId")
                        .value(teamId))
                .andReturn();

        String location = created.getResponse().getHeader("Location");
        assertNotNull(location);
        URI uri = URI.create(location);
        assertEquals("http", uri.getScheme());
        assertEquals("localhost", uri.getHost());
        assertNull(uri.getQuery());
        assertTrue(uri.getPath().startsWith("/lab/api/users/"));
        long id = Long.parseLong(
                uri.getPath().substring(uri.getPath().lastIndexOf('/') + 1));
        assertTrue(id > 0);
        assertEquals("Researcher", jdbc.queryForObject(
                "SELECT name FROM users WHERE id = ?", String.class, id));
        assertEquals(teamId, jdbc.queryForObject(
                "SELECT team_id FROM users WHERE id = ?", Long.class, id));

        mvc.perform(
                MockMvcRequestBuilders.get(uri.getPath()).contextPath("/lab"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(id))
                .andExpect(MockMvcResultMatchers.content()
                        .json(created.getResponse().getContentAsString()));
    }

    @Test
    void returnsNotFoundForUnknownUser() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/users/0"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void rejectsUnknownTeamWithoutCreatingUser() throws Exception {
        Long before = userCount();
        mvc.perform(MockMvcRequestBuilders.post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Researcher\","
                        + "\"teamId\":9223372036854775807}"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
        assertEquals(before, userCount());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"teamId\":1}", "{\"name\":null,\"teamId\":1}",
            "{\"name\":\"\",\"teamId\":1}",
            "{\"name\":\" \\t \\n\",\"teamId\":1}",
            "{\"name\":\"Researcher\"}",
            "{\"name\":\"Researcher\",\"teamId\":null}",
            "{\"name\":\"Researcher\",\"teamId\":0}",
            "{\"name\":\"Researcher\",\"teamId\":-1}"})
    void rejectsInvalidRequestsWithoutCreatingUser(String body)
            throws Exception {
        Long before = userCount();
        mvc.perform(MockMvcRequestBuilders.post("/api/users")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.content()
                        .json("{\"message\":\"Invalid argument\"}"));
        assertEquals(before, userCount());
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-number", "9223372036854775808"})
    void rejectsInvalidIds(String id) throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/users/{id}", id))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    private Long userCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class);
    }
}
