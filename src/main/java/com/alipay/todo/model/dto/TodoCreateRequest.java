package com.alipay.todo.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增待办事项请求
 *
 * @author AiWork
 */
@Data
@Schema(description = "新增待办事项请求")
public class TodoCreateRequest {

    @NotBlank(message = "事项名称必填")
    @Size(max = 64, message = "事项名称不超过64字")
    @Schema(description = "事项名称", required = true, maxLength = 64)
    private String name;

    @Size(max = 512, message = "事项描述不超过512字")
    @Schema(description = "事项描述", maxLength = 512)
    private String description;
}