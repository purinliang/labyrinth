package dev.labyrinth.ticket;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;

import dev.labyrinth.team.Team;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "text", nullable = false)
    private String title;

    @Column(columnDefinition = "text", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text", nullable = false)
    private Status status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "current_team_id", nullable = false)
    private Team currentTeam;

    @Version
    @Column(nullable = false)
    private Integer version;

    @CreationTimestamp(source = SourceType.DB)
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "timestamptz")
    private Instant createdAt;

    protected Ticket() {
    }

    public Ticket(String title, String content, Team currentTeam) {
        this.title = title;
        this.content = content;
        this.status = Status.OPEN;
        this.currentTeam = currentTeam;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Status getStatus() {
        return status;
    }

    public Team getCurrentTeam() {
        return currentTeam;
    }

    public Integer getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void close() {
        this.status = Status.CLOSED;
    }

    public void reopen() {
        this.status = Status.OPEN;
    }

    public void transferTo(Team targetTeam) {
        this.currentTeam = targetTeam;
    }

    public enum Status {
        OPEN, CLOSED
    }
}
