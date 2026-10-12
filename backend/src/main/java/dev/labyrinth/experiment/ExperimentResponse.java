package dev.labyrinth.experiment;

public record ExperimentResponse(long id, String name,
        ExperimentStatus status) {

    public static ExperimentResponse from(Experiment experiment) {
        return new ExperimentResponse(experiment.getId(), experiment.getName(),
                experiment.getStatus());
    }
}
