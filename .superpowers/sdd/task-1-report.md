# Task 1 实施报告：待办事项创建接口

## What you implemented

- 在 `app.py` 的 `init_db()` 中新增 `todos` 表：`id TEXT PRIMARY KEY`、`name TEXT NOT NULL`、`description TEXT`、`created_at TEXT`。
- 新增 `CreateTodoRequest` Pydantic 请求模型，支持必填 `name` 与可选 `description`。
- 新增 `POST /api/todos`：
  - 成功返回 HTTP 201，以及 `{success: true, data: {id, name, description, created_at}}`。
  - 使用 UUID 作为主键，使用 UTC ISO 8601（`Z` 后缀）时间戳。
  - 名称为空或超过 100 字符时返回 HTTP 400、`ERR_TODO_001`。
  - 描述超过 500 字符时返回 HTTP 400、`ERR_TODO_002`。
  - 数据库写入异常返回统一格式的 HTTP 500、`ERR_TODO_003`。
- 在 `requirements.txt` 中加入 `pytest==7.4.4` 与 `httpx==0.25.2`。
- 新增简报指定的四个接口测试。

## What you tested and test results

### RED

在新增测试、尚未实现接口时运行：

```text
PYTHONPATH=. .venv/bin/python -m pytest tests/test_todos.py -v
```

结果：4 tests collected，4 FAILED；失败原因均为 `/api/todos` 尚不存在并返回 404。

### GREEN

实现接口后运行：

```text
.venv/bin/python -m pytest tests/test_todos.py -v
```

结果：`4 passed in 0.31s`。

另外曾用临时根级 `conftest.py` 验证简报指定的 `pytest tests/test_todos.py -v` 入口可通过；该临时文件未纳入最终交付。SQLite schema 检查确认运行 `app.init_db()` 后存在 `todos` 表，字段为 `id`、`name`、`description`、`created_at`，其中 `id` 为主键、`name` 为 NOT NULL。

## Files changed

- `app.py` — 数据表初始化、请求模型、创建接口。
- `requirements.txt` — pytest/httpx 测试依赖。
- `tests/test_todos.py` — 成功、名称为空、名称过长、描述过长四个测试。
- `.superpowers/sdd/task-1-report.md` — 本报告。

## Self-review findings

- 接口只新增待办创建能力，没有实现列表、更新、删除、状态或认证功能。
- 校验、错误码、响应字段、UUID 与 UTC 时间戳均与任务简报一致。
- SQL 使用参数绑定，避免将用户字段直接拼接进 SQL。
- `todos` 表通过 `CREATE TABLE IF NOT EXISTS` 初始化，对已有数据库兼容。
- 测试依赖已固定为简报指定版本。

## Issues or concerns

- 宿主 Python 使用 PEP 668 的 externally-managed 环境，因此测试依赖安装在未纳入版本控制的本地 `.venv/` 中；仓库提交不包含该目录。
- 使用 `.venv/bin/python -m pytest tests/test_todos.py -v` 运行测试，确保项目根目录可导入；该方式已验证通过。
- `tracking.db` 为现有二进制数据库；测试中由 `init_db()` 创建 `todos` 表并完成 schema 验证，但不提交本地生成的数据库状态或测试数据。部署/运行应用时，`init_db()` 会自动创建该表。

---

# 修复报告（任务评审 Important 项）

## What you changed

1. **`app.py` — 新增 `RequestValidationError` 统一异常处理器**
   - 在异常处理器区域新增 `request_validation_exception_handler`，将所有 Pydantic 预路由校验失败转换为统一错误格式 `{success, error_code, message, detail}`，HTTP 状态码 422。
   - 对 `/api/todos` 请求使用待办专属错误码 `ERR_TODO_004`，消息为“待办请求数据无效，请检查名称和描述”；其他端点兜底使用 `ERR_SYS_422`，保持处理器通用、可复用。
   - 校验失败明细（`detail`）保留 `exc.errors()` 的结构化列表，便于排查字段错误。
   - 覆盖三类触发路径：缺少必填 `name` 字段、`name` 类型错误、请求体 JSON 畸形。
2. **`tests/test_todos.py` — 补充测试覆盖**
   - 成功创建后直接查询 SQLite `todos` 表，断言存在 id 对应且四字段与响应一致的行（对库中已有历史数据鲁棒，只按返回的 id 查询）。
   - 新增 `test_create_todo_without_description`：省略 `description` 成功，`description` 为 `null`。
   - 新增 `test_create_todo_name_boundary_lengths`：100 字符通过（201）、101 字符失败（400，`ERR_TODO_001`）。
   - 新增 `test_create_todo_description_boundary_lengths`：500 字符通过（201）、501 字符失败（400，`ERR_TODO_002`）。
   - 新增 `test_create_todo_missing_name_uses_unified_validation_error`：缺 `name` 返回 422，且为统一格式（`success=false`、`error_code=ERR_TODO_004`、`detail` 为列表）。
   - 原 4 个测试保持不变。

## Test command and output

```text
$ .venv/bin/python -m pytest tests/test_todos.py -v
============================= test session starts ==============================
platform linux -- Python 3.12.3, pytest-7.4.4, pluggy-1.6.0
collected 8 items

tests/test_todos.py::test_create_todo_success PASSED                     [ 12%]
tests/test_todos.py::test_create_todo_without_description PASSED         [ 25%]
tests/test_todos.py::test_create_todo_name_boundary_lengths PASSED       [ 37%]
tests/test_todos.py::test_create_todo_description_boundary_lengths PASSED [ 50%]
tests/test_todos.py::test_create_todo_missing_name_uses_unified_validation_error PASSED [ 62%]
tests/test_todos.py::test_create_todo_name_empty PASSED                  [ 75%]
tests/test_todos.py::test_create_todo_name_too_long PASSED               [ 87%]
tests/test_todos.py::test_create_todo_description_too_long PASSED        [100%]

============================== 8 passed in 0.38s ===============================
```

补充实路径诊断（畸形体/类型错误/空体，均为 422 统一格式，`error_code=ERR_TODO_004`）：

```text
malformed json: 422 {"success":false,"error_code":"ERR_TODO_004","message":"待办请求数据无效，请检查名称和描述","detail":[...]}
wrong type name: 422 {"success":false,"error_code":"ERR_TODO_004","message":"待办请求数据无效，请检查名称和描述","detail":[...]}
empty body:     422 {"success":false,"error_code":"ERR_TODO_004","message":"待办请求数据无效，请检查名称和描述","detail":[...]}
```

## Any remaining concerns

- 本环境没有预置 `.venv`，测试在临时本地 `.venv`（非版本控制）中运行，执行完毕已删除；仓库中 `app.py`/`tests/test_todos.py` 之外无残留。
- 测试将待办记录写入现有 `tracking.db`；`tracking.db` 是已跟踪文件，测试写入已还原，提交中不含测试数据。
- 通用（非 todo）端点的 `RequestValidationError` 使用 `ERR_SYS_422` 兜底，未逐端点设计错误码，符合“保持处理器通用”的要求。
- 描述为 `None` 时路由内 `len()` 不会执行，`null` 描述可正常入库（已有测试覆盖省略场景）。

## Files changed

- `app.py` — 新增 `RequestValidationError` 异常处理器（+1 import，+20 行）。
- `tests/test_todos.py` — 新增 4 个测试，并将成功测试扩展为校验数据库落库（+58 行）。
- `.superpowers/sdd/task-1-report.md` — 追加本修复报告。
