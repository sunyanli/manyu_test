package com.example.algorithmdemo.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * 冒泡排序请求
 */
public class BubbleSortRequest {

    @NotEmpty(message = "数组不能为空")
    @Size(max = 1000, message = "数组长度不能超过1000")
    private int[] array;

    private String order;

    public int[] getArray() { return array; }
    public void setArray(int[] array) { this.array = array; }

    public String getOrder() { return order; }
    public void setOrder(String order) { this.order = order; }
}