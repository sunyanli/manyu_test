# 代码评审问题修复报告（todo-create）

> **修复对象** `.agents/20261029-目标_帮助用户记录日常待办事项_核心功能/cr_report.md`（P0=3 / P1=6 / P2=10）
> **分支** `task/DEV-966dcd0a-7905-11f1-9649-3b4281182f10-27fdbcad-6a5f-4d77-a539-94db5a84337e` · **日期** `2026-10-29` · **执行者** AiWork
> **规范依据** `dtazziboot-java-coding-standards`（naming / exception-logging / security / mysql / unit-testing / constants / formatting）
> **验证** `mvn -B test` → **Tests run: 111, Failures: 0, Errors: 0, Skipped: 0 · BUILD SUCCESS**（JDK 17.0.14 + Maven 3.9.9，真实执行，非静态推断）

---

## 1. 修复总览

| 等级 | 数量 | 已修复 | 未修复 | 说明 |
|------|:----:|:------:|:------:|------|
| P0 | 3 | 3 | 0 | 安全红线、可灰度/应急、可监控三支柱全部落地 |
| P1 | 6 | 6 | 0 | 租户缺失、事务边界、冲突解析、雪花节点、审计告警、限流口径 |
| P2 | 10 | 10 | 0 | 时区、错误码、配置接线、内存淘汰、停机排空、行宽、死常量、测试缺口、忙等 |

---

## 2. P0 修复

### P0-1 身份与权限不再信任客户端可控请求头

| 项 | 内容 |
|----|------|
| 新增 | `integration/support/GatewaySignatureVerifier.java` — 网关注入载荷 `HMAC-SHA256(secret, timestamp\nuserId\nuserName\ntenantId\npermissions)` 校验 + 时间戳重放窗口 + 常量时间比较（`MessageDigest.isEqual`） |
| 改造 | `integration/impl/GatewayAuthIntegrationImpl.java` — 先取 `X-User-Id`/`X-Tenant-Id`（缺失即 TODO_0005），再校验签名，最后才构造 `OperatorContext`；权限点改读 `X-User-Permissions`（网关从统一权限平台解析后签名透传） |
| 改造 | `service/impl/TodoServiceImpl#checkPermission` — 判定源为已验签的权限点列表，客户端自加 `*`/`TODO_CREATE` 因签名不匹配被拒 |
| 配置 | `auth.gateway.verify.enabled` 默认 **true**；`auth.gateway.sign-secret` 由配置中心注入；**密钥缺失时 fail-closed**（记 ERROR + TODO_0005），杜绝「默认信任客户端头」 |
| 删除 | `X-User-Roles` 头与 `auth.verify.enabled` 旧开关（全仓库无残留引用） |
| 测试 | `GatewaySignatureVerifierTest`（7）：签名通过/缺失/载荷篡改/时间戳越界/密钥未配置/时间戳非数字/开关关闭；`TodoControllerTest`（7）：伪造头无签名、篡改权限、密钥未配置均 TODO_0005 |

### P0-2 功能总开关与租户尾号灰度

| 项 | 内容 |
|----|------|
| 新增 | `service/component/TodoCreateGate.java` — `todo.create.enabled` 总开关 + `todo.create.gray.tenants` 尾号白名单（空=全量） |
| 接线 | `TodoServiceImpl#doCreate` 在身份校验后、权限校验前调用 `todoCreateGate.check(tenantId)`；未开放返回 **TODO_0010（HTTP 503）**，不再复用限流错误码 |
| 配置 | `application.yml` 补齐 `todo.create.enabled` / `todo.create.gray.tenants`（支持环境变量覆盖） |
| 测试 | `TodoCreateGateTest`（4）+ `TodoServiceImplTest` 三例（开关关闭、未命中灰度、命中灰度） |

### P0-3 指标埋点（系分 §6.5 五项指标）

| 项 | 内容 |
|----|------|
| 依赖 | `pom.xml` 增加 `spring-boot-starter-actuator`、`micrometer-registry-prometheus` |
| 新增 | `service/component/TodoMetrics.java` — `todo_create_total{code}`、`todo_create_cost_ms`（P50/P99 分布摘要）、`todo_create_idempotent_hit_total`、`todo_create_rate_limited_total`、`todo_no_degrade_total`、`todo_create_gray_blocked_total`、`todo_audit_degrade_total` |
| 接线 | 创建入口成功/失败计时与错误码维度计数；幂等命中；取号降级（`TodoSequenceProvider#isDegraded`）；审计降级；灰度拦截 |
| 暴露 | `management.endpoints.web.exposure.include=health,info,metrics,prometheus` + `application` 公共标签 |
| 测试 | `TodoServiceImplTest`（成功/存储异常/限流/幂等指标断言）、`TodoNoGeneratorImplTest`（降级指标） |

