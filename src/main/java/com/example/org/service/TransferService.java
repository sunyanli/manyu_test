package com.example.org.service;

import com.example.org.exception.BusinessException;
import com.example.org.model.dto.ResignRequestDTO;
import com.example.org.model.dto.TransferRequestDTO;
import com.example.org.model.entity.Department;
import com.example.org.model.entity.Employee;
import com.example.org.model.entity.TransferRecord;
import com.example.org.event.EmployeeResignedEvent;
import com.example.org.event.EmployeeTransferredEvent;
import com.example.org.event.OrgEventPublisher;
import com.example.org.repository.DepartmentRepository;
import com.example.org.repository.EmployeeRepository;
import com.example.org.repository.TransferRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class TransferService {

    private final EmployeeRepository employeeRepository;
    private final TransferRecordRepository transferRecordRepository;
    private final DepartmentRepository departmentRepository;
    private final OrgEventPublisher eventPublisher;

    /**
     * Transfer an employee to a new department.
     */
    public void transfer(Long employeeId, TransferRequestDTO dto) {
        Employee employee = employeeRepository.selectById(employeeId);
        if (employee == null) {
            throw new BusinessException(404, "员工不存在");
        }
        if (!"ACTIVE".equals(employee.getStatus())) {
            throw new BusinessException(400, "仅在职员工可进行调岗");
        }
        // Validate target department
        Department newDept = departmentRepository.selectById(dto.getNewDeptId());
        if (newDept == null || !"ACTIVE".equals(newDept.getStatus())) {
            throw new BusinessException(400, "目标部门不存在或已停用");
        }
        // Create transfer record
        TransferRecord record = new TransferRecord();
        record.setEmployeeId(employeeId);
        record.setFromDeptId(employee.getDeptId());
        record.setToDeptId(dto.getNewDeptId());
        record.setFromPosition(employee.getPosition());
        record.setToPosition(dto.getNewPosition());
        record.setReason(dto.getReason());
        record.setOperatorId(null);
        record.setTransferTime(LocalDateTime.now());
        transferRecordRepository.insert(record);
        // Update employee
        employee.setDeptId(dto.getNewDeptId());
        if (dto.getNewPosition() != null) {
            employee.setPosition(dto.getNewPosition());
        }
        employeeRepository.updateById(employee);

        // Publish transfer event
        EmployeeTransferredEvent event = new EmployeeTransferredEvent();
        event.setEmployeeId(employeeId);
        event.setFromDeptId(record.getFromDeptId());
        event.setToDeptId(dto.getNewDeptId());
        event.setFromPosition(record.getFromPosition());
        event.setToPosition(dto.getNewPosition());
        event.setTransferTime(record.getTransferTime());
        event.setOperatorId(null);
        eventPublisher.publishEmployeeTransferred(event);
    }

    /**
     * Resign an employee.
     */
    public void resign(Long employeeId, ResignRequestDTO dto) {
        Employee employee = employeeRepository.selectById(employeeId);
        if (employee == null) {
            throw new BusinessException(404, "员工不存在");
        }
        if (!"ACTIVE".equals(employee.getStatus())) {
            throw new BusinessException(400, "仅在职员工可办理离职");
        }
        employee.setStatus("RESIGNED");
        employee.setResignDate(dto.getResignDate());
        employeeRepository.updateById(employee);

        // Publish resign event
        EmployeeResignedEvent event = new EmployeeResignedEvent();
        event.setEmployeeId(employeeId);
        event.setResignDate(dto.getResignDate());
        event.setOperatorId(null);
        eventPublisher.publishEmployeeResigned(event);
    }
}