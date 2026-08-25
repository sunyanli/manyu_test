package com.example.org.repository;

import com.example.org.model.entity.Department;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DepartmentRepository extends BaseMapper<Department> {

    @Select("WITH RECURSIVE dept_tree AS (" +
            "SELECT id, name, parent_id, sort_order, status, created_at, updated_at " +
            "FROM departments WHERE parent_id IS NULL AND status = 'ACTIVE' " +
            "UNION ALL " +
            "SELECT d.id, d.name, d.parent_id, d.sort_order, d.status, d.created_at, d.updated_at " +
            "FROM departments d INNER JOIN dept_tree dt ON d.parent_id = dt.id " +
            "WHERE d.status = 'ACTIVE'" +
            ") " +
            "SELECT * FROM dept_tree ORDER BY sort_order")
    List<Department> selectFullTree();
}