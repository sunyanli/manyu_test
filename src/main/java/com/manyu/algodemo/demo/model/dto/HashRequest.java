package com.manyu.algodemo.demo.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 哈希算法接口入参。
 */
public class HashRequest {

    /** 待哈希文本，非空。 */
    @NotBlank(message = "text 不能为空")
    private String text;

    /** 哈希算法，可选，默认 SHA256。 */
    private String algorithm;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }
}
