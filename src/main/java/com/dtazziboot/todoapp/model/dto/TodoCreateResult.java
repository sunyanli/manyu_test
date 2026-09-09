package com.dtazziboot.todoapp.model.dto;

import java.time.LocalDateTime;

/**
 * 新增待办事项返回结果
 *
 * @author AiWork
 * @date 2026/09/09
 */
public class TodoCreateResult {

    /**
     * 主键 ID
     */
    private Long id;

    /**
     * 事项名称
     */
    private String title;

    /**
     * 事项描述
     */
    private String description;

    /**
     * 创建人 ID
     */
    private String creatorId;

    /**
     * 租户 ID
     */
    private String tenantId;

    /**
     * 创建时间
     */
    private LocalDateTime gmtCreate;

    /**
     * 修改时间
     */
    private LocalDateTime gmtModified;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
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
}