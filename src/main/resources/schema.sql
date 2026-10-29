CREATE DATABASE IF NOT EXISTS algorithm_demo DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE algorithm_demo;

CREATE TABLE IF NOT EXISTS algorithm_call_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(64) NOT NULL,
    api_path VARCHAR(128) NOT NULL,
    algorithm_type VARCHAR(32) NOT NULL,
    caller_id VARCHAR(64) NOT NULL,
    caller_name VARCHAR(64),
    caller_type VARCHAR(32),
    caller_level VARCHAR(32),
    caller_dept VARCHAR(128),
    request_params TEXT,
    response_result VARCHAR(16),
    cost_time_ms INT,
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_call_request_id (request_id),
    KEY idx_call_api_path (api_path),
    KEY idx_call_caller_id (caller_id),
    KEY idx_call_alg_type (algorithm_type),
    KEY idx_call_create_time (gmt_create),
    KEY idx_call_caller_type (caller_type),
    KEY idx_call_caller_level (caller_level),
    KEY idx_call_caller_dept (caller_dept)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS algorithm_export_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL,
    algorithm_type VARCHAR(32) NOT NULL,
    params TEXT,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    file_url VARCHAR(512),
    error_msg VARCHAR(512),
    operator_id VARCHAR(64) NOT NULL,
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_export_task_id (task_id),
    KEY idx_export_status (status),
    KEY idx_export_operator (operator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
