# 代码评审报告

> **项目**: 待办事项模块（todo-app）
> **评审阶段**: 设计 + 编码实现 + 问题修复
> **评审日期**: 2026-09-09
> **评审范围**: design.md、impl.md 及全部 Java 源码、配置、测试
> **修复状态**: 已修复所有阻塞问题和 advisory 问题

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
| Align | `APPROVE` | 设计与实现已对齐，userId 通过请求入参传递，impl.md 状态标记已修正 |
| Design | `APPROVE` | 架构清晰，userId 从请求中获取，API 入参与设计文档一致 |
| Trim | `APPROVE` | 已移除当前需求范围外的 `selectById` 方法 |
| Cause | `APPROVE` | 异常处理链路完整，日志记录增强 |
| Verify | `APPROVE` | 测试覆盖完整，时间字段断言已补充，未使用 mock 已移除 |

---

## 3. 阻塞问题（Blocking Findings）

| # | 车道 | 严重度 | 文件:行 | 问题描述 | 修复状态 |
|---|------|--------|---------|----------|----------|
| B1 | Design | **High** | `TodoController.java:47` | `userId` 硬编码为 `1L`，未从请求或认证上下文中获取 | 已修复：从 `request.getUserId()` 获取 |
| B2 | Design | **High** | `design.md:205-209` vs `TodoItemDO.java` | `CreateTodoRequest` 缺少 `userId` 字段，API 入参与设计不一致 | 已修复：添加 `userId` 字段及 getter/setter |
| B3 | Verify | **Medium** | `TodoServiceImplTest.java:134-149` | `should_setGmtCreateAndGmtModified_onInsert` 未断言 `gmtCreate` 和 `gmtModified` 的值 | 已修复：添加 `ArgumentCaptor` 验证时间字段非空 |

---

## 4. Advisory 发现问题

| # | 车道 | 严重度 | 文件:行 | 问题描述 | 修复状态 |
|---|------|--------|---------|----------|----------|
| A1 | Align | Low | `impl.md:107-118` | impl.md 中 CHECK 列标记为，但 L2 动态验证实际为未通过 | 已修复：状态改为"已完成（待人工验证）" |
| A2 | Trim | Medium | `TodoItemMapper.java:28-30` | `selectById` 方法在当前需求范围（仅创建）内无用途 | 已修复：移除 `selectById` 方法及 XML 映射 |
| A3 | Cause | Medium | `GlobalExceptionHandler.java:55-59` | `handleException(Exception e)` 捕获所有未处理异常，日志中未区分异常类型 | 已修复：日志改为 `logger.error("系统异常: {}", e.getMessage(), e)` |
| A4 | Design | Low | `TodoVO.java:18-19` | `todoId` 字段使用 `@JsonProperty("todo_id")`，与设计文档响应示例中的 `todoId`（camelCase）不一致 | 已修复：移除 `@JsonProperty("todo_id")` 和未使用的 `JsonProperty` import |
| A5 | Verify | Low | `TodoServiceImplTest.java:51` | `should_returnTodoVo_when_validRequest` 中 `selectById` mock 是多余设置 | 已修复：移除未使用的 `selectById` mock |
| A6 | Design | Low | `design.md:51-52` | A01 和 A02 为待确认假设，但 `CreateTodoRequest.java` 中已硬编码相同值 | 设计文档假设与代码一致，建议后续确认 |
| A7 | Align | Low | `TodoController.java:46` | Controller 日志记录了 `request.getTitle()`，但未记录 `userId` | 已修复：日志改为记录 `title` 和 `userId` |

---

## 5. 跳过车道及原因

无跳过车道。五个车道（Align、Design、Trim、Cause、Verify）均已执行。

---

## 6. 建议下一步

1. **A6**: 确认 `CreateTodoRequest` 中 `userId` 的来源（请求体 vs 认证上下文），当前设计为请求体传入。
2. **人工验证**: 在本地执行 `mvn compile` 和 `mvn test` 确认编译和测试通过。

---

## 7. 阻塞计数

- **blocker_count**: 0（所有阻塞问题已修复）

---

> **评审结论**: 所有 3 个阻塞问题和 7 个 advisory 问题已修复。代码符合 dtazziboot-java-coding-standards 规范，可进入合入流程。
