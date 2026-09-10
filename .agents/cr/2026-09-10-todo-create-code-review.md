# Code Review Report

> **Change** `todo-create` · **分支/Commit** `AI/task-DEV-eecb99b0-9b79-11f1-a438-852be3c2a082-7628fb0e-ecff-42a7-8075-a0a772ecfc5b` · **日期** `2026-09-10` · **审查者** AI

> **AI**：等级 **P0 / P1 / P2**；G/S 以 checklist 行内定义为准；Bug 模式 Blocker→P0、Major→P1、Info→P2。

---

## §1 审查范围

| 维度 | 值 |
|------|-----|
| 变更文件数 | 12 (Java 9 + 配置/SQL 3) |
| Java 文件数 | 9 |
| 已审队列 | 9/9 (0 待审) |
| 自动化预扫规则覆盖 | 52/222 |

---

## §2 功能性核对结论

全部 5 项 REQ 通过 ✅。新增待办事项最小闭环功能完整实现：POST 接口、DTO 校验、持久化、响应封装均符合需求规格。

---

## §3 问题清单

### P1 — 推荐修复（合并前应修复）

| # | 等级 | 来源 | 描述 | 位置 |
|---|------|------|------|------|
| 1 | P1 | M016/Bug模式 | `LocalDateTime.now()` 未指定时区，部署环境时区不同时产生不一致时间戳 | `TodoItemServiceImpl.java:23,24` |
| 2 | P1 | A5/G7 | 通用异常处理器 `handleException` 未记录异常堆栈日志，生产故障无法定位根因 | `GlobalExceptionHandler.java:26` |

### P2 — 参考改进（可选）

| # | 等级 | 来源 | 描述 | 位置 |
|---|------|------|------|------|
| 3 | P2 | A3 | 所有公共类和方法缺少 Javadoc 注释 | 全部 Java 文件 |
| 4 | P2 | A6 | ApiResponse 中硬编码状态码 200/400/500，建议抽取为常量或枚举 | `ApiResponse.java:19,23` |
| 5 | P2 | G4 | Service 层 createTodo 方法未加 `@Transactional`，当前单次 insert 影响小，但扩展时需补加 | `TodoItemServiceImpl.java:19` |

### 提醒项（非阻塞）

| # | 描述 | 位置 |
|---|------|------|
| R1 | 确认 application.yml 中数据库密码是否明文存储，建议使用加密或环境变量注入 | `application.yml` |

---

## §4 安全检查摘要

- SQL 注入：✅ MyBatis-Plus 参数化绑定
- 输入校验：✅ @Valid + @NotBlank + @Size
- 认证/授权：N/A（内部用户最小闭环）
- 密钥泄露：⚠️ 需确认配置文件密码存储方式

---

## §5 可靠性检查摘要

- 事务边界：⚠️ P2 缺少 @Transactional
- 异常日志：❌ P1 通用异常未记录
- 时区安全：❌ P1 LocalDateTime.now() 未指定 ZoneId
- 其余军规项（并发/超时/资源/幂等）：N/A（最小闭环不涉及）

---

## §6 可读性检查摘要

- 格式/命名/组织：✅ 符合阿里巴巴 Java 代码风格
- 注释：⚠️ P2 缺少 Javadoc
- 日志：❌ P1 异常处理缺日志
- 魔法值：⚠️ P2 状态码硬编码

---

## §7 自定义扩展检查

N/A（未启用自定义规则）

---

## §8 修复任务列表

- [ ] **[P1]** TodoItemServiceImpl.java:23,24 — 将 `LocalDateTime.now()` 改为 `LocalDateTime.now(ZoneId.of("Asia/Shanghai"))` 或使用数据库自动填充（MetaObjectHandler / DEFAULT CURRENT_TIMESTAMP）
- [ ] **[P1]** GlobalExceptionHandler.java:26 — 在 `handleException` 中添加 `log.error("Unhandled exception", ex)` 并引入 Slf4j Logger
- [ ] **[P2]** 全部公共类/方法补充 Javadoc 注释
- [ ] **[P2]** ApiResponse.java — 将状态码 200/400/500 抽取为常量或枚举
- [ ] **[P2]** TodoItemServiceImpl.java:19 — 添加 `@Transactional` 注解
- [ ] **[提醒]** 确认 application.yml 数据库密码存储方式，建议加密或外部化
