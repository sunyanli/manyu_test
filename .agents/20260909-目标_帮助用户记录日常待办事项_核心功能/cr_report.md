# 代码评审报告 — 待办事项记录 核心功能

> **评审日期**: 2026-09-09
> **评审范围**: 新增待办事项（F01），最小闭环仅创建
> **涉及文件**: 13 个源码/配置/文档文件 + 1 个测试文件

---

## Project profile

**State**: CREATED_AND_USED
**Source**: `REVIEW.md`（新建）
**Notes**: 项目无 AGENTS.md，基于 pom.xml、application.yml、分层架构和设计文档生成了项目级评审配置。

---

## Lane verdict table

| Lane | Verdict | Notes |
|---|---|---|
| `align` | APPROVE_WITH_COMMENTS | 接口路径、入参/出参格式、错误码编号与设计一致；Service 方法签名与设计存在偏差 |
| `design` | APPROVE_WITH_COMMENTS | 分层清晰，归属合理；Controller 内联转换逻辑可接受；tenant_id 硬编码为已知 TODO |
| `trim` | APPROVE | 无死代码、无过度抽象、无多余公开面 |
| `cause` | NOT_RUN | 本次为新功能开发，非 bug 修复 |
| `verify` | REJECT | **测试断言错误**：6 个错误码测试使用 `hasMessageContaining("TODO_00X")` 检查 message，但 BusinessException 将 errorCode 与 message 分离存储，实际 message 为中文文本，所有错误码测试必然失败 |

---

## Blocking findings

### [CRITICAL] [VERIFY] [IMPLEMENTATION-BUG] src/test/java/com/dtazzy/todo/service/TodoItemServiceImplTest.java:74,88,103,118,149,197

**Finding**: 所有 6 个涉及错误码的测试使用 `hasMessageContaining("TODO_00X")` 断言，但 `BusinessException` 的构造函数为 `BusinessException(String errorCode, String message)`，其中 `errorCode` 存储为独立字段，`message` 传入 `super(message)` 作为异常消息。实际异常的 `getMessage()` 返回中文文本（如 "事项名称不能为空"），不包含 "TODO_001" 等错误码。

**Evidence**:
- `BusinessException.java:10-12`: `super(message)` — errorCode 仅存于私有字段，不在 message 中
- `TodoItemServiceImpl.java:51`: `new BusinessException("TODO_001", "事项名称不能为空")` — message 为中文
- `TodoItemServiceImplTest.java:74`: `.hasMessageContaining("TODO_001")` — 断言检查 message 而非 errorCode
- 同理影响 line 88, 103 (TODO_001), line 118 (TODO_002), line 149 (TODO_003), line 197 (TODO_004)

**Recommendation**: 将断言改为 `assertThatThrownBy(...).isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo("TODO_001")` 或在 BusinessException 中重写 `getMessage()` 使其包含 errorCode。

---

### [HIGH] [VERIFY] [BOUNDARY-CASE] src/main/java/com/dtazzy/todo/service/impl/TodoItemServiceImpl.java:30

**Finding**: `createTodoItem(String tenantId, ...)` 未对 `tenantId` 做 null 检查。若 Controller 传入 null（当前不会，因为硬编码 "default"），将直接传递给 Mapper，导致 INSERT 违反 `tenant_id VARCHAR(64) NOT NULL` 约束，抛出非业务异常（如 `DataIntegrityViolationException`），被 Service 的 `catch(Exception)` 兜底为 TODO_004 但丢失了原始错误的语义。

**Evidence**:
- `TodoItemServiceImpl.java:35`: `todoItem.setTenantId(tenantId)` — 未校验
- `schema.sql:7`: `tenant_id VARCHAR(64) NOT NULL` — DB 约束
- `TodoItemController.java:71`: `return "default"` — 当前硬编码掩盖了此问题

**Recommendation**: 在 `validateName()` 之前增加 `if (tenantId == null || tenantId.isBlank()) throw new BusinessException("TODO_005", "租户标识不能为空")`。

---

### [HIGH] [VERIFY] [TEST-GAP] src/test/java/com/dtazzy/todo/service/TodoItemServiceImplTest.java

