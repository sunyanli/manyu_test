package com.example.org.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentMoveDTO {

    @NotNull
    private Long newParentId;

    private Integer sortOrder;
}