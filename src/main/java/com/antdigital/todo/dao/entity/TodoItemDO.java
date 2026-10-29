package com.antdigital.todo.dao.entity;

import com.antdigital.todo.common.util.DesensitizeUtil;

import java.time.LocalDateTime;

/**
 * 待办事项数据对象，对应表 todo_item。
 *
 * <p>同时承担 Mapper 查询条件载体：按幂等键查询时设置 tenantId 与 requestId 即可，
 * 保持单参数调用，避免依赖特定持久化框架的多参数绑定注解。
 *
 * @author AiWork
 * @date 2026/10/29
 */
public class TodoItemDO {

    /**
     * 自增主键
     */
    private Long id;

    /**
     * 待办展示编号：TD + yyyyMMdd + 6 位当日序号
     */
    private String todoNo;

    /**
     * 创建请求幂等键
     */
    private String requestId;

    /**
     * 租户标识
     */
    private String tenantId;

    /**
     * 事项名称
     */
    private String itemName;

    /**
     * 事项描述
     */
    private String itemDesc;

    /**
     * 待办状态，0=待处理
     */
    private Integer status;

    /**
     * 创建人用户 ID
     */
    private String creatorId;

    /**
     * 创建人展示名
     */
    private String creatorName;

    /**
     * 创建来源
     */
    private String source;

    /**
     * 逻辑删除标记，0=未删除
     */
    private Integer deleted;

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

    public String getTodoNo() {
        return todoNo;
    }

    public void setTodoNo(String todoNo) {
        this.todoNo = todoNo;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemDesc() {
        return itemDesc;
    }

    public void setItemDesc(String itemDesc) {
        this.itemDesc = itemDesc;
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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
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

    /**
     * 输出脱敏后的字段快照，事项名称与描述原文不进入日志。
     *
     * @return 字段快照
     */
    @Override
    public String toString() {
        return "TodoItemDO{id=" + id
                + ", todoNo='" + todoNo + '\''
                + ", requestId='" + requestId + '\''
                + ", tenantId='" + tenantId + '\''
                + ", itemName='" + DesensitizeUtil.mask(itemName) + '\''
                + ", itemDesc='" + DesensitizeUtil.mask(itemDesc) + '\''
                + ", status=" + status
                + ", creatorId='" + creatorId + '\''
                + ", source='" + source + '\''
                + ", deleted=" + deleted
                + ", gmtCreate=" + gmtCreate
                + '}';
    }
}
