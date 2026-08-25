# 组织架构管理模块 — 代码评审报告

> 生成时间：2026-08-25
> 任务阶段：loop-2 / 代码评审
> 关联任务：DEV-9d10e310-7901-11f1-8a9f-59ecae612580-c975ca68-86c4-4ca4-950f-53bed3ea6224
> 评审范围：manyu_test (38 files) + manyu_test1 (1 file)

---

## 1. 评审概要

| 维度 | 状态 | 说明 |
|------|------|------|
| 需求1-部门树 | ✅ 通过 | 完整树 + 懒加载 + 拖拽移动 + 循环引用校验均已实现 |
| 需求2-员工新增 | ⚠️ 有缺陷 | 唯一性校验逻辑与 DB 索引存在不一致（见 Blocker #1） |
| 需求3-人员调动 | ⚠️ 有缺陷 | 核心逻辑正确，operatorId 硬编码为 null |
| 需求4-员工离职 | ✅ 通过 | 逻辑删除 + 事件发布 + 历史保留均已实现 |
| 跨仓契约 | ⚠️ 有缺陷 | manyu_test1 消费者使用原始参数而非事件对象 |
| 代码质量 | ⚠️ 有缺陷 | 缺异常日志、N+1 查询、输入校验不足 |

### 问题统计

| 严重级别 | 数量 | 说明 |
|----------|------|------|
| 🔴 Blocker | 3 | 必须修复才能上线 |
| 🟠 Major | 4 | 影响可维护性/性能，建议修复 |
| 🟡 Minor | 5 | 代码风格/最佳实践 |
| **总计** | **12** | |

---

## 2. 🔴 Blocker 问题

### Blocker #1: phone 唯一索引与应用层校验逻辑不一致

- **文件**: `[manyu_test] src/main/resources/db/V1__init.sql` L32 + `[manyu_test] src/main/java/com/example/org/repository/EmployeeRepository.java` L15-L16
- **严重级别**: 🔴 Blocker
- **问题描述**:

  DDL 中 `employees` 表定义了 `UNIQUE INDEX uk_phone (phone)`，对**所有员工**（包括已离职）强制 phone 全局唯一。

  但 `EmployeeRepository.countByPhone()` 查询加入了 `AND status != 'RESIGNED'`，仅检查在职员工的 phone 唯一性。

  这导致一个**数据不一致场景**：
  1. 员工 A 离职 (status='RESIGNED')，phone='13800138000'
  2. 新员工 B 入职，phone='13800138000'
  3. 应用层 `countByPhone("13800138000")` 返回 0（因为排除了 RESIGNED）
  4. 应用层校验通过，执行 INSERT
  5. **数据库抛出 `DuplicateKeyException`**（因为全局唯一索引冲突）
  6. 用户看到错误信息："数据已存在/冲突"（GlobalExceptionHandler 的通用错误消息）

- **修复建议**:

  方案 A（推荐）：DDL 的 unique index 也排除 RESIGNED —— MySQL 不支持条件唯一索引，需改用函数索引或应用层控制。建议改为：**phone 字段全局唯一（包括离职员工），不允许复用离职员工手机号**。修改 `EmployeeRepository.countByPhone()` 去掉 `AND status != 'RESIGNED'` 条件。

  方案 B：如果业务确实需要复用离职员工手机号，则需要将 DB 唯一索引改为普通索引，在应用层完全控制唯一性（风险较高，不推荐）。

- **相关需求**: 需求2-员工新增（唯一性校验）

---

### Blocker #2: GlobalExceptionHandler 未记录异常日志

- **文件**: `[manyu_test] src/main/java/com/example/org/exception/GlobalExceptionHandler.java` L48-L51
- **严重级别**: 🔴 Blocker
- **问题描述**:

  `@ExceptionHandler(Exception.class)` 兜底处理器仅返回 `ApiResponse.error(500, "Internal Server Error")`，**完全没有日志记录**（缺少 `log.error()`）。这意味着：

  - 生产环境中任何未预期的异常都**无法追溯**根因
  - 没有异常堆栈，无法定位问题代码
  - 运维人员只能看到 500 错误，不知道具体发生了什么

