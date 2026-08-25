package com.example.org.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeTransferredEvent {

    private String eventType = "employee.transferred";
    private Long employeeId;
    private Long fromDeptId;
    private Long toDeptId;
    private String fromPosition;
    private String toPosition;
    private LocalDateTime transferTime;
    private Long operatorId;
}