package dev.labyrinth;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.ResponseStatusException;

import dev.labyrinth.ticket.TicketController;
import dev.labyrinth.ticket.TicketService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void returnsFallbackMessageWhenReasonIsMissing(String reason)
            throws Exception {
        MockMvc mvc = mvcFor(
                new ResponseStatusException(HttpStatus.CONFLICT, reason));
        mvc.perform(MockMvcRequestBuilders.post("/api/tickets/1/close")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":0}"))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.content()
                        .json("{\"message\":\"Request failed\"}"));
    }

    @Test
    void preservesExceptionResponseHeaders() throws Exception {
        MockMvc mvc = mvcFor(
                new MethodNotAllowedException("POST", Set.of(HttpMethod.GET)));
        mvc.perform(MockMvcRequestBuilders.post("/api/tickets/1/close")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":0}"))
                .andExpect(MockMvcResultMatchers.status().isMethodNotAllowed())
                .andExpect(MockMvcResultMatchers.header()
                        .string(HttpHeaders.ALLOW, "GET"));
    }

    private MockMvc mvcFor(ResponseStatusException exception) {
        TicketService service = mock(TicketService.class);
        when(service.closeTicket(1L, 0)).thenThrow(exception);
        return MockMvcBuilders.standaloneSetup(new TicketController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
}
