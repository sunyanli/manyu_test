-- ============================================================
-- 算法展示与监控系统 - 数据库初始化脚本
-- ============================================================

-- 接口调用日志表
CREATE TABLE IF NOT EXISTS api_call_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '系统自增主键',
    api_name VARCHAR(64) NOT NULL COMMENT '接口名称：helloworld/hash/bubble_sort',
    user_id VARCHAR(64) NOT NULL COMMENT '调用人 ID',
    user_name VARCHAR(64) NOT NULL DEFAULT '' COMMENT '调用人姓名',
    user_type VARCHAR(32) NOT NULL DEFAULT 'unknown' COMMENT '人员类型：staff/contractor/partner',
    user_level VARCHAR(32) NOT NULL DEFAULT 'unknown' COMMENT '人员层级：P6/P7/P8/P9/M1/M2',
    user_department VARCHAR(64) NOT NULL DEFAULT 'unknown' COMMENT '人员部门',
    request_params TEXT COMMENT '请求参数摘要（JSON）',
    response_code VARCHAR(16) NOT NULL DEFAULT 'OK' COMMENT '响应码',
    duration_ms INT NOT NULL DEFAULT 0 COMMENT '处理耗时（毫秒）',
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    INDEX idx_api_call_log_api (api_name),
    INDEX idx_api_call_log_user (user_id),
    INDEX idx_api_call_log_gmt_create (gmt_create),
    INDEX idx_api_call_log_type (user_type),
    INDEX idx_api_call_log_dept (user_department),
    INDEX idx_api_call_log_level (user_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接口调用日志表';

-- 用户信息表
CREATE TABLE IF NOT EXISTS user_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '系统自增主键',
    user_id VARCHAR(64) NOT NULL COMMENT '用户唯一标识',
    user_name VARCHAR(64) NOT NULL COMMENT '用户姓名',
    user_type VARCHAR(32) NOT NULL COMMENT '人员类型',
    user_level VARCHAR(32) NOT NULL COMMENT '人员层级',
    user_department VARCHAR(64) NOT NULL COMMENT '人员部门',
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    UNIQUE KEY uk_user_info_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- 导出记录表
CREATE TABLE IF NOT EXISTS export_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '系统自增主键',
    export_type VARCHAR(32) NOT NULL COMMENT '导出类型：helloworld/hash/bubble_sort',
    user_id VARCHAR(64) NOT NULL COMMENT '导出人',
    file_name VARCHAR(128) NOT NULL COMMENT '导出文件名',
    record_count INT NOT NULL DEFAULT 0 COMMENT '导出记录数',
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    INDEX idx_export_record_user (user_id),
    INDEX idx_export_record_gmt_create (gmt_create)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导出记录表';