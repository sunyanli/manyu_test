package com.manyu.algodemo.demo.model.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 冒泡排序接口入参。
 */
public class BubbleSortRequest {

    /** 待排序数值数组，元素为有限 decimal，数量 1..10000。 */
    @NotEmpty(message = "data 不能为空")
    private List<Double> data;

    /** 排序方向，可选，默认 ASC。 */
    private String order;

    /** 是否启用优化版（提前终止），可选，默认 true。 */
    private Boolean optimized;

    public List<Double> getData() {
        return data;
    }

    public void setData(List<Double> data) {
        this.data = data;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public Boolean getOptimized() {
        return optimized;
    }

    public void setOptimized(Boolean optimized) {
        this.optimized = optimized;
    }
}
