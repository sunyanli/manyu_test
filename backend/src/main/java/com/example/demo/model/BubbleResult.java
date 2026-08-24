package com.example.demo.model;

import java.util.List;

public class BubbleResult {
    private List<Integer> originalArray;
    private List<Integer> sortedArray;
    private int swapCount;
    private int comparisonCount;

    public BubbleResult() {}

    public BubbleResult(List<Integer> originalArray, List<Integer> sortedArray, int swapCount, int comparisonCount) {
        this.originalArray = originalArray;
        this.sortedArray = sortedArray;
        this.swapCount = swapCount;
        this.comparisonCount = comparisonCount;
    }

    public List<Integer> getOriginalArray() { return originalArray; }
    public void setOriginalArray(List<Integer> originalArray) { this.originalArray = originalArray; }
    public List<Integer> getSortedArray() { return sortedArray; }
    public void setSortedArray(List<Integer> sortedArray) { this.sortedArray = sortedArray; }
    public int getSwapCount() { return swapCount; }
    public void setSwapCount(int swapCount) { this.swapCount = swapCount; }
    public int getComparisonCount() { return comparisonCount; }
    public void setComparisonCount(int comparisonCount) { this.comparisonCount = comparisonCount; }
}