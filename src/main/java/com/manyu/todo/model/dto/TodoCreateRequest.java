package com.manyu.todo.model.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 新增待办事项请求对象。
 *
 * @author AiWork
 */
public class TodoCreateRequest {

    /** 事项名称，必填，最长 64 字符 */
    @NotBlank(message = "事项名称不能为空")
    @Size(max = 64, message = "事项名称不能超过64个字符")
    private String name;

    /** 事项描述，选填，最长 512 字符 */
    @Size(max = 512, message = "事项描述不能超过512个字符")
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "TodoCreateRequest{name='" + name + "'}";
    }
}
