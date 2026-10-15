package dev.labyrinth.ticket;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import dev.labyrinth.user.User;
import dev.labyrinth.user.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class TicketTransferApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TicketService service;

    @Autowired
    private TeamRepository teams;

    @Autowired
    private UserRepository users;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void transfersTicketAndCommitsItsComment() throws Exception {
        Fixture fixture = fixture();
        long id = fixture.ticket().getId();
        String response = mvc
                .perform(MockMvcRequestBuilders
                        .post("/api/tickets/{id}/transfer", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(fixture.target().getId().toString(),
                                fixture.author().getId().toString(),
                                "\"Please investigate\"", "0")))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(id))
                .andExpect(MockMvcResultMatchers.jsonPath("$.currentTeamId")
                        .value(fixture.target().getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status")
                        .value("OPEN"))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.version").value(1))
                .andReturn().getResponse().getContentAsString();
        mvc.perform(MockMvcRequestBuilders.get("/api/tickets/{id}", id))
                .andExpect(MockMvcResultMatchers.content().json(response));

        Ticket reloaded = service.getTicket(id);
        assertEquals(fixture.target().getId(),
                reloaded.getCurrentTeam().getId());
        assertEquals(fixture.ticket().getTitle(), reloaded.getTitle());
        assertEquals(fixture.ticket().getContent(), reloaded.getContent());
        assertEquals(fixture.ticket().getCreatedAt(), reloaded.getCreatedAt());
        assertEquals(1L, commentCount(id));
        assertEquals("Please investigate",
                jdbc.queryForObject(
                        "SELECT content FROM comments WHERE ticket_id = ?",
                        String.class, id));
        assertEquals(fixture.author().getId(),
                jdbc.queryForObject(
                        "SELECT author_id FROM comments WHERE ticket_id = ?",
                        Long.class, id));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ticket", "author", "target", "outsider", "same",
            "closed", "stale"})
    void rejectsInvalidTransferWithoutSideEffects(String scenario)
            throws Exception {
        Fixture fixture = fixture();
        long id = fixture.ticket().getId();
        long targetId = fixture.target().getId();
        long authorId = fixture.author().getId();
        int version = 0;
        int expectedStatus;
        switch (scenario) {
            case "ticket" -> {
                id = Long.MAX_VALUE;
                expectedStatus = 404;
            }
            case "author" -> {
                authorId = Long.MAX_VALUE;
                expectedStatus = 404;
            }
            case "target" -> {
                targetId = Long.MAX_VALUE;
                expectedStatus = 404;
            }
            case "outsider" -> {
                authorId = users.save(new User("Outsider", fixture.target()))
                        .getId();
                expectedStatus = 403;
            }
            case "same" -> {
                targetId = fixture.source().getId();
                expectedStatus = 409;
            }
            case "closed" -> {
                service.closeTicket(id, 0);
                version = 1;
                expectedStatus = 409;
            }
            case "stale" -> {
                service.closeTicket(id, 0);
                service.reopenTicket(id, 1);
                expectedStatus = 409;
            }
            default -> throw new IllegalArgumentException(scenario);
        }
        String before = getTicketBody(fixture.ticket().getId());
        mvc.perform(MockMvcRequestBuilders
                .post("/api/tickets/{id}/transfer", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(Long.toString(targetId), Long.toString(authorId),
                        "\"Note\"", Integer.toString(version))))
                .andExpect(MockMvcResultMatchers.status().is(expectedStatus));
        assertEquals(before, getTicketBody(fixture.ticket().getId()));
        assertEquals(0L, commentCount(fixture.ticket().getId()));
    }

    @ParameterizedTest
    @MethodSource("invalidFields")
    void rejectsInvalidFieldsWithoutSideEffects(String target, String author,
            String comment, String version) throws Exception {
        Fixture fixture = fixture();
        String before = getTicketBody(fixture.ticket().getId());
        mvc.perform(MockMvcRequestBuilders
                .post("/api/tickets/{id}/transfer", fixture.ticket().getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(target, author, comment, version)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
        assertEquals(before, getTicketBody(fixture.ticket().getId()));
        assertEquals(0L, commentCount(fixture.ticket().getId()));
    }

    private static Stream<Arguments> invalidFields() {
        return Stream.of(Arguments.of("null", "1", "\"Note\"", "0"),
                Arguments.of("0", "1", "\"Note\"", "0"),
                Arguments.of("-1", "1", "\"Note\"", "0"),
                Arguments.of("1", "null", "\"Note\"", "0"),
                Arguments.of("1", "0", "\"Note\"", "0"),
                Arguments.of("1", "-1", "\"Note\"", "0"),
                Arguments.of("1", "1", "null", "0"),
                Arguments.of("1", "1", "\"\"", "0"),
                Arguments.of("1", "1", "\" \\t \\n\"", "0"),
                Arguments.of("1", "1", "\"Note\"", "null"),
                Arguments.of("1", "1", "\"Note\"", "-1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"targetTeamId\":"})
    void rejectsMissingFieldsAndMalformedJson(String body) throws Exception {
        Fixture fixture = fixture();
        String before = getTicketBody(fixture.ticket().getId());
        mvc.perform(MockMvcRequestBuilders
                .post("/api/tickets/{id}/transfer", fixture.ticket().getId())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
        assertEquals(before, getTicketBody(fixture.ticket().getId()));
        assertEquals(0L, commentCount(fixture.ticket().getId()));
    }

    private String body(String target, String author, String comment,
            String version) {
        return "{\"targetTeamId\":" + target + ",\"authorId\":" + author
                + ",\"comment\":" + comment + ",\"expectedVersion\":" + version
                + "}";
    }

    private Fixture fixture() {
        Team source = teams.save(new Team("Source team"));
        Team target = teams.save(new Team("Destination team"));
        User author = users.save(new User("Researcher", source));
        Ticket ticket =
                service.createTicket("Demo", "Content", source.getId());
        return new Fixture(ticket, source, target, author);
    }

    private String getTicketBody(long id) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.get("/api/tickets/{id}", id))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn()
                .getResponse().getContentAsString();
    }

    private Long commentCount(long ticketId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM comments WHERE ticket_id = ?",
                Long.class, ticketId);
    }

    private record Fixture(Ticket ticket, Team source, Team target,
            User author) {
    }
}
