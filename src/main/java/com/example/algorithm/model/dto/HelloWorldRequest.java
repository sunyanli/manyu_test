package com.example.algorithm.model.dto;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for HelloWorld execution.
 */
public class HelloWorldRequest {

    @Size(max = 100, message = "ALG_001")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
