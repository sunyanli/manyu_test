package com.manyu.algodemo.demo.model.dto;

import jakarta.validation.constraints.Size;

/**
 * helloworld 接口入参。
 */
public class HelloWorldRequest {

    /** 问候对象，默认 World，长度 ≤ 64。 */
    @Size(max = 64, message = "name 长度不能超过64")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
