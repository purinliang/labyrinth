package dev.labyrinth.experiment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExperimentRepositoryTest {

    @Test
    void concurrentCreatesHaveUniqueIdsAndPreserveEveryExperiment()
            throws Exception {
        ExperimentRepository repository = new ExperimentRepository();
        int workers = 8;
        int perWorker = 100;
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<List<Experiment>>> futures = new ArrayList<>();
        var executor = Executors.newFixedThreadPool(workers);

        try {
            for (int worker = 0; worker < workers; worker++) {
                int workerId = worker;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Start timed out");
                    }
                    List<Experiment> created = new ArrayList<>();
                    for (int i = 0; i < perWorker; i++) {
                        created.add(repository.create(workerId + "-" + i));
                    }
                    return created;
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            Set<Long> ids = new HashSet<>();
            for (Future<List<Experiment>> future : futures) {
                for (Experiment experiment : future.get(10,
                        TimeUnit.SECONDS)) {
                    assertTrue(experiment.id() > 0);
                    assertTrue(ids.add(experiment.id()), "Duplicate ID");
                    assertEquals(ExperimentStatus.PENDING,
                            experiment.status());
                    assertEquals(experiment,
                            repository.findById(experiment.id()));
                }
            }
            assertEquals(workers * perWorker, ids.size());
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
}
