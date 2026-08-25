package com.example.org.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.org.exception.BusinessException;
import com.example.org.model.dto.DepartmentCreateDTO;
import com.example.org.model.dto.DepartmentMoveDTO;
import com.example.org.model.dto.DepartmentStatusDTO;
import com.example.org.model.dto.DepartmentTreeDTO;
import com.example.org.model.dto.DepartmentUpdateDTO;
import com.example.org.model.entity.Department;
import com.example.org.model.enums.DepartmentStatus;
import com.example.org.repository.DepartmentRepository;
import com.example.org.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    /**
     * Get the full department tree.
     */
    public List<DepartmentTreeDTO> getTree() {
        List<Department> departments = departmentRepository.selectFullTree();
        return buildTree(departments, null);
    }

    /**
     * Get child departments by parent ID.
     */
    public List<Department> getChildren(Long parentId) {
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Department::getParentId, parentId)
                .eq(Department::getStatus, "ACTIVE")
                .orderByAsc(Department::getSortOrder);
        return departmentRepository.selectList(wrapper);
    }

    /**
     * Create a new department.
     */
    public Department create(DepartmentCreateDTO dto) {
        if (dto.getParentId() != null) {
            Department parent = departmentRepository.selectById(dto.getParentId());
            if (parent == null || !"ACTIVE".equals(parent.getStatus())) {
                throw new BusinessException(400, "父部门不存在或已停用");
            }
        }
        Department department = new Department();
        department.setName(dto.getName());
        department.setParentId(dto.getParentId());
        department.setSortOrder(dto.getSortOrder());
        department.setStatus("ACTIVE");
        departmentRepository.insert(department);
        return department;
    }

    /**
     * Update department name and/or sort order.
     */
    public Department update(Long id, DepartmentUpdateDTO dto) {
        Department department = departmentRepository.selectById(id);
        if (department == null) {
            throw new BusinessException(404, "部门不存在");
        }
        if (dto.getName() != null) {
            department.setName(dto.getName());
        }
        if (dto.getSortOrder() != null) {
            department.setSortOrder(dto.getSortOrder());
        }
        departmentRepository.updateById(department);
        return department;
    }

    /**
     * Move a department under a new parent, with cyclic-move prevention.
     */
    @Transactional
    public Department move(Long id, DepartmentMoveDTO dto) {
        Department department = departmentRepository.selectById(id);
        if (department == null) {
            throw new BusinessException(404, "部门不存在");
        }
        Long newParentId = dto.getNewParentId();
        if (newParentId.equals(id)) {
            throw new BusinessException(400, "不能将部门移动到自身或子部门下");
        }
        Department newParent = departmentRepository.selectById(newParentId);
        if (newParent == null || !"ACTIVE".equals(newParent.getStatus())) {
            throw new BusinessException(400, "父部门不存在或已停用");
        }
        // Check that newParentId is not a descendant of the moved department
        List<Long> descendantIds = collectDescendantIds(id);
        if (descendantIds.contains(newParentId)) {
            throw new BusinessException(400, "不能将部门移动到自身或子部门下");
        }
        department.setParentId(newParentId);
        if (dto.getSortOrder() != null) {
            department.setSortOrder(dto.getSortOrder());
        }
        departmentRepository.updateById(department);
        return department;
    }

    /**
     * Update department status.
     */
    public void updateStatus(Long id, DepartmentStatusDTO dto) {
        Department department = departmentRepository.selectById(id);
        if (department == null) {
            throw new BusinessException(404, "部门不存在");
        }
        // Validate status value is a valid enum
        try {
            DepartmentStatus.valueOf(dto.getStatus());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(400, "无效的状态值，仅支持 ACTIVE / DISABLED");
        }
        department.setStatus(dto.getStatus());
        departmentRepository.updateById(department);
    }

    /**
     * Recursively collect all descendant department IDs.
     */
    private List<Long> collectDescendantIds(Long id) {
        List<Long> result = new ArrayList<>();
        List<Department> children = departmentRepository.selectList(
                new LambdaQueryWrapper<Department>().eq(Department::getParentId, id));
        for (Department child : children) {
            result.add(child.getId());
            result.addAll(collectDescendantIds(child.getId()));
        }
        return result;
    }

    /**
     * Build a department tree from a flat list.
     */
    private List<DepartmentTreeDTO> buildTree(List<Department> departments, Long parentId) {
        List<DepartmentTreeDTO> tree = new ArrayList<>();
        for (Department dept : departments) {
            if ((parentId == null && dept.getParentId() == null)
                    || (parentId != null && parentId.equals(dept.getParentId()))) {
                DepartmentTreeDTO dto = new DepartmentTreeDTO();
                dto.setId(dept.getId());
                dto.setName(dept.getName());
                dto.setParentId(dept.getParentId());
                dto.setSortOrder(dept.getSortOrder());
                dto.setStatus(dept.getStatus());
                List<DepartmentTreeDTO> children = buildTree(departments, dept.getId());
                dto.setChildren(children != null ? children : new ArrayList<>());
                tree.add(dto);
            }
        }
        return tree;
    }
}