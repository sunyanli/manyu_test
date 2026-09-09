# 代码评审报告

> **项目**: 待办事项模块（todo-app）
> **评审阶段**: 设计 + 编码实现
> **评审日期**: 2026-09-09
> **评审范围**: design.md、impl.md 及全部 Java 源码、配置、测试

---

## 1. 项目概况

- **项目状态**: 无 REVIEW.md（首次评审），已根据项目入口文件（Pom.xml、TodoApplication.java、impl.md）生成概要。
- **技术栈**: Spring Boot 3.3.5 + MyBatis 3.0.4 + MySQL + Jakarta Validation + JUnit 5 + Mockito
- **模块**: 单体待办事项模块，仅支持创建操作（最小闭环）
- **Profile 状态**: `CREATED_AND_USED` — 无现有 REVIEW.md，已从项目上下文生成概要

---

## 2. 评审车道（Lane）裁决表

| 车道 | 裁决 | 摘要 |
|------|------|------|
| Align | `APPROVE_WITH_COMMENTS` | 设计与实现整体对齐，但 impl.md 中 CHECK 列标记为 ✅ 与实际未通过动态验证矛盾 |
| Design | `APPROVE_WITH_COMMENTS` | 架构清晰，但 `TodoController` 硬编码 `userId=1L` 与设计文档中 `user_id` 关联用户的假设不一致 |
| Trim | `APPROVE``APPROVE_WITH_COMMENTS` | `TodoItemMapper` 暴露了 `selectById` 但当前需求不包含查询功能，属于过度设计 |
| Cause | `APPROVE_WITH_COMMENTS` | 异常处理链路完整，但 `GlobalExceptionHandler` 中 `handleException` 捕获 `Exception` 过宽，可能掩盖非系统异常 |
| Verify | `APPROVE_WITH_COMMENTS` | 测试覆盖良好，但 `TodoServiceImplTest` 中 `should_setGmtCreateAndGmtModified_onInsert` 未验证时间字段值 |

---

## 3. 阻塞问题（Blocking Findings）

| # | 车道 | 严重度 | 文件:行 | 问题描述 |
|---|------|--------|---------|----------|
| B1 | Design | **High** | `TodoController.java:47` | `userId` 硬编码为 `1L`，未从请求或认证上下文中获取。设计文档明确 `user_id` 为 NOT NULL 关联用户，但 Controller 未传递真实用户身份，导致所有待办事项归属同一用户。 |
| B2 | Design | **High** | `design.md:205-209` vs `TodoItemDO.java` | 设计文档定义 `todo_item` 表包含 `user_id` 字段，但 `TodoItemDO` 实体类缺少 `userId` 字段的 Javadoc 注释，且 `schema.sql` 与实体定义一致，但 `CreateTodoRequest` 未包含 `userId` 字段，API 入参与设计不一致。 |
| B3 | Verify | **Medium** | `TodoServiceImplTest.java:134-149` | `should_setGmtCreateAndGmtModified_onInsert` 测试方法名为"设置时间字段"，但未断言 `gmtCreate` 和 `gmtModified` 的值不为 null 或符合预期，仅验证了 `todoId`。测试名与断言不匹配。 |

---

## 4.  advisory 发现问题

| # | 车道 | 严重度 | 文件:行 | 问题描述 |
|---|------|--------|---------|----------|
| A1 | Align | Low | `impl.md:107-118` | impl.md 中 CHECK 列标记为 ✅，但 L2 动态验证（编译/单测）实际为 ⚠️ 未通过。报告内部存在矛盾，状态标记不准确。 |
| A2 | Trim | Medium | `TodoItemMapper.java:28-30` | `selectById` 方法在当前需求范围（仅创建）内无用途，属于超出范围的 API 暴露。建议在后续迭代中按需移除或保留并记录。 |
| A3 | Cause | Medium | `GlobalExceptionHandler.java:55-59` | `handleException(Exception e)` 捕获所有未处理异常，包括 `NullPointerException`、`ArrayIndexOutOfBoundsException` 等编程错误，与设计文档中 TODO_004 "系统异常" 的语义一致，但日志中未区分异常类型，不利于排查。 |
| A4 | Design | Low | `TodoVO.java:18-19` | `todoId` 字段使用 `@JsonProperty("todo_id")`，与设计文档响应示例中的 `todoId`（camelCase）不一致。设计文档示例输出为 `"todoId": 1`，但 Jackson 序列化结果为 `"todo_id": 1`。 |
| A5 | Verify | Low | `TodoServiceImplTest.java:51` | `should_returnTodoVo_when_validRequest` 测试中 `when(todoItemMapper.selectById(1L)).thenReturn(savedItem)` 是多余设置，因为 `createTodo` 方法中并未调用 `selectById`。测试包含未使用的 mock 行为。 |
| A6 | Design | Low | `design.md:51-52` | A01（事项名称最大长度 200）和 A02（描述最大长度 2000）为待确认假设，但 `CreateTodoRequest.java` 中已硬编码相同值。建议在假设确认后补充设计文档的确认状态。 |
| A7 | Align | Low | `TodoController.java:46` | Controller 日志记录了 `request.getTitle()`，但未记录 `userId`，与 impl.md 中"记录创建操作的入参和出参"的设计要求不完全一致。 |

---

## 5. 跳过车道及原因

无跳过车道。五个车道（Align、Design、Trim、Cause、Verify）均已执行。

---

## 6. 建议下一步

1. **修复 B1（阻塞）**: 从请求上下文中获取真实 `userId`，或通过请求头/认证框架注入。`TodoController.createTodo` 第 47 行将 `1L` 替换为从安全上下文提取的用户 ID。
2. **修复 B2（阻塞）**: 确认 `CreateTodoRequest` 是否需要 `userId` 字段，或在 Controller 层从认证上下文注入。确保 API 入参与设计文档一致。
3. **修复 B3（阻塞）**: 为 `should_setGmtCreateAndGmtModified_onInsert` 添加对 `gmtCreate` 和 `gmtModified` 非空断言。
4. **修复 A4**: 确认 `TodoVO.todoId` 的 JSON 序列化名称，与设计文档响应示例保持一致（建议移除 `@JsonProperty("todo_id")` 或更新设计文档）。
5. **清理 A2**: 评估 `selectById` 是否在当前迭代范围内，若不需要则移除或标记为 `@Deprecated`。
6. **修正 A1**: 更新 `impl.md` 中 CHECK 列状态，使其与 L2 动态验证实际结果一致。
7. **补充 A5**: 移除 `should_returnTodoVo_when_validRequest` 中未使用的 `selectById` mock。

---

## 7. 阻塞计数

- **blocker_count**: 3

---

> **评审结论**: 3 个阻塞问题需修复后方可合入。阻塞问题主要集中在 `userId` 硬编码（设计一致性）和测试断言不匹配。其余 7 个 advisory 问题建议在本次迭代中一并处理。
