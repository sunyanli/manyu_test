# 代码评审报告 — 组织架构管理模块

> 生成时间：2026-08-25
> 任务阶段：loop-2 / 代码评审
> 关联任务：DEV-9d10e310-7901-11f1-8a9f-59ecae612580-c975ca68-86c4-4ca4-950f-53bed3ea6224
> 评审范围：manyu_test (34 Java + 3 config/SQL) + manyu_test1 (1 桩文件)

---

## 1. 评审概要

| 维度 | 结果 |
|------|------|
| 架构一致性 | ✅ 与 plan.md 高度一致 |
| API 契约完整性 | ✅ 所有端点均已实现 |
| 数据库设计 | ✅ DDL 与 plan §3.1 一致 |
| 异常处理 | 🔴 缺失 DataIntegrityViolationException 处理 |
| 事务边界 | 🔴 EmployeeService.create() 缺少 @Transactional |
| 分页功能 | 🔴 MyBatisPlusInterceptor 未配置，分页失效 |
| 数据校验 | 🔴 DepartmentService.updateStatus() 无枚举校验 |
| 跨仓契约 | ⚠️ 分页格式一致但功能失效；事件字段一致 |
| 代码质量 | 🟢 结构清晰，分层合理 |

---

## 2. Blocker 级别问题（共 5 项）

### 🔴 Blocker #1: EmployeeService.update() 缺少手机号唯一性校验

- **文件**: `src/main/java/com/example/org/service/EmployeeService.java` L123-142
- **问题**: `update()` 允许修改 phone 字段，但未校验新手机号是否已被其他员工占用。若更新为已存在的手机号，数据库 `uk_phone` 唯一索引将抛出 `DataIntegrityViolationException`，用户收到 500 错误。
- **对比**: `create()` 方法 L58-61 有正确的 phone 唯一性校验，但 `update()` 遗漏。
- **修复建议**: 在 `update()` 中，当 `dto.getPhone() != null` 时，先查询是否有其他员工占用该手机号（排除当前员工 ID），若占用则抛出 `BusinessException(400, "手机号已被其他员工使用")`。

### 🔴 Blocker #2: 缺少 DataIntegrityViolationException 全局异常处理

- **文件**: `src/main/java/com/example/org/exception/GlobalExceptionHandler.java`
- **问题**: 当数据库唯一约束冲突（`DataIntegrityViolationException` / `DuplicateKeyException`）发生时，用户收到的是原始 500 错误而非语义化的 400 提示。这与 plan.md 中 "数据库唯一索引保底" 的设计意图矛盾——保底异常应转为友好提示。
- **修复建议**: 在 `GlobalExceptionHandler` 中添加 `@ExceptionHandler(DataIntegrityViolationException.class)`，解析异常消息中的约束名，返回 `ApiResponse.error(400, "数据已存在/冲突")`。

### 🔴 Blocker #3: DepartmentService.updateStatus() 未校验 status 值合法性

- **文件**: `src/main/java/com/example/org/service/DepartmentService.java` L118-125
- **问题**: `updateStatus()` 直接接受任意字符串作为 status 值，未校验是否为 `DepartmentStatus` 枚举（ACTIVE / DISABLED）的有效值。可写入无效值导致数据污染。
- **修复建议**: 在 `updateStatus()` 中校验 `dto.getStatus()` 是否为 `DepartmentStatus` 枚举的有效值，无效则抛出 `BusinessException(400, "无效的状态值")`。

### 🔴 Blocker #4: MyBatis-Plus 分页插件未配置

- **文件**: 全局配置缺失（需新增 MyBatis-Plus 配置类）
- **问题**: `EmployeeService.list()` 使用 `selectPage()` 进行分页查询，但项目中未注册 `MybatisPlusInterceptor` Bean（需包含 `PaginationInnerInterceptor`）。MyBatis-Plus 在没有分页插件时，`selectPage` 会忽略分页参数并返回全量数据。`GET /api/employees?page=1&pageSize=20` 实际返回全部员工。
- **修复建议**: 创建配置类，注册 `MybatisPlusInterceptor` 并添加 `PaginationInnerInterceptor(DbType.MYSQL)`。

### 🔴 Blocker #5: EmployeeService.create() 缺少事务边界

- **文件**: `src/main/java/com/example/org/service/EmployeeService.java` L47-73
- **问题**: `create()` 执行"检查唯一性 → 插入"两步操作，但未标注 `@Transactional`。在并发场景下，两个请求可能同时通过唯一性检查，然后先后插入——数据库唯一索引虽然能阻止第二个插入，但此时用户会收到 500 而非友好的 400 错误。与 plan.md 的 "双保险防并发" 设计意图矛盾。
- **修复建议**: 在 `create()` 方法上添加 `@Transactional` 注解。

---

## 3. Major 级别问题（共 3 项）

### 🟡 Major #1: selectFullTree() 递归 CTE 仅返回 ACTIVE 部门

