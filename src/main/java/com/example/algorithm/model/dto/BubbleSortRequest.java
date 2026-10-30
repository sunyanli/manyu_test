package com.example.algorithm.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.List;

/**
 * Request DTO for bubble sort execution.
 */
public class BubbleSortRequest {

    @NotEmpty(message = "ALG_004")
    private List<Integer> array;

    @Pattern(regexp = "^(ASC|DESC)$", message = "ALG_005")
    private String order = "ASC";

    public List<Integer> getArray() {
        return array;
    }

    public void setArray(List<Integer> array) {
        this.array = array;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }
}
