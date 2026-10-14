package dev.labyrinth.comment;

import org.springframework.stereotype.Service;

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
     * Returns the saved comment, or null if the author or ticket does not
     * exist.
     */
    public Comment createComment(String content, long authorId,
            long ticketId) {
        User author = userRepository.findById(authorId).orElse(null);
        if (author == null) {
            return null;
        }

        Ticket ticket = ticketRepository.findById(ticketId).orElse(null);
        if (ticket == null) {
            return null;
        }

        return repository.save(new Comment(content, author, ticket));
    }

    /**
     * Returns the comment, or null when it does not exist.
     */
    public Comment getComment(long id) {
        return repository.findById(id).orElse(null);
    }
}
