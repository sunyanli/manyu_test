-- call_record 埋点调用记录表（manyu_test 后端唯一新增数据契约，供前端报表 W05/W06/W07 聚合）
CREATE TABLE IF NOT EXISTS call_record (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '系统自增主键',
    biz_type         VARCHAR(32)  NOT NULL COMMENT '业务类型：HELLO_WORLD/HASH/BUBBLE_SORT/EXPORT',
    caller_id        VARCHAR(64)  NOT NULL COMMENT '调用人ID',
    caller_name      VARCHAR(64)  NOT NULL COMMENT '调用人姓名',
    caller_type      VARCHAR(32)  NOT NULL COMMENT '人员类型：EMPLOYEE/OUTSOURCER/VISITOR/SYSTEM',
    caller_level     VARCHAR(32)  NOT NULL COMMENT '人员层级：P1..P9/M 序列',
    caller_dept_code VARCHAR(64)  NOT NULL COMMENT '人员部门编码',
    caller_dept_name VARCHAR(128) NOT NULL COMMENT '人员部门名称',
    req_summary      VARCHAR(512) NULL COMMENT '入参摘要（不含敏感原文）',
    resp_summary     VARCHAR(1024) NULL COMMENT '出参摘要',
    cost_time_ms     BIGINT       NOT NULL DEFAULT 0 COMMENT '处理耗时（毫秒）',
    result_status    VARCHAR(16)  NOT NULL COMMENT '结果状态：SUCCESS/FAIL',
    error_code       VARCHAR(32)  NULL COMMENT '失败错误码',
    gmt_create       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（调用时间）',
    gmt_modified     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_call_record_biz_time (biz_type, gmt_create),
    KEY idx_call_record_type_time (caller_type, gmt_create),
    KEY idx_call_record_level_time (caller_level, gmt_create),
    KEY idx_call_record_dept_time (caller_dept_code, gmt_create),
    KEY idx_call_record_status (result_status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '接口调用埋点记录表';
