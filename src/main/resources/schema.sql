CREATE TABLE IF NOT EXISTS api_call_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '系统自增主键',
    api_name VARCHAR(64) NOT NULL COMMENT '接口名（hello/hash/sort）',
    caller_id VARCHAR(64) NOT NULL COMMENT '调用人工号',
    caller_name VARCHAR(128) NOT NULL COMMENT '调用人姓名',
    person_type VARCHAR(64) NOT NULL DEFAULT '' COMMENT '人员类型',
    person_level VARCHAR(64) NOT NULL DEFAULT '' COMMENT '人员层级',
    person_dept VARCHAR(256) NOT NULL DEFAULT '' COMMENT '人员部门',
    call_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '调用时间',
    result_status VARCHAR(16) NOT NULL DEFAULT 'success' COMMENT '调用结果（success/failure）',
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录修改时间'
);

CREATE INDEX IF NOT EXISTS idx_api_call_record_api_time ON api_call_record(api_name, call_time);
CREATE INDEX IF NOT EXISTS idx_api_call_record_person_type ON api_call_record(person_type, call_time);
CREATE INDEX IF NOT EXISTS idx_api_call_record_person_dept ON api_call_record(person_dept, call_time);