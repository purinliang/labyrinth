package dev.labyrinth.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record TransferTicketRequest(@NotNull @Positive Long targetTeamId,
        @NotNull @Positive Long authorId, @NotBlank String comment,
        @NotNull @PositiveOrZero Integer expectedVersion) {
}
