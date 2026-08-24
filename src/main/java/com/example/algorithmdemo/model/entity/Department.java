package com.example.algorithmdemo.model.entity;

import java.time.LocalDateTime;

/**
 * 部门信息实体
 */
public class Department {

    private Long id;

    /** 部门名称 */
    private String deptName;

    /** 父部门ID，0表示根部门 */
    private Long parentId;

    /** 部门层级 */
    private Integer deptLevel;

    /** 创建时间 */
    private LocalDateTime gmtCreate;

    /** 修改时间 */
    private LocalDateTime gmtModified;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public Integer getDeptLevel() { return deptLevel; }
    public void setDeptLevel(Integer deptLevel) { this.deptLevel = deptLevel; }

    public LocalDateTime getGmtCreate() { return gmtCreate; }
    public void setGmtCreate(LocalDateTime gmtCreate) { this.gmtCreate = gmtCreate; }

    public LocalDateTime getGmtModified() { return gmtModified; }
    public void setGmtModified(LocalDateTime gmtModified) { this.gmtModified = gmtModified; }
}