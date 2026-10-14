package dev.labyrinth.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateTicketRequest(@NotBlank String title,
        @NotBlank String content, @NotNull @Positive Long currentTeamId) {
}
