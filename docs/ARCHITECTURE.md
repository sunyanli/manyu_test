# 架构文档

## 应用概览

待办事项服务（todo-service）：帮助内部用户记录日常待办事项，当前最小闭环为「新增待办事项」。

## 模块列表

| 模块 | 文档 | 职责 |
|------|------|------|
| todo | `docs/modules/todo/README.md` | 待办事项新增（创建） |

## 分层架构

```
api/controller（Web 层，参数校验）
    ↓
service/service.impl（业务层，业务校验与转换）
    ↓
dao/mapper（DAO 层，MyBatis）→ MySQL（todo 表）
```

## 公共约束

- 统一响应：`Result<T>`，code 为 `SUCCESS` 或 5 位错误码（A/B/C + 4 位数字）
- 异常：业务异常 `BizException`，由 `GlobalExceptionHandler` 统一转化
- 日志：SLF4J + 占位符
- 数据库：表必备 id/gmt_create/gmt_modified；SQL 参数化 `#{}`
