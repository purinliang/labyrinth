package dev.labyrinth.ticket;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import dev.labyrinth.comment.Comment;
import dev.labyrinth.comment.CommentRepository;
import dev.labyrinth.team.Team;
import dev.labyrinth.team.TeamRepository;
import dev.labyrinth.user.User;
import dev.labyrinth.user.UserRepository;

@Service
public class TicketService {

    private final TicketRepository repository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    public TicketService(TicketRepository repository,
            TeamRepository teamRepository, UserRepository userRepository,
            CommentRepository commentRepository) {
        this.repository = repository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
    }

    /**
     * Returns the saved ticket, or null if the team does not exist.
     */
    public Ticket createTicket(String title, String content, long teamId) {
        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null) {
            return null;
        }

        return repository.save(new Ticket(title, content, team));
    }

    /**
     * Returns the ticket, or null when it does not exist.
     */
    public Ticket getTicket(long id) {
        return repository.findById(id).orElse(null);
    }

    /**
     * Closes the ticket, rejecting missing tickets, stale versions, and
     * already closed tickets with HTTP 404 or 409.
     */
    @Transactional
    public Ticket closeTicket(long id, int expectedVersion) {
        Ticket ticket = repository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Ticket not found"));

        if (ticket.getVersion() != expectedVersion) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Version conflict");
        }

        if (ticket.getStatus() == Ticket.Status.CLOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ticket already closed");
        }

        ticket.close();
        return ticket;
    }

    /**
     * Reopens the ticket, rejecting missing tickets, stale versions, and
     * already open tickets with HTTP 404 or 409.
     */
    @Transactional
    public Ticket reopenTicket(long id, int expectedVersion) {
        Ticket ticket = repository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Ticket not found"));

        if (ticket.getVersion() != expectedVersion) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Version conflict");
        }

        if (ticket.getStatus() == Ticket.Status.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ticket already open");
        }

        ticket.reopen();
        return ticket;
    }

    /**
     * Transfers an open ticket and records its comment in one transaction.
     * Rejects missing records with 404, an author outside the current team
     * with 403, and stale versions or invalid transfers with 409.
     */
    @Transactional
    public Ticket transferTicket(long id, long targetTeamId, long authorId,
            String commentContent, int expectedVersion) {
        // Load a managed ticket so Hibernate tracks the team and version.
        Ticket ticket = repository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Ticket not found"));

        // Reject decisions based on an outdated team or ticket status.
        if (ticket.getVersion() != expectedVersion) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Version conflict");
        }

        // A closed ticket must be reopened before its ownership can change.
        if (ticket.getStatus() == Ticket.Status.CLOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ticket already closed");
        }

        // The transfer comment must reference an existing author.
        User author = userRepository.findById(authorId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Author not found"));

        // Only the current team may hand the ticket to another team.
        if (!author.belongsTo(ticket.getCurrentTeam())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Author does not belong to the current team");
        }

        // Resolve the destination before writing either the comment or ticket.
        Team targetTeam = teamRepository.findById(targetTeamId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Target team not found"));

        // A transfer to the same team would add a misleading comment/version.
        if (targetTeam.getId().equals(ticket.getCurrentTeam().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ticket already belongs to the target team");
        }

        // Join this transaction so a failed ticket update removes the comment.
        commentRepository.save(new Comment(commentContent, author, ticket));

        // Dirty checking updates the team and version at commit. A version
        // conflict rolls back both writes, including the transfer comment.
        ticket.transferTo(targetTeam);
        return ticket;
    }
}
