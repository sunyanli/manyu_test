# 待办事项服务设计文档

## 1. 概述

为内部用户提供一个极简的 Web 服务，用于记录日常待办事项。本阶段最小闭环为：仅支持新增待办事项。

## 2. 目标与非目标

### 2.1 目标
- 提供一个 HTTP 接口创建待办事项。
- 保存事项名称和描述。
- 服务轻量、易于本地启动。

### 2.2 非目标
- 不支持查询、编辑、删除待办事项。
- 不支持多用户、权限、登录。
- 不部署到生产环境。

## 3. 用户故事

> 作为一名内部用户，我可以通过一个 Web 接口提交事项名称和描述，系统将其保存下来，以便后续查看。

## 4. 技术栈

| 组件 | 选择 | 理由 |
|------|------|------|
| Web 框架 | FastAPI | 原生支持数据校验与自动生成 OpenAPI 文档 |
| 数据库 | SQLite 文件 | 零外部依赖、本地即可运行 |
| 数据校验 | Pydantic | 与 FastAPI 集成，减少样板代码 |
| 服务器 | uvicorn | ASGI 实现，轻量 |

## 5. 目录结构

```
todo_service/
  __init__.py    # 包入口
  main.py        # FastAPI 应用与路由
  models.py      # SQLAlchemy 数据模型
  schemas.py     # Pydantic 请求/响应模型
  database.py    # 数据库连接与依赖注入
tests/
  test_main.py   # 创建接口测试
```

## 6. API 设计

### 6.1 创建待办事项

- **URL**: `POST /todos`
- **Content-Type**: `application/json`

**请求体**

```json
{
  "name": "整理周报",
  "description": "汇总本周工作进展"
}
```

- `name`（必填，字符串）：事项名称
- `description`（可选，字符串）：事项描述

**成功响应 201 Created**

```json
{
  "id": 1,
  "name": "整理周报",
  "description": "汇总本周工作进展",
  "created_at": "2026-08-27T10:00:00"
}
```

## 7. 数据模型

### 7.1 数据表 `todos`

| 字段 | 类型 | 约束 |
|------|------|------|
| id | INTEGER | 主键、自增 |
| name | VARCHAR | 非空 |
| description | TEXT | 允许为空 |
| created_at | DATETIME | 默认当前时间 |

### 7.2 Pydantic 模型

- `TodoCreate`：创建请求模型，包含 `name` 和 `description`。
- `TodoRead`：响应模型，额外包含 `id` 和 `created_at`。

## 8. 数据流

1. 客户端向 `POST /todos` 发送 JSON。
2. FastAPI 使用 Pydantic 校验请求体。
3. 创建数据库会话，写入 `todos` 表。
4. 提交事务并返回新记录。

## 9. 错误处理

| 场景 | 返回 |
|------|------|
| 请求体缺少 `name` 或类型错误 | 422 Unprocessable Entity |
| 数据库写入异常 | 500 Internal Server Error |

## 10. 测试

使用 `fastapi.testclient.TestClient` 编写测试：

- 正常创建请求返回 201 且包含 id。
- 缺少 `name` 返回 422。
- 使用内存 SQLite 数据库隔离测试数据。

## 11. 启动方式

```bash
uvicorn todo_service.main:app --reload
```

## 12. 后续可扩展方向

- 查询待办事项列表（GET /todos）
- 更新/删除待办事项
- 状态字段（待办/已完成）
