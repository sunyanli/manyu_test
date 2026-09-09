package com.dtazzy.todo.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 新增待办事项请求对象。
 */
public class CreateTodoItemRequest {

    /** 待办事项名称，必填，最大 200 字符 */
    @NotBlank(message = "事项名称不能为空")
    @Size(max = 200, message = "事项名称长度不能超过 200 字符")
    private String name;

    /** 待办事项描述，非必填，最大 2000 字符 */
    @Size(max = 2000, message = "事项描述长度不能超过 2000 字符")
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
}