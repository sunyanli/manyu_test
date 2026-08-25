# 组织架构管理模块 — 实施计划

> 生成时间：2026-08-25
> 任务阶段：loop-1 / 实施计划
> 前置文档：dima.md（需求澄清）
> 关联任务：DEV-9d10e310-7901-11f1-8a9f-59ecae612580-c975ca68-86c4-4ca4-950f-53bed3ea6224

---

## 1. 概述

### 1.1 项目目标
构建组织架构管理模块，作为组织主数据源，提供部门树形结构管理与员工全生命周期管理（入职 → 调动 → 离职），为审批、权限等下游系统提供准确的人员数据源。

### 1.2 交付范围（本期）
| 需求编号 | 功能 | 优先级 |
|----------|------|--------|
| 需求1 | 部门树形结构加载与交互（含懒加载、拖拽调整层级） | P0 |
| 需求2 | 员工新增（唯一性校验与防重） | P0 |
| 需求3 | 人员调动（级联更新与快照） | P0 |
| 需求4 | 员工离职（逻辑删除与状态隔离） | P0 |

### 1.3 仓库分工

```
[manyu_test] 组织架构核心服务
  ├── 部门 CRUD + 树形查询
  ├── 员工 CRUD + 唯一性校验
  ├── 调动 / 离职业务逻辑
  ├── 数据库模型（departments / employees / transfer_records）
  └── 事件发布（employee.transferred / employee.resigned）

[manyu_test1] 下游消费模块（后续迭代）
  └── 审批流节点变更 / 权限回收（消费 manyu_test 发布的事件）
```

---

## 2. 技术决策

基于 DIMA 需求澄清阶段的分析建议，以下决策已定稿：

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 后端框架 | Spring Boot 3.x + Java 17 | 企业级主流选型，生态成熟 |
| 数据库 | MySQL 8.0 | 通用关系型，支持递归CTE |
| 树存储方案 | 邻接表 + parent_id + sort_order | 实现简单，递归CTE查完整树，懒加载按 parent_id 查 |
| ORM | MyBatis-Plus / JPA | 待细化（建议 MyBatis-Plus，灵活控制 SQL） |
| 员工状态 | 枚举字段 status: ACTIVE / RESIGNED / SUSPENDED | 语义清晰，扩展性好 |
| 唯一性校验 | 数据库唯一索引 + 应用层预校验 | 双保险防并发 |
| 调动事件通知 | 应用层事件 + 异步处理（Spring Event / MQ） | 解耦审批模块 |
| 权限模型 | RBAC + 数据级部门过滤 | 接口级角色控制，SQL 层 dept_id 过滤 |
| 认证方式 | JWT（Bearer Token） | 无状态，适合微服务 |
| API 响应格式 | `{ "code": 200, "data": ..., "msg": "..." }` | 统一规范 |
| 分页 | page/pageSize（默认 page=1, pageSize=20） | 常规分页方案 |

---

## 3. 数据库设计

### 3.1 表结构

#### departments（部门表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 部门ID |
| name | VARCHAR(100) | NOT NULL | 部门名称 |
| parent_id | BIGINT | NULLABLE, FK→departments.id | 父部门ID；NULL 表示根部门 |
| sort_order | INT | DEFAULT 0 | 同级排序权重 |
| status | VARCHAR(20) | DEFAULT 'ACTIVE' | ACTIVE / DISABLED |
| created_at | DATETIME | DEFAULT NOW() | 创建时间 |
| updated_at | DATETIME | ON UPDATE NOW() | 更新时间 |

索引：
- `idx_parent_id` ON (parent_id)
- `idx_status` ON (status)

#### employees（员工表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 员工ID |
| name | VARCHAR(50) | NOT NULL | 姓名 |
| employee_no | VARCHAR(30) | NOT NULL, UNIQUE | 工号 |
| phone | VARCHAR(20) | UNIQUE | 手机号 |
| dept_id | BIGINT | NOT NULL, FK→departments.id | 所属部门 |
| position | VARCHAR(100) | | 职位 |
| status | VARCHAR(20) | DEFAULT 'ACTIVE' | ACTIVE / RESIGNED / SUSPENDED |
| entry_date | DATE | | 入职日期 |
| resign_date | DATE | | 离职日期 |
| created_at | DATETIME | DEFAULT NOW() | 创建时间 |
| updated_at | DATETIME | ON UPDATE NOW() | 更新时间 |

索引：
- `uk_employee_no` UNIQUE (employee_no)
- `uk_phone` UNIQUE (phone)
- `idx_dept_id` ON (dept_id)
- `idx_status` ON (status)

