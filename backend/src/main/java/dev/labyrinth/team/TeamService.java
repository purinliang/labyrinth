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

    public Team getTeam(long id) {
        return repository.findById(id).orElse(null);
    }
}
