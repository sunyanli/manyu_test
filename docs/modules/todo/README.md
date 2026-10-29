# 待办事项模块（todo）

> **文档元信息**
>
> | 项目 | 内容 |
> |------|------|
> | 模块根包 | `com.antdigital.todo` |
> | 作者 | AiWork |
> | 创建日期 | 2026/10/29 |
> | 系分来源 | `.agents/20261029-目标_帮助用户记录日常待办事项_核心功能/design.md` |

## 模块职责

承载待办的**创建**能力（本期最小闭环）：入参校验与规范化、身份与租户注入、限流、幂等判定、
编号生成、落库、审计留痕与回执组装。列表/完成/删除等能力在后续阶段以新增接口方式演进，复用同一 Service。

## 关键类

| 类 | 层 | 说明 |
|----|----|------|
| `TodoApplication` | 启动 | Spring Boot 入口，扫描 `com.antdigital.todo.dao.mapper` |
| `TodoController` | 交互 | `POST /api/todo/item/create`，解析身份头并组装统一响应体 |
| `TodoExceptionHandler` | 交互 | 异常 → 错误码 + HTTP 状态 + 统一响应体 |
| `TodoService` / `TodoServiceImpl` | 业务 | 创建链路编排（S01）、幂等查询（S02） |
| `TodoValidator` | 业务组件 | 入参校验与规范化（S03），规则 R01~R04 |
| `TodoNoGenerator` / `TodoNoGeneratorImpl` | 业务组件 | 待办编号生成（S04） |
| `TodoSequenceProvider` / `SnowflakeSequenceProvider` | 业务组件 | 取号 SPI 与本地雪花降级实现 |
| `TodoRateLimiter` | 业务组件 | 单用户滑动窗口限流（R11） |
| `AuthIntegration` / `GatewayAuthIntegrationImpl` | 集成 | 登录态与身份解析（I01） |
| `AuditIntegration` / `AuditIntegrationImpl` | 集成 | 创建审计异步投递（I02），失败降级 |
| `TodoItemMapper` | DAO | 幂等查询与新增（MyBatis，XML 映射） |
| `TodoItemDO` | DAO | `todo_item` 数据对象，兼作查询条件载体 |

## 依赖关系

- 上游：统一网关（透传 `X-User-Id` / `X-User-Name` / `X-Tenant-Id` / `X-User-Roles`）。
- 存储：MySQL `todo_item`（单表写入，创建走主库）。
- 弱依赖：审计日志采集（失败降级为本地 ERROR 日志）、取号中间件（不可用时降级雪花）。

## API 接口

| 编号 | 方法 | 路径 | 说明 |
|------|------|------|------|
| W01 | POST | `/api/todo/item/create` | 新增待办事项，同一 `requestId` 重复提交返回首次结果 |

请求体：`requestId`（必填，≤64，字母/数字/`-`/`_`）、`itemName`（必填，1~128 字符）、
`itemDesc`（选填，≤512 字符）、`source`（选填，默认 `web`）。

响应体：`{ code, msg, data }`，`data` 含 `id`（String）、`todoNo`、`itemName`、`itemDesc`、
`status`、`creatorId`、`gmtCreate`（`yyyy-MM-dd HH:mm:ss`）、`idempotent`。

## 错误码

| 错误码 | HTTP | 说明 |
|--------|:----:|------|
| TODO_0001 | 400 | 请求参数缺失或格式非法 |
| TODO_0002 | 400 | 事项名称去空白后为空 |
| TODO_0003 | 400 | 事项名称超长（>128 字符） |
| TODO_0004 | 400 | 事项描述超长（>512 字符） |
| TODO_0005 | 401 | 未登录或登录态失效 |
| TODO_0006 | 403 | 无创建待办权限 |
| TODO_0007 | 500 | 待办编号生成冲突 |
| TODO_0008 | 500 | 存储异常，创建失败 |
| TODO_0009 | 429 | 请求过于频繁 |

## 配置项

| 配置 | 默认值 | 说明 |
|------|--------|------|
| `auth.verify.enabled` | `false` | 是否要求二次票据校验（信任网关时关闭） |
| `todo.create.rate-limit-per-minute` | `30` | 单用户每分钟创建上限 |
| `mybatis.mapper-locations` | `classpath*:mapper/*.xml` | 映射文件位置 |

## 建表脚本

`src/main/resources/db/todo_item.sql`（全新增表，含唯一索引与查询索引）。
