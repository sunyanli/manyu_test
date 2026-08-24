# 人员看板 — 需求澄清与设计文档

> 日期: 2026-08-24
> 状态: 需求澄清完成 | 待实现
> 技术栈: Python 后端（FastAPI）+ 独立前端（Vue 3）+ SQLite

---

## 1. 需求总览

开发一个**人员看板系统**，核心功能包括：

| 功能模块 | 说明 |
|---------|------|
| 员工基本信息管理 | 记录员工基本信息的入口，支持增删改查（CRUD） |
| 数据导入 | 支持单条录入和批量导入（如 CSV/Excel） |
| 成本预算管理 | 记录员工相关成本预算，支持白名单机制 |
| 白名单 | 控制哪些员工/部门可导入或可进行成本预算操作 |

---

## 2. 跨仓架构

```
┌─────────────────────────────────┐
│  manyu_test1 (前端)              │
│  Vue 3 + Vite + Element Plus     │
│  ┌───────────────────────────┐   │
│  │ 员工看板页面               │   │
│  │  - 员工列表/CRUD           │   │
│  │  - 导入页面                │   │
│  │  - 成本预算页面            │   │
│  │  - 白名单管理页面          │   │
│  └──────────┬────────────────┘   │
└─────────────┼───────────────────┘
              │ HTTP REST API
              ▼
┌─────────────────────────────────┐
│  manyu_test (后端)               │
│  FastAPI + SQLAlchemy + SQLite   │
│  ┌───────────────────────────┐   │
│  │ API 层                     │   │
│  │  - /api/employees/*       │   │
│  │  - /api/import/*          │   │
│  │  - /api/budget/*          │   │
│  │  - /api/whitelist/*       │   │
│  ├───────────────────────────┤   │
│  │ 数据库层 (SQLite)          │   │
│  │  - employees 表            │   │
│  │  - budgets 表              │   │
│  │  - whitelist 表            │   │
│  └───────────────────────────┘   │
└─────────────────────────────────┘
```

### 仓间对齐点

| 对齐点 | 后端 (manyu_test) | 前端 (manyu_test1) |
|--------|------------------|-------------------|
| 员工 CRUD | `GET/POST/PUT/DELETE /api/employees` | EmployeeList.vue, EmployeeForm.vue |
| 批量导入 | `POST /api/import/employees` (CSV/Excel) | ImportDialog.vue |
| 成本预算 | `GET/POST/PUT/DELETE /api/budgets` | BudgetList.vue |
| 白名单 | `GET/POST/DELETE /api/whitelist` | WhitelistManager.vue |

---

## 3. 数据模型设计

### 3.1 员工表 (employees)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER (PK) | 自增主键 |
| employee_id | VARCHAR(32) UNIQUE | 员工工号 |
| name | VARCHAR(64) | 姓名 |
| department | VARCHAR(128) | 部门 |
| position | VARCHAR(128) | 职位 |
| phone | VARCHAR(20) | 手机号 |
| email | VARCHAR(128) | 邮箱 |
| hire_date | DATE | 入职日期 |
| status | VARCHAR(16) | 在职/离职 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### 3.2 成本预算表 (budgets)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER (PK) | 自增主键 |
| employee_id | VARCHAR(32) FK | 关联员工工号 |
| budget_year | INTEGER | 预算年份 |
| budget_amount | DECIMAL(12,2) | 预算金额 |
| actual_amount | DECIMAL(12,2) | 实际支出 |
| description | TEXT | 预算说明 |
| created_at | DATETIME | 创建时间 |

### 3.3 白名单表 (whitelist)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INTEGER (PK) | 自增主键 |
| employee_id | VARCHAR(32) UNIQUE | 员工工号（NULL 表示部门级白名单） |
| department | VARCHAR(128) | 部门名 |
| whitelist_type | VARCHAR(16) | import / budget / all |
| enabled | BOOLEAN | 是否启用 |
| created_at | DATETIME | 创建时间 |

---

## 4. API 接口设计

### 员工管理
- `GET    /api/employees` — 员工列表（分页+搜索）
- `GET    /api/employees/{id}` — 员工详情
- `POST   /api/employees` — 新增员工
- `PUT    /api/employees/{id}` — 编辑员工
- `DELETE /api/employees/{id}` — 删除员工

### 批量导入
- `POST   /api/import/employees` — 批量导入（接收 CSV/Excel）
- `GET    /api/import/template` — 下载导入模板

### 成本预算
- `GET    /api/budgets` — 预算列表
- `POST   /api/budgets` — 新增预算
- `PUT    /api/budgets/{id}` — 编辑预算
- `DELETE /api/budgets/{id}` — 删除预算

### 白名单
- `GET    /api/whitelist` — 白名单列表
- `POST   /api/whitelist` — 新增白名单条目
- `DELETE /api/whitelist/{id}` — 删除白名单条目

---

## 5. 需求澄清已确认项

| 问题 | 结论 |
|------|------|
| 技术栈 | Python 后端 + 独立前端（前后端分离） |
| 后端框架 | FastAPI（默认推荐） |
| 前端框架 | Vue 3 + Element Plus（默认推荐） |
| 数据库 | SQLite（默认推荐，可后续迁移） |
| 仓库分配 | manyu_test → 后端, manyu_test1 → 前端 |

### 待后续确认项（可迭代中细化）
- 员工基本信息具体字段列表
- 导入文件格式（CSV/Excel/两者皆支持）
- 成本预算的审批流程
- 白名单的权限层级
- 前端 UI 风格偏好

---

## 6. 后续实施计划

1. **后端（manyu_test）**：搭建 FastAPI 项目结构 → 数据模型 → CRUD API → 导入 API → 白名单 API
2. **前端（manyu_test1）**：搭建 Vue3 项目 → 员工列表/表单 → 导入页面 → 预算页面 → 白名单管理
3. **联调**：前后端对接测试
4. **验收**：全流程功能验证