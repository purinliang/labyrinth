package dev.labyrinth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    private final HelloService service;

    public HelloController(HelloService service) {
        this.service = service;
    }

    @GetMapping("/api/hello")
    public HelloResponse getMethod() {
        return new HelloResponse(service.getMessage());
    }
}
