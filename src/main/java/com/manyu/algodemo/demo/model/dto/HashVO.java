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

    /**
     * 摘要字符串：含算法与哈希值前 16 位，供埋点出参摘要记录（不含全文，避免超长落库）。
     *
     * @return 形如 {@code algorithm=SHA256,hash=b94d27b9934d3e08...,inputLength=11}
     */
    @Override
    public String toString() {
        String hashPrefix = hash == null || hash.length() <= 16 ? safe(hash) : hash.substring(0, 16) + "...";
        return "algorithm=" + safe(algorithm) + ",hash=" + hashPrefix + ",inputLength=" + inputLength;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
