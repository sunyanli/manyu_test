package com.example.org.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDetailDTO {

    private Long id;
    private String name;
    private String employeeNo;
    private String phone;
    private Long deptId;
    private String deptName;
    private String position;
    private String status;
    private LocalDate entryDate;
    private LocalDate resignDate;
    private LocalDateTime createdAt;
}