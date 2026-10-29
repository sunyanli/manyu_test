package com.example.algorithm.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for export task creation.
 */
public class ExportRequest {

    @NotBlank(message = "EXPORT_001")
    private String algorithmType;
    private Object params;

    public String getAlgorithmType() {
        return algorithmType;
    }

    public void setAlgorithmType(String algorithmType) {
        this.algorithmType = algorithmType;
    }

    public Object getParams() {
        return params;
    }

    public void setParams(Object params) {
        this.params = params;
    }
}
