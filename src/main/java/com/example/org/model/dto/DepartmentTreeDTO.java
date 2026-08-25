package com.example.org.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentTreeDTO {

    private Long id;
    private String name;
    private Long parentId;
    private Integer sortOrder;
    private String status;
    private List<DepartmentTreeDTO> children = new ArrayList<>();
}