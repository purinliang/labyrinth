package dev.labyrinth.ticket;

import org.springframework.stereotype.Service;

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
}