> 告警规则（成功率、P99、TODO_0008 次数、降级次数、审计失败次数）在监控平台侧按上表指标配置；本仓库交付**数据源**。

---

## 3. P1 修复

| 编号 | 修复动作 | 位置 |
|------|----------|------|
| P1-1 | 删除 `tenantId` 静默回落 `"0"`，`userId`/`tenantId` 任一缺失即 TODO_0005；`TodoServiceImpl#validateOperator` 同步兜底 | `GatewayAuthIntegrationImpl`、`TodoServiceImpl` |
| P1-2 | 审计改为**事务提交后**投递：`TransactionSynchronizationManager.registerSynchronization(...afterCommit)`，无事务上下文时直接投递 | `TodoServiceImpl#auditAfterCommit` |
| P1-3 | 唯一键冲突**先**按 `(tenant_id, request_id)` 加锁回查（新增 `selectByRequestIdForUpdate`，`FOR UPDATE` 当前读），命中即返回首次结果；仅当确认非幂等键冲突才重试取号。索引名解析降级为日志诊断依据 | `TodoItemManager`、`TodoItemMapper(.xml)`、`TodoServiceImpl` |
| P1-4 | 删除 `(0L, 0L)` 默认构造，雪花节点改为 `todo.no.worker-id` / `todo.no.datacenter-id` 注入，节点为 0/0 时输出 WARN；保留 6 位序号（系分 A09 格式约定），唯一性仍由 `uk_todo_item_no` + 单次重试兜底 | `SnowflakeSequenceProvider` |
| P1-5 | 审计队列满/消费异常/停机丢弃均计入 `todo_audit_degrade_total`（ERROR 日志 + 指标），满足 R10「ERROR 日志 + 监控告警」 | `AuditIntegrationImpl` |
| P1-6 | 明确双层限流落点：限流阈值接线 `todo.create.rate-limit-per-minute`，并在 `docs/modules/todo/README.md` 增设「限流口径说明」，标注单实例口径与网关全局限额的边界 | `TodoRateLimiter`、模块文档 |

---

## 4. P2 修复

| 编号 | 修复动作 |
|------|----------|
| P2-1 | 新增 `TodoConstants.BIZ_ZONE`（`Asia/Shanghai`，与 `serverTimezone` 一致），`gmt_create` 用 `LocalDateTime.now(BIZ_ZONE)`、编号日期用 `LocalDate.now(BIZ_ZONE)`，消除对 JVM 默认时区依赖 |
| P2-2 | 新增错误码 `TODO_0011 UNKNOWN_ERROR`；兜底 `Exception` 处理改映射到它，与 `TODO_0008` 存储异常区分 |
| P2-3 | `TodoRateLimiter` 生产构造改为 `@Value("${todo.create.rate-limit-per-minute:30}")`，配置生效 |
| P2-4 | 保留 VO 层 `id` 为 String（规避前端精度丢失），同步修订 `design.md` W01 出参表 `data.id` → `String` |
| P2-5 | 限流器新增空闲用户条目回收（用户数超 `RATE_LIMIT_IDLE_SCAN_THRESHOLD=1024` 时触发全量清理），消除 Map 无界增长 |
| P2-6 | 停机改为「先停止拉取 → join 排空 → 超时再中断」，未投递事件计数并 ERROR 告警 |
| P2-7 | 3 处超长行按 120 字符换行（`GatewayAuthIntegrationImpl`、`TodoValidatorTest`）；全量 `src` 复查 0 处超限 |
| P2-8 | 删除未引用常量 `SNOWFLAKE_WORKER_ID_BITS`、`TODO_NO_FALLBACK_SUFFIX_LENGTH` 与已失效的 `DEFAULT_TENANT_ID` |
| P2-9 | 新增用例：索引名不可识别仍返回首次结果、跨租户隔离、租户缺失 TODO_0005、功能开关/灰度、签名正反向；`TodoItemManagerTest` 覆盖加锁回查 |
| P2-10 | `waitUntil` 改短 `Thread.sleep`（1ms 上限）替代自旋；时钟回拨超 5s 快速失败并 ERROR 记录 |

---

