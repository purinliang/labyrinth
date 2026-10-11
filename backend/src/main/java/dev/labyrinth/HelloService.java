package dev.labyrinth;

import org.springframework.stereotype.Service;

@Service
public class HelloService {
    public String getMessage() {
        return "Hello, Labyrinth!";
    }
}
