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

    public Ticket createTicket(String title, String content, long teamId) {
        Team team = teamRepository.findById(teamId).orElse(null);

        if (team == null) {
            return null;
        }

        return repository.save(new Ticket(title, content, team));
    }

    public Ticket getTicket(long id) {
        return repository.findById(id).orElse(null);
    }
}
