package dev.labyrinth;

import jakarta.validation.constraints.NotBlank;

public record HelloRequest(@NotBlank String name) {
}
