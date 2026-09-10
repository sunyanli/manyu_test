package com.example.todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TodoCreateRequest {

    @NotBlank(message = "事项名称不能为空")
    @Size(max = 200, message = "事项名称长度不能超过200个字符")
    private String title;

    @Size(max = 2000, message = "描述长度不能超过2000个字符")
    private String description;
}
