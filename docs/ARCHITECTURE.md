# 架构文档

> **文档元信息**
>
> | 项目 | 内容 |
> |------|------|
> | 文档版本 | v1.0 |
> | 作者 | AiWork |
> | 创建日期 | 2026/10/29 |
> | 适用范围 | 仓库 `sort-core`（含排序模块与待办服务模块） |

## 1. 仓库形态

单 Maven 模块仓库，`groupId=com.antdigital`、`artifactId=sort-core`，Java 8 编译目标。仓库内按业务包划分模块能力，模块之间**互不依赖**：

| 模块 | 根包 | 职责 | 依赖 |
|------|------|------|------|
| 排序核心（sort） | `com.antdigital.sort` | 纯 Java 排序算法与静态门面 | 无第三方运行时依赖 |
| 待办服务（todo） | `com.antdigital.todo` | 待办事项创建接口（oneapi REST），支撑后续列表/完成/删除演进 | Spring Boot 2.7.x、MyBatis、MySQL |

> 待办模块以新增包方式独立引入，不修改排序模块任何行为；两者无交叉依赖。

## 2. 待办服务分层

```
com.antdigital.todo
├── TodoApplication            启动类（@SpringBootApplication + @MapperScan）
├── controller                 交互层：REST 入口、统一异常处理
├── service                    业务层：TodoService 接口 + impl、component 子能力
├── integration                集成层：登录态解析（I01）、审计投递（I02）及其实现
├── dao                        DAO 层：entity 数据对象、mapper MyBatis 接口
├── model                      DTO / VO
└── common                     constant / enums / exception / util / model(统一响应体)
```

调用方向单向：controller → service → (component | integration | dao) → MySQL，禁止反向依赖。

## 3. 关键约束

- **身份可信**：`creatorId/creatorName/tenantId` 只取网关注入请求头，请求体中的同名字段一律忽略。
- **幂等**：`uk_todo_item_req(tenant_id, request_id)` 唯一索引为最终防线，冲突后重查返回首次结果。
- **编号**：`TD + yyyyMMdd + 6 位序号`，取号能力通过 `TodoSequenceProvider` 抽象，默认雪花实现，`uk_todo_item_no` 兜底。
- **弱依赖降级**：审计投递失败、取号降级均不阻断创建；登录态校验为强依赖，失败即快速失败。
- **错误码**：`TODO_0001~TODO_0009` 与系分错误码表一一对应，由 `TodoErrorCodeEnum` 唯一维护。
- **日志安全**：事项名称/描述原文不落日志，`TodoItemDO#toString` 与审计事件均做脱敏/隐藏。

## 4. 模块文档索引

- 待办事项模块：[docs/modules/todo/README.md](modules/todo/README.md)
