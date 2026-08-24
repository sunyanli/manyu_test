package com.example.algorithmdemo.model.vo;

/**
 * 冒泡排序返回视图对象
 */
public class BubbleSortVO {

    private int[] originalArray;
    private int[] sortedArray;
    private String order;
    private long sortTime;
    private String timestamp;

    public BubbleSortVO() {}

    public BubbleSortVO(int[] originalArray, int[] sortedArray, String order, long sortTime, String timestamp) {
        this.originalArray = originalArray;
        this.sortedArray = sortedArray;
        this.order = order;
        this.sortTime = sortTime;
        this.timestamp = timestamp;
    }

    public int[] getOriginalArray() { return originalArray; }
    public void setOriginalArray(int[] originalArray) { this.originalArray = originalArray; }

    public int[] getSortedArray() { return sortedArray; }
    public void setSortedArray(int[] sortedArray) { this.sortedArray = sortedArray; }

    public String getOrder() { return order; }
    public void setOrder(String order) { this.order = order; }

    public long getSortTime() { return sortTime; }
    public void setSortTime(long sortTime) { this.sortTime = sortTime; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}