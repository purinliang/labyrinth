package dev.labyrinth.team;

import org.springframework.stereotype.Service;

@Service
public class TeamService {

    private final TeamRepository repository;

    public TeamService(TeamRepository repository) {
        this.repository = repository;
    }

    public Team createTeam(String name) {
        return repository.save(new Team(name));
    }

    /**
     * Returns the team, or null when it does not exist.
     */
    public Team getTeam(long id) {
        return repository.findById(id).orElse(null);
    }
}
