package com.example.demo.dto;

public class HashRequest {
    private String input;
    private String algorithm;

    public HashRequest() {}

    public HashRequest(String input, String algorithm) {
        this.input = input;
        this.algorithm = algorithm;
    }

    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }
    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
}