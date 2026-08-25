# 组织架构管理模块 — 实施计划

> 阶段：实施计划 (loop-1)
> 基于：dima.md v1.1（需求澄清与设计分析）
> 生成时间：2026-08-25
> 目标仓库：`manyu_test`（主仓），`manyu_test1`（预留扩展仓）

---

## 一、总览

### 1.1 目标

在 `manyu_test` 仓库从零搭建组织架构管理模块后端，实现部门树管理、员工新增/调动/离职四大核心功能，为审批、权限等业务系统提供可靠的人员数据源。

### 1.2 仓库定位

| 仓库 | 角色 | 内容 |
|------|------|------|
| `manyu_test` | **主仓** | 后端 API 服务（FastAPI）+ 数据层（SQLAlchemy + Alembic）+ 权限中间件 |
| `manyu_test1` | 预留扩展仓 | 后续前端 SPA 或独立微服务（本次不涉及） |

### 1.3 技术栈（已确认）

| 层面 | 方案 | 版本约束 |
|------|------|---------|
| 语言 | Python | 3.10+ |
| Web 框架 | FastAPI | ≥0.100 |
| ORM | SQLAlchemy (async) | ≥2.0 |
| 迁移工具 | Alembic | ≥1.12 |
| 数据库 | MySQL | 8.0+（需递归 CTE 支持） |
| 缓存 | Redis（可选） | 6.0+ |
| 审批对接 | 独立服务 HTTP 回调 | 调动时 POST 通知 |
| 权限 | 本模块内置中间件 | 角色枚举（超管/HR/部门主管） |

---

## 二、项目结构

### 2.1 目录规划 (`manyu_test`)

```
manyu_test/
├── app/
│   ├── __init__.py
│   ├── main.py                    # FastAPI 应用入口，挂载路由
│   ├── config.py                  # 配置管理（DB/Redis/审批回调 URL）
│   ├── database.py                # 异步 SQLAlchemy engine + session
│   ├── models/
│   │   ├── __init__.py
│   │   ├── base.py                # Base ORM 声明基类
│   │   ├── department.py          # Department 模型
│   │   ├── employee.py            # Employee 模型
│   │   └── transfer_record.py     # TransferRecord 模型
│   ├── schemas/
│   │   ├── __init__.py
│   │   ├── common.py              # 统一响应体 {code, data, msg}
│   │   ├── department.py          # 部门请求/响应 Schema
│   │   ├── employee.py            # 员工请求/响应 Schema
│   │   └── transfer.py            # 调动请求/响应 Schema
│   ├── routers/
│   │   ├── __init__.py
│   │   ├── departments.py         # /api/departments/*
│   │   └── employees.py           # /api/employees/*
│   ├── services/
│   │   ├── __init__.py
│   │   ├── department_service.py  # 部门业务逻辑
│   │   ├── employee_service.py    # 员工业务逻辑
│   │   └── approval_callback.py   # 审批系统 HTTP 回调
│   ├── middleware/
│   │   ├── __init__.py
│   │   └── auth.py                # 角色鉴权中间件
│   └── utils/
│       ├── __init__.py
│       └── exceptions.py          # 自定义异常 + 全局异常处理器
├── alembic/
│   ├── env.py
│   └── versions/                  # 迁移脚本
├── alembic.ini
├── requirements.txt
├── pyproject.toml
└── tests/
    ├── __init__.py
    ├── conftest.py                # pytest fixtures（DB/客户端）
    ├── test_departments.py
    ├── test_employees.py
    └── test_transfers.py
```

---

## 三、数据模型设计

### 3.1 Department（部门表）

```sql
CREATE TABLE departments (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL COMMENT '部门名称',
    parent_id   BIGINT        DEFAULT NULL COMMENT '父部门ID，NULL 表示根节点',
    level       TINYINT       NOT NULL DEFAULT 1 COMMENT '层级深度，根=1',
    path        VARCHAR(500)  DEFAULT '' COMMENT '物化路径，如 /1/3/7',
    sort_order  INT           NOT NULL DEFAULT 0 COMMENT '同级排序',
    status      TINYINT       NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_parent_id (parent_id),
    INDEX idx_path (path),
    CONSTRAINT fk_parent FOREIGN KEY (parent_id) REFERENCES departments(id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';
```

