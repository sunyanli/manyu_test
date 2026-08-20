package com.manyu.algodemo.demo.model.dto;

/**
 * 哈希算法接口出参。
 */
public class HashVO {

    /** 实际使用的算法。 */
    private String algorithm;
    /** 哈希值（十六进制）。 */
    private String hash;
    /** 输入文本 UTF-8 字节数。 */
    private int inputLength;
    /** 处理耗时（毫秒）。 */
    private long costTimeMs;

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public int getInputLength() {
        return inputLength;
    }

    public void setInputLength(int inputLength) {
        this.inputLength = inputLength;
    }

    public long getCostTimeMs() {
        return costTimeMs;
    }

    public void setCostTimeMs(long costTimeMs) {
        this.costTimeMs = costTimeMs;
    }
}
