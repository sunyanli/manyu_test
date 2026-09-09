# Code Review Checklist

> **Change** `todo-app` · **分支/Commit** `AI/task-DEV-eecb99b0-9b79-11f1-a438-852be3c2a082-bd826ef0-b543-432d-b1ad-b675d86217fd` / `a43da5b` · **日期** `2026-09-09`
>
> **AI**：唯一进度源；状态仅用 `⬜` `✅` `❌` `⚠️` `N/A`。
> **完成标准**：所有核销项必须从 `⬜` 变为其他状态；`N/A` 需写原因。
>
> **自动化预扫**：已执行 `scan-all-rules.sh src/` → **无命中**（0/52 可程序化规则触发）。以下为 LLM 全量审查结果。

---

## Step 1 — 执行队列（产物 A）

> **注意**：本次变更为 Python 项目（非 Java），以下按 Python 代码审查执行。S/G/A 规则中 Java 特定项标记 `N/A(非Java)`。

| # | 文件（仓库相对路径） | 归属原因 | Step2 | Step3 | 总状态 |
|---|----------------------|----------|-------|-------|--------|
| 1 | `src/model/entity/TodoItem.py` | REQ-F01 实体 | ✅ | ✅ | ⚠️ 已审有问题 |
| 2 | `src/model/entity/User.py` | REQ-F04 实体 | ✅ | ✅ | ✅ 已审 |
| 3 | `src/model/dto/TodoRequest.py` | REQ-F01 DTO | ✅ | ✅ | ✅ 已审 |
| 4 | `src/model/dto/TodoResponse.py` | REQ-F01 DTO | ✅ | ✅ | ✅ 已审 |
| 5 | `src/repository/todo_repository.py` | REQ-F01/F02/F03 数据层 | ✅ | ✅ | ⚠️ 已审有问题 |
| 6 | `src/service/todo_service.py` | REQ-F01/F02/F03 业务层 | ✅ | ✅ | ⚠️ 已审有问题 |
| 7 | `src/service/auth_service.py` | REQ-F04 认证服务 | ✅ | ✅ | ⚠️ 已审有问题 |
| 8 | `src/controller/todo_controller.py` | REQ-F01/F02/F03 控制器 | ✅ | ✅ | ⚠️ 已审有问题 |
| 9 | `src/controller/auth_controller.py` | REQ-F04 控制器 | ✅ | ✅ | ⚠️ 已审有问题 |
| 10 | `src/models.py` | 数据模型 ORM | ✅ | ✅ | ⚠️ 已审有问题 |
| 11 | `src/database.py` | 数据库连接 | ✅ | ✅ | ✅ 已审 |
| 12 | `src/main.py` | 应用入口 | ✅ | ✅ | ✅ 已审 |
| 13 | `tests/test_todo_service.py` | 单测 | ✅ | ✅ | ⚠️ 已审有问题 |
| 14 | `tests/test_auth_service.py` | 单测 | ✅ | ✅ | ⚠️ 已审有问题 |
| 15 | `src/__init__.py` 等 7 个 `__init__.py` | 包初始化 | N/A(空文件) | N/A(空文件) | ✅ 跳过(空) |

---

## Step 2 — 功能（产物 B）

| REQ | Scenario | Spec证据（原文/章节） | 关联文件 | 状态 | 代码证据（文件/测试/接口） |
|-----|----------|----------------------|----------|------|----------------------------|
| REQ-1 | Given 用户已登录，When 提交合法 title+description，Then 创建待办成功 | design.md §5.2.3.1 F01, R01-R03 | `todo_service.py`, `todo_controller.py`, `todo_repository.py` | ⚠️ | `todo_service.py:40-59` 有内存路径不调用 repository；`todo_controller.py:21` 注入了 repository 但 service 不使用 |
| REQ-2 | Given 用户已登录，When 请求待办列表，Then 返回该用户所有待办（按时间倒序、分页） | design.md §5.2.3.2 F02, R04-R07 | `todo_service.py`, `todo_controller.py` | ⚠️ | `todo_service.py:61-65` 无排序/无分页委托；`todo_controller.py:47` 调用 service 而非 repository |
| REQ-3 | Given 用户已登录，When 请求详情，Then 返回对应待办 | design.md §5.2.3.3 F03, R08 | `todo_service.py`, `todo_controller.py` | ⚠️ | `todo_service.py:67-72` 内存查找；`todo_controller.py:65` 同上 |
| REQ-4 | Given 用户提供正确凭据，When 登录，Then 返回 Token（24小时有效） | design.md §5.2.2 W01 | `auth_service.py`, `auth_controller.py` | ⚠️ | `auth_service.py:51-74` 内存存储；`auth_controller.py:29` 正确调用 |
| REQ-5 | Given 无效 Token，When 访问 /api/todo，Then 返回 AUTH_002 | design.md §5.2.3.1 异常场景 | `todo_controller.py` | ❌ | `todo_controller.py:29` `current_user_id: int = 1` 硬编码，无 Token 校验 |

---

## Step 3 — 可读性检查（产物 C）

