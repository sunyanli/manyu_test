package com.example.algorithm.model.dto;

/**
 * Response DTO for HelloWorld execution.
 */
public class HelloWorldResponse {

    private String message;

    public HelloWorldResponse() {
    }

    public HelloWorldResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
