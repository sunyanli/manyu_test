-- 待办事项表 DDL
-- 数据库: todo_db
-- MySQL 8.0+

CREATE TABLE IF NOT EXISTS todo_item (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '系统自增主键',
    tenant_id   VARCHAR(64)  NOT NULL                COMMENT '租户/用户标识，用于数据隔离',
    name        VARCHAR(200) NOT NULL                COMMENT '待办事项名称',
    description VARCHAR(2000) DEFAULT NULL           COMMENT '待办事项描述',
    gmt_create  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modified DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    INDEX idx_todo_item_tenant_id (tenant_id),
    INDEX idx_todo_item_gmt_create (gmt_create)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='待办事项表';