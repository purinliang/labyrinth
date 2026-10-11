package dev.labyrinth.experiment;

import jakarta.validation.constraints.NotBlank;

public record CreateExperimentRequest(@NotBlank String name) {
}
