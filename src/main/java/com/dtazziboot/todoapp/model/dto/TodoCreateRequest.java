package com.dtazziboot.todoapp.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 新增待办事项请求 DTO
 *
 * @author AiWork
 * @date 2026/09/09
 */
public class TodoCreateRequest {

    /**
     * 事项名称（必填，最大 128 字符）
     */
    @NotBlank(message = "事项名称不能为空")
    @Size(max = 128, message = "事项名称长度不能超过128字符")
    private String title;

    /**
     * 事项描述（可选，最大 1024 字符）
     */
    @Size(max = 1024, message = "事项描述长度不能超过1024字符")
    private String description;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}