-- 待办事项表：本期仅创建，状态字段为后续扩展预留
CREATE TABLE IF NOT EXISTS `todo_item` (
    `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id`    varchar(64)  NOT NULL DEFAULT '' COMMENT '租户标识，取自登录态',
    `title`        varchar(200) NOT NULL DEFAULT '' COMMENT '事项名称，业务限制 1~100 字符',
    `description`  varchar(1000) NOT NULL DEFAULT '' COMMENT '事项描述，业务限制 <=500 字符',
    `status`       tinyint      NOT NULL DEFAULT 0 COMMENT '待办状态：0=待办',
    `creator_id`   varchar(64)  NOT NULL DEFAULT '' COMMENT '创建人 ID，取自登录态',
    `creator_name` varchar(128) NOT NULL DEFAULT '' COMMENT '创建人展示名',
    `gmt_create`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_todo_item_tenant_creator` (`tenant_id`, `creator_id`, `gmt_create`)
) DEFAULT CHARSET = utf8mb4 COMMENT = '待办事项表';
