package dev.labyrinth.hello;

import jakarta.validation.constraints.NotBlank;

public record HelloRequest(@NotBlank String name) {
}
