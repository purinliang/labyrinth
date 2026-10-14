package dev.labyrinth.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateCommentRequest(@NotBlank String content,
        @NotNull @Positive Long authorId, @NotNull @Positive Long ticketId) {
}
