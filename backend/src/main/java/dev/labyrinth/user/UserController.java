package dev.labyrinth.user;

import java.net.URI;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        User user = service.createUser(request.name(), request.teamId());

        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}").buildAndExpand(user.getId()).toUri();

        return ResponseEntity.created(location).body(UserResponse.from(user));
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable long id) {
        return UserResponse.from(service.getUser(id));
    }

    public record CreateUserRequest(@NotBlank String name,
            @NotNull @Positive Long teamId) {
    }

    public record UserResponse(long id, String name, long teamId) {

        public static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getName(),
                    user.getTeam().getId());
        }
    }
}
