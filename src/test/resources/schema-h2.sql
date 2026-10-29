-- 测试库表结构：与生产 todo_item 字段保持一致（H2 MySQL 兼容模式）
DROP TABLE IF EXISTS `todo_item`;

CREATE TABLE `todo_item` (
    `id`           bigint        NOT NULL AUTO_INCREMENT,
    `tenant_id`    varchar(64)   NOT NULL DEFAULT '',
    `title`        varchar(200)  NOT NULL DEFAULT '',
    `description`  varchar(1000) NOT NULL DEFAULT '',
    `status`       tinyint       NOT NULL DEFAULT 0,
    `creator_id`   varchar(64)   NOT NULL DEFAULT '',
    `creator_name` varchar(128)  NOT NULL DEFAULT '',
    `gmt_create`   timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
);
