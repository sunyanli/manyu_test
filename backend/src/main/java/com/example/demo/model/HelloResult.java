package com.example.demo.model;

public class HelloResult {
    private String greeting;

    public HelloResult() {}

    public HelloResult(String greeting) {
        this.greeting = greeting;
    }

    public String getGreeting() { return greeting; }
    public void setGreeting(String greeting) { this.greeting = greeting; }
}