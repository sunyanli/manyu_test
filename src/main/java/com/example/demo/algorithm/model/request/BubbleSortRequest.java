package com.example.demo.algorithm.model.request;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 冒泡排序请求
 *
 * @author AiWork
 */
public class BubbleSortRequest {

    /** 待排序的整数数组 */
    @NotEmpty(message = "数组不能为空")
    private List<Integer> array;

    /** 排序方向：asc（默认升序）/ desc（降序） */
    private String order;

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

    @Override
    public String toString() {
        return "BubbleSortRequest{array=" + array + ", order='" + order + "'}";
    }
}