package com.antdigital.todo.model.dto;

import java.time.LocalDateTime;

/**
 * 创建待办结果（S01/S02 出参），同时作为幂等命中时的首次结果快照。
 *
 * @author AiWork
 * @date 2026/10/29
 */
public class TodoCreateResult {

    /**
     * 待办主键 ID
     */
    private Long id;

    /**
     * 待办展示编号
     */
    private String todoNo;

    /**
     * 规范化后的事项名称
     */
    private String itemName;

    /**
     * 规范化后的事项描述
     */
    private String itemDesc;

    /**
     * 待办状态
     */
    private Integer status;

    /**
     * 创建人用户 ID
     */
    private String creatorId;

    /**
     * 创建时间
     */
    private LocalDateTime gmtCreate;

    /**
     * 是否幂等命中：true 表示本次为重复提交，返回首次创建结果
     */
    private boolean idempotent;

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

    public LocalDateTime getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(LocalDateTime gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    /**
     * 是否幂等命中。
     *
     * @return true 表示本次为重复提交
     */
    public boolean isIdempotent() {
        return idempotent;
    }

    public void setIdempotent(boolean idempotent) {
        this.idempotent = idempotent;
    }

    /**
     * 输出字段快照，名称与描述不打印原文。
     *
     * @return 字段快照
     */
    @Override
    public String toString() {
        return "TodoCreateResult{id=" + id
                + ", todoNo='" + todoNo + '\''
                + ", status=" + status
                + ", creatorId='" + creatorId + '\''
                + ", gmtCreate=" + gmtCreate
                + ", idempotent=" + idempotent
                + '}';
    }
}