#### transfer_records（调动记录表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 记录ID |
| employee_id | BIGINT | NOT NULL, FK→employees.id | 员工ID |
| from_dept_id | BIGINT | FK→departments.id | 原部门 |
| to_dept_id | BIGINT | NOT NULL, FK→departments.id | 目标部门 |
| from_position | VARCHAR(100) | | 原职位 |
| to_position | VARCHAR(100) | | 新职位 |
| reason | VARCHAR(500) | | 调动原因 |
| operator_id | BIGINT | | 操作人ID |
| transfer_time | DATETIME | DEFAULT NOW() | 调动时间 |

索引：
- `idx_employee_id` ON (employee_id)
- `idx_transfer_time` ON (transfer_time)

### 3.2 ER 关系

```
departments 1 ──── N employees
     │
     └── parent_id (self-referencing)
     
employees 1 ──── N transfer_records
```

---

## 4. API 设计

### 4.1 部门接口

#### 4.1.1 获取部门树（完整）
```
GET /api/departments/tree
Response: {
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "研发部",
      "parentId": null,
      "sortOrder": 0,
      "status": "ACTIVE",
      "children": [
        { "id": 2, "name": "前端组", "parentId": 1, "sortOrder": 0, "status": "ACTIVE", "children": [] }
      ]
    }
  ]
}
```

#### 4.1.2 获取子部门（懒加载）
```
GET /api/departments/{id}/children
Response: {
  "code": 200,
  "data": [
    { "id": 2, "name": "前端组", "parentId": 1, "sortOrder": 0, "status": "ACTIVE" }
  ]
}
```

#### 4.1.3 拖拽调整父节点
```
PUT /api/departments/{id}/move
Request:  { "newParentId": 5, "sortOrder": 1 }
Response: { "code": 200, "msg": "移动成功" }
```

校验规则：
- newParentId 必须存在且状态为 ACTIVE
- 禁止将节点移动到自身或其子孙节点下（防止循环引用）
- 事务内完成 parent_id 和 sort_order 更新

#### 4.1.4 新增部门
```
POST /api/departments
Request:  { "name": "测试组", "parentId": 1, "sortOrder": 0 }
Response: { "code": 200, "data": { "id": 10, "name": "测试组", ... } }
```

#### 4.1.5 编辑部门
```
PUT /api/departments/{id}
Request:  { "name": "新名称", "sortOrder": 2 }
Response: { "code": 200, "msg": "更新成功" }
```

#### 4.1.6 停用/启用部门
```
PUT /api/departments/{id}/status
Request:  { "status": "DISABLED" }
Response: { "code": 200, "msg": "状态更新成功" }
```

### 4.2 员工接口

#### 4.2.1 唯一性校验
```
GET /api/employees/check?field=employeeNo&value=10086
GET /api/employees/check?field=phone&value=13800138000
Response: { "code": 200, "data": { "isExist": false } }
```

支持的 field 值：`employeeNo`、`phone`

#### 4.2.2 新增员工
```
POST /api/employees
Request: {
  "name": "张三",
  "employeeNo": "10086",
  "deptId": 2,
  "phone": "13800138000",
  "position": "前端工程师",
  "entryDate": "2023-10-01"
}
Response: { "code": 200, "data": { "id": 100, ... } }
```

校验规则：
- employeeNo 全局唯一（应用层 + 数据库唯一索引）
- phone 全局唯一
- deptId 必须存在且状态为 ACTIVE
- name, employeeNo, deptId 为必填字段

#### 4.2.3 员工详情
```
GET /api/employees/{id}
Response: { "code": 200, "data": { "id": 100, "name": "张三", "deptName": "前端组", ... } }
```

#### 4.2.4 员工列表（按部门/状态筛选）
```
GET /api/employees?deptId=2&status=ACTIVE&page=1&pageSize=20
Response: {
  "code": 200,
  "data": {
    "total": 50,
    "page": 1,
    "pageSize": 20,
    "list": [ ... ]
  }
}
```

#### 4.2.5 编辑员工
```
PUT /api/employees/{id}
Request: { "name": "张三丰", "phone": "13900139000", "position": "高级前端" }
Response: { "code": 200, "msg": "更新成功" }
```

权限约束：部门主管仅可编辑本部门及子部门员工的部分信息（name、phone、position），不可编辑 employeeNo、deptId。

#### 4.2.6 人员调动
```
POST /api/employees/{id}/transfer
Request: {
  "newDeptId": 3,
  "newPosition": "Java开发",
  "reason": "业务调整"
}
Response: { "code": 200, "msg": "调动成功" }
```

