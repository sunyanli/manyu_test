package com.example.algorithm.model.dto;

import java.util.List;

/**
 * Response DTO for bubble sort execution.
 */
public class BubbleSortResponse {

    private List<Integer> sortedArray;
    private String order;

    public BubbleSortResponse() {
    }

    public BubbleSortResponse(List<Integer> sortedArray, String order) {
        this.sortedArray = sortedArray;
        this.order = order;
    }

    public List<Integer> getSortedArray() {
        return sortedArray;
    }

    public void setSortedArray(List<Integer> sortedArray) {
        this.sortedArray = sortedArray;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }
}
