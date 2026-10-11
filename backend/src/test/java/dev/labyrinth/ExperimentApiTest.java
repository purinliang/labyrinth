package dev.labyrinth;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
class ExperimentApiTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void createsExperimentAndRetrievesItThroughLocation() throws Exception {
        MvcResult created = mvc
                .perform(MockMvcRequestBuilders.post("/lab/api/experiments")
                        .contextPath("/lab").queryParam("ignored", "value")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Demo\"}"))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.name").value("Demo"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status")
                        .value("PENDING"))
                .andReturn();

        String location = created.getResponse().getHeader("Location");
        assertNotNull(location);
        URI uri = URI.create(location);
        assertEquals("http", uri.getScheme());
        assertEquals("localhost", uri.getHost());
        assertEquals(null, uri.getQuery());
        assertTrue(uri.getPath().startsWith("/lab/api/experiments/"));
        long id = Long.parseLong(
                uri.getPath().substring(uri.getPath().lastIndexOf('/') + 1));
        assertTrue(id > 0);

        mvc.perform(
                MockMvcRequestBuilders.get(uri.getPath()).contextPath("/lab"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(id))
                .andExpect(MockMvcResultMatchers.content()
                        .json(created.getResponse().getContentAsString()));
    }

    @Test
    void createsSeparateExperimentsWithTheSameName() throws Exception {
        String first = create("Same name");
        String second = create("Same name");
        assertNotEquals(first, second);
        for (String location : new String[]{first, second}) {
            mvc.perform(MockMvcRequestBuilders.get(URI.create(location)))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.name")
                            .value("Same name"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"name\":null}", "{\"name\":\"\"}",
            "{\"name\":\" \\t \\n\"}"})
    void rejectsMissingNullAndBlankNames(String body) throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/api/experiments")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.content()
                        .json("{\"message\":\"Invalid argument\"}"));
    }

    @Test
    void returnsNotFoundForUnknownId() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/experiments/0"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-number", "9223372036854775808"})
    void rejectsInvalidIds(String id) throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/experiments/{id}", id))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/api/experiments")
                .contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    private String create(String name) throws Exception {
        return mvc
                .perform(MockMvcRequestBuilders.post("/api/experiments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn().getResponse().getHeader("Location");
    }
}
