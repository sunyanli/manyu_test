package com.antdigital.todo.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 创建待办事项请求
 *
 * @author AiWork
 * @since 2026-09-09
 */
public class CreateTodoRequest implements Serializable {

    /**
     * 事项名称最大长度
     */
    public static final int TITLE_MAX_LENGTH = 200;

    /**
     * 描述最大长度
     */
    public static final int DESCRIPTION_MAX_LENGTH = 2000;

    /**
     * 事项名称（必填）
     */
    @NotBlank(message = "事项名称不能为空")
    @Size(max = TITLE_MAX_LENGTH, message = "事项名称长度不能超过200字符")
    private String title;

    /**
     * 事项描述（可选）
     */
    @Size(max = DESCRIPTION_MAX_LENGTH, message = "事项描述长度不能超过2000字符")
    private String description;

    /**
     * 创建用户ID
     */
    private Long userId;

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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
