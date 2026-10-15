package dev.labyrinth.team;

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
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService service;

    public TeamController(TeamService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(
            @Valid @RequestBody CreateTeamRequest request) {
        Team team = service.createTeam(request.name());

        TeamResponse response = TeamResponse.from(team);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}").buildAndExpand(team.getId()).toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeamResponse> getTeam(@PathVariable long id) {
        Team team = service.getTeam(id);
        if (team == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(TeamResponse.from(team));
    }

    public record CreateTeamRequest(@NotBlank String name) {
    }

    public record TeamResponse(long id, String name) {

        public static TeamResponse from(Team team) {
            return new TeamResponse(team.getId(), team.getName());
        }
    }
}
