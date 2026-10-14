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

    public User createUser(String name, long teamId) {
        Team team = teamRepository.findById(teamId).orElse(null);

        if (team == null) {
            return null;
        }

        return repository.save(new User(name, team));
    }

    public User getUser(long id) {
        return repository.findById(id).orElse(null);
    }
}
