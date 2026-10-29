package com.antdigital.todo.item.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 待办事项持久化对象，对应表 {@code todo_item}。
 *
 * @author AiWork
 */
@TableName("todo_item")
public class TodoItemDO {

    /** 主键，自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户标识，取自登录态。 */
    private String tenantId;

    /** 事项名称。 */
    private String title;

    /** 事项描述，空描述存空串。 */
    private String description;

    /** 待办状态，0=待办。 */
    private Integer status;

    /** 创建人 ID，取自登录态。 */
    private String creatorId;

    /** 创建人展示名。 */
    private String creatorName;

    /** 创建时间。 */
    private LocalDateTime gmtCreate;

    /** 最后修改时间。 */
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
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