- **修复建议**:

  ```java
  @ExceptionHandler(Exception.class)
  public ApiResponse<Void> handleException(Exception ex) {
      log.error("Unhandled exception", ex);  // 添加此行
      return ApiResponse.error(500, "Internal Server Error");
  }
  ```

  同时考虑在 `handleDataIntegrityViolation` 中也添加日志，以便追踪具体的约束冲突。

- **相关需求**: 跨切面（运维可观测性）

---

### Blocker #3: EmployeeController.checkUnique() 缺少输入校验

- **文件**: `[manyu_test] src/main/java/com/example/org/controller/EmployeeController.java` L29-L32
- **严重级别**: 🔴 Blocker
- **问题描述**:

  `GET /api/employees/check?field=xxx&value=yyy` 接口的 `field` 和 `value` 参数没有做任何校验：

  - `field` 可以为空字符串 `""`，此时 `EmployeeService.checkUnique()` 中 `"employeeNo".equals("")` 和 `"phone".equals("")` 均为 false，抛出 `BusinessException(400, "不支持的校验字段")` —— 这是预期行为但错误信息不够友好
  - `value` 可以为空字符串，此时 `countByEmployeeNo("")` 和 `countByPhone("")` 会查询空字符串，逻辑上不正确
  - 缺少 `field` 参数的白名单校验——虽然 `checkUnique()` 方法中用了 if/else 做限制，但 Controller 层没有输入契约约束

- **修复建议**:

  在 Controller 方法参数上添加校验注解，或在 `checkUnique` 方法开头增加防御性校验：

  ```java
  // Controller 层增加校验
  @GetMapping("/check")
  public ApiResponse<?> checkUnique(@RequestParam @NotBlank String field,
                                     @RequestParam @NotBlank String value) {
      return ApiResponse.success(employeeService.checkUnique(field, value));
  }
  ```

  同时在 `EmployeeService.checkUnique()` 中增加对 `value` 的空值校验：
  ```java
  if (value == null || value.isBlank()) {
      throw new BusinessException(400, "校验值不能为空");
  }
  ```

- **相关需求**: 需求2-员工新增（唯一性校验）

---

## 3. 🟠 Major 问题

### Major #1: DepartmentService.getChildren() 返回 Entity 而非 DTO

- **文件**: `[manyu_test] src/main/java/com/example/org/service/DepartmentService.java` L41-L47
- **严重级别**: 🟠 Major
- **问题描述**:

  `GET /api/departments/{id}/children` 接口直接返回 `List<Department>` 实体，这会将内部数据库字段（如 `createdAt`、`updatedAt`）暴露给前端。虽然当前前端可能不使用这些字段，但这违反了分层架构原则，且增加了未来数据库 Schema 变更时对 API 的耦合风险。

- **修复建议**:

  返回 `List<DepartmentTreeDTO>`（复用 tree DTO），或创建专用的 `DepartmentChildDTO`。

---

### Major #2: DepartmentService.move() 中 collectDescendantIds 存在 N+1 查询

- **文件**: `[manyu_test] src/main/java/com/example/org/service/DepartmentService.java` L137-L146
- **严重级别**: 🟠 Major
- **问题描述**:

  `collectDescendantIds()` 方法使用递归逐层查询子部门。对于深度为 N 的树，会产生 N 次数据库查询。虽然部门树通常深度有限（≤10层），但在每次拖拽操作时执行此查询，性能不佳。

- **修复建议**:

  使用单次递归 CTE 查询替代多次查询：

  ```java
  @Select("WITH RECURSIVE descendants AS (" +
          "SELECT id FROM departments WHERE parent_id = #{id} " +
          "UNION ALL " +
          "SELECT d.id FROM departments d INNER JOIN descendants ds ON d.parent_id = ds.id" +
          ") SELECT id FROM descendants")
  List<Long> selectDescendantIds(@Param("id") Long id);
  ```

---

### Major #3: TransferService 中 operatorId 硬编码为 null

- **文件**: `[manyu_test] src/main/java/com/example/org/service/TransferService.java` L55, L73, L96
- **严重级别**: 🟠 Major
- **问题描述**:

  调动记录和事件中的 `operatorId` 全部设置为 `null`。虽然 JWT 鉴权尚未集成（SecurityConfig 中标注为后续阶段），但至少应在代码中添加 TODO 注释和占位逻辑，以便后续集成时不会遗漏。

