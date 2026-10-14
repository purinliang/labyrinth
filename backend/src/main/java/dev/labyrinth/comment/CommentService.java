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

    public Comment getComment(long id) {
        return repository.findById(id).orElse(null);
    }
}
