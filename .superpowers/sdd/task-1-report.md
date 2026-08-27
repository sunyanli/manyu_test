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