| ID | 检查项 | 状态 | 备注（命中写 `path:line`） |
|----|--------|------|----------------------------|
| A1 | 源文件格式 | N/A(非Java) | Python 项目 |
| A2 | 源文件结构/import 顺序 | N/A(非Java) | Python import 无静态/非静态分组 |
| A3 | 代码样式 | N/A(非Java) | — |
| A4 | 命名规范 | ✅ | 类名 UpperCamelCase，方法名 lower_snake_case（Python 惯例），常量 UPPER_SNAKE_CASE |
| A5 | 编码实践 | N/A(非Java) | — |
| A6 | 特定元素样式 | N/A(非Java) | — |
| A7 | 文档规范 | ⚠️ | 所有 public 方法有 docstring（Python 等效 Javadoc），但 `TodoService.__init__` 参数 `todo_list` 未说明仅用于测试 |

---

## Step 4 — 可靠性检查（产物 D）

> 预扫结果：`scan-all-rules.sh src/` 无命中（52 条规则全部通过）

### 4.1 Bug 模式（`bug-pattern-checklist.md`）

| ID | 状态 | 备注 |
|----|------|------|
| B001–B081 | N/A(非Java) | Python 项目，Java 特定规则不适用 |
| M001–M027 | N/A(非Java) | 同上 |
| I001–I010 | N/A(非Java) | 同上 |

### 4.2 可靠性（`reliability-checklist.md`）

| ID | 状态 | 备注 |
|----|------|------|
| G1.1–G1.4 | N/A | 无并发写场景（最小闭环单用户创建） |
| G2.1–G2.3 | N/A | 无幂等需求 |
| G3.1–G3.2 | N/A | 无分布式事务 |
| G4.1–G4.3 | ⚠️ | `todo_service.py:61-65` 未使用 repository 的分页查询，数据量大时内存溢出 |
| G5.1 | N/A | 无 MQ |
| G6.1–G6.2 | N/A | 无缓存 |
| G7.1–G7.2 | N/A | 无调度任务 |
| G8.1–G8.6 | ⚠️ | `auth_service.py:19-20` 内存存储用户/Token，进程重启即丢失；未持久化 |
| G9.1–G9.3 | N/A | 无外部调用 |
| G10.1–G10.2 | N/A | 无接口契约变更 |
| G11.1–G11.4 | ⚠️ | 测试存在但断言与实现行为不一致（见 test 文件分析） |
| G12.1–G12.2 | N/A | 无资金相关 |
| G13.1 | N/A | 无日志级别问题 |
| G14.1–G14.4 | N/A | 无国际化/多租户 |
| G15.1–G15.3 | N/A | 初始版本无灰度需求 |
| G16.1–G16.4 | ⚠️ | 无日志记录（关键操作无审计日志） |
| G17.1–G17.3 | N/A | 无应急需求（单实例内部工具） |

### 4.3 安全（`security-checklist.md`）

| ID | 状态 | 备注 |
|----|------|------|
| S1.1–S1.3 | N/A | 使用 ORM，无原生 SQL |
| S2.1–S2.3 | N/A | 无前端渲染 |
| S3.1–S3.3 | N/A | 无外部 URL |
| S4.1–S4.2 | N/A | 无系统命令 |
| S5.1–S5.2 | N/A | 无 XML |
| S6.1–S6.3 | N/A | 无反序列化风险 |
| S7.1–S7.3 | N/A | 无文件操作 |
| S8.1 | ❌ | `todo_controller.py:29` `current_user_id: int = 1` 硬编码，**无鉴权**，任何人可操作任何用户数据 |
| S8.2 | ✅ | 无 GET 执行增删改 |
| S8.3 | ⚠️ | 使用自增 ID，可预测遍历 |
| S8.4 | N/A | 无 Cookie |
| S9.1 | ⚠️ | `auth_service.py` 无硬编码密钥，但 Token 生成后存储于内存字典 |
| S9.2 | ✅ | 无敏感信息日志 |
| S9.3 | N/A | 无传输加密需求（内部工具） |
| S9.4 | ✅ | `secrets.token_hex(32)` 使用安全随机数 |
| S10.1–S10.3 | N/A | 无 CSRF/CORS 需求 |

---

## Step 5 — 自定义扩展检查（产物 E）

| ID | 状态 | 备注 |
|----|------|------|
| U1.1–U1.3 | N/A(未启用自定义规则) | 项目无自定义规则配置 |
| U2.1–U2.3 | N/A(未启用自定义规则) | 同上 |

---

## 终检（防漏检）

- [x] 执行队列中每个文件 `Step2`、`Step3` 均非 `⬜`（跳过文件除外）
- [x] Step 2 的每个 REQ 均非 `⬜`
- [x] Step 3 的 A1–A7 均非 `⬜`
- [x] Step 4 全部 G/S 与 B/M/I ID 均非 `⬜`（允许 N/A，已注明原因）
- [x] Step 5 全部 U* ID 均非 `⬜`（已注明 N/A 原因）
- [x] 所有 `❌/⚠️` 已写入 report，且包含 `ID + path:line`
