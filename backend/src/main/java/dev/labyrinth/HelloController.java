package dev.labyrinth;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    private final HelloService service;

    public HelloController(HelloService service) {
        this.service = service;
    }

    @GetMapping("/api/hello")
    public HelloResponse greetByQuery(
            @RequestParam(defaultValue = "Labyrinth") String name) {
        return new HelloResponse(service.getMessage(name));
    }

    @GetMapping("/api/hello/{name}")
    public HelloResponse greetByPath(@PathVariable String name) {
        return new HelloResponse(service.getMessage(name));
    }

    @PostMapping("/api/hello")
    public HelloResponse greetByBody(
            @Valid @RequestBody HelloRequest request) {
        return new HelloResponse(service.getMessage(request.name()));
    }
}
