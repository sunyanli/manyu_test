package com.manyu.todo.model.vo;

import java.util.Date;

/**
 * 待办事项视图对象。
 *
 * @author AiWork
 */
public class TodoVO {

    /** 待办事项 ID */
    private Long id;

    /** 事项名称 */
    private String name;

    /** 事项描述 */
    private String description;

    /** 创建时间 */
    private Date gmtCreate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Date getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    @Override
    public String toString() {
        return "TodoVO{id=" + id + ", name='" + name + "'}";
    }
}
