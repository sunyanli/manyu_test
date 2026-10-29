package com.antdigital.todo.item.model;

/**
 * 新增待办响应（对应统一出参的 data 对象）。
 *
 * @author AiWork
 */
public class CreateTodoResponse {

    /** 新建记录主键。 */
    private Long id;

    /** 规范化后的事项名称。 */
    private String title;

    /** 记录状态，固定 0。 */
    private Integer status;

    /** 创建时间，格式 yyyy-MM-dd HH:mm:ss。 */
    private String gmtCreate;

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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(String gmtCreate) {
        this.gmtCreate = gmtCreate;
    }
}