**Finding**: 测试仅覆盖 Service 层，缺少 Controller 层集成测试。Controller 中的 `toVO()` 转换逻辑、`resolveTenantId()` 逻辑、`BusinessException` 到 HTTP 400 响应的映射均未被测试覆盖。

**Evidence**:
- `TodoItemController.java:44-65`: `create()` 方法包含参数绑定、tenantId 解析、DO→VO 转换、异常映射等逻辑
- 测试目录中仅存在 `TodoItemServiceImplTest.java`，无 Controller 测试

**Recommendation**: 增加 `TodoItemControllerTest` 使用 `@WebMvcTest` + `MockMvc`，验证正常响应 JSON 结构和错误码映射。

---

## Advisory findings

### [WARNING] [ALIGN] [CLAIM-DRIFT] design.md:177 vs TodoItemService.java:18

**Finding**: 设计文档定义 Service 方法签名为 `TodoItem createTodoItem(CreateTodoItemRequest request)`（无 tenantId 参数，返回 `TodoItem`），但实现为 `TodoItemDO createTodoItem(String tenantId, CreateTodoItemRequest request)`（增加 tenantId 参数，返回 `TodoItemDO`）。设计中的时序图显示 Service 层"从上下文获取 tenant_id"，但实现将 tenantId 作为参数传入。

**Recommendation**: 在 impl.md 中记录此偏差，或更新设计文档使其与实现一致。两种方案均可接受：参数化方式更利于测试，上下文方式更隐蔽。

---

### [WARNING] [ALIGN] [CLAIM-DRIFT] TodoItemController.java:46

**Finding**: tenant_id 硬编码为 `"default"`，设计文档承诺"通过全局拦截器从请求上下文获取 tenant_id"。虽已标注 TODO 注释，但该占位符在无认证拦截器的环境下无法区分用户。

**Recommendation**: 建议在 impl.md 中明确标注此 TODO 的优先级和预计完成时间。

---

### [WARNING] [DESIGN] [OBSERVABILITY-GAP] TodoItemController.java:58

**Finding**: Controller 捕获 `BusinessException` 时仅 `logger.warn(...)` 记录 errorCode 和 message，未记录堆栈。而 Service 层的 DB 异常已通过 `logger.error(..., e)` 记录完整堆栈。两层日志不一致会导致排查链路不完整。

**Recommendation**: Controller 的 catch 中增加 `logger.warn("...", e)` 以保留堆栈，或统一使用 `@ControllerAdvice` 全局异常处理器。

---

### [WARNING] [VERIFY] [TEST-GAP] src/test/java/com/dtazzy/todo/service/TodoItemServiceImplTest.java:53

**Finding**: 正常路径测试 `shouldCreateTodoItem_whenRequestIsValid` 未断言 `result.getId()` 的值。Mock 的 `insert()` 返回 1 但不会设置 DO 的 id 字段（这是 MyBatis `useGeneratedKeys` 的行为，Mock 无法模拟）。测试未覆盖此场景。

**Recommendation**: 使用 `verify(todoItemMapper).insert(any(TodoItemDO.class))` 已可确认调用，但建议增加注释说明 id 字段需集成测试验证。

---

## Skipped lanes and reasons

| Lane | Reason |
|---|---|
| `cause` | 本次为新功能开发，无 bug 修复声明，不适用 Cause Lane |

---

## Suggested next actions

1. **立即修复**: 将测试中的 `hasMessageContaining("TODO_00X")` 改为 `extracting("errorCode").isEqualTo("TODO_00X")`（6 处）
2. **建议修复**: 在 Service 中增加 tenantId 非空校验
3. **建议补充**: 增加 `TodoItemControllerTest` 集成测试
4. **建议同步**: 更新设计文档或 impl.md 记录 Service 方法签名偏差
5. **环境验证**: 在 Maven 环境中执行 `mvn test -Dtest=TodoItemServiceImplTest` 确认测试通过

---

## VERDICT: **REJECT**

**原因**: Verify Lane 存在 1 个 CRITICAL 和 2 个 HIGH 阻塞项，测试断言错误导致全部错误码测试必然失败。