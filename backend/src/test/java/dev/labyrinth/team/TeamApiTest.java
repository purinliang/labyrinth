package dev.labyrinth.team;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class TeamApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void createsPersistsAndRetrievesTeamThroughLocation() throws Exception {
        var created = mvc
                .perform(MockMvcRequestBuilders.post("/lab/api/teams")
                        .contextPath("/lab").queryParam("ignored", "value")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Backend\"}"))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name")
                        .value("Backend"))
                .andReturn();

        String location = created.getResponse().getHeader("Location");
        assertNotNull(location);
        URI uri = URI.create(location);
        assertEquals("http", uri.getScheme());
        assertEquals("localhost", uri.getHost());
        assertNull(uri.getQuery());
        assertTrue(uri.getPath().startsWith("/lab/api/teams/"));
        long id = Long.parseLong(
                uri.getPath().substring(uri.getPath().lastIndexOf('/') + 1));
        assertTrue(id > 0);
        assertEquals("Backend", jdbc.queryForObject(
                "SELECT name FROM teams WHERE id = ?", String.class, id));

        mvc.perform(
                MockMvcRequestBuilders.get(uri.getPath()).contextPath("/lab"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(id))
                .andExpect(MockMvcResultMatchers.content()
                        .json(created.getResponse().getContentAsString()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"name\":null}", "{\"name\":\"\"}",
            "{\"name\":\" \\t \\n\"}"})
    void rejectsInvalidNamesWithoutCreatingTeam(String body) throws Exception {
        Long before =
                jdbc.queryForObject("SELECT COUNT(*) FROM teams", Long.class);
        mvc.perform(MockMvcRequestBuilders.post("/api/teams")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.content()
                        .json("{\"message\":\"Invalid argument\"}"));
        assertEquals(before,
                jdbc.queryForObject("SELECT COUNT(*) FROM teams", Long.class));
    }

    @Test
    void returnsNotFoundForUnknownTeam() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/teams/0"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message")
                        .value("Team not found"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-number", "9223372036854775808"})
    void rejectsInvalidIds(String id) throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/teams/{id}", id))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    void createsIndependentTeamsWithSameName() throws Exception {
        String first = createTeam();
        String second = createTeam();
        assertNotEquals(first, second);
        for (String location : new String[]{first, second}) {
            mvc.perform(MockMvcRequestBuilders.get(URI.create(location)))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.name")
                            .value("Repeated"));
        }
    }

    private String createTeam() throws Exception {
        return mvc
                .perform(MockMvcRequestBuilders.post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Repeated\"}"))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn().getResponse().getHeader("Location");
    }
}