处理流程：
1. 校验 newDeptId 存在且状态为 ACTIVE
2. 校验员工当前状态为 ACTIVE
3. 更新 employees.dept_id、employees.position
4. 写入 transfer_records 调动历史
5. 发布 `employee.transferred` 事件（异步通知审批模块）

#### 4.2.7 员工离职
```
PUT /api/employees/{id}/resign
Request:  { "resignDate": "2023-11-01" }
Response: { "code": 200, "msg": "离职办理成功" }
```

处理流程：
1. 校验员工当前状态为 ACTIVE
2. 更新 status = 'RESIGNED'，记录 resign_date
3. 发布 `employee.resigned` 事件（触发权限回收、账号许可释放）

---

## 5. 项目结构

### 5.1 manyu_test 包结构（Spring Boot）

```
com.example.org
├── OrgApplication.java                    # 启动类
├── config/
│   ├── WebConfig.java                     # CORS / 拦截器配置
│   └── SecurityConfig.java                # JWT 鉴权配置
├── controller/
│   ├── DepartmentController.java          # 部门接口
│   └── EmployeeController.java            # 员工接口
├── service/
│   ├── DepartmentService.java
│   ├── EmployeeService.java
│   └── TransferService.java
├── repository/
│   ├── DepartmentRepository.java
│   ├── EmployeeRepository.java
│   └── TransferRecordRepository.java
├── model/
│   ├── entity/
│   │   ├── Department.java
│   │   ├── Employee.java
│   │   └── TransferRecord.java
│   ├── dto/
│   │   ├── DepartmentTreeDTO.java
│   │   ├── EmployeeCreateDTO.java
│   │   ├── TransferRequestDTO.java
│   │   └── ResignRequestDTO.java
│   └── enums/
│       ├── EmployeeStatus.java
│       └── DepartmentStatus.java
├── event/
│   ├── EmployeeTransferredEvent.java
│   ├── EmployeeResignedEvent.java
│   └── OrgEventListener.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   └── BusinessException.java
└── common/
    ├── ApiResponse.java                   # 统一响应体
    └── PageResult.java                    # 分页结果
```

### 5.2 manyu_test1（后续迭代，本次仅预留接口契约）

```
com.example.approval
├── event/
│   └── OrgEventConsumer.java              # 消费 manyu_test 事件
└── ...
```

---

## 6. 实施步骤与里程碑

### Phase 1：基础设施搭建（预计 1-2 天）

| 步骤 | 内容 | 仓库 |
|------|------|------|
| 1.1 | 初始化 Spring Boot 项目骨架（manyu_test） | manyu_test |
| 1.2 | 配置 MySQL 数据源、MyBatis-Plus/JPA | manyu_test |
| 1.3 | 创建数据库迁移脚本（departments / employees / transfer_records 三张表） | manyu_test |
| 1.4 | 统一响应体 `ApiResponse` + 全局异常处理 | manyu_test |
| 1.5 | JWT 鉴权中间件 + RBAC 角色定义 | manyu_test |

### Phase 2：部门管理（预计 2-3 天）

| 步骤 | 内容 | 仓库 |
|------|------|------|
| 2.1 | Department 实体 + Repository | manyu_test |
| 2.2 | `GET /api/departments/tree` — 递归CTE完整树查询 | manyu_test |
| 2.3 | `GET /api/departments/{id}/children` — 懒加载子节点 | manyu_test |
| 2.4 | `POST /api/departments` — 新增部门 | manyu_test |
| 2.5 | `PUT /api/departments/{id}` — 编辑部门 | manyu_test |
| 2.6 | `PUT /api/departments/{id}/move` — 拖拽移动（含循环引用校验） | manyu_test |
| 2.7 | `PUT /api/departments/{id}/status` — 停用/启用 | manyu_test |
| 2.8 | 单元测试 + 集成测试 | manyu_test |

### Phase 3：员工管理（预计 2-3 天）

| 步骤 | 内容 | 仓库 |
|------|------|------|
| 3.1 | Employee 实体 + Repository | manyu_test |
| 3.2 | `GET /api/employees/check` — 唯一性校验 | manyu_test |
| 3.3 | `POST /api/employees` — 新增员工（含双重唯一性校验） | manyu_test |
| 3.4 | `GET /api/employees/{id}` — 员工详情 | manyu_test |
| 3.5 | `GET /api/employees` — 分页列表（按部门/状态筛选） | manyu_test |
| 3.6 | `PUT /api/employees/{id}` — 编辑员工（含部门主管权限边界） | manyu_test |
| 3.7 | 单元测试 + 集成测试 | manyu_test |