- **修复建议**:

  添加 `// TODO: 从 SecurityContext 获取当前登录用户ID` 注释，并考虑从 SecurityContextHolder 获取占位值。

---

### Major #4: EmployeeService.getById() 存在 N+1 查询

- **文件**: `[manyu_test] src/main/java/com/example/org/service/EmployeeService.java` L80-L104
- **严重级别**: 🟠 Major
- **问题描述**:

  每次查询员工详情时，先查 `employees` 表，再单独查 `departments` 表获取 `deptName`。对于单个详情查询，这可以接受；但如果未来需要批量查询员工列表并附带部门名称，此模式会导致性能问题。

- **修复建议**:

  当前阶段作为单条详情查询，接受此模式。建议在 Repository 中添加 LEFT JOIN 查询方法，或使用 MyBatis-Plus 的关联查询注解。

---

## 4. 🟡 Minor 问题

### Minor #1: JWT Secret 硬编码在 application.yml

- **文件**: `[manyu_test] src/main/resources/application.yml` L25-L27
- **严重级别**: 🟡 Minor
- **问题描述**:

  `jwt.secret` 明文硬编码在配置文件中。虽然当前 JWT 功能尚未集成，但一旦启用，这将导致安全风险。

- **修复建议**:

  使用环境变量 `${JWT_SECRET}` 或配置中心管理敏感配置。

---

### Minor #2: DTO 缺少 @Size 长度校验

- **文件**: `[manyu_test] src/main/java/com/example/org/model/dto/EmployeeCreateDTO.java`
- **严重级别**: 🟡 Minor
- **问题描述**:

  `EmployeeCreateDTO` 中 `name` 字段只标注了 `@NotBlank`，但 DB 列定义为 `VARCHAR(50)`。如果前端传入超长字符串，将在数据库层报错，而非在 Controller 层给出友好提示。

- **修复建议**:

  添加 `@Size(max = 50)` 注解，与 DB 列长度对齐。同理检查 `employeeNo`（VARCHAR(30)）、`phone`（VARCHAR(20)）等字段。

---

### Minor #3: CORS 配置存在逻辑矛盾

- **文件**: `[manyu_test] src/main/java/com/example/org/config/WebConfig.java` L15-L19
- **严重级别**: 🟡 Minor
- **问题描述**:

  `allowCredentials(true)` 与 `allowedOriginPatterns("*")` 同时使用。根据 CORS 规范，当 `allowCredentials=true` 时，`Access-Control-Allow-Origin` 不能为 `*`。浏览器会拒绝此类响应。Spring 的 `allowedOriginPatterns` 虽然可以绕过这个限制，但某些浏览器仍可能拒绝。

- **修复建议**:

  开发环境可保留，但生产环境应配置具体的允许域名列表。

---

### Minor #4: BusinessException 冗余覆盖父类 message 字段

- **文件**: `[manyu_test] src/main/java/com/example/org/exception/BusinessException.java` L9, L22-L24
- **严重级别**: 🟡 Minor
- **问题描述**:

  `BusinessException` 声明了 `private final String message` 字段，与 `Throwable.message` 产生命名冲突。虽然 `getMessage()` 被正确覆盖，但 `Throwable` 的 `detailMessage` 和 `this.message` 是两个独立字段，可能在某些日志框架/调试工具中产生混淆。

- **修复建议**:

  移除 `private final String message` 字段，直接使用 `super(message)` 传递的父类字段。`getMessage()` 覆盖也可移除。

---

### Minor #5: 事件对象缺少 @JsonFormat 日期格式化

- **文件**: `[manyu_test] src/main/java/com/example/org/event/EmployeeTransferredEvent.java` L20 + `EmployeeResignedEvent.java` L16
- **严重级别**: 🟡 Minor
- **问题描述**:

  `EmployeeTransferredEvent.transferTime`（`LocalDateTime`）和 `EmployeeResignedEvent.resignDate`（`LocalDate`）没有 `@JsonFormat` 注解。如果未来事件通过消息队列（JSON 序列化）发送，日期格式可能因不同 Jackson 配置而产生不一致。

