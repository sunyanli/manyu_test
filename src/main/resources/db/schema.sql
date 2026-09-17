-- 小猫喵喵叫文生视频服务 数据库初始化脚本

CREATE TABLE IF NOT EXISTS video_task (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '系统自增主键',
    tenant_id VARCHAR(32) NOT NULL COMMENT '租户ID',
    task_no VARCHAR(64) NOT NULL COMMENT '任务编号（对外）',
    prompt VARCHAR(1024) NOT NULL COMMENT '文生视频提示词',
    duration_sec INT NOT NULL DEFAULT 3 COMMENT '视频时长（秒）',
    resolution VARCHAR(16) NOT NULL DEFAULT '720p' COMMENT '分辨率',
    format VARCHAR(16) NOT NULL DEFAULT 'mp4' COMMENT '视频格式',
    aspect_ratio VARCHAR(16) NOT NULL DEFAULT '16:9' COMMENT '画面比例',
    status VARCHAR(16) NOT NULL DEFAULT 'CREATED' COMMENT '任务状态',
    engine_job_id VARCHAR(64) DEFAULT NULL COMMENT '外部引擎作业ID',
    error_code VARCHAR(32) DEFAULT NULL COMMENT '失败错误码',
    error_msg VARCHAR(512) DEFAULT NULL COMMENT '失败原因',
    idempotency_key VARCHAR(64) DEFAULT NULL COMMENT '幂等键',
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_video_task_task_no (task_no),
    UNIQUE KEY uk_video_task_idem (tenant_id, idempotency_key),
    KEY idx_video_task_tenant_status (tenant_id, status, gmt_create)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频生成任务表';

CREATE TABLE IF NOT EXISTS video_result (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '系统自增主键',
    tenant_id VARCHAR(32) NOT NULL COMMENT '租户ID',
    task_id BIGINT NOT NULL COMMENT '关联video_task.id',
    result_url VARCHAR(512) NOT NULL COMMENT '视频访问URL',
    object_key VARCHAR(256) NOT NULL COMMENT '对象存储key',
    file_size BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
    duration_sec INT NOT NULL DEFAULT 3 COMMENT '实际时长',
    checksum VARCHAR(64) DEFAULT NULL COMMENT '文件校验',
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_video_result_task (task_id),
    UNIQUE KEY uk_video_result_task_obj (task_id, object_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频生成结果表';

CREATE TABLE IF NOT EXISTS ai_engine_job (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '系统自增主键',
    tenant_id VARCHAR(32) NOT NULL COMMENT '租户ID',
    task_id BIGINT NOT NULL COMMENT '关联video_task.id',
    engine_job_id VARCHAR(64) NOT NULL COMMENT '外部引擎作业ID',
    engine_type VARCHAR(32) NOT NULL DEFAULT 'default' COMMENT '引擎类型',
    callback_status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '回调状态',
    raw_payload TEXT DEFAULT NULL COMMENT '原始回调报文（审计）',
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_engine_job_task (task_id, engine_type),
    KEY idx_ai_engine_job_eid (engine_job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外部引擎作业映射表';