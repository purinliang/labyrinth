package dev.labyrinth.comment;

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
import dev.labyrinth.ticket.Ticket;
import dev.labyrinth.ticket.TicketRepository;
import dev.labyrinth.user.User;
import dev.labyrinth.user.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class CommentApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TeamRepository teams;

    @Autowired
    private UserRepository users;

    @Autowired
    private TicketRepository tickets;

    @Test
    void createsPersistsAndRetrievesCommentThroughLocation() throws Exception {
        Team team = teams.save(new Team("Comment API team"));
        long authorId = users.save(new User("Researcher", team)).getId();
        long ticketId =
                tickets.save(new Ticket("Demo", "Content", team)).getId();
        var created = mvc.perform(MockMvcRequestBuilders
                .post("/lab/api/comments").contextPath("/lab")
                .queryParam("ignored", "value")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Comment content\",\"authorId\":"
                        + authorId + ",\"ticketId\":" + ticketId + "}"))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content")
                        .value("Comment content"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.authorId")
                        .value(authorId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.ticketId")
                        .value(ticketId))
                .andExpect(MockMvcResultMatchers.jsonPath("$.createdAt")
                        .isString())
                .andReturn();

        String location = created.getResponse().getHeader("Location");
        assertNotNull(location);
        URI uri = URI.create(location);
        assertEquals("http", uri.getScheme());
        assertEquals("localhost", uri.getHost());
        assertNull(uri.getQuery());
        assertTrue(uri.getPath().startsWith("/lab/api/comments/"));
        long id = Long.parseLong(
                uri.getPath().substring(uri.getPath().lastIndexOf('/') + 1));
        assertTrue(id > 0);
        assertEquals("Comment content",
                jdbc.queryForObject(
                        "SELECT content FROM comments WHERE id = ?",
                        String.class, id));
        assertEquals(authorId,
                jdbc.queryForObject(
                        "SELECT author_id FROM comments WHERE id = ?",
                        Long.class, id));
        assertEquals(ticketId,
                jdbc.queryForObject(
                        "SELECT ticket_id FROM comments WHERE id = ?",
                        Long.class, id));

        mvc.perform(
                MockMvcRequestBuilders.get(uri.getPath()).contextPath("/lab"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(id))
                .andExpect(MockMvcResultMatchers.content()
                        .json(created.getResponse().getContentAsString()));
    }

    @Test
    void returnsNotFoundForUnknownComment() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/comments/0"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void rejectsUnknownAuthorWithoutCreatingComment() throws Exception {
        Team team = teams.save(new Team("Missing author API team"));
        long ticketId =
                tickets.save(new Ticket("Demo", "Content", team)).getId();
        Long before = commentCount();
        mvc.perform(MockMvcRequestBuilders.post("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Content\",\"authorId\":"
                        + Long.MAX_VALUE + ",\"ticketId\":" + ticketId + "}"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
        assertEquals(before, commentCount());
    }

    @Test
    void rejectsUnknownTicketWithoutCreatingComment() throws Exception {
        Team team = teams.save(new Team("Missing ticket API team"));
        long authorId = users.save(new User("Researcher", team)).getId();
        Long before = commentCount();
        mvc.perform(MockMvcRequestBuilders.post("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Content\",\"authorId\":" + authorId
                        + ",\"ticketId\":" + Long.MAX_VALUE + "}"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
        assertEquals(before, commentCount());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"authorId\":1,\"ticketId\":1}",
            "{\"content\":null,\"authorId\":1,\"ticketId\":1}",
            "{\"content\":\" \",\"authorId\":1,\"ticketId\":1}",
            "{\"content\":\"Content\",\"ticketId\":1}",
            "{\"content\":\"Content\",\"authorId\":null,\"ticketId\":1}",
            "{\"content\":\"Content\",\"authorId\":0,\"ticketId\":1}",
            "{\"content\":\"Content\",\"authorId\":-1,\"ticketId\":1}",
            "{\"content\":\"Content\",\"authorId\":1}",
            "{\"content\":\"Content\",\"authorId\":1,\"ticketId\":null}",
            "{\"content\":\"Content\",\"authorId\":1,\"ticketId\":0}",
            "{\"content\":\"Content\",\"authorId\":1,\"ticketId\":-1}"})
    void rejectsInvalidRequestsWithoutCreatingComment(String body)
            throws Exception {
        Long before = commentCount();
        mvc.perform(MockMvcRequestBuilders.post("/api/comments")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.content()
                        .json("{\"message\":\"Invalid argument\"}"));
        assertEquals(before, commentCount());
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-number", "9223372036854775808"})
    void rejectsInvalidIds(String id) throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/comments/{id}", id))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    private Long commentCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM comments",
                Long.class);
    }
}