### Phase 4：调动与离职（预计 2 天）

| 步骤 | 内容 | 仓库 |
|------|------|------|
| 4.1 | TransferRecord 实体 + Repository | manyu_test |
| 4.2 | `POST /api/employees/{id}/transfer` — 调动（含级联留痕） | manyu_test |
| 4.3 | `PUT /api/employees/{id}/resign` — 离职（逻辑删除） | manyu_test |
| 4.4 | 事件发布机制：EmployeeTransferredEvent / EmployeeResignedEvent | manyu_test |
| 4.5 | 单元测试 + 集成测试 | manyu_test |

### Phase 5：权限与数据隔离（预计 1-2 天）

| 步骤 | 内容 | 仓库 |
|------|------|------|
| 5.1 | 超管/HR 角色：全量数据访问 | manyu_test |
| 5.2 | 部门主管角色：递归子部门数据过滤（SQL 层 dept_id IN 子查询） | manyu_test |
| 5.3 | 接口级权限注解（@PreAuthorize） | manyu_test |
| 5.4 | 操作审计日志 | manyu_test |

### Phase 6：联调与文档（预计 1 天）

| 步骤 | 内容 | 仓库 |
|------|------|------|
| 6.1 | Swagger/OpenAPI 文档生成 | manyu_test |
| 6.2 | manyu_test1 预留事件消费桩代码 | manyu_test1 |
| 6.3 | 跨仓契约对齐验证 | manyu_test + manyu_test1 |

---

## 7. 跨仓契约对齐点

### 7.1 manyu_test → manyu_test1 接口契约

| 方向 | 契约 | 类型 | 说明 |
|------|------|------|------|
| manyu_test → manyu_test1 | `GET /api/departments/tree` | REST | 审批模块获取组织架构 |
| manyu_test → manyu_test1 | `GET /api/employees/{id}` | REST | 审批模块获取员工信息 |
| manyu_test → manyu_test1 | `EmployeeTransferredEvent` | 事件 | 员工调动 → 审批流节点变更 |
| manyu_test → manyu_test1 | `EmployeeResignedEvent` | 事件 | 员工离职 → 权限回收 |

### 7.2 事件结构定义

```json
// EmployeeTransferredEvent
{
  "eventType": "employee.transferred",
  "employeeId": 100,
  "fromDeptId": 2,
  "toDeptId": 3,
  "fromPosition": "前端工程师",
  "toPosition": "Java开发",
  "transferTime": "2023-10-15T10:30:00",
  "operatorId": 1
}

// EmployeeResignedEvent
{
  "eventType": "employee.resigned",
  "employeeId": 100,
  "resignDate": "2023-11-01",
  "operatorId": 1
}
```

---

## 8. 风险与依赖

| 风险/依赖 | 影响 | 缓解措施 |
|-----------|------|----------|
| 数据库选型未最终确认 | 阻塞 Phase 1 | 默认 MySQL 8.0，若更换则调整 SQL 方言 |
| 外部 IAM/SSO 接口契约不明确 | 离职时"清除登录权限"无法对接 | Phase 4 先发布事件，下游对接后续迭代 |
| 审批模块跨仓依赖尚未就绪 | 调动级联更新无法端到端验证 | 事件发布后 manyu_test1 预留消费者桩 |
| 部门主管可编辑字段范围未敲定 | 权限边界模糊 | 默认：name / phone / position 可编辑；employeeNo / deptId 不可编辑 |
| 数据规模不明确 | 树查询性能风险 | 邻接表 + 递归CTE 在万级部门下可接受；大规模时加 Redis 缓存 |
| 并发拖拽部门冲突 | 树结构不一致 | Phase 2 考虑乐观锁（version 字段） |

---

## 9. 待澄清问题（阻塞实施）

以下问题在 DIMA 阶段标记为 P0/P1，需在实施前确认：

| 优先级 | 问题 | 默认假设（若不确认则按此执行） |
|--------|------|-------------------------------|
| P0 | 懒加载端点：`/tree` 返回完整树还是仅顶层？ | `/tree` 返回完整树，`/{id}/children` 支持懒加载 |
| P0 | 允许多个顶级部门（森林）还是单根？ | 允许多个顶级部门（parent_id IS NULL） |
| P0 | 员工新增必填字段？ | name, employeeNo, deptId 必填；phone, position, entryDate 可选 |
| P0 | 工号生成规则：手动还是自动？ | 手动输入（前端录入），后端校验唯一性 |
| P1 | 员工状态枚举：仅 active/resigned 还是更多？ | ACTIVE / RESIGNED / SUSPENDED |
| P1 | 离职后是否支持复职？ | 本期不支持，后续迭代 |
| P1 | 调动是否需要审批？ | 本期直接操作，无需审批流 |

