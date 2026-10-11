package dev.labyrinth;

import org.springframework.stereotype.Service;

@Service
public class HelloService {
    public String getMessage(String name) {
        return String.format("Hello, %s!", name);
    }
}