- **修复建议**:

  添加 `@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")` 和 `@JsonFormat(pattern = "yyyy-MM-dd")` 注解。

---

## 5. 跨仓契约对齐检查

### 5.1 manyu_test → manyu_test1 事件契约

| 契约 | manyu_test (发布方) | manyu_test1 (消费方) | 对齐状态 |
|------|---------------------|----------------------|----------|
| EmployeeTransferredEvent | `EmployeeTransferredEvent` 对象 (eventType, employeeId, fromDeptId, toDeptId, fromPosition, toPosition, transferTime, operatorId) | `onEmployeeTransferred(Long employeeId, Long fromDeptId, Long toDeptId, String fromPosition, String toPosition)` | ⚠️ **不对齐**：消费方使用原始参数而非事件对象；缺少 `transferTime` 和 `operatorId` 参数 |
| EmployeeResignedEvent | `EmployeeResignedEvent` 对象 (eventType, employeeId, resignDate, operatorId) | `onEmployeeResigned(Long employeeId, String resignDate)` | ⚠️ **不对齐**：消费方使用原始参数；`resignDate` 类型为 `String` 而非 `LocalDate`；缺少 `operatorId` 参数 |

**说明**: manyu_test1 的 `OrgEventConsumer` 是预留桩代码（stub），其方法签名与 manyu_test 的事件对象不完全匹配。后续集成时需统一为事件对象或确认参数列表。

### 5.2 REST API 响应格式

| 检查项 | 预期 | 实际 | 状态 |
|--------|------|------|------|
| 统一响应体 | `{ "code": ..., "data": ..., "msg": "..." }` | `ApiResponse` 字段: code, msg, data | ✅ 对齐 |
| 分页格式 | `{ "total": ..., "page": ..., "pageSize": ..., "list": [...] }` | `PageResult` 字段: total, page, pageSize, list | ✅ 对齐 |
| 部门树结构 | `{ "id": ..., "name": ..., "children": [...] }` | `DepartmentTreeDTO` 包含 id, name, parentId, sortOrder, status, children | ✅ 对齐（额外字段不影响） |

---

## 6. 需求逐项验收追踪

| 需求 | 验收项 | 代码实现状态 | 评审结论 |
|------|--------|-------------|----------|
| 需求1-部门树 | `GET /api/departments/tree` 返回完整树 | `DepartmentService.getTree()` → `DepartmentRepository.selectFullTree()` (递归CTE) | ✅ 通过 |
| 需求1-懒加载 | `GET /api/departments/{id}/children` 按需返回子节点 | `DepartmentService.getChildren()` | ✅ 通过 |
| 需求1-拖拽 | `PUT /api/departments/{id}/move` 含循环引用校验 | `DepartmentService.move()` → `collectDescendantIds()` 校验 | ✅ 通过 |
| 需求2-唯一性校验 | `GET /api/employees/check?field=employeeNo&value=xxx` | `EmployeeService.checkUnique()` | ⚠️ Blocker #3 |
| 需求2-新增 | `POST /api/employees` 含双重唯一性校验 | `EmployeeService.create()` | ⚠️ Blocker #1 |
| 需求3-调动 | `POST /api/employees/{id}/transfer` 更新 dept_id + 写记录 + 发事件 | `TransferService.transfer()` | ✅ 通过（Major #3 operatorId） |
| 需求4-离职 | `PUT /api/employees/{id}/resign` 逻辑删除 + 发事件 | `TransferService.resign()` | ✅ 通过 |

---

## 7. 文件清单

### 7.1 manyu_test 已评审文件

