package dev.labyrinth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@AutoConfigureMockMvc
class HelloApiTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void greetsLabyrinthByDefault() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/hello"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message")
                        .value("Hello, Labyrinth!"));
    }

    @Test
    void greetsNameFromQuery() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/hello").param("name",
                "Alice")).andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message")
                        .value("Hello, Alice!"));
    }

    @Test
    void greetsNameFromPath() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/hello/Alice"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message")
                        .value("Hello, Alice!"));
    }

    @Test
    void greetsNameFromJson() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/api/hello")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Alice\"}"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message")
                        .value("Hello, Alice!"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"name\":null}", "{\"name\":\"\"}",
            "{\"name\":\" \\t \\n\"}"})
    void rejectsMissingNullAndBlankNames(String body) throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/api/hello")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.content()
                        .json("{\"message\":\"Invalid argument\"}"));
    }
}
