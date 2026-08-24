package com.example.demo.service;

import com.example.demo.model.HelloResult;
import org.springframework.stereotype.Service;

@Service
public class HelloService {
    public HelloResult greet(String name) {
        String safeName = (name != null && !name.isBlank()) ? name : "World";
        String greeting = "Hello, " + safeName + "! Welcome to DTCoder Demo.";
        return new HelloResult(greeting);
    }
}