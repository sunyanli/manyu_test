package com.alipay.todo.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 待办事项视图对象
 *
 * @author AiWork
 */
@Data
@Schema(description = "待办事项响应DTO")
public class TodoItemDTO {

    @Schema(description = "待办事项ID")
    private Long id;

    @Schema(description = "事项名称")
    private String name;

    @Schema(description = "事项描述")
    private String description;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "创建人工号")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime gmtCreate;
}