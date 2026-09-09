package com.alipay.todo.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 待办事项数据对象
 *
 * @author AiWork
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TodoItemDO {

    private Long id;
    private String tenantId;
    private String name;
    private String description;
    private String status;
    private String creator;
    private Integer isDeleted;
    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModified;
}