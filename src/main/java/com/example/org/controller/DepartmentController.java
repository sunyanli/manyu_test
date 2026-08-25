package com.example.org.controller;

import com.example.org.common.ApiResponse;
import com.example.org.model.dto.DepartmentCreateDTO;
import com.example.org.model.dto.DepartmentMoveDTO;
import com.example.org.model.dto.DepartmentStatusDTO;
import com.example.org.model.dto.DepartmentUpdateDTO;
import com.example.org.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping("/tree")
    public ApiResponse<?> getTree() {
        return ApiResponse.success(departmentService.getTree());
    }

    @GetMapping("/{id}/children")
    public ApiResponse<?> getChildren(@PathVariable Long id) {
        return ApiResponse.success(departmentService.getChildren(id));
    }

    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody DepartmentCreateDTO dto) {
        return ApiResponse.success(departmentService.create(dto));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable Long id, @Valid @RequestBody DepartmentUpdateDTO dto) {
        departmentService.update(id, dto);
        return ApiResponse.success("更新成功", null);
    }

    @PutMapping("/{id}/move")
    public ApiResponse<?> move(@PathVariable Long id, @Valid @RequestBody DepartmentMoveDTO dto) {
        departmentService.move(id, dto);
        return ApiResponse.success("移动成功", null);
    }

    @PutMapping("/{id}/status")
    public ApiResponse<?> updateStatus(@PathVariable Long id, @Valid @RequestBody DepartmentStatusDTO dto) {
        departmentService.updateStatus(id, dto);
        return ApiResponse.success("状态更新成功", null);
    }
}