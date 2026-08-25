-- 组织架构管理模块 DDL
-- 数据库: org_db

CREATE TABLE IF NOT EXISTS departments (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '部门ID',
    name        VARCHAR(100) NOT NULL              COMMENT '部门名称',
    parent_id   BIGINT       DEFAULT NULL          COMMENT '父部门ID; NULL表示根部门',
    sort_order  INT          DEFAULT 0             COMMENT '同级排序权重',
    status      VARCHAR(20)  DEFAULT 'ACTIVE'      COMMENT 'ACTIVE / DISABLED',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_status (status),
    CONSTRAINT fk_dept_parent FOREIGN KEY (parent_id) REFERENCES departments(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

CREATE TABLE IF NOT EXISTS employees (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '员工ID',
    name         VARCHAR(50)  NOT NULL              COMMENT '姓名',
    employee_no  VARCHAR(30)  NOT NULL              COMMENT '工号',
    phone        VARCHAR(20)  DEFAULT NULL          COMMENT '手机号',
    dept_id      BIGINT       NOT NULL              COMMENT '所属部门',
    position     VARCHAR(100) DEFAULT NULL          COMMENT '职位',
    status       VARCHAR(20)  DEFAULT 'ACTIVE'      COMMENT 'ACTIVE / RESIGNED / SUSPENDED',
    entry_date   DATE         DEFAULT NULL          COMMENT '入职日期',
    resign_date  DATE         DEFAULT NULL          COMMENT '离职日期',
    created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_employee_no (employee_no),
    UNIQUE INDEX uk_phone (phone),
    INDEX idx_dept_id (dept_id),
    INDEX idx_status (status),
    CONSTRAINT fk_emp_dept FOREIGN KEY (dept_id) REFERENCES departments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工表';

CREATE TABLE IF NOT EXISTS transfer_records (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    employee_id    BIGINT       NOT NULL              COMMENT '员工ID',
    from_dept_id   BIGINT       DEFAULT NULL          COMMENT '原部门',
    to_dept_id     BIGINT       NOT NULL              COMMENT '目标部门',
    from_position  VARCHAR(100) DEFAULT NULL          COMMENT '原职位',
    to_position    VARCHAR(100) DEFAULT NULL          COMMENT '新职位',
    reason         VARCHAR(500) DEFAULT NULL          COMMENT '调动原因',
    operator_id    BIGINT       DEFAULT NULL          COMMENT '操作人ID',
    transfer_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '调动时间',
    PRIMARY KEY (id),
    INDEX idx_employee_id (employee_id),
    INDEX idx_transfer_time (transfer_time),
    CONSTRAINT fk_tr_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT fk_tr_from_dept FOREIGN KEY (from_dept_id) REFERENCES departments(id),
    CONSTRAINT fk_tr_to_dept FOREIGN KEY (to_dept_id) REFERENCES departments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调动记录表';