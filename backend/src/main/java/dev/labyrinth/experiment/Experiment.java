package dev.labyrinth.experiment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "experiments")
public class Experiment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "text", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text", nullable = false)
    private ExperimentStatus status;

    protected Experiment() {
    }

    public Experiment(String name) {
        this.name = name;
        this.status = ExperimentStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ExperimentStatus getStatus() {
        return status;
    }
}
