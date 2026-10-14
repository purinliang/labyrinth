package dev.labyrinth.ticket;

import java.time.Instant;

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
