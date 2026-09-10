# Code Review Checklist

> **Change** `todo-create` · **分支/Commit** `AI/task-DEV-eecb99b0-9b79-11f1-a438-852be3c2a082-7628fb0e-ecff-42a7-8075-a0a772ecfc5b` · **日期** `2026-09-10`

> **AI**：唯一进度源；状态仅用 `⬜` `✅` `❌` `⚠️` `N/A`。

---

## Step 1: 执行队列

| # | 文件路径 | 归属原因 | 状态 |
|---|---------|---------|------|
| 1 | src/main/java/com/example/todo/TodoApplication.java | Spring Boot 启动类 | ✅ 已审 |
| 2 | src/main/java/com/example/todo/common/ApiResponse.java | 统一响应封装 | ✅ 已审 |
| 3 | src/main/java/com/example/todo/common/GlobalExceptionHandler.java | 全局异常处理 | ⚠️ 已审有问题 |
| 4 | src/main/java/com/example/todo/controller/TodoItemController.java | REST 控制器 | ✅ 已审 |
| 5 | src/main/java/com/example/todo/dto/TodoCreateRequest.java | 创建请求 DTO | ✅ 已审 |
| 6 | src/main/java/com/example/todo/entity/TodoItem.java | 实体类 | ✅ 已审 |
| 7 | src/main/java/com/example/todo/mapper/TodoItemMapper.java | MyBatis-Plus Mapper | ✅ 已审 |
| 8 | src/main/java/com/example/todo/service/TodoItemService.java | Service 接口 | ✅ 已审 |
| 9 | src/main/java/com/example/todo/service/impl/TodoItemServiceImpl.java | Service 实现 | ⚠️ 已审有问题 |

---

## Step 2: 功能性检查 (REQ)

> 来源：需求描述 — "核心功能：新增待办事项。任务信息：事项名称和描述。最小闭环：仅创建"

- [x] REQ-1: 提供 POST 接口用于新增待办事项 → ✅ TodoItemController.java:21-25 提供 POST /api/todo-items
- [x] REQ-2: 请求体包含事项名称(title)和描述(description)字段 → ✅ TodoCreateRequest.java:12,15
- [x] REQ-3: 事项名称必填校验 → ✅ TodoCreateRequest.java:10 @NotBlank
- [x] REQ-4: 创建成功后返回完整的待办事项对象(含id、createTime等) → ✅ TodoItemServiceImpl.java:20-26 insert 后返回含 id 的实体
- [x] REQ-5: 数据持久化到数据库 → ✅ TodoItemMapper.java:8 继承 BaseMapper.insert

---

## Step 3: 可读性检查 (A1–A7)

- [x] A1 源文件格式 → ✅ UTF-8、4空格缩进、格式一致
- [x] A2 命名规范 → ✅ 类名大驼峰、方法小驼峰、包名合规
- [x] A3 注释与文档 → ⚠️ P2: 所有公共类和方法缺少 Javadoc 注释
- [x] A4 代码组织 → ✅ import 无通配符、方法体简短、分层清晰
- [x] A5 日志规范 → ❌ P1: GlobalExceptionHandler.java:26 handleException 未记录异常日志
- [x] A6 魔法值 → ⚠️ P2: ApiResponse.java:19,23 硬编码状态码 200/400/500
- [x] A7 其他风格 → ✅ 无明显风格问题

---

## Step 4: 可靠性检查

### 自动化预扫结果
```
[P1] M016 — JavaTimeDefaultTimeZone: TodoItemServiceImpl.java:23
[P1] M016 — JavaTimeDefaultTimeZone: TodoItemServiceImpl.java:24
```

### G 可靠性（军规）
- [x] G1 并发控制 → N/A
- [x] G2 超时/重试/限流 → N/A
- [x] G3 资源释放 → N/A
- [x] G4 事务边界 → ⚠️ P2: TodoItemServiceImpl.java:19 createTodo 未加 @Transactional
- [x] G5 幂等 → N/A
- [x] G6 边界条件 → ✅ DTO 校验完备
- [x] G7 灰度/监控/应急 → ❌ P1: GlobalExceptionHandler.java:26 通用异常未记录堆栈日志

### S 安全
- [x] S1 SQL 注入 → ✅ MyBatis-Plus 参数化绑定
- [x] S2 认证/授权 → N/A（内部用户最小闭环）
- [x] S3 输入校验 → ✅ @Valid + @NotBlank + @Size
- [x] S4 密钥泄露 → ⚠️ 提醒: 确认 application.yml 数据库密码是否明文存储
- [x] S5 依赖安全 → N/A

### B/M/I Bug 模式
- [x] M016 JavaTimeDefaultTimeZone → ❌ P1 x2: TodoItemServiceImpl.java:23,24 LocalDateTime.now() 未指定时区

---

## Step 5: 自定义扩展检查

- N/A(未启用自定义规则)