**设计说明**：
- `parent_id` 实现邻接表模型，天然支持懒加载
- `path` 物化路径辅助全树查询和子树快速定位
- `level` 限制深度校验（默认最大 10 层）
- 外键 `ON DELETE RESTRICT` 防止误删有子节点的部门

### 3.2 Employee（员工表）

```sql
CREATE TABLE employees (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(50)   NOT NULL COMMENT '姓名',
    employee_no  VARCHAR(30)   NOT NULL COMMENT '工号',
    phone        VARCHAR(20)   NOT NULL COMMENT '手机号',
    dept_id      BIGINT        NOT NULL COMMENT '所属部门ID',
    position     VARCHAR(100)  DEFAULT '' COMMENT '职位',
    status       TINYINT       NOT NULL DEFAULT 1 COMMENT '1=在职 2=离职',
    entry_date   DATE          DEFAULT NULL COMMENT '入职日期',
    resign_date  DATE          DEFAULT NULL COMMENT '离职日期',
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE INDEX uk_employee_no (employee_no),
    UNIQUE INDEX uk_phone (phone),
    INDEX idx_dept_id (dept_id),
    INDEX idx_status (status),
    CONSTRAINT fk_dept FOREIGN KEY (dept_id) REFERENCES departments(id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工表';
```

**设计说明**：
- `employee_no` + `phone` 双唯一索引，DB 层保底防重
- `status` 枚举：1=在职，2=离职（逻辑删除）
- 外键约束确保部门存在且不被误删

### 3.3 TransferRecord（调动记录表）

```sql
CREATE TABLE transfer_records (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id    BIGINT        NOT NULL COMMENT '员工ID',
    from_dept_id   BIGINT        NOT NULL COMMENT '原部门ID',
    to_dept_id     BIGINT        NOT NULL COMMENT '目标部门ID',
    from_position  VARCHAR(100)  DEFAULT '' COMMENT '原职位',
    to_position    VARCHAR(100)  DEFAULT '' COMMENT '新职位',
    reason         VARCHAR(500)  DEFAULT '' COMMENT '调动原因',
    operator_id    BIGINT        DEFAULT NULL COMMENT '操作人ID',
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_employee_id (employee_id),
    INDEX idx_created_at (created_at),
    CONSTRAINT fk_transfer_employee FOREIGN KEY (employee_id) REFERENCES employees(id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调动记录表';
```

**设计说明**：
- 独立留痕表，每次调动写入一条历史
- 记录操作人 `operator_id` 用于审计
- 不设外键指向部门（保留历史快照语义，即使部门后续被删除记录仍可读）

---

## 四、API 详细设计

### 4.1 统一响应格式

```json
{
  "code": 200,
  "data": {},
  "msg": "ok"
}
```

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 404 | 资源不存在 |
| 409 | 冲突（如工号/手机号重复） |
| 422 | 校验失败（Pydantic） |
| 500 | 服务端内部错误 |

### 4.2 部门接口

#### 4.2.1 获取部门树（懒加载）

```
GET /api/departments/tree?parentId=0
```

- **Query**: `parentId` (可选，默认 0=根节点)
- **响应**:

```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "研发部",
      "parentId": null,
      "level": 1,
      "sortOrder": 0,
      "hasChildren": true,
      "children": []
    }
  ]
}
```

**实现策略**：
- 首次请求不带 `parentId` 或 `parentId=0` → 返回顶级部门（`parent_id IS NULL`）
- 展开节点时带 `parentId={id}` → 返回该节点的直接子部门
- `hasChildren` 通过 `SELECT COUNT(1) FROM departments WHERE parent_id = ?` 判断
- 后续可引入 Redis 缓存整棵树（读多写少）

#### 4.2.2 拖拽调整父部门

```
PUT /api/departments/{id}/move
```

- **请求体**:

```json
{
  "newParentId": 5
}
```

- **响应**:

```json
{
  "code": 200,
  "msg": "调整成功"
}
```

**校验逻辑**：
1. 目标部门 `{id}` 是否存在
2. `newParentId` 是否存在且状态为启用
3. **循环引用检测**：`newParentId` 不能是 `{id}` 自身或其子孙节点（沿 `path` 或递归查询）
4. 深度限制：移动后层级不能超过最大深度（10）
5. 事务更新：`parent_id` + `level` + `path`（重新计算物化路径）

