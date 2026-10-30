package com.alipay.xchain.web.demo.request;

import javax.validation.constraints.NotBlank;

/**
 * 哈希算法请求体
 *
 * @author AiWork
 * @since 2026-10-30
 */
public class HashRequest {

    /** 待计算哈希的原文，非空 */
    @NotBlank(message = "输入参数不能为空")
    private String input;

    /** 算法，默认 SHA-256，可选 MD5/SHA-1/SHA-256 */
    private String algorithm;

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public String toString() {
        return "HashRequest{input='" + input + "', algorithm='" + algorithm + "'}";
    }
}