package dev.labyrinth.ticket;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ChangeTicketStatusRequest(
        @NotNull @PositiveOrZero Integer expectedVersion) {
}
