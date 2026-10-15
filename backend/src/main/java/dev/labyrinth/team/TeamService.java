package dev.labyrinth.team;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
     * Returns the team; rejects a missing team with HTTP 404.
     */
    public Team getTeam(long id) {
        return repository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Team not found"));
    }
}
