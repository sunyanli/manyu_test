package com.example.org.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.org.common.PageResult;
import com.example.org.exception.BusinessException;
import com.example.org.model.dto.EmployeeCreateDTO;
import com.example.org.model.dto.EmployeeDetailDTO;
import com.example.org.model.dto.EmployeeUpdateDTO;
import com.example.org.model.entity.Department;
import com.example.org.model.entity.Employee;
import com.example.org.repository.DepartmentRepository;
import com.example.org.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    /**
     * Check uniqueness of a field (employeeNo or phone).
     */
    public Map<String, Boolean> checkUnique(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(400, "校验值不能为空");
        }
        boolean isExist;
        if ("employeeNo".equals(field)) {
            isExist = employeeRepository.countByEmployeeNo(value) != 0;
        } else if ("phone".equals(field)) {
            isExist = employeeRepository.countByPhone(value) != 0;
        } else {
            throw new BusinessException(400, "不支持的校验字段");
        }
        Map<String, Boolean> result = new HashMap<>();
        result.put("isExist", isExist);
        return result;
    }

    /**
     * Create a new employee.
     */
    @Transactional
    public Employee create(EmployeeCreateDTO dto) {
        // Validate department exists and is ACTIVE
        Department department = departmentRepository.selectById(dto.getDeptId());
        if (department == null || !"ACTIVE".equals(department.getStatus())) {
            throw new BusinessException(400, "部门不存在或已停用");
        }
        // Check employeeNo uniqueness
        if (employeeRepository.countByEmployeeNo(dto.getEmployeeNo()) != 0) {
            throw new BusinessException(400, "员工编号已存在");
        }
        // Check phone uniqueness if provided
        if (dto.getPhone() != null && !dto.getPhone().isEmpty()) {
            if (employeeRepository.countByPhone(dto.getPhone()) != 0) {
                throw new BusinessException(400, "手机号已存在");
            }
        }
        Employee employee = new Employee();
        employee.setName(dto.getName());
        employee.setEmployeeNo(dto.getEmployeeNo());
        employee.setPhone(dto.getPhone());
        employee.setDeptId(dto.getDeptId());
        employee.setPosition(dto.getPosition());
        employee.setStatus("ACTIVE");
        employee.setEntryDate(dto.getEntryDate());
        employeeRepository.insert(employee);
        return employee;
    }

    /**
     * Get employee detail by ID, including department name.
     */
    public EmployeeDetailDTO getById(Long id) {
        Employee employee = employeeRepository.selectById(id);
        if (employee == null) {
            throw new BusinessException(404, "员工不存在");
        }
        EmployeeDetailDTO dto = new EmployeeDetailDTO();
        dto.setId(employee.getId());
        dto.setName(employee.getName());
        dto.setEmployeeNo(employee.getEmployeeNo());
        dto.setPhone(employee.getPhone());
        dto.setDeptId(employee.getDeptId());
        dto.setPosition(employee.getPosition());
        dto.setStatus(employee.getStatus());
        dto.setEntryDate(employee.getEntryDate());
        dto.setResignDate(employee.getResignDate());
        dto.setCreatedAt(employee.getCreatedAt());
        // Query department name
        if (employee.getDeptId() != null) {
            Department department = departmentRepository.selectById(employee.getDeptId());
            if (department != null) {
                dto.setDeptName(department.getName());
            }
        }
        return dto;
    }

    /**
     * Paginated employee list with optional filters.
     */
    public PageResult<Employee> list(Long deptId, String status, int page, int pageSize) {
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        if (deptId != null) {
            wrapper.eq(Employee::getDeptId, deptId);
        }
        if (status != null) {
            wrapper.eq(Employee::getStatus, status);
        }
        Page<Employee> mpPage = new Page<>(page, pageSize);
        Page<Employee> result = employeeRepository.selectPage(mpPage, wrapper);
        return PageResult.of(result.getTotal(), (int) result.getCurrent(), (int) result.getSize(), result.getRecords());
    }

    /**
     * Update employee fields.
     */
    public Employee update(Long id, EmployeeUpdateDTO dto) {
        Employee employee = employeeRepository.selectById(id);
        if (employee == null) {
            throw new BusinessException(404, "员工不存在");
        }
        if ("RESIGNED".equals(employee.getStatus())) {
            throw new BusinessException(400, "已离职员工不可修改");
        }
        if (dto.getName() != null) {
            employee.setName(dto.getName());
        }
        if (dto.getPhone() != null) {
            if (!dto.getPhone().equals(employee.getPhone())) {
                if (employeeRepository.countByPhone(dto.getPhone()) != 0) {
                    throw new BusinessException(400, "手机号已被其他员工使用");
                }
            }
            employee.setPhone(dto.getPhone());
        }
        if (dto.getPosition() != null) {
            employee.setPosition(dto.getPosition());
        }
        employeeRepository.updateById(employee);
        return employee;
    }
}