## 5. 变更文件清单

**主代码（新增 4）**

- `src/main/java/com/antdigital/todo/integration/support/GatewaySignatureVerifier.java`
- `src/main/java/com/antdigital/todo/service/component/TodoCreateGate.java`
- `src/main/java/com/antdigital/todo/service/component/TodoMetrics.java`

**主代码（修改 17）**

- `pom.xml`、`src/main/resources/application.yml`、`src/main/resources/mapper/TodoItemMapper.xml`、`src/main/resources/db/todo_item.sql`
- `common/constant/TodoConstants.java`、`common/enums/TodoErrorCodeEnum.java`
- `controller/TodoController.java`、`controller/TodoExceptionHandler.java`
- `dao/manager/TodoItemManager.java`、`dao/mapper/TodoItemMapper.java`
- `integration/AuthIntegration.java`、`integration/impl/GatewayAuthIntegrationImpl.java`、`integration/impl/AuditIntegrationImpl.java`
- `service/impl/TodoServiceImpl.java`
- `service/component/SnowflakeSequenceProvider.java`、`service/component/TodoNoGeneratorImpl.java`、`service/component/TodoRateLimiter.java`、`service/component/TodoSequenceProvider.java`

**测试（新增 3 / 修改 5）**

- 新增：`integration/support/GatewaySignatureVerifierTest.java`、`integration/support/GatewaySignatureVerifierTestSupport.java`、`service/component/TodoCreateGateTest.java`
- 修改：`controller/TodoControllerTest.java`、`dao/manager/TodoItemManagerTest.java`、`service/component/TodoNoGeneratorImplTest.java`、`service/component/TodoValidatorTest.java`、`service/impl/TodoServiceImplTest.java`

**文档（修改 3）**

- `docs/ARCHITECTURE.md`（关键约束更新为签名信任、指标、开关灰度、错误码扩展、时区）
- `docs/modules/todo/README.md`（请求头签名载荷、错误码 TODO_0010/0011、全部配置项、限流口径、告警指标表）
- `.agents/20261029-.../design.md`（`data.id` → String、错误码表补 TODO_0010/0011）

---

## 6. 验证证据

| 验证项 | 命令 | 结果 |
|--------|------|------|
| 编译 | `mvn -B -DskipTests compile` | BUILD SUCCESS（43 个源文件） |
| 单元测试 | `mvn -B test` | **Tests run: 111, Failures: 0, Errors: 0, Skipped: 0** |

各测试类结果：

| 测试类 | 用例数 | 结果 |
|--------|:------:|:----:|
| `SortUtilTest`（既有） | 21 | ✅ |
| `TodoTextUtilTest` | 12 | ✅ |
| `TodoValidatorTest` | 22 | ✅ |
| `TodoControllerTest` | 7 | ✅ |
| `GatewaySignatureVerifierTest` | 7 | ✅ |
| `TodoItemManagerTest` | 5 | ✅ |
| `TodoCreateGateTest` | 4 | ✅ |
| `TodoNoGeneratorImplTest` | 9 | ✅ |
| `TodoServiceImplTest` | 24 | ✅ |

静态复查：`src` 全量 Java 文件 0 处行宽 > 120 字符；无 `X-User-Roles` / `auth.verify.enabled` / `DEFAULT_TENANT_ID` 残留引用；SQL 全 `#{}` 参数化。

---

## 7. 残留风险与外部依赖

1. **网关需同步改造**：签名载荷与密钥分发由网关侧实现，本仓库只做校验；上线前须与网关确认 `X-Gateway-Sign`/`X-Gateway-Timestamp`/`X-User-Permissions` 的生成与本实现字节级一致（`\n` 分隔、权限点顺序按请求头原文）。
2. **权限点仍需权限平台配置**：服务端校验的是网关签名透传的权限点，未在权限平台初始化「待办-创建」权限点的账号将返回 TODO_0006。
3. **未直连 MySQL 验证**：`selectByRequestIdForUpdate` 的 `FOR UPDATE` 行为、并发幂等回查在真实 InnoDB 下的表现未做集成测试（本仓库无 DB 依赖测试基建）；建议在预发环境执行并发 200 同 `requestId` 压测。
4. **限流为单实例口径**：集群全局限额依赖网关层，已在文档明确标注；若要求服务侧自主全局限额，需引入 Redis 令牌桶。
5. **告警规则未落地为代码**：本仓库交付指标数据源，成功率/P99 等告警阈值需在监控平台按 §6.5 配置。
