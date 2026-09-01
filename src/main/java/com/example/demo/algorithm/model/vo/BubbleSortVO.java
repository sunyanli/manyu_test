package com.example.demo.algorithm.model.vo;

import java.util.List;

/**
 * 冒泡排序响应
 *
 * @author AiWork
 */
public class BubbleSortVO {

    /** 原始数组 */
    private List<Integer> original;

    /** 排序后数组 */
    private List<Integer> sorted;

    /** 排序方向 */
    private String order;

    /** 执行耗时（毫秒） */
    private long durationMs;

    public BubbleSortVO() {
    }

    public BubbleSortVO(List<Integer> original, List<Integer> sorted, String order, long durationMs) {
        this.original = original;
        this.sorted = sorted;
        this.order = order;
        this.durationMs = durationMs;
    }

    public List<Integer> getOriginal() {
        return original;
    }

    public void setOriginal(List<Integer> original) {
        this.original = original;
    }

    public List<Integer> getSorted() {
        return sorted;
    }

    public void setSorted(List<Integer> sorted) {
        this.sorted = sorted;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    @Override
    public String toString() {
        return "BubbleSortVO{original=" + original + ", sorted=" + sorted
                + ", order='" + order + "', durationMs=" + durationMs + "}";
    }
}