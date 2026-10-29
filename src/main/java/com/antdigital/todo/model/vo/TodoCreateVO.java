package com.antdigital.todo.model.vo;

/**
 * 创建待办成功回执（W01 出参 data 部分）。
 *
 * <p>主键 ID 按前后端规约以 String 返回，避免 JS Number 精度丢失。
 *
 * @author AiWork
 * @date 2026/10/29
 */
public class TodoCreateVO {

    /**
     * 待办主键 ID（字符串形式，规避前端精度丢失）
     */
    private String id;

    /**
     * 待办展示编号，如 TD20261029000001
     */
    private String todoNo;

    /**
     * 事项名称（服务端规范化后的值）
     */
    private String itemName;

    /**
     * 事项描述（服务端规范化后的值）
     */
    private String itemDesc;

    /**
     * 待办状态，创建后固定 0=待处理
     */
    private Integer status;

    /**
     * 创建人用户 ID
     */
    private String creatorId;

    /**
     * 创建时间，格式 yyyy-MM-dd HH:mm:ss
     */
    private String gmtCreate;

    /**
     * 是否为幂等命中：true 表示本次为重复提交，返回首次结果
     */
    private boolean idempotent;

    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    public String getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(String gmtCreate) {
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
}
