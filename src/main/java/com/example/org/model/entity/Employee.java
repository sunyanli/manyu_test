package com.example.org.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("employees")
public class Employee {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    @TableField("employee_no")
    private String employeeNo;

    private String phone;

    @TableField("dept_id")
    private Long deptId;

    private String position;

    private String status;

    @TableField("entry_date")
    private LocalDate entryDate;

    @TableField("resign_date")
    private LocalDate resignDate;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}