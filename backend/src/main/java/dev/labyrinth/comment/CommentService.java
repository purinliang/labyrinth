package dev.labyrinth.comment;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import dev.labyrinth.ticket.Ticket;
import dev.labyrinth.ticket.TicketRepository;
import dev.labyrinth.user.User;
import dev.labyrinth.user.UserRepository;

@Service
public class CommentService {

    private final CommentRepository repository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    public CommentService(CommentRepository repository,
            UserRepository userRepository, TicketRepository ticketRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
    }

    /**
     * Creates a comment; rejects a missing author or ticket with HTTP 404.
     */
    public Comment createComment(String content, long authorId,
            long ticketId) {
        User author = userRepository.findById(authorId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Author not found"));

        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Ticket not found"));

        return repository.save(new Comment(content, author, ticket));
    }

    /**
     * Returns the comment; rejects a missing comment with HTTP 404.
     */
    public Comment getComment(long id) {
        return repository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Comment not found"));
    }
}
