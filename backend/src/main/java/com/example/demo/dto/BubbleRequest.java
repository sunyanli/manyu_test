package com.example.demo.dto;

import java.util.List;

public class BubbleRequest {
    private List<Integer> array;

    public BubbleRequest() {}

    public BubbleRequest(List<Integer> array) {
        this.array = array;
    }

    public List<Integer> getArray() { return array; }
    public void setArray(List<Integer> array) { this.array = array; }
}