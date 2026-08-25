package com.example.org.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeCreateDTO {

    @NotBlank
    private String name;

    @NotBlank
    private String employeeNo;

    @NotNull
    private Long deptId;

    private String phone;

    private String position;

    private LocalDate entryDate;
}