---

## 10. 验收标准 (Acceptance Criteria)

### 10.1 需求逐项验收

| 需求 | 验收项 | 验证方式 |
|------|--------|----------|
| 需求1-部门树 | `GET /api/departments/tree` 返回正确树形 JSON；`/{id}/children` 懒加载可用；`/{id}/move` 拖拽禁止循环引用 | 集成测试 + 手动 API 调用 |
| 需求2-员工新增 | `GET /api/employees/check` 唯一性校验正确；`POST /api/employees` 并发插入被唯一索引拒绝 | 单元测试 + 并发测试 |
| 需求3-人员调动 | `POST /api/employees/{id}/transfer` 更新 dept_id + 写入 transfer_records + 发布事件 | 集成测试 + 事件断言 |
| 需求4-员工离职 | `PUT /api/employees/{id}/resign` 仅改 status，不物理删除；历史数据保留 | 集成测试 + 数据完整性检查 |

### 10.2 跨仓契约验收

| 契约方向 | 验收项 | 验证方式 |
|----------|--------|----------|
| manyu_test → manyu_test1 (REST) | 响应体格式 `{code, data, msg}` 一致；分页格式 `{total, page, pageSize, list}` 一致 | API 文档对比 |
| manyu_test → manyu_test1 (Event) | `EmployeeTransferredEvent` / `EmployeeResignedEvent` 字段完整且 manyu_test1 预留消费者桩可反序列化 | 事件 schema 校验 |

### 10.3 非功能验收

- 部门树递归CTE在1000节点内响应 < 200ms
- 唯一性校验接口响应 < 50ms
- 调动/离职事务内完成，无部分成功状态

---

## 11. DIMA 问题闭环追溯

以下将 DIMA 阶段标记的 P0/P1 问题与 plan 中的默认假设建立闭环：

| DIMA 问题 | 优先级 | Plan 默认假设（§9） | 闭环状态 |
|-----------|--------|---------------------|----------|
| Q-D1 懒加载端点 | P0 | `/tree` 完整树 + `/{id}/children` 懒加载 | ✅ §4.1.1/4.1.2 |
| Q-D4 根部门数量 | P0 | 允许多顶级部门（森林） | ✅ §9 |
| Q-E3 必填字段 | P0 | name, employeeNo, deptId 必填 | ✅ §4.2.2 |
| Q-E4 工号规则 | P0 | 手动输入，后端校验唯一性 | ✅ §9 |
| Q-CC4 数据库 | P0 | MySQL 8.0 + 邻接表 | ✅ §2 |
| Q-T1 级联审批 | P1 | 异步事件通知，manyu_test1 预留消费者 | ✅ §4.2.6, §7.2 |
| Q-T2 调动记录字段 | P1 | from/to dept/position + reason + operator + time | ✅ §3.1 transfer_records |
| Q-RS1 状态枚举 | P1 | ACTIVE / RESIGNED / SUSPENDED | ✅ §2 |
| Q-RS4 复职 | P1 | 本期不支持 | ✅ §9 |
| Q-CC1 认证 | P1 | JWT Bearer Token | ✅ §2 |
| Q-CC2 权限粒度 | P1 | RBAC + SQL 层 dept_id 过滤 | ✅ §2, §5.1 |

> 未闭环的 P2 问题（Q-D3 排序、Q-D5 停用展示、Q-E5 批量导入、Q-RS6 GDPR）已纳入后续迭代范围，不阻塞本期交付。

---

## 12. 下一步

1. **确认待澄清问题**（第9节），定稿默认假设或修正
2. **启动 Phase 1**：初始化 manyu_test Spring Boot 项目骨架
3. **产出**：数据库 DDL + 核心实体类 + API 接口定义
4. **进入下一阶段**：loop-2 / 概要设计（详细代码实现）

---

> 文档所有者：DTCoder
> 关联任务：DEV-9d10e310-7901-11f1-8a9f-59ecae612580-c975ca68-86c4-4ca4-950f-53bed3ea6224
> 前置文档：dima.md（需求澄清）
> DIMA 闭环：30 个澄清问题中 P0/P1 共 11 个，已全部在本计划中以默认假设闭环