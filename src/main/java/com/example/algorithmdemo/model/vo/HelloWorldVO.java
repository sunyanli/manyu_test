package com.example.algorithmdemo.model.vo;

/**
 * HelloWorld 返回视图对象
 */
public class HelloWorldVO {

    private String message;
    private String timestamp;

    public HelloWorldVO() {}

    public HelloWorldVO(String message, String timestamp) {
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}