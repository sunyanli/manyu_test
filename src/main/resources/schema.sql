-- 算法演示与可视化平台 数据库初始化脚本
-- 数据库: algorithm_demo

CREATE DATABASE IF NOT EXISTS algorithm_demo
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE algorithm_demo;

-- 调用记录表
CREATE TABLE IF NOT EXISTS call_record (
    id BIGINT AUTO_INCREMENT COMMENT '主键',
    user_id VARCHAR(64) NOT NULL COMMENT '调用人用户ID',
    user_name VARCHAR(100) NOT NULL COMMENT '调用人姓名',
    user_type VARCHAR(32) NOT NULL COMMENT '人员类型（正式/实习/外包）',
    user_level VARCHAR(32) NOT NULL COMMENT '人员层级（P5/P6/P7/M1等）',
    user_dept_id BIGINT NOT NULL COMMENT '人员所属部门ID',
    api_name VARCHAR(64) NOT NULL COMMENT '调用的接口名称（hello/hash/bubble-sort）',
    call_result VARCHAR(16) NOT NULL COMMENT '调用结果（SUCCESS/FAIL）',
    call_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '调用时间',
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id) USING BTREE,
    INDEX idx_call_record_api_name (api_name) USING BTREE,
    INDEX idx_call_record_user_id (user_id) USING BTREE,
    INDEX idx_call_record_call_time (call_time) USING BTREE,
    INDEX idx_call_record_user_type (user_type) USING BTREE,
    INDEX idx_call_record_user_level (user_level) USING BTREE,
    INDEX idx_call_record_dept_id (user_dept_id) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='接口调用记录表';

-- 部门信息表
CREATE TABLE IF NOT EXISTS department (
    id BIGINT AUTO_INCREMENT COMMENT '主键',
    dept_name VARCHAR(100) NOT NULL COMMENT '部门名称',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父部门ID，0表示根部门',
    dept_level INT NOT NULL DEFAULT 1 COMMENT '部门层级',
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id) USING BTREE,
    INDEX idx_department_parent_id (parent_id) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门信息表';

-- 插入测试部门数据
INSERT INTO department (id, dept_name, parent_id, dept_level) VALUES
(1, '技术部', 0, 1),
(2, '产品部', 0, 1),
(3, '市场部', 0, 1),
(4, '前端组', 1, 2),
(5, '后端组', 1, 2),
(6, '测试组', 1, 2);