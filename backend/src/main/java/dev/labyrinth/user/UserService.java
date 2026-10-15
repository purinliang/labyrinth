package dev.labyrinth.user;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import dev.labyrinth.team.Team;
import dev.labyrinth.team.TeamRepository;

@Service
public class UserService {

    private final UserRepository repository;
    private final TeamRepository teamRepository;

    public UserService(UserRepository repository,
            TeamRepository teamRepository) {
        this.repository = repository;
        this.teamRepository = teamRepository;
    }

    /**
     * Creates a user; rejects a missing team with HTTP 404.
     */
    public User createUser(String name, long teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Team not found"));

        return repository.save(new User(name, team));
    }

    /**
     * Returns the user; rejects a missing user with HTTP 404.
     */
    public User getUser(long id) {
        return repository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found"));
    }
}
