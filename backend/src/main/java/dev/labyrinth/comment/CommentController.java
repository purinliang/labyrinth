package dev.labyrinth.comment;

import java.net.URI;

import jakarta.validation.Valid;

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
        if (comment == null) {
            return ResponseEntity.notFound().build();
        }

        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}").buildAndExpand(comment.getId()).toUri();
        return ResponseEntity.created(location)
                .body(CommentResponse.from(comment));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommentResponse> getComment(@PathVariable long id) {
        Comment comment = service.getComment(id);
        if (comment == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(CommentResponse.from(comment));
    }
}
