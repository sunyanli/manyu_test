package com.example.org.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentUpdateDTO {

    private String name;

    private Integer sortOrder;
}