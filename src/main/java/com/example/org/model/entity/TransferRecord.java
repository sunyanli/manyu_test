package com.example.org.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("transfer_records")
public class TransferRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("employee_id")
    private Long employeeId;

    @TableField("from_dept_id")
    private Long fromDeptId;

    @TableField("to_dept_id")
    private Long toDeptId;

    @TableField("from_position")
    private String fromPosition;

    @TableField("to_position")
    private String toPosition;

    private String reason;

    @TableField("operator_id")
    private Long operatorId;

    @TableField("transfer_time")
    private LocalDateTime transferTime;
}