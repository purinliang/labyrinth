package dev.labyrinth.comment;

import java.time.Instant;

public record CommentResponse(long id, String content, long authorId,
        long ticketId, Instant createdAt) {

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(comment.getId(), comment.getContent(),
                comment.getAuthor().getId(), comment.getTicket().getId(),
                comment.getCreatedAt());
    }
}
