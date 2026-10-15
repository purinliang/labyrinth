package dev.labyrinth.comment;

import java.net.URI;
import java.time.Instant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService service;

    public CommentController(CommentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CommentResponse> createComment(
            @Valid @RequestBody CreateCommentRequest request) {
        Comment comment = service.createComment(request.content(),
                request.authorId(), request.ticketId());

        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}").buildAndExpand(comment.getId()).toUri();

        return ResponseEntity.created(location)
                .body(CommentResponse.from(comment));
    }

    @GetMapping("/{id}")
    public CommentResponse getComment(@PathVariable long id) {
        return CommentResponse.from(service.getComment(id));
    }

    public record CreateCommentRequest(@NotBlank String content,
            @NotNull @Positive Long authorId,
            @NotNull @Positive Long ticketId) {
    }

    public record CommentResponse(long id, String content, long authorId,
            long ticketId, Instant createdAt) {

        public static CommentResponse from(Comment comment) {
            return new CommentResponse(comment.getId(), comment.getContent(),
                    comment.getAuthor().getId(), comment.getTicket().getId(),
                    comment.getCreatedAt());
        }
    }
}
