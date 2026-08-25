package com.example.org.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResignedEvent {

    private String eventType = "employee.resigned";
    private Long employeeId;
    private LocalDate resignDate;
    private Long operatorId;
}