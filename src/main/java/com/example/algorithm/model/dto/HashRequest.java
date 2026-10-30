package com.example.algorithm.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for hash algorithm execution.
 */
public class HashRequest {

    @NotBlank(message = "ALG_003")
    @Size(max = 10 * 1024 * 1024, message = "ALG_007")
    private String content;

    private String algorithm;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }
}
