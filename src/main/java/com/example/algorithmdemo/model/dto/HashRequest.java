package com.example.algorithmdemo.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 哈希算法请求
 */
public class HashRequest {

    @NotBlank(message = "输入字符串不能为空")
    @Size(max = 10240, message = "输入字符串长度不能超过10KB")
    private String input;

    private String algorithm;

    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
}