#### 4.2.3 新增部门（补充）

```
POST /api/departments
```

```json
{
  "name": "后端组",
  "parentId": 1,
  "sortOrder": 1
}
```

#### 4.2.4 编辑部门（补充）

```
PUT /api/departments/{id}
```

```json
{
  "name": "后端开发组",
  "sortOrder": 2
}
```

#### 4.2.5 删除部门（补充）

```
DELETE /api/departments/{id}
```

- 校验：无子部门、无在职员工
- 软删除：`status = 0`

### 4.3 员工接口

#### 4.3.1 实时唯一性校验

```
GET /api/employees/check?field=employeeNo&value=10086
```

- **Query**:
  - `field`: `employeeNo` | `phone`
  - `value`: 待校验值
- **响应**:

```json
{
  "code": 200,
  "data": { "isExist": false }
}
```

#### 4.3.2 新增员工

```
POST /api/employees
```

- **请求体**:

```json
{
  "name": "张三",
  "employeeNo": "10086",
  "deptId": 2,
  "phone": "13800138000",
  "position": "前端开发",
  "entryDate": "2023-10-01"
}
```

- **响应**:

```json
{
  "code": 200,
  "data": { "id": 1 },
  "msg": "新增成功"
}
```

**校验链**：
1. Pydantic Schema 校验（手机号格式、必填字段）
2. 工号唯一性 → 409
3. 手机号唯一性 → 409
4. 部门存在且启用 → 404
5. DB 唯一索引最终保底

#### 4.3.3 员工列表（补充）

```
GET /api/employees?deptId=2&status=1&page=1&pageSize=20
```

- **Query**:
  - `deptId`: 部门筛选（可选，含子部门）
  - `status`: 1=在职 2=离职（可选）
  - `keyword`: 姓名/工号模糊搜索（可选）
  - `page` / `pageSize`: 分页

#### 4.3.4 员工详情（补充）

```
GET /api/employees/{id}
```

#### 4.3.5 编辑员工（补充）

```
PUT /api/employees/{id}
```

- 可编辑：姓名、手机号、职位、部门（需走调动流程？）
- 不可编辑：工号、状态（通过专用接口变更）

#### 4.3.6 人员调动

```
POST /api/employees/{id}/transfer
```

- **请求体**:

```json
{
  "newDeptId": 3,
  "newPosition": "Java开发",
  "reason": "业务调整"
}
```

- **响应**:

```json
{
  "code": 200,
  "msg": "调动成功"
}
```

**事务流程**：
1. 校验员工存在且为在职状态
2. 校验 `newDeptId` 存在且启用
3. 开启事务：
   a. 更新 `employees.dept_id` + `position`
   b. 写入 `transfer_records` 留痕
4. 提交事务
5. **异步**：HTTP POST 通知审批系统更新审批节点（失败不阻塞调动，记录日志 + 重试队列）

#### 4.3.7 办理离职

```
PUT /api/employees/{id}/resign
```

- **请求体**:

```json
{
  "resignDate": "2023-11-01"
}
```

- **响应**:

```json
{
  "code": 200,
  "msg": "离职办理成功"
}
```

**事务流程**：
1. 校验员工存在且为在职状态
2. 更新 `status = 2`，写入 `resign_date`
3. ~~资源释放（账号许可/登录权限）~~ → 预留回调接口，待外部系统对接

#### 4.3.8 调动记录查询（补充）

```
GET /api/employees/{id}/transfers?page=1&pageSize=20
```

---

## 五、权限模型

### 5.1 角色定义

```python
from enum import IntEnum

class Role(IntEnum):
    SUPER_ADMIN = 1   # 超管：全量 CRUD
    HR = 2            # HR：管理部门 + 所有人员
    DEPT_MANAGER = 3  # 部门主管：查看本部门及子部门，编辑本部门人员部分信息
```

### 5.2 鉴权中间件

- 通过 FastAPI `Depends` 注入当前用户角色
- 请求头 `X-User-Id` + `X-User-Role`（MVP 阶段简化；生产环境对接统一认证中心）
- 部门主管访问时，校验目标部门是否在其管辖范围内（沿 `path` 匹配）

### 5.3 权限矩阵

| 操作 | 超管 | HR | 部门主管 |
|------|:---:|:--:|:------:|
| 查看部门树 | ✅ | ✅ | ✅ (仅本部门及子部门) |
| 新增/编辑/删除部门 | ✅ | ✅ | ❌ |
| 拖拽调整部门 | ✅ | ✅ | ❌ |
| 查看员工列表 | ✅ | ✅ | ✅ (仅本部门及子部门) |
| 新增员工 | ✅ | ✅ | ❌ |
| 编辑员工 | ✅ | ✅ | ✅ (仅职位/手机号) |
| 员工调动 | ✅ | ✅ | ❌ |
| 办理离职 | ✅ | ✅ | ❌ |

---

## 六、跨仓对齐点

### 6.1 当前状态

`manyu_test` 与 `manyu_test1` 之间无代码依赖，本次实施全部在 `manyu_test` 中完成。

### 6.2 后续对齐契约

当 `manyu_test1` 开始消费 API 时：

| 对齐项 | 提供方 | 消费方 | 格式 |
|--------|--------|--------|------|
| API 契约 | `manyu_test` (OpenAPI JSON) | `manyu_test1` | `/openapi.json` |
| 统一响应体 | `manyu_test` (schemas/common.py) | `manyu_test1` (类型定义) | `{code, data, msg}` |
| 错误码枚举 | `manyu_test` (utils/exceptions.py) | `manyu_test1` | 共享常量文件 |
| 审批回调 | `manyu_test` → 审批系统 | 外部服务 | HTTP POST |

### 6.3 审批回调接口契约（预留）

```
POST {approval_service_url}/api/approval/nodes/refresh
```

```json
{
  "employeeId": 1,
  "oldDeptId": 2,
  "newDeptId": 3,
  "newPosition": "Java开发",
  "triggerTime": "2023-11-01T10:00:00Z"
}
```

---

## 七、实施阶段

### Phase 1：基础设施搭建（预计 1 天）

| 步骤 | 内容 | 产出 |
|------|------|------|
| 1.1 | 初始化项目结构 | `app/` 目录骨架、`requirements.txt`、`pyproject.toml` |
| 1.2 | 配置管理 | `config.py`（DB/Redis/审批回调 URL 环境变量读取） |
| 1.3 | 数据库连接 | `database.py`（异步 engine + session factory） |
| 1.4 | Alembic 初始化 | `alembic init` + 迁移环境配置 |
| 1.5 | FastAPI 入口 | `main.py`（应用工厂 + 生命周期事件） |

### Phase 2：数据模型与迁移（预计 0.5 天）

| 步骤 | 内容 | 产出 |
|------|------|------|
| 2.1 | ORM 模型定义 | `models/department.py`、`employee.py`、`transfer_record.py` |
| 2.2 | 初始迁移脚本 | `alembic/versions/001_init.sql`（三张表 DDL） |
| 2.3 | 迁移执行 | `alembic upgrade head` 验证 |

### Phase 3：部门管理（预计 1 天）

| 步骤 | 内容 | 产出 |
|------|------|------|
| 3.1 | Schema 定义 | `schemas/department.py`（请求/响应 Pydantic 模型） |
| 3.2 | 部门服务层 | `services/department_service.py`（树查询、移动、CRUD） |
| 3.3 | 部门路由 | `routers/departments.py`（5 个端点） |
| 3.4 | 单元测试 | `tests/test_departments.py`（树查询、移动、循环引用检测） |

### Phase 4：员工管理（预计 1.5 天）

| 步骤 | 内容 | 产出 |
|------|------|------|
| 4.1 | Schema 定义 | `schemas/employee.py`（含校验器） |
| 4.2 | 员工服务层 | `services/employee_service.py`（唯一性校验、新增、列表、详情、编辑） |
| 4.3 | 员工路由 | `routers/employees.py`（8 个端点） |
| 4.4 | 单元测试 | `tests/test_employees.py`（唯一性校验、新增、列表筛选） |

### Phase 5：调动与离职（预计 1 天）

| 步骤 | 内容 | 产出 |
|------|------|------|
| 5.1 | 调动 Schema + 服务 | `schemas/transfer.py` + `employee_service.transfer()` |
| 5.2 | 调动记录查询 | 路由 + 服务 |
| 5.3 | 离职服务 | `employee_service.resign()` |
| 5.4 | 审批回调 | `services/approval_callback.py`（异步 HTTP + 重试） |
| 5.5 | 单元测试 | `tests/test_transfers.py`（调动事务、留痕、离职状态） |

### Phase 6：权限与中间件（预计 0.5 天）

| 步骤 | 内容 | 产出 |
|------|------|------|
| 6.1 | 权限中间件 | `middleware/auth.py`（角色注入 + 部门主管范围校验） |
| 6.2 | 路由挂载鉴权 | 各路由 `Depends` 注入 |
| 6.3 | 权限测试 | 角色越权场景 |

### Phase 7：集成测试与文档（预计 0.5 天）

| 步骤 | 内容 | 产出 |
|------|------|------|
| 7.1 | 集成测试 | 端到端场景（部门树→新增员工→调动→离职） |
| 7.2 | OpenAPI 文档 | 自动生成 `/docs`（Swagger UI） |
| 7.3 | 部署说明 | 环境变量清单、启动命令、迁移步骤 |

---

## 八、风险与缓解

| 风险 | 等级 | 缓解措施 | 当前状态 |
|------|:----:|---------|:--------:|
| 审批系统回调接口未定义 | 🔴 高 | 先实现调动+留痕，回调接口预留 + 异步解耦，失败不影响主流程 | ⚠️ 待审批系统接口文档 |
| 兼岗需求未确认 | 🟡 中 | 当前按 N:1 设计；如需兼岗，后续加 `employee_dept` 中间表 | ⚠️ 待确认 |
| 工号生成规则待定 | 🟡 中 | 当前手动输入，预留自动生成策略接口 | ⚠️ 待确认 |
| 资源释放外部系统接口未知 | 🟡 中 | 离职流程解耦，释放逻辑通过事件回调预留 | ⚠️ 待外部系统对接 |
| 部门树深度性能 | 🟢 低 | 物化路径 + 懒加载，MySQL 8.0 CTE 支持递归查询 | ✅ 已设计 |
| 并发唯一性校验 | 🟢 低 | 应用层校验 + DB 唯一索引双保底 | ✅ 已设计 |

---

## 九、待澄清问题（非阻塞）

以下问题不影响核心实施，可在开发过程中逐步确认：

| # | 问题 | 默认假设 | 影响范围 |
|---|------|---------|---------|
| 1 | 工号生成规则 | 手动输入 | 员工新增表单 |
| 2 | 部门树深度限制 | 最大 10 层 | 拖拽移动校验 |
| 3 | 离职后是否可复职 | 暂不支持 | 后续新增接口 |
| 4 | 资源释放接口 | 预留回调，暂不实现 | 离职流程 |
| 5 | 跨部门兼岗 | 不支持（N:1） | 数据模型 |
| 6 | 批量导入/操作 | 暂不支持 | 后续迭代 |
| 7 | 部门排序机制 | sort_order + 拖拽 | 部门管理 |
| 8 | 审计日志 | 仅调动留痕 | 后续可扩展 |
| 9 | 国际化 | 暂不支持 | 前端文案 |

---

## 十、验收标准

### 功能验收

| 需求 | 验收标准 |
|------|---------|
| 部门树加载 | `GET /api/departments/tree` 返回正确树结构，懒加载正常 |
| 部门拖拽 | `PUT /api/departments/{id}/move` 成功变更 parent_id，循环引用被拒绝 |
| 员工唯一性校验 | `GET /api/employees/check` 正确返回 isExist，并发下 DB 唯一索引保底 |
| 员工新增 | `POST /api/employees` 成功写入，重复工号/手机号返回 409 |
| 员工调动 | `POST /api/employees/{id}/transfer` 更新 dept_id + 写入调动记录 |
| 员工离职 | `PUT /api/employees/{id}/resign` 状态变更为离职，列表筛选正常 |
| 权限控制 | 部门主管无法访问非管辖范围数据 |

### 非功能验收

- 所有 API 通过 Pydantic 入参校验
- 数据库迁移脚本可正确执行和回滚
- 核心接口覆盖率 ≥ 80%
- OpenAPI 文档可访问（`/docs`）

---

> 文档版本：v1.0 | 作者：DTCoder | 状态：实施计划就绪，可进入 Phase 1 开发