| 文件 | 行数 | 评审结论 |
|------|------|----------|
| pom.xml | 111 | ✅ 依赖版本正确 |
| OrgApplication.java | 16 | ✅ 启动类配置正确 |
| common/ApiResponse.java | 55 | ✅ 统一响应体 |
| common/PageResult.java | 36 | ✅ 分页结果 |
| config/SecurityConfig.java | 35 | ✅ 鉴权框架（JWT 待集成） |
| config/WebConfig.java | 21 | ⚠️ Minor #3 CORS 配置 |
| config/MybatisPlusConfig.java | 21 | ✅ 分页插件已注册 |
| controller/DepartmentController.java | 58 | ✅ REST 端点完整 |
| controller/EmployeeController.java | 69 | ⚠️ Blocker #3 输入校验 |
| model/entity/Department.java | 34 | ✅ 实体映射正确 |
| model/entity/Employee.java | 45 | ✅ 实体映射正确 |
| model/entity/TransferRecord.java | 41 | ✅ 实体映射正确 |
| model/enums/DepartmentStatus.java | 6 | ✅ 枚举定义 |
| model/enums/EmployeeStatus.java | 7 | ✅ 枚举定义 |
| model/dto/DepartmentCreateDTO.java | 19 | ✅ |
| model/dto/DepartmentMoveDTO.java | 17 | ✅ |
| model/dto/DepartmentStatusDTO.java | 15 | ✅ |
| model/dto/DepartmentTreeDTO.java | 21 | ✅ |
| model/dto/DepartmentUpdateDTO.java | 15 | ✅ |
| model/dto/EmployeeCreateDTO.java | 30 | ⚠️ Minor #2 缺长度校验 |
| model/dto/EmployeeDetailDTO.java | 26 | ✅ |
| model/dto/EmployeeUpdateDTO.java | 17 | ✅ |
| model/dto/TransferRequestDTO.java | 19 | ✅ |
| model/dto/ResignRequestDTO.java | 17 | ✅ |
| repository/DepartmentRepository.java | 22 | ✅ 递归CTE正确 |
| repository/EmployeeRepository.java | 17 | ⚠️ Blocker #1 phone 查询 |
| repository/TransferRecordRepository.java | 9 | ✅ |
| service/DepartmentService.java | 169 | ⚠️ Major #1 #2 |
| service/EmployeeService.java | 150 | ⚠️ Major #4, Blocker #1 |
| service/TransferService.java | 99 | ⚠️ Major #3 |
| event/EmployeeTransferredEvent.java | 22 | ✅ 字段完整 |
| event/EmployeeResignedEvent.java | 18 | ✅ 字段完整 |
| event/OrgEventListener.java | 34 | ✅ 异步监听器 |
| event/OrgEventPublisher.java | 24 | ✅ 事件发布器 |
| exception/BusinessException.java | 25 | ⚠️ Minor #4 |
| exception/GlobalExceptionHandler.java | 52 | ⚠️ Blocker #2 缺日志 |
| resources/application.yml | 31 | ⚠️ Minor #1 JWT Secret |
| resources/db/V1__init.sql | 54 | ⚠️ Blocker #1 索引 |

### 7.2 manyu_test1 已评审文件

| 文件 | 行数 | 评审结论 |
|------|------|----------|
| approval/event/OrgEventConsumer.java | 36 | ⚠️ 跨仓契约不对齐 |

---

## 8. 总结与建议

### 8.1 修复优先级

| 优先级 | 问题编号 | 修复工时估算 | 阻塞上线 |
|--------|----------|-------------|----------|
| P0 | Blocker #1 (phone 唯一索引) | 0.5h | ✅ 是 |
| P0 | Blocker #2 (缺异常日志) | 0.1h | ✅ 是 |
| P0 | Blocker #3 (checkUnique 输入校验) | 0.2h | ✅ 是 |
| P1 | Major #1 (getChildren 返回 Entity) | 0.3h | 否 |
| P1 | Major #2 (collectDescendantIds N+1) | 0.5h | 否 |
| P1 | Major #3 (operatorId null) | 0.1h | 否 |
| P1 | Major #4 (getById N+1) | 0.2h | 否 |
| P2 | Minor #1-#5 | 1h | 否 |

### 8.2 整体评价

代码整体架构清晰，遵循了 plan.md 的设计。核心业务逻辑（部门树递归CTE、调动记录留痕、离职逻辑删除、事件发布）实现正确。主要风险集中在**数据一致性边界**（phone 唯一索引 vs 应用层校验）和**可观测性缺失**（异常日志）。修复 3 个 Blocker 后即可进入测试阶段。

---

> 文档所有者：DTCoder
> 关联任务：DEV-9d10e310-7901-11f1-8a9f-59ecae612580-c975ca68-86c4-4ca4-950f-53bed3ea6224