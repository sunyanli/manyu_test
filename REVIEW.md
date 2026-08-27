# Project Review Profile

## Project

FastAPI + SQLite 单服务演示应用（三接口演示服务）。核心约定：统一 JSON 响应格式 `{success, data}` / `{success, error_code, message, detail}`，所有新增接口必须复用该格式。

## Gates

- **Scope**: 仅实现待办事项创建；不实现查询、修改、删除、状态、权限。
- **Request/Response**: 使用 Pydantic 模型；错误返回统一格式；成功返回 201 + `data` 包层。
- **Data**: SQLite 本地存储；SQL 必须使用参数化查询，禁止拼接。
- **Tests**: 新增接口必须有 pytest 覆盖，运行命令 `pytest tests/test_todos.py -v`。
- **No source edits during review**: review only.
