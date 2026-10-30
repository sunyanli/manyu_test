package com.alipay.xchain.web.demo.response;

/**
 * 演示接口统一返回 VO
 *
 * @author AiWork
 * @since 2026-10-30
 */
public class DemoResultVO {

    /** 接口类型：hello/hash/sort */
    private String type;

    /** 欢迎消息（hello 专用） */
    private String message;

    /** 时间戳 */
    private String timestamp;

    /** 哈希算法名（hash 专用） */
    private String algorithm;

    /** 输入原文（hash 专用） */
    private String input;

    /** 哈希输出（hash 专用） */
    private String output;

    /** 排序方向（sort 专用） */
    private String order;

    /** 原始数组（sort 专用） */
    private int[] original;

    /** 排序后数组（sort 专用） */
    private int[] sorted;

    /** 交换次数（sort 专用） */
    private Integer swapCount;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public int[] getOriginal() {
        return original;
    }

    public void setOriginal(int[] original) {
        this.original = original;
    }

    public int[] getSorted() {
        return sorted;
    }

    public void setSorted(int[] sorted) {
        this.sorted = sorted;
    }

    public Integer getSwapCount() {
        return swapCount;
    }

    public void setSwapCount(Integer swapCount) {
        this.swapCount = swapCount;
    }

    @Override
    public String toString() {
        return "DemoResultVO{type='" + type + "', timestamp='" + timestamp + "'}";
    }
}