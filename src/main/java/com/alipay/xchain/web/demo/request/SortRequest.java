package com.alipay.xchain.web.demo.request;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 冒泡排序请求体
 *
 * @author AiWork
 * @since 2026-10-30
 */
public class SortRequest {

    /** 待排序数字数组，非空且全为数字 */
    @NotEmpty(message = "排序输入不能为空")
    private List<Integer> numbers;

    /** 排序方向，默认 asc，可选 asc/desc */
    private String order;

    public List<Integer> getNumbers() {
        return numbers;
    }

    public void setNumbers(List<Integer> numbers) {
        this.numbers = numbers;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    @Override
    public String toString() {
        return "SortRequest{numbers=" + numbers + ", order='" + order + "'}";
    }
}