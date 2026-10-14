package dev.labyrinth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class HealthApiTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void reportsHealthyWithPostgresWithoutExposingDetails() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/actuator/health")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.status").value("UP"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.components")
                        .doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.details")
                        .doesNotExist());
    }

    @Test
    void leavesEnvironmentEndpointUnexposed() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/actuator/env"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }
}
