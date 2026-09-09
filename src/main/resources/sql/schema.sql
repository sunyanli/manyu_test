CREATE TABLE IF NOT EXISTS todo_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '系统自增主键',
    title VARCHAR(128) NOT NULL COMMENT '事项名称',
    description VARCHAR(1024) NULL DEFAULT NULL COMMENT '事项描述',
    creator_id VARCHAR(64) NOT NULL COMMENT '创建人 ID',
    tenant_id VARCHAR(64) NOT NULL COMMENT '租户 ID',
    gmt_create DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    INDEX idx_todo_item_creator (creator_id),
    INDEX idx_todo_item_tenant (tenant_id)
) COMMENT='待办事项表';