- **文件**: `src/main/java/com/example/org/repository/DepartmentRepository.java` L13-22
- **问题**: CTE 的 anchor 和 recursive 部分均过滤 `status = 'ACTIVE'`。DISABLED 父部门下的 ACTIVE 子部门也一并消失（因为父部门不在结果集中，递归无法触达其子节点）。若业务上需要"停用父部门但保留子部门可见"或"灰显停用部门"，当前实现不足。
- **修复建议**: 移除 CTE 中的 `status = 'ACTIVE'` 过滤，改为在 `buildTree()` 或 `DepartmentTreeDTO` 中标记 status 字段，由前端根据 status 决定展示方式。

### 🟡 Major #2: application.yml 配置了无效的 logic-delete-field

- **文件**: `src/main/resources/application.yml` L24-26
- **问题**: `logic-delete-field: deleted` 配置了一个所有实体都不存在的字段。所有实体使用显式 `status` 字段进行逻辑删除。此配置是误导性残留。
- **修复建议**: 移除 `logic-delete-field`、`logic-delete-value`、`logic-not-delete-value` 配置，或在实体中添加 `deleted` 字段并统一使用 MyBatis-Plus 逻辑删除。

### 🟡 Major #3: EmployeeService.checkUnique() 包含已离职员工

- **文件**: `src/main/java/com/example/org/repository/EmployeeRepository.java` L12-16
- **问题**: `countByEmployeeNo` 和 `countByPhone` 查询全表，未排除 `status = 'RESIGNED'` 的员工。离职员工的工号和手机号永远无法被复用。
- **修复建议**: 如需支持离职员工信息复用，在 SQL 中添加 `AND status != 'RESIGNED'` 条件。

---

## 4. Minor 级别问题（共 5 项）

### 🟢 Minor #1: buildTree() 使用 O(n²) 算法

- **文件**: `src/main/java/com/example/org/service/DepartmentService.java` L144-161
- **建议**: 通过一次 `Map<Long, List<Department>>` 分组优化为 O(n)。

### 🟢 Minor #2: getChildren() 无分页

- **文件**: `src/main/java/com/example/org/service/DepartmentService.java` L40-46
- **建议**: 大数据量时考虑添加分页参数。

### 🟢 Minor #3: collectDescendantIds() 产生 N+1 查询

- **文件**: `src/main/java/com/example/org/service/DepartmentService.java` L130-139
- **建议**: 使用一次 CTE 查询获取所有后代 ID。

### 🟢 Minor #4: TransferService 事件发布在事务内

- **文件**: `src/main/java/com/example/org/service/TransferService.java` L66-74, L93-97
- **建议**: 使用 `@TransactionalEventListener(phase = AFTER_COMMIT)` 替代 `@EventListener`，确保事务提交后再消费事件。

### 🟢 Minor #5: manyu_test1 OrgEventConsumer 使用原始参数

- **文件**: `[manyu_test1] src/main/java/com/example/approval/event/OrgEventConsumer.java`
- **建议**: 后续集成时改为接收 `EmployeeTransferredEvent` / `EmployeeResignedEvent` 对象。

---

## 5. 跨仓契约对齐

| 契约方向 | 状态 | 说明 |
|----------|------|------|
| REST API 响应格式 `{code, data, msg}` | ✅ 一致 | ApiResponse 与 plan 完全匹配 |
| 分页格式 `{total, page, pageSize, list}` | ⚠️ 功能失效 | Blocker #4：缺少 MyBatisPlusInterceptor |
| `EmployeeTransferredEvent` 字段 | ✅ 一致 | 与 plan §7.2 完全匹配 |
| `EmployeeResignedEvent` 字段 | ✅ 一致 | 与 plan §7.2 完全匹配 |
| manyu_test1 消费桩 | ⚠️ 签名不匹配 | Minor #5：使用原始参数而非事件对象 |

---

## 6. 验收标准逐项检查

| 需求 | 验收项 | 结果 |
|------|--------|------|
| 需求1-部门树 | `GET /api/departments/tree` 返回正确树形 JSON | ✅ 通过 |
| 需求1-懒加载 | `GET /api/departments/{id}/children` 可用 | ✅ 通过 |
| 需求1-拖拽 | `PUT /api/departments/{id}/move` 防循环引用 | ✅ 通过 |
| 需求2-唯一性校验 | `GET /api/employees/check` 正确 | ✅ 通过 |
| 需求2-新增 | `POST /api/employees` 双重校验 | ⚠️ 事务边界缺失 |
| 需求3-调动 | `POST /api/employees/{id}/transfer` 三合一 | ✅ 通过 |
| 需求4-离职 | `PUT /api/employees/{id}/resign` 逻辑删除 | ✅ 通过 |

---

## 7. 评审结论

- **整体质量**: 良好。代码结构清晰，与设计文档高度一致，API 接口完整。
- **Blocker 数量**: 5 项，建议在进入下一阶段前全部修复。
- **可进入下一阶段的条件**: 修复全部 5 项 Blocker，特别是 #4（分页功能失效）和 #2（异常处理缺失）直接影响用户体验。

---

> 评审人：DTCoder
> 关联任务：DEV-9d10e310-7901-11f1-8a9f-59ecae612580-c975ca68-86c4-4ca4-950f-53bed3ea6224