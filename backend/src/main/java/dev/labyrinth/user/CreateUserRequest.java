package dev.labyrinth.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateUserRequest(@NotBlank String name,
        @NotNull @Positive Long teamId) {
}
