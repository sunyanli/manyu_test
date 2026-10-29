package com.example.algorithm.model.dto;

/**
 * Response DTO for hash algorithm execution.
 */
public class HashResponse {

    private String hashValue;
    private String algorithm;

    public HashResponse() {
    }

    public HashResponse(String hashValue, String algorithm) {
        this.hashValue = hashValue;
        this.algorithm = algorithm;
    }

    public String getHashValue() {
        return hashValue;
    }

    public void setHashValue(String hashValue) {
        this.hashValue = hashValue;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }
}
