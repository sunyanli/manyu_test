# todo-item 模块文档

## 模块职责

待办事项的创建和数据持久化，提供 REST API 供内部用户使用。

## 关键类说明

| 类 | 类型 | 说明 |
|----|------|------|
| `TodoItemController` | Controller | POST /api/todo-items 创建待办事项 |
| `TodoItemService` | 接口 | 业务服务接口 |
| `TodoItemServiceImpl` | 实现 | 业务逻辑校验 + 持久化 |
| `TodoItemMapper` | Mapper | MyBatis 数据访问 |
| `TodoItemDO` | 实体 | 对应 todo_item 表 |
| `TodoItemVO` | VO | API 响应视图对象 |
| `CreateTodoItemRequest` | DTO | 创建请求对象 |
| `BusinessException` | 异常 | 业务异常（携带错误码） |

## 依赖关系

- 依赖：MySQL 数据库
- 被依赖：无

## API 接口列表

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/todo-items | 新增待办事项 |

## 数据库

- 表名：`todo_item`
- 字段：id, tenant_id, name, description, gmt_create, gmt_modified
- 索引：pk_todo_item, idx_todo_item_tenant_id, idx_todo_item_gmt_create