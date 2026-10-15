package dev.labyrinth.experiment;

import java.net.URI;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/experiments")
public class ExperimentController {

    private final ExperimentService service;

    public ExperimentController(ExperimentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ExperimentResponse> createExperiment(
            @Valid @RequestBody CreateExperimentRequest request) {
        Experiment experiment = service.createExperiment(request.name());

        ExperimentResponse response = ExperimentResponse.from(experiment);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}").buildAndExpand(experiment.getId()).toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExperimentResponse> getExperiment(
            @PathVariable long id) {
        Experiment experiment = service.getExperiment(id);
        if (experiment == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(ExperimentResponse.from(experiment));
    }

    public record CreateExperimentRequest(@NotBlank String name) {
    }

    public record ExperimentResponse(long id, String name,
            ExperimentStatus status) {

        public static ExperimentResponse from(Experiment experiment) {
            return new ExperimentResponse(experiment.getId(),
                    experiment.getName(), experiment.getStatus());
        }
    }
}
