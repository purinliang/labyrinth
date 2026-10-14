package dev.labyrinth.user;

import org.springframework.stereotype.Service;

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
     * Returns the saved user, or null if the team does not exist.
     */
    public User createUser(String name, long teamId) {
        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null) {
            return null;
        }

        return repository.save(new User(name, team));
    }

    /**
     * Returns the user, or null when it does not exist.
     */
    public User getUser(long id) {
        return repository.findById(id).orElse(null);
    }
}
