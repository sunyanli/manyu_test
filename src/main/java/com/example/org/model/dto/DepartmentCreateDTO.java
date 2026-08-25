package com.example.org.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentCreateDTO {

    @NotBlank
    private String name;

    private Long parentId;

    private Integer sortOrder;
}