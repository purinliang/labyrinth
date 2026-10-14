package dev.labyrinth.ticket;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import dev.labyrinth.team.Team;
import dev.labyrinth.team.TeamRepository;

@Service
public class TicketService {

    private final TicketRepository repository;
    private final TeamRepository teamRepository;

    public TicketService(TicketRepository repository,
            TeamRepository teamRepository) {
        this.repository = repository;
        this.teamRepository = teamRepository;
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

        if (ticket.getStatus() == TicketStatus.CLOSED) {
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

        if (ticket.getStatus() == TicketStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ticket already open");
        }

        ticket.reopen();
        return ticket;
    }
}
