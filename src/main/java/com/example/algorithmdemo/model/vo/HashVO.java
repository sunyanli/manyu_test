package com.example.algorithmdemo.model.vo;

/**
 * 哈希算法返回视图对象
 */
public class HashVO {

    private String input;
    private String algorithm;
    private String hashValue;
    private String timestamp;

    public HashVO() {}

    public HashVO(String input, String algorithm, String hashValue, String timestamp) {
        this.input = input;
        this.algorithm = algorithm;
        this.hashValue = hashValue;
        this.timestamp = timestamp;
    }

    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

    public String getHashValue() { return hashValue; }
    public void setHashValue(String hashValue) { this.hashValue = hashValue; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}