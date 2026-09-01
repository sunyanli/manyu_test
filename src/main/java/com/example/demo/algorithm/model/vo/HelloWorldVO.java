package com.example.demo.algorithm.model.vo;

/**
 * HelloWorld 响应
 *
 * @author AiWork
 */
public class HelloWorldVO {

    /** 问候语 */
    private String message;

    public HelloWorldVO() {
    }

    public HelloWorldVO(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "HelloWorldVO{message='" + message + "'}";
    }
}