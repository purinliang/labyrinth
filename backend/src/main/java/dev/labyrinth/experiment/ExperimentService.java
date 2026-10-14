package dev.labyrinth.experiment;

import org.springframework.stereotype.Service;

@Service
public class ExperimentService {

    private final ExperimentRepository repository;

    public ExperimentService(ExperimentRepository repository) {
        this.repository = repository;
    }

    public Experiment createExperiment(String name) {
        return repository.save(new Experiment(name));
    }

    /**
     * Returns the experiment, or null when it does not exist.
     */
    public Experiment getExperiment(long id) {
        return repository.findById(id).orElse(null);
    }
}
