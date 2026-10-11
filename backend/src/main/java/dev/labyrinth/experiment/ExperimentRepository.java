package dev.labyrinth.experiment;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

@Repository
public class ExperimentRepository {

    private final Map<Long, Experiment> experiments =
            new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public Experiment create(String name) {
        long id = nextId.getAndIncrement();
        Experiment experiment =
                new Experiment(id, name, ExperimentStatus.PENDING);
        experiments.put(id, experiment);
        return experiment;
    }

    public Experiment findById(long id) {
        return experiments.get(id);
    }
}
