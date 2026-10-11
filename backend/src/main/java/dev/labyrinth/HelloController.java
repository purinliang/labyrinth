package dev.labyrinth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    public record HelloResponse(String message) {
    }

    @GetMapping("/api/hello")
    public HelloResponse getMethod() {
        return new HelloResponse("Hello, Labyrinth!");
    }
}
