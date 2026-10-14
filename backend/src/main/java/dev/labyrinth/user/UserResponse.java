package dev.labyrinth.user;

public record UserResponse(long id, String name, long teamId) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(),
                user.getTeam().getId());
    }
}
