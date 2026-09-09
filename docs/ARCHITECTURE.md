# 待办事项应用 架构文档

## 1. 项目概述

待办事项应用（todo-app）是一个轻量级的待办事项记录工具，面向内部用户，当前最小闭环仅支持创建待办事项。

## 2. 技术栈

| 组件 | 选型 | 版本 |
|------|------|------|
| 运行环境 | JDK | 21 |
| 框架 | Spring Boot | 3.2.0 |
| 持久层 | MyBatis + MySQL | 3.0.3 / 8.0+ |
| 构建工具 | Maven | 3.x |
| 测试框架 | JUnit 5 + Mockito + AssertJ | - |

## 3. 模块列表

| 模块 | 职责 | 状态 |
|------|------|------|
| todo-item | 待办事项的创建、数据持久化 | ✅ 已完成 |

## 4. 分层架构

```
com.dtazzy.todo
├── api/controller     # Web 层（REST 控制器）
├── api/request        # 请求对象
├── service/           # Service 接口
├── service/impl/      # Service 实现
├── dao/mapper/        # MyBatis Mapper
├── dao/entity/        # 数据对象（DO）
├── model/vo/          # 视图对象（VO）
└── common/exception/  # 自定义异常
```

## 5. 部署架构

- 应用层：双实例部署，Nginx 负载均衡
- 数据层：MySQL 单主库