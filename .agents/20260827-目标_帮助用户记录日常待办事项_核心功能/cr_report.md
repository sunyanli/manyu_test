# Code Review Report — 待办事项创建接口

Review summary

本次变更在现有 FastAPI 服务中新增 `POST /api/todos` 接口，用于内部用户创建待办事项。变更范围包括 `app.py`（新增 `todos` 表、`CreateTodoRequest` 模型、创建接口与 `RequestValidationError` 统一异常处理器）、`tests/test_todos.py`（8 个测试用例）和 `requirements.txt`（补充 pytest/httpx）。

Project profile
State: CREATED_AND_USED
Source: REVIEW.md
Notes: 仓库原不存在 REVIEW.md；本次评审前基于项目上下文（FastAPI + SQLite、统一 JSON 错误响应、最小创建闭环）新建项目评审画像。

Lane verdict table
| Lane | Verdict | Notes |
|---|---|---|
| align | APPROVE | Spec、计划、实现、测试四者一致，均围绕“仅创建”最小闭环展开。 |
| design | APPROVE_WITH_COMMENTS | 存在相对路径/导入时初始化、路径硬编码等设计债务，但不影响当前最小闭环。 |
| trim | APPROVE_WITH_COMMENTS | 少量可简化/未使用参数，但无冗余阻塞。 |
| cause | NOT_RUN | 本次变更非 bug 修复，无根因闭合声明。 |
| verify | APPROVE_WITH_COMMENTS | 8 个测试全部通过，边界覆盖较完整；存在少量可补充的测试场景。 |

Blocking findings

无。

Advisory findings

```text
[WARNING] [DESIGN] [BOUNDARY-LEAK] app.py:39 - DB_PATH/EXPORT_DIR 使用相对路径，且 init_db() 在模块导入时执行。
Evidence: DB_PATH = "tracking.db"、EXPORT_DIR = "exports"，init_db() 在 import 时调用。
Recommendation: 后续若支持多工作目录部署，可将路径改为基于环境变量或项目根目录；当前已通过测试 fixture 临时切换 cwd 来规避。
```

```text
[INFO] [TRIM] [UNUSED-ABSTRACTION] app.py:217 - create_todo 路由函数接收 request 参数但未使用。
Evidence: async def create_todo(request: Request, body: CreateTodoRequest) 中 request 未被读取。
Recommendation: 若短期内不需要从请求头读取调用者信息，可移除该参数以减少噪音。
```

```text
[WARNING] [DESIGN] [PATCH-ON-PATCH] app.py:374 - RequestValidationError 异常处理器通过硬编码路径 "api/todos" 分发错误码。
Evidence: if request.url.path == "/api/todos": error_code = "ERR_TODO_004" else "ERR_SYS_422"。
Recommendation: 若后续新增其他业务端点，需为每个端点继续扩展分支；可考虑在路由层统一封装或在异常信息中携带业务码，避免路径硬编码。
```

```text
[INFO] [VERIFY] [TEST-GAP] tests/test_todos.py - 缺少 description 为空字符串、name 仅含空白字符或特殊字符的场景覆盖。
Evidence: 现有测试覆盖 name 空、超长，description 超长、省略，以及缺 name 的 422 场景。
Recommendation: 可作为后续增强用例补充，以明确空字符串/空白字符串的契约行为。
```

Skipped lanes and reasons

- cause：本次变更仅为新增功能，无 bug 修复或根因闭合声明，因此未运行 Cause Lane。

Suggested next actions

1. 运行完整测试集确认无回归（已运行 `pytest tests/test_todos.py -v`，8 passed）。
2. 若后续扩展待办模块，建议将 DB 初始化与业务路由进一步解耦，并引入依赖注入或 repository 层。
3. 补充 description 空字符串、name 空白字符等边界测试。

VERDICT: APPROVE_WITH_COMMENTS
