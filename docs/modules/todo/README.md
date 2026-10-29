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

承载待办的**创建**能力（本期最小闭环）：入参校验与规范化、身份与租户注入、功能开关与灰度、
限流、幂等判定、编号生成、落库、审计留痕、指标埋点与回执组装。列表/完成/删除等能力在后续阶段
以新增接口方式演进，复用同一 Service。

## 关键类

| 类 | 层 | 说明 |
|----|----|------|
| `TodoApplication` | 启动 | Spring Boot 入口，扫描 `com.antdigital.todo.dao.mapper` |
| `TodoController` | 交互 | `POST /api/todo/item/create`，解析身份头并组装统一响应体 |
| `TodoExceptionHandler` | 交互 | 异常 → 错误码 + HTTP 状态 + 统一响应体 |
| `TodoService` / `TodoServiceImpl` | 业务 | 创建链路编排（S01）、幂等查询（S02） |
| `TodoValidator` | 业务组件 | 入参校验与规范化（S03），规则 R01~R04 |
| `TodoCreateGate` | 业务组件 | 功能总开关与租户尾号灰度（§7.2/§7.3） |
| `TodoMetrics` | 业务组件 | 指标埋点出口（§6.5）：`todo_create_*` / `todo_no_degrade_total` / `todo_audit_degrade_total` |
| `TodoNoGenerator` / `TodoNoGeneratorImpl` | 业务组件 | 待办编号生成（S04） |
| `TodoSequenceProvider` / `SnowflakeSequenceProvider` | 业务组件 | 取号 SPI 与本地雪花降级实现（节点 ID 配置化） |
| `TodoRateLimiter` | 业务组件 | 单用户滑动窗口限流（R11，单实例口径） |
| `AuthIntegration` / `GatewayAuthIntegrationImpl` | 集成 | 登录态与身份解析（I01） |
| `GatewaySignatureVerifier` | 集成支撑 | 网关身份/权限头 HMAC-SHA256 签名校验（S8.1） |
| `AuditIntegration` / `AuditIntegrationImpl` | 集成 | 创建审计异步投递（I02），失败降级并计数 |
| `TodoItemMapper` | DAO | 幂等查询（含加锁读）与新增（MyBatis，XML 映射） |
| `TodoItemDO` | DAO | `todo_item` 数据对象，兼作查询条件载体 |

## 依赖关系

- 上游：统一网关。**身份与权限头必须由网关用共享密钥签名**，请求头清单：
  `X-User-Id`、`X-User-Name`、`X-Tenant-Id`、`X-User-Permissions`、
  `X-Gateway-Timestamp`、`X-Gateway-Sign`（`HMAC-SHA256(secret, timestamp\nuserId\nuserName\ntenantId\npermissions)`）。
- 存储：MySQL `todo_item`（单表写入，创建走主库）。
- 弱依赖：审计日志采集（失败降级为本地 ERROR 日志 + `todo_audit_degrade_total`）、
  取号中间件（不可用时降级雪花，计入 `todo_no_degrade_total`）。
- 可观测：`/actuator/prometheus` 暴露指标，供 §6.5 告警规则消费。

## API 接口

| 编号 | 方法 | 路径 | 说明 |
|------|------|------|------|
| W01 | POST | `/api/todo/item/create` | 新增待办事项，同一 `requestId` 重复提交返回首次结果 |

请求体：`requestId`（必填，≤64，字母/数字/`-`/`_`）、`itemName`（必填，1~128 字符）、
`itemDesc`（选填，≤512 字符）、`source`（选填，默认 `web`）。

响应体：`{ code, msg, data }`，`data` 含 `id`（String，规避前端 Number 精度丢失）、`todoNo`、`itemName`、
`itemDesc`、`status`、`creatorId`、`gmtCreate`（`yyyy-MM-dd HH:mm:ss`）、`idempotent`。

## 错误码

| 错误码 | HTTP | 说明 |
|--------|:----:|------|
| TODO_0001 | 400 | 请求参数缺失或格式非法 |
| TODO_0002 | 400 | 事项名称去空白后为空 |
| TODO_0003 | 400 | 事项名称超长（>128 字符） |
| TODO_0004 | 400 | 事项描述超长（>512 字符） |
| TODO_0005 | 401 | 未登录、身份/租户缺失或网关签名校验失败 |
| TODO_0006 | 403 | 无创建待办权限 |
| TODO_0007 | 500 | 待办编号生成冲突 |
| TODO_0008 | 500 | 存储异常，创建失败 |
| TODO_0009 | 429 | 请求过于频繁 |
| TODO_0010 | 503 | 功能未开放（总开关关闭或租户未命中灰度） |
| TODO_0011 | 500 | 未预期异常（与存储异常区分，便于排障） |

## 配置项

| 配置 | 默认值 | 说明 |
|------|--------|------|
| `auth.gateway.verify.enabled` | `true` | 是否校验网关签名（生产禁止关闭；密钥未配置时 fail-closed） |
| `auth.gateway.sign-secret` | 空 | 网关共享密钥，由配置中心注入 |
| `auth.gateway.timestamp-tolerance-seconds` | `300` | 签名时间戳允许偏差，超出视为重放 |
| `todo.create.enabled` | `true` | 创建功能总开关（§7.3 一级应急：关闭即止血） |
| `todo.create.gray.tenants` | 空 | 灰度租户尾号白名单（如 `1,3,5`），空表示全量开放 |
| `todo.create.rate-limit-per-minute` | `30` | 单用户每分钟创建上限 |
| `todo.no.worker-id` / `todo.no.datacenter-id` | `0` / `0` | 雪花节点 ID，多副本必须注入唯一组合 |
| `mybatis.mapper-locations` | `classpath*:mapper/*.xml` | 映射文件位置 |

## 限流口径说明

`TodoRateLimiter` 为**进程内滑动窗口**，额度按单实例计算：N 副本部署时单用户实际可用额度约为
`rate-limit-per-minute × N`。集群级「30 次/分钟」全局限额由**网关层限流**保证（系分 §6.3
「本地 + 网关双层」），需在网关配置单用户配额；若后续要求服务侧自主全局限额，应替换为
Redis/集中式令牌桶实现。该偏差已在系分 §6.3 与本文件明确标注，避免上线后口径误解。

## 告警指标

| 指标 | 说明 | 关联告警（§6.5） |
|------|------|------------------|
| `todo_create_total{code}` | 创建调用总数（含错误码维度） | 成功率 < 99% 持续 5 分钟 → P2 |
| `todo_create_cost_ms` | 创建耗时（P50/P99） | P99 > 500ms 持续 5 分钟 → P2 |
| `todo_create_idempotent_hit_total` | 幂等命中次数 | 幂等命中率观测 |
| `todo_create_rate_limited_total` | 限流次数 | 异常刷单识别 |
| `todo_create_gray_blocked_total` | 开关/灰度拦截次数 | 灰度与应急观测 |
| `todo_no_degrade_total` | 取号降级次数 | > 10 次/分钟 → P2 |
| `todo_audit_degrade_total` | 审计投递降级次数 | > 50 次/分钟 → P3 |

## 建表脚本

`src/main/resources/db/todo_item.sql`（全新增表，含唯一索引与查询索引）。
