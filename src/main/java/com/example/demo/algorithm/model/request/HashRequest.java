package com.example.demo.algorithm.model.request;

import javax.validation.constraints.NotBlank;

/**
 * 哈希算法请求
 *
 * @author AiWork
 */
public class HashRequest {

    /** 待哈希的原始字符串 */
    @NotBlank(message = "输入不能为空")
    private String input;

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    @Override
    public String toString() {
        return "HashRequest{input='" + input + "'}";
    }
}