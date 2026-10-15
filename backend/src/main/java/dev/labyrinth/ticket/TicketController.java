package dev.labyrinth.ticket;

import java.net.URI;
import java.time.Instant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService service;

    public TicketController(TicketService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody CreateTicketRequest request) {
        Ticket ticket = service.createTicket(request.title(),
                request.content(), request.currentTeamId());
        if (ticket == null) {
            return ResponseEntity.notFound().build();
        }

        TicketResponse response = TicketResponse.from(ticket);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}").buildAndExpand(ticket.getId()).toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicket(@PathVariable long id) {
        Ticket ticket = service.getTicket(id);
        if (ticket == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(TicketResponse.from(ticket));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<TicketResponse> closeTicket(@PathVariable long id,
            @Valid @RequestBody ChangeTicketStatusRequest request) {
        Ticket ticket = service.closeTicket(id, request.expectedVersion());

        return ResponseEntity.ok(TicketResponse.from(ticket));
    }

    @PostMapping("/{id}/reopen")
    public ResponseEntity<TicketResponse> reopenTicket(@PathVariable long id,
            @Valid @RequestBody ChangeTicketStatusRequest request) {
        Ticket ticket = service.reopenTicket(id, request.expectedVersion());

        return ResponseEntity.ok(TicketResponse.from(ticket));
    }

    @PostMapping("/{id}/transfer")
    public ResponseEntity<TicketResponse> transferTicket(@PathVariable long id,
            @Valid @RequestBody TransferTicketRequest request) {
        Ticket ticket = service.transferTicket(id, request.targetTeamId(),
                request.authorId(), request.comment(),
                request.expectedVersion());

        return ResponseEntity.ok(TicketResponse.from(ticket));
    }

    public record CreateTicketRequest(@NotBlank String title,
            @NotBlank String content, @NotNull @Positive Long currentTeamId) {
    }

    public record ChangeTicketStatusRequest(
            @NotNull @PositiveOrZero Integer expectedVersion) {
    }

    public record TransferTicketRequest(@NotNull @Positive Long targetTeamId,
            @NotNull @Positive Long authorId, @NotBlank String comment,
            @NotNull @PositiveOrZero Integer expectedVersion) {
    }

    public record TicketResponse(long id, String title, String content,
            TicketStatus status, long currentTeamId, int version,
            Instant createdAt) {

        public static TicketResponse from(Ticket ticket) {
            return new TicketResponse(ticket.getId(), ticket.getTitle(),
                    ticket.getContent(), ticket.getStatus(),
                    ticket.getCurrentTeam().getId(), ticket.getVersion(),
                    ticket.getCreatedAt());
        }
    }
}
