# TODO 应用 — 新增待办事项

## 目标
帮助用户记录日常待办事项。最小闭环：仅创建（新增待办事项）。

## 技术栈
- Python 3
- SQLite（持久化存储）
- CLI 命令行交互

## 全局约束
- 事项信息包含：事项名称（title，必填）和描述（description，可选）
- 创建成功后返回确认信息，包含 ID 和创建时间
- 数据库表自动初始化（幂等建表）
- 数据库文件：`todos.db`，放在当前工作目录

## 任务列表

### Task 1: 实现 TODO 创建功能
**产出文件：** `todo.py`
**功能规格：**
- 提供 `add_todo(title: str, description: str = "") -> dict` 函数，将待办事项写入 SQLite
- 数据库表 `todos` 结构：`id INTEGER PRIMARY KEY AUTOINCREMENT`, `title TEXT NOT NULL`, `description TEXT DEFAULT ''`, `created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP`
- 返回新建事项的完整字典：`{"id": int, "title": str, "description": str, "created_at": str}`
- `init_db()` 函数：幂等建表，可多次调用不报错
- `main()` CLI 入口：`python todo.py add "标题" "描述"` 添加待办事项
- 包含单元测试 `test_todo.py`
- 测试覆盖：正常创建、空标题拒绝、仅标题创建、带描述创建