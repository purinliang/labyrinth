package dev.labyrinth.team;

public record TeamResponse(long id, String name) {

    public static TeamResponse from(Team team) {
        return new TeamResponse(team.getId(), team.getName());
    }
}
