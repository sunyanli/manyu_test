-- 待办事项表（系分 5.1.1.1）
-- 约定：整型单列自增主键、无物理外键、无 enum/timestamp/float、索引列 NOT NULL 且有默认值、
--      时间字段统一 datetime、表名与字段名小写加下划线且总长小于 26。

CREATE TABLE IF NOT EXISTS `todo_item`
(
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '系统自增主键',
    `todo_no`      VARCHAR(32)     NOT NULL DEFAULT '' COMMENT '待办展示编号：TD+yyyyMMdd+6位序号',
    `request_id`   VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '创建请求幂等键（前端 UUID）',
    `tenant_id`    VARCHAR(32)     NOT NULL DEFAULT '0' COMMENT '租户标识，本期固定 0',
    `item_name`    VARCHAR(128)    NOT NULL DEFAULT '' COMMENT '事项名称，必填',
    `item_desc`    VARCHAR(512)    NOT NULL DEFAULT '' COMMENT '事项描述，选填，纯文本',
    `status`       TINYINT         NOT NULL DEFAULT 0 COMMENT '待办状态：0-待处理，1-已完成，2-已取消',
    `creator_id`   VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '创建人用户 ID，取自登录态',
    `creator_name` VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '创建人展示名，取自登录态',
    `source`       VARCHAR(16)     NOT NULL DEFAULT 'web' COMMENT '创建来源：web/app/api',
    `is_deleted`   TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    `gmt_create`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_todo_item_no` (`todo_no`),
    UNIQUE KEY `uk_todo_item_req` (`tenant_id`, `request_id`),
    KEY `idx_todo_item_creator` (`tenant_id`, `creator_id`, `gmt_create`),
    KEY `idx_todo_item_status` (`tenant_id`, `status`, `gmt_create`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='待办事项表';
