package com.example.org.controller;

import com.example.org.common.ApiResponse;
import com.example.org.model.dto.EmployeeCreateDTO;
import com.example.org.model.dto.EmployeeUpdateDTO;
import com.example.org.model.dto.ResignRequestDTO;
import com.example.org.model.dto.TransferRequestDTO;
import com.example.org.service.EmployeeService;
import com.example.org.service.TransferService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@Validated
public class EmployeeController {

    private final EmployeeService employeeService;
    private final TransferService transferService;

    @GetMapping("/check")
    public ApiResponse<?> checkUnique(@RequestParam @NotBlank String field, @RequestParam @NotBlank String value) {
        return ApiResponse.success(employeeService.checkUnique(field, value));
    }

    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody EmployeeCreateDTO dto) {
        return ApiResponse.success(employeeService.create(dto));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> getById(@PathVariable Long id) {
        return ApiResponse.success(employeeService.getById(id));
    }

    @GetMapping
    public ApiResponse<?> list(@RequestParam(required = false) Long deptId,
                               @RequestParam(required = false) String status,
                               @RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(employeeService.list(deptId, status, page, pageSize));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable Long id, @Valid @RequestBody EmployeeUpdateDTO dto) {
        employeeService.update(id, dto);
        return ApiResponse.success("更新成功", null);
    }

    @PostMapping("/{id}/transfer")
    public ApiResponse<?> transfer(@PathVariable Long id, @Valid @RequestBody TransferRequestDTO dto) {
        transferService.transfer(id, dto);
        return ApiResponse.success("调动成功", null);
    }

    @PutMapping("/{id}/resign")
    public ApiResponse<?> resign(@PathVariable Long id, @Valid @RequestBody ResignRequestDTO dto) {
        transferService.resign(id, dto);
        return ApiResponse.success("离职办理成功", null);
    }
}