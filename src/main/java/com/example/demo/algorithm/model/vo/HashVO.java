package com.example.demo.algorithm.model.vo;

/**
 * 哈希算法响应
 *
 * @author AiWork
 */
public class HashVO {

    /** 原始输入 */
    private String input;

    /** 算法类型 */
    private String algorithm;

    /** 哈希值（十六进制） */
    private String hash;

    public HashVO() {
    }

    public HashVO(String input, String algorithm, String hash) {
        this.input = input;
        this.algorithm = algorithm;
        this.hash = hash;
    }

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

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    @Override
    public String toString() {
        return "HashVO{input='" + input + "', algorithm='" + algorithm + "', hash='" + hash + "'}";
    }
}