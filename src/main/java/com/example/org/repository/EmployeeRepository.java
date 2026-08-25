package com.example.org.repository;

import com.example.org.model.entity.Employee;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EmployeeRepository extends BaseMapper<Employee> {

    @Select("SELECT COUNT(*) FROM employees WHERE employee_no = #{employeeNo}")
    int countByEmployeeNo(@Param("employeeNo") String employeeNo);

    @Select("SELECT COUNT(*) FROM employees WHERE phone = #{phone}")
    int countByPhone(@Param("phone") String phone);
}