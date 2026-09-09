package com.dtazzy.todo.dao.entity;

import java.time.LocalDateTime;

/**
 * 待办事项数据对象（对应 todo_item 表）。
 */
public class TodoItemDO {

    /** 系统自增主键 */
    private Long id;

    /** 租户/用户标识 */
    private String tenantId;

    /** 待办事项名称 */
    private String name;

    /** 待办事项描述 */
    private String description;

    /** 创建时间 */
    private LocalDateTime gmtCreate;

    /** 修改时间 */
    private LocalDateTime gmtModified;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

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

    public LocalDateTime getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(LocalDateTime gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public LocalDateTime getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(LocalDateTime gmtModified) {
        this.gmtModified = gmtModified;
    }

    @Override
    public String toString() {
        return "TodoItemDO{"
                + "id=" + id
                + ", tenantId='" + tenantId + '\''
                + ", name='" + name + '\''
                + ", description='" + description + '\''
                + ", gmtCreate=" + gmtCreate
                + ", gmtModified=" + gmtModified
                + '}';
    }
}