# 组织架构管理模块 — 系统分析设计文档

> 阶段：系分 (System Analysis & Design)
> 基于：dima.md v1.1（需求澄清）+ plan.md v1.0（实施计划）
> 生成时间：2026-08-25
> 目标仓库：`manyu_test`（主仓），`manyu_test1`（预留扩展仓）

---

## 一、跨仓依赖与现状摘要

### 1.1 仓库现状

| 仓库 ID | 物理路径 | 分支 | 当前内容 | 角色定位 |
|---------|---------|------|---------|---------|
| `manyu_test` | `.../manyu_test-cred-test-20260716022903` | `AI/task-DEV-...` (base: `cred-test-20260716022903`) | `bubble_sort.py`、`cred-helper-test.txt`、`.agents/` | **主仓**：承载组织架构模块后端（FastAPI + SQLAlchemy + Alembic） |
| `manyu_test1` | `.../manyu_test1-main` | `AI/task-DEV-...` (base: `main`) | `README.md`（仅 `# manyu_test1`） | **预留扩展仓**：后续前端 SPA 或独立微服务 |

**结论**：两个仓库均为空/初始状态，无现有代码可复用。`manyu_test` 从零搭建组织架构核心模块，`manyu_test1` 本次不涉及。

### 1.2 跨仓依赖关系

当前无代码级依赖。后续 `manyu_test1` 消费 `manyu_test` API 时，需在以下层面对齐：

| 对齐项 | 提供方 | 消费方 | 契约载体 |
|--------|--------|--------|---------|
| API 接口定义 | `manyu_test` | `manyu_test1` | OpenAPI JSON (`/openapi.json`) |
| 统一响应格式 | `manyu_test` (`schemas/common.py`) | `manyu_test1` | `{code, data, msg}` |
| 错误码枚举 | `manyu_test` (`utils/exceptions.py`) | `manyu_test1` | 共享常量/类型定义 |
| 审批回调接口 | `manyu_test` → 审批系统 | 外部审批服务 | HTTP POST 契约 |

---

## 二、领域模型设计

### 2.1 核心实体

```
┌──────────────────┐       ┌──────────────────┐
│   Department      │       │    Employee       │
│──────────────────│       │──────────────────│
│ id (PK)          │──┐    │ id (PK)           │
│ name             │  │    │ name              │
│ parent_id (FK)   │◄─┘    │ employee_no (UQ)  │
│ level            │ 1:N   │ phone (UQ)        │
│ path (物化路径)   │       │ dept_id (FK)      │
│ sort_order       │       │ position          │
│ status (1/0)     │       │ status (1=在职/2=离职)│
│ created_at       │       │ entry_date        │
│ updated_at       │       │ resign_date       │
└──────────────────┘       │ created_at        │
                           │ updated_at        │
                           └────────┬─────────┘
                                    │
                                    │ 1:N
                                    ▼
                           ┌──────────────────┐
                           │ TransferRecord   │
                           │──────────────────│
                           │ id (PK)          │
                           │ employee_id (FK) │
                           │ from_dept_id     │
                           │ to_dept_id       │
                           │ from_position    │
                           │ to_position      │
                           │ reason           │
                           │ operator_id      │
                           │ created_at       │
                           └──────────────────┘
```

### 2.2 实体关系说明

| 关系 | 方向 | 说明 |
|------|------|------|
| Department → Department | 自引用 1:N | `parent_id` 指向自身，形成树形结构。根节点 `parent_id IS NULL` |
| Department → Employee | 1:N | 一个部门包含多名员工。当前设计 N:1（一人一部门），不支持兼岗 |
| Employee → TransferRecord | 1:N | 每次调动写入一条历史记录 |

### 2.3 关键设计决策

| 决策点 | 方案 | 理由 |
|--------|------|------|
| 部门树模型 | 邻接表（parent_id）+ 物化路径（path） | 邻接表支持懒加载逐层展开；物化路径辅助全树查询、子树快速定位、循环引用检测 |
| 员工-部门关系 | N:1（单部门） | 需求未明确兼岗，按最简实现；如需兼岗后续加 `employee_dept` 中间表 |
| 离职处理 | 逻辑删除（status=2） | 保留历史数据，考勤/审批记录可追溯 |
| 唯一性保障 | 应用层校验 + DB 唯一索引双保底 | 防止并发竞态，唯一索引为最终防线 |
| 审批流对接 | 独立服务 HTTP 回调 + 异步解耦 | 调动主流程不阻塞于审批系统，回调失败记录日志+重试 |

---

## 三、数据模型设计（DDL）

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

| 字段 | 设计要点 |
|------|---------|
| `parent_id` | NULL 表示根节点，外键 `ON DELETE RESTRICT` 防止删除有子节点的部门 |
| `level` | 用于拖拽时深度校验（默认最大 10 层） |
| `path` | 物化路径，如 `/1/3/7`，辅助子树查询和循环引用检测 |
| `sort_order` | 同级排序，配合拖拽调整 |

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

| 字段 | 设计要点 |
|------|---------|
| `employee_no` | 唯一索引，当前手动输入，预留自动生成策略接口 |
| `phone` | 唯一索引，Pydantic 层校验格式（中国手机号 11 位） |
| `status` | 1=在职 / 2=离职（逻辑删除），列表筛选依据 |
| `dept_id` | 外键约束，防止指向不存在的部门 |

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

| 字段 | 设计要点 |
|------|---------|
| `from_dept_id` / `to_dept_id` | 不设外键指向部门——保留历史快照语义，即使部门后续被删除，记录仍可读 |
| `operator_id` | 操作人 ID，用于审计追溯 |
| `reason` | 调动原因，最长 500 字符 |

---

## 四、模块架构设计

### 4.1 分层架构

```
┌─────────────────────────────────────────────────────┐
│  Routers (API 层)                                    │
│  routers/departments.py  routers/employees.py        │
│  - 参数校验（Pydantic Schema）                        │
│  - 鉴权依赖注入（Depends）                            │
│  - 统一响应封装                                       │
├─────────────────────────────────────────────────────┤
│  Services (业务逻辑层)                                │
│  services/department_service.py                      │
│  services/employee_service.py                        │
│  services/approval_callback.py                       │
│  - 事务管理                                          │
│  - 业务规则校验（循环引用、深度限制、唯一性）           │
│  - 外部系统回调（审批通知）                            │
├─────────────────────────────────────────────────────┤
│  Models (数据访问层)                                  │
│  models/department.py  models/employee.py            │
│  models/transfer_record.py                           │
│  - SQLAlchemy ORM 声明                               │
│  - 关系映射                                          │
├─────────────────────────────────────────────────────┤
│  Middleware (横切关注点)                              │
│  middleware/auth.py                                  │
│  - 角色解析（X-User-Id / X-User-Role）                │
│  - 部门主管管辖范围校验                                │
├─────────────────────────────────────────────────────┤
│  Utils (基础设施)                                     │
│  utils/exceptions.py — 自定义异常 + 全局异常处理器     │
│  config.py — 环境变量配置管理                          │
│  database.py — 异步 engine + session factory          │
└─────────────────────────────────────────────────────┘
```

### 4.2 目录结构

```
manyu_test/
├── app/
│   ├── __init__.py
│   ├── main.py                    # FastAPI 应用入口，挂载路由 + 生命周期
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

## 五、API 契约设计

### 5.1 统一响应格式

```json
{
  "code": 200,
  "data": {},
  "msg": "ok"
}
```

| HTTP Status | code | 含义 |
|-------------|------|------|
| 200 | 200 | 成功 |
| 400 | 400 | 请求参数错误 |
| 404 | 404 | 资源不存在 |
| 409 | 409 | 冲突（工号/手机号重复） |
| 422 | 422 | Pydantic 校验失败 |
| 500 | 500 | 服务端内部错误 |

### 5.2 部门接口

| 方法 | 路径 | 用途 | 权限 |
|------|------|------|------|
| GET | `/api/departments/tree?parentId=0` | 懒加载获取子部门（含 hasChildren 标记） | 超管/HR/部门主管 |
| POST | `/api/departments` | 新增部门 | 超管/HR |
| PUT | `/api/departments/{id}` | 编辑部门（名称/排序） | 超管/HR |
| PUT | `/api/departments/{id}/move` | 拖拽调整父部门（含循环引用+深度校验） | 超管/HR |
| DELETE | `/api/departments/{id}` | 软删除部门（status=0） | 超管/HR |

#### 获取部门树（懒加载）

```
GET /api/departments/tree?parentId=0
```

- `parentId` 可选，默认 0=根节点（`parent_id IS NULL`）
- 展开节点时传入 `parentId={id}`，返回该节点的直接子部门
- `hasChildren` 通过 `SELECT COUNT(1) FROM departments WHERE parent_id=?` 判断

响应体：
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

#### 拖拽调整父部门

```
PUT /api/departments/{id}/move
Content-Type: application/json
{"newParentId": 5}
```

校验链：
1. 目标部门 `{id}` 存在且启用
2. `newParentId` 存在且启用
3. **循环引用检测**：`newParentId` 不能是 `{id}` 自身或其子孙节点（沿 `path` 匹配）
4. 深度限制：移动后层级 ≤ 10
5. 事务更新：`parent_id` + `level` + `path`（重新计算物化路径）

### 5.3 员工接口

| 方法 | 路径 | 用途 | 权限 |
|------|------|------|------|
| GET | `/api/employees/check?field=&value=` | 实时唯一性校验（工号/手机号） | 公开 |
| POST | `/api/employees` | 新增员工 | 超管/HR |
| GET | `/api/employees?deptId=&status=&page=&pageSize=` | 员工列表（分页+筛选） | 超管/HR/部门主管 |
| GET | `/api/employees/{id}` | 员工详情 | 超管/HR/部门主管 |
| PUT | `/api/employees/{id}` | 编辑员工信息 | 超管/HR/部门主管(受限) |
| POST | `/api/employees/{id}/transfer` | 人员调动（事务+留痕+回调） | 超管/HR |
| PUT | `/api/employees/{id}/resign` | 办理离职（逻辑删除） | 超管/HR |
| GET | `/api/employees/{id}/transfers` | 调动记录查询 | 超管/HR |

#### 实时唯一性校验

```
GET /api/employees/check?field=employeeNo&value=10086
```

响应：
```json
{"code": 200, "data": {"isExist": false}}
```

#### 新增员工

```
POST /api/employees
{
  "name": "张三",
  "employeeNo": "10086",
  "deptId": 2,
  "phone": "13800138000",
  "position": "前端开发",
  "entryDate": "2023-10-01"
}
```

校验链：
1. Pydantic Schema 校验（手机号格式、必填字段）
2. 工号唯一性 → 409
3. 手机号唯一性 → 409
4. 部门存在且启用 → 404
5. DB 唯一索引最终保底（防并发）

#### 人员调动

```
POST /api/employees/{id}/transfer
{
  "newDeptId": 3,
  "newPosition": "Java开发",
  "reason": "业务调整"
}
```

事务流程：
```
BEGIN
  1. 校验员工存在且 status=1（在职）
  2. 校验 newDeptId 存在且启用
  3. 读取当前 dept_id + position 作为快照
  4. UPDATE employees SET dept_id=?, position=? WHERE id=?
  5. INSERT INTO transfer_records (employee_id, from_dept_id, to_dept_id, ...)
COMMIT
ASYNC: HTTP POST → 审批系统（失败不阻塞，记录日志+重试队列）
```

#### 办理离职

```
PUT /api/employees/{id}/resign
{"resignDate": "2023-11-01"}
```

事务流程：
```
1. 校验员工存在且 status=1（在职）
2. UPDATE employees SET status=2, resign_date=? WHERE id=?
3. 资源释放（账号许可/登录权限）→ 预留回调接口，待外部系统对接
```

---

## 六、权限模型设计

### 6.1 角色枚举

```python
from enum import IntEnum

class Role(IntEnum):
    SUPER_ADMIN = 1   # 超管：全量 CRUD，可管理部门及所有人员
    HR = 2            # HR：管理部门 + 所有人员信息
    DEPT_MANAGER = 3  # 部门主管：仅本部门及子部门，编辑受限
```

### 6.2 鉴权机制

- **MVP 阶段**：请求头 `X-User-Id` + `X-User-Role` 注入当前用户身份（简化方案）
- **生产环境**：对接统一认证中心，替换为 JWT Token 解析
- **注入方式**：FastAPI `Depends(get_current_user)` 依赖注入

### 6.3 部门主管管辖范围校验

部门主管访问时，通过 `departments.path` 物化路径匹配验证目标资源是否在其管辖范围内：

```python
# 伪代码
if current_user.role == Role.DEPT_MANAGER:
    managed_dept = get_managed_department(current_user.id)
    if not target_dept.path.startswith(managed_dept.path):
        raise ForbiddenException("无权访问该部门数据")
```

### 6.4 权限矩阵

| 操作 | 超管 | HR | 部门主管 |
|------|:---:|:--:|:------:|
| 查看部门树 | ✅ | ✅ | ✅（仅本部门及子部门） |
| 新增/编辑/删除部门 | ✅ | ✅ | ❌ |
| 拖拽调整部门 | ✅ | ✅ | ❌ |
| 查看员工列表 | ✅ | ✅ | ✅（仅本部门及子部门） |
| 新增员工 | ✅ | ✅ | ❌ |
| 编辑员工 | ✅ | ✅ | ✅（仅职位/手机号） |
| 员工调动 | ✅ | ✅ | ❌ |
| 办理离职 | ✅ | ✅ | ❌ |

---

## 七、核心业务流程设计

### 7.1 部门树懒加载流程

```
前端                          后端
 │                             │
 │── GET /api/departments/tree（首次加载）──→
 │                             │ 查询 parent_id IS NULL 的部门
 │                             │ 对每个部门 COUNT 子节点 → hasChildren
 │←── {data: [{id, name, hasChildren, children:[]}]} ──
 │                             │
 │── 用户点击展开"研发部"（id=1）──→
 │── GET /api/departments/tree?parentId=1 ──→
 │                             │ 查询 parent_id=1 的子部门
 │                             │ 对每个子部门 COUNT 孙节点 → hasChildren
 │←── {data: [{id, name, hasChildren, children:[]}]} ──
```

### 7.2 员工新增（唯一性校验）流程

```
前端                          后端                           DB
 │                             │                             │
 │── 用户输入工号"10086" ──→   │                             │
 │── 光标移开（blur） ──→      │                             │
 │── GET /api/employees/check?field=employeeNo&value=10086 ──→
 │                             │── SELECT COUNT(1) FROM employees WHERE employee_no='10086' ──→
 │                             │←── count=0 ──────────────────────────────
 │←── {data: {isExist: false}} ──│
 │                             │                             │
 │── 用户点击提交 ──→           │                             │
 │── POST /api/employees ──→   │                             │
 │                             │── 应用层校验 deptId 存在 ──→ │
 │                             │── 应用层校验 employeeNo 唯一 ─→
 │                             │── 应用层校验 phone 唯一 ──→  │
 │                             │── INSERT INTO employees ... ──→
 │                             │  （唯一索引保底，并发冲突 → 409）│
 │←── {code:200, data:{id:1}} ──│
```

### 7.3 人员调动（级联更新与留痕）流程

```
前端                          后端                           DB              审批系统
 │                             │                             │               │
 │── POST /api/employees/{id}/transfer ──→                  │               │
 │                             │                             │               │
 │                             │── 校验员工在职 ──→           │               │
 │                             │── 校验 newDeptId 有效 ──→    │               │
 │                             │                             │               │
 │                             │────────── BEGIN TRANSACTION ──→             │
 │                             │  读取当前 dept_id + position │               │
 │                             │  UPDATE employees SET ...    │               │
 │                             │  INSERT transfer_records ... │               │
 │                             │────────── COMMIT ──────────────────→          │
 │                             │                             │               │
 │                             │── 异步 ──→ POST /api/approval/nodes/refresh ──→
 │                             │   {employeeId, oldDeptId,    │               │
 │                             │    newDeptId, newPosition}   │               │
 │                             │                             │               │
 │←── {code:200, msg:"调动成功"} ──│                           │               │
```

**关键设计**：审批回调异步执行，失败不阻塞调动主流程。失败时记录日志，通过重试队列补偿。

### 7.4 员工离职（逻辑删除）流程

```
前端                          后端                           DB
 │                             │                             │
 │── PUT /api/employees/{id}/resign ──→                     │
 │   {resignDate: "2023-11-01"}  │                             │
 │                             │── 校验员工在职（status=1）────→
 │                             │── UPDATE employees           │
 │                             │   SET status=2,               │
 │                             │       resign_date='2023-11-01'│
 │                             │   WHERE id=?                  │
 │                             │─────────────────────────────→
 │                             │                             │
 │                             │──（预留）通知账号系统释放许可 │
 │                             │──（预留）通知权限系统清除登录权限 │
 │                             │                             │
 │←── {code:200, msg:"离职办理成功"} ──│
```

---

## 八、技术选型与约束

| 层面 | 方案 | 版本约束 | 说明 |
|------|------|---------|------|
| 语言 | Python | 3.10+ | async/await 原生支持 |
| Web 框架 | FastAPI | ≥0.100 | 异步高性能，Pydantic 类型校验，自动 OpenAPI |
| ORM | SQLAlchemy (async) | ≥2.0 | FastAPI 生态首选，支持异步 |
| 迁移工具 | Alembic | ≥1.12 | 数据库版本管理 |
| 数据库 | MySQL | 8.0+ | 关系型，递归 CTE 支持 |
| 缓存 | Redis（可选） | 6.0+ | 部门树缓存（读多写少场景） |
| 审批对接 | 独立服务 HTTP 回调 | — | 异步解耦，失败不阻塞主流程 |
| 权限 | 本模块内置中间件 | — | 角色枚举 + Depends 注入 |

---

## 九、跨仓对齐点

### 9.1 当前状态

本次实施全部在 `manyu_test` 中完成，`manyu_test1` 不涉及代码变更。

### 9.2 后续对齐契约

当 `manyu_test1` 开始消费 `manyu_test` 提供的 API 时：

| 对齐项 | 提供方 | 消费方 | 契约载体 |
|--------|--------|--------|---------|
| API 接口定义 | `manyu_test` (`/openapi.json`) | `manyu_test1` | OpenAPI 3.0 JSON |
| 统一响应体 | `manyu_test` (`schemas/common.py`) | `manyu_test1` (类型定义) | `{code: int, data: T, msg: str}` |
| 错误码枚举 | `manyu_test` (`utils/exceptions.py`) | `manyu_test1` | 共享常量文件 |
| 审批回调接口 | `manyu_test` → 审批系统 | 外部审批服务 | HTTP POST |

### 9.3 审批回调接口契约（预留）

```
POST {approval_service_url}/api/approval/nodes/refresh
```

请求体：
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

## 十、风险与缓解措施

| 风险 | 等级 | 影响 | 缓解措施 | 状态 |
|------|:----:|------|---------|:----:|
| 审批系统回调接口未定义 | 🔴 高 | 调动级联更新无法完整实现 | 先实现调动+留痕，回调接口预留 + 异步解耦，失败不阻塞主流程；待审批系统接口文档后补全 | ⚠️ |
| 兼岗需求未确认 | 🟡 中 | 数据模型可能需要重构 | 当前按 N:1 设计；如需兼岗，后续加 `employee_dept` 中间表，迁移成本可控 | ⚠️ |
| 工号生成规则待定 | 🟡 中 | 员工新增表单行为不确定 | 当前手动输入，预留自动生成策略接口（`EmployeeNoGenerator` 抽象） | ⚠️ |
| 资源释放外部系统接口未知 | 🟡 中 | 离职流程中资源释放无法落地 | 离职流程解耦，释放逻辑通过事件回调预留，待外部系统对接后补全 | ⚠️ |
| 部门树深度性能 | 🟢 低 | 大量部门时查询变慢 | 物化路径 + 懒加载 + MySQL 8.0 CTE；后续可加 Redis 缓存整棵树 | ✅ |
| 并发唯一性校验 | 🟢 低 | 并发注册可能插入重复 | 应用层校验 + DB 唯一索引双保底，冲突时返回 409 | ✅ |

---

## 十一、待决策问题（非阻塞）

以下问题已在 dima.md 中记录，不影响核心架构设计，可在开发过程中逐步确认：

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

## 十二、验收标准

### 功能验收

| 需求 | 验收标准 |
|------|---------|
| 部门树加载 | `GET /api/departments/tree` 返回正确树结构，`hasChildren` 标记准确，懒加载正常 |
| 部门拖拽 | `PUT /api/departments/{id}/move` 成功变更 parent_id，循环引用被拒绝，深度超限被拒绝 |
| 员工唯一性校验 | `GET /api/employees/check` 正确返回 isExist，并发下 DB 唯一索引保底 |
| 员工新增 | `POST /api/employees` 成功写入，重复工号/手机号返回 409，无效部门返回 404 |
| 员工调动 | `POST /api/employees/{id}/transfer` 事务更新 dept_id + position，写入调动记录留痕 |
| 员工离职 | `PUT /api/employees/{id}/resign` status 变更为 2，列表按状态筛选正常，离职员工不可编辑 |
| 权限控制 | 部门主管无法访问非管辖范围数据，无法执行超管/HR 专属操作 |

### 非功能验收

- 所有 API 通过 Pydantic 入参校验
- 数据库迁移脚本可正确执行和回滚（`alembic upgrade` / `downgrade`）
- 核心接口单元测试覆盖率 ≥ 80%
- OpenAPI 文档可访问（`/docs` Swagger UI）
- 统一响应格式 `{code, data, msg}` 贯穿所有接口

---

> 文档版本：v1.0 | 作者：DTCoder | 状态：系分完成，可进入实施阶段 (Phase 1)

---

## 附录 A：与前置文档的承继关系

| 前置文档 | 版本 | 本设计文档引用/落地内容 |
|---------|------|----------------------|
| dima.md | v1.1 | 领域模型、API 契约、技术选型决策、待决策问题 |
| plan.md | v1.0 | 项目结构、数据模型 DDL、API 详细设计、权限模型、实施阶段 |

## 附录 B：变更记录

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|---------|------|
| v1.0 | 2026-08-25 | 初始版本，系统分析设计文档 | DTCoder |