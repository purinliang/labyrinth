package dev.labyrinth.hello;

import org.springframework.stereotype.Service;

@Service
public class HelloService {

    public String getMessage(String name) {
        return String.format("Hello, %s!", name);
    }
}
