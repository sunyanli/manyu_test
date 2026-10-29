# Code Review Report

> **Change** `todo-create`（新增待办事项最小闭环） · **分支/Commit** `task/DEV-966dcd0a-7905-11f1-9649-3b4281182f10-27fdbcad-6a5f-4d77-a539-94db5a84337e` / `009d45d` · **日期** `2026-10-29` · **审查者** AI
>
> **AI**：等级 **P0 / P1 / P2**；G/S 以 checklist 行内定义为准；Bug 模式 Blocker→P0、Major→P1、Info→P2。已**先**运行 `scan-all-rules.sh`（输出见 §4/§5 备注），再出 LLM 结论。
>
> **变更驱动源**：`.agents/20261029-目标_帮助用户记录日常待办事项_核心功能/2026-10-29-todo-create-cr-checklist.md`（Step1–Step5 逐 ID 核销已完成，`⬜ 待审 = 0`）。

---

## 1. 审查范围

| 项 | 值 |
|----|-----|
| `.java` 文件数 | `39`（主代码 33 + 测试 6） |
| 变更行数 | `+4680 / -0`（全部为新增文件） |
| 非 Java 变更 | `pom.xml`、`application.yml`、`TodoItemMapper.xml`、`db/todo_item.sql`、`docs/*`、`.agents/*`（作证据引用，不计入 Java 队列） |

| 类/接口 | 路径 | 角色 |
|---------|------|------|
| `TodoController` | `src/main/java/com/antdigital/todo/controller/TodoController.java` | W01 入口 `POST /api/todo/item/create` |
| `TodoExceptionHandler` | `src/main/java/com/antdigital/todo/controller/TodoExceptionHandler.java` | 异常 → 错误码 + HTTP 状态 |
| `TodoService`/`TodoServiceImpl` | `src/main/java/com/antdigital/todo/service/` | S01 创建编排 / S02 幂等查询 |
| `TodoValidator` | `.../service/component/TodoValidator.java` | S03 校验与规范化（R01–R04） |
| `TodoNoGeneratorImpl` / `SnowflakeSequenceProvider` | `.../service/component/` | S04 编号生成 + 取号降级 |
| `TodoRateLimiter` | `.../service/component/TodoRateLimiter.java` | R11 单用户限流 |
| `GatewayAuthIntegrationImpl` | `.../integration/impl/` | I01 身份解析（R05） |
| `AuditIntegrationImpl` | `.../integration/impl/` | I02 审计异步投递（R10） |
| `TodoItemManager` | `.../dao/manager/TodoItemManager.java` | 唯一键冲突语义转换 |
| `TodoItemMapper` / `TodoItemMapper.xml` | `.../dao/mapper/` + `resources/mapper/` | `#{}` 参数化 SQL |
| `TodoItemDO` | `.../dao/entity/TodoItemDO.java` | 数据对象 + 查询条件载体 |
| 其余枚举/常量/工具/DTO/VO | `common/**`、`model/**` | 支撑类型 |

---

## 2. 问题计数

| P0 | P1 | P2 |
|----|----|-----|
| 3 | 6 | 10 |

> `scan-all-rules.sh` 原始预扫 10 条（P0=7 / P1=1 / P2=2），复核后 **7 条 `G16.2 CatchWithoutLogging` 为误报**（脚本仅匹配 `log.*` 变量名，本仓库统一使用 `LOGGER`，逐条核对 catch 内均有日志），不计入计数。上表为 LLM 复核后的最终结论。

---

## 3. Step 2 — 功能（REQ）

### REQ-1: `F01 新增待办事项（名称 + 描述落库）`

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| Given 合法入参与可信身份，When 调用 W01，Then 落库一条 `todo_item` 并返回 `todoNo`/`status=0`/`gmtCreate` | ✅ | design §5.1.2 W01「POST /api/todo/item/create」；§5.1.3.1 时序图；R08 | `TodoServiceImpl.java:104-150`、`TodoItemMapper.xml:42-51`、`TodoServiceImplTest#should_createTodo_when_validRequest` | 编排顺序与设计时序图一致 |

### REQ-2: `F02 创建入参校验与规范化`

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| Given 名称/描述，When 校验，Then 名称 trim 后 1~128 字符、描述 0~512 字符、NFC 规范化 + 剔除控制字符（保留 `\n`/`\t`），超长**不截断**直接报错 | ✅ | R02/R03/R04、A05「≤128 / ≤512 字符（按字符计，非字节）」、S03 | `TodoValidator.java:69-92`、`TodoTextUtil.java:46-123`、`TodoValidatorTest`（含 128/129、512/513、emoji、全角空格） | 按 code point 计长，emoji 计 1 |

### REQ-3: `F03 创建幂等防重`

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| Given 同一 `tenantId+requestId` 先查后写，When 已存在，Then 返回首次结果且 `idempotent=true` | ✅ | R07、§5.1.3.1 时序图「alt 幂等命中」 | `TodoServiceImpl.java:111-116`、`TodoItemMapper.xml:32-40` | 查询强制带 `tenant_id` |
| Given 并发重复提交，When 插入触发 `uk_todo_item_req`，Then 重查并返回首次结果 | ⚠️ | §5.1.3.1「唯一键冲突 → 重查返回首次结果，`idempotent=true`，不向用户报错」 | `TodoServiceImpl.java:123-135`、`TodoItemManager.java:76-89` | 分支正确，但**依赖 DB 错误文案解析索引名**，解析失败会降级为「编号冲突重试」→ 见 P1-3 |

### REQ-4: `F04 创建结果回执与操作留痕`

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| Given 创建成功，When 组装回执，Then 返回 id/todoNo/itemName/itemDesc/status/creatorId/gmtCreate/idempotent | ⚠️ | W01 出参表（`data.id` 类型 `Long`、`data.gmtCreate` 格式 `yyyy-MM-dd HH:mm:ss`） | `TodoController.java:82-94`、`TodoCreateVO.java:16,46` | 字段齐全、时间格式一致；`data.id` 以 **String** 返回与系分表 `Long` 不符 → P2-4 |
| Given 创建成功，When 留痕，Then 写审计（操作人/编号/结果），审计失败不阻断 | ⚠️ | R10、I02 | `TodoServiceImpl.java:143,256-263`、`AuditIntegrationImpl.java:74-80` | 异步 + 队列满降级 ✅；但审计在事务内发出且降级无告警 → P1-2、P1-5 |

### REQ-5: `F05 创建人身份与租户隔离注入`

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| Given Body 携带同名字段，When 创建，Then 身份/租户只取登录上下文，Body 字段被忽略 | ✅ | R05「只取网关注入的登录上下文，忽略 Body 中的同名字段」 | `TodoCreateRequest.java`（无身份字段）、`TodoServiceImpl.java:224-229` | 契约层根本未暴露身份字段 |
| Given 网关注入身份头，When 解析，Then 得到可信 `userId/userName/tenantId/roles` | ⚠️ | R05、§6.4.2.1 | `GatewayAuthIntegrationImpl.java:50-75` | 直接读 `X-User-Id`/`X-User-Tenant-Id`/`X-User-Roles` → P0-1 |
| Given 上下文缺失，Then 返回 TODO_0005 | ⚠️ | R05「若上下文缺失 → TODO_0005」 | `TodoServiceImpl.java:187-194`、`GatewayAuthIntegrationImpl.java:56-58,70-73` | `userId` 缺失 → TODO_0005 ✅；**`tenantId` 缺失静默回落 `"0"`** → P1-1 |
| Given 无「待办-创建」权限点，Then 返回 TODO_0006 | ⚠️ | R06「通过统一权限平台配置权限点，服务端以注解 + 权限点方式校验」 | `TodoServiceImpl.java:201-210` | 判定源为请求头 `X-User-Roles`，非权限平台 → P0-1 |

### REQ-6: `R11 单用户创建限流`

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| Given 同一用户 1 分钟超 30 次，Then 返回 TODO_0009 | ⚠️ | R11「默认 30 次/分钟（可配置）」；§6.3「本地 + 网关双层」 | `TodoRateLimiter.java:66-76`、`application.yml:29-32` | 滑动窗口本身正确；配置项未接线（P2-3）、单实例口径放大限额（P1-6） |

### REQ-7: `错误码与异常契约`

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| Given 各类失败，When 返回，Then 错误码 TODO_0001~0009 与 HTTP 状态符合 W01 错误码表，不泄露堆栈 | ⚠️ | §5.1.2 错误码表、§5.1.3.1 异常场景表 | `TodoExceptionHandler.java:56-127,169-187`、`TodoErrorCodeEnum.java` | 9 个错误码齐全、映射基本一致；未知异常统一映射 TODO_0008 → P2-2 |

### REQ-8: `可灰度 / 可应急（配置中心开关）`

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| Given 发布期，When 运营灰度，Then 按 `todo.create.gray.tenants` 尾号灰度 10%→50%→100% | ❌ | §7.2「灰度开关：`todo.create.gray.tenants` + `todo.create.enabled`」 | 无实现（`rg` 全 `src/main` 无 `todo.create.enabled`/gray 相关代码，`application.yml` 仅有限流配置） | P0-2 |
| Given 线上故障，When 一级应急，Then 关闭 `todo.create.enabled` 立即止血（无需回滚） | ❌ | §7.3「一级应急（优先，无需回滚）：配置中心关闭 `todo.create.enabled` 总开关」 | 同上 | P0-2 |

### REQ-9: `可监控 / 告警`

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| Given 线上运行，When 观测，Then 埋点 `todo_create_total`/`todo_create_cost_ms`/`todo_create_idempotent_hit_total`/`todo_create_rate_limited_total`/`todo_no_degrade_total` 支撑 §6.5 五条告警 | ❌ | §6.5、§7.1 | `TodoServiceImpl.java` 全链路仅 SLF4J 日志；`rg` 无 `MeterRegistry`/`Counter`/actuator 依赖（`pom.xml`） | P0-3 |

### REQ-10: `R09 编号冲突重试`

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| Given 编号唯一键冲突，When 插入失败，Then 重试 1 次取新号；仍冲突返回 TODO_0007 | ✅ | R09、S04 | `TodoServiceImpl.java:119-149`、`TodoConstants.java:66` | `should_throwTodoNoConflict_when_todoNoAlwaysConflicts` 断言插入恰好 2 次 |

---

## 4. Step 3 — 可读性检查

| 结果 | 说明（违规写 Ax.x 与 `path:行`） |
|------|--------------------------------|
| ⚠️ | A1 ✅ 文件命名/UTF-8/无 Tab；A2 ✅ 无 `import *`、static import 独立分组；A3 ⚠️ **A3.4 行宽 >120**：`GatewayAuthIntegrationImpl.java:74`(121)、`TodoValidatorTest.java:67`(121)、`TodoValidatorTest.java:133`(134)（P2-7）；A4 ✅ 命名规范；A5 ✅ `@Override` 齐全、无空 catch；A6 ✅ `switch` 有 `default`、`long` 字面量大写 `L`；A7 ✅ Javadoc 完整且标记顺序正确 |

> 预扫 A3.4 命中 2 处，人工复核补出 `TodoValidatorTest.java:67`（脚本遗漏）。

---

## 5. Step 4 — 可靠性检查

| 域 | 参考 | 结果 | 等级 | 说明（列命中 ID 或「已扫无命中」） |
|----|------|------|------|-------------------------------------|
| 可靠性 | `reliability-checklist.md` G1–G17 | ❌ | P0–P2 | 命中：**G17.1/G15.3/G12.2 缺失→P0-2**、**G16.1 缺失→P0-3**、G3.2→P1-2、G8.1→P1-5、G6.1→P2-5、G11.2→P2-9、G10.1→P2-2、G14.3→P2-1；G2.1/G2.2/G2.3/G4.*/G9.2/G14.2/G15.1/G16.2~G16.4 通过 |
| 安全 | `security-checklist.md` S1–S10 | ❌ | P0–P1 | 命中：**S8.1（身份/权限信任客户端头）→P0-1**、S8.1/G14.2（tenantId 回落）→P1-1；S1.1 全 `#{}` 通过、S9.1/S9.2/S9.3 通过、S6.2 通过；S2–S5/S7/S10 与变更无关 N/A |
| Bug 模式 | `bug-pattern-checklist.md` B/M/I（120） | ⚠️ | P0–P2 | 预扫：`scan-all-rules.sh` → `[P1] M016 TodoServiceImpl.java:220`、`[P2] A3.4 ×2`、7 条 `G16.2` 误报复核排除；LLM 逐条核销 120 条：B001–B081 / M001–M027 / I001–I010 **无 Blocker/Major 命中**，仅 `M016`（默认时区）→P2-1；`B008` 未用 `Executors`（审计用裸 `Thread` + 有界队列）通过 |

---

## 6. Step 5 — 自定义扩展检查

| 域 | 参考 | 结果 | 等级 | 说明 |
|----|------|------|------|------|
| 自定义扩展 | `customized-checklist.md` U* | N/A | — | `N/A(未启用自定义规则)`；示例项 U1.1（Controller 必须 `@Valid`）实际已满足（`TodoController.java:69`） |

---

## 7. 结论

- **合并建议**：**阻止合并**（存在 3 项 P0）
- **P0**：
  1. **P0-1** `S8.1` — 身份与权限完全信任客户端可控的请求头，`R06` 权限平台校验未落地，服务直接可达时可伪造 `X-User-Id`/`X-User-Roles` 越权创建。
  2. **P0-2** `G17.1`/`G15.3`/`G12.2` — 缺 `todo.create.enabled` 总开关与 `todo.create.gray.tenants` 灰度，系分 §7.2/§7.3 的一级应急与灰度策略无法执行。
  3. **P0-3** `G16.1` — 全链路无指标埋点，系分 §6.5 的 5 条告警与 §7.1 的依赖/业务埋点无法建立，线上变更「不可监控」。
- **P1/P2**：
  1. **P1-1** `S8.1`/`G14.2` — `tenantId` 缺失静默回落 `"0"`，与 R05「上下文缺失 → TODO_0005」冲突，未注入租户的请求会写入共享租户 0。
  2. **P1-2** `G3.2` — 审计投递位于 `@Transactional` 方法内，事务后续回滚时仍会留下「创建成功」审计。
  3. **P1-3** `G2.2`/`G9.3` — 唯一键冲突类型依赖 DB 错误文案 `contains` 判断，解析失败时幂等键冲突被误当编号冲突，重试耗尽后返回 TODO_0007 而非首次结果。
  4. **P1-4** `G1.1`/`G2` — 雪花取号默认全实例 `workerId=0/datacenterId=0`，且再经 6 位取模压缩，多实例高并发下 `todo_no` 碰撞概率高，与「多实例下不重复」设计目标不符。
  5. **P1-5** `G8.1`/`G16.2` — 审计降级仅写本地 ERROR 日志、无告警，未满足 R10「ERROR 日志 + 监控告警」。
  6. **P1-6** 功能偏差 — 限流为纯本地单实例口径，多副本部署下单用户实际额度放大约 N 倍，与 R11「30 次/分钟」及 §6.3「本地 + 网关双层」不符（网关层限流未在本仓库落地也未在文档标注为外部待办）。
  7. **P2-1** `M016`/`G14.3` — `LocalDateTime.now()`/`LocalDate.now()` 依赖 JVM 默认时区。
  8. **P2-2** `G10.1` — 未知异常统一映射 TODO_0008「创建失败，请稍后重试」，参数错误与系统异常语义混用。
  9. **P2-3** — `todo.create.rate-limit-per-minute` 配置项未接线，`TodoRateLimiter` 恒用常量 30。
  10. **P2-4** — `data.id` VO 层为 String，与系分 W01 出参表 `Long` 不一致（需同步修订文档或契约）。
  11. **P2-5** `G6.1` — 限流器 `Map<String, Deque<Long>>` 无用户维度淘汰，长期存在内存增长风险。
  12. **P2-6** — 审计 shutdown 时队列剩余事件直接丢弃，无告警计数。
  13. **P2-7** `A3.4` — 3 处行宽超 120 字符。
  14. **P2-8** — 未使用常量 `TODO_NO_FALLBACK_SUFFIX_LENGTH`、`SNOWFLAKE_WORKER_ID_BITS` 与已剥离的降级文案，存在死代码。
  15. **P2-9** `G11.2` — 测试缺口：跨租户隔离、索引名不可识别的冲突分支、`/create` 缺 `requestId` 的 HTTP 错误码映射无覆盖。
  16. **P2-10** — `SnowflakeSequenceProvider.waitUntil` 纯自旋忙等，时钟回拨/同毫秒耗尽时占用 CPU。
- **一句话**：主链路（校验 → 幂等 → 取号 → 落库 → 回执）实现与系分一致、单测覆盖到位、SQL 全参数化、日志脱敏到位；但**可运维三支柱（可监控/可灰度/可应急）整体缺失**，且身份与权限判定建立在客户端可控请求头上，属安全红线，需先修 P0 再合入。

---

## 7.1 问题片段（必填）

### P0-1 `S8.1` — 身份与权限信任客户端可控请求头

`src/main/java/com/antdigital/todo/integration/impl/GatewayAuthIntegrationImpl.java:50-75`

片段范围：`src/main/java/com/antdigital/todo/integration/impl/GatewayAuthIntegrationImpl.java:50-75`

```java
L50|    public OperatorContext resolveOperator(Map<String, String> headers) {
L51|        if (headers == null) {
L52|            throw new TodoBizException(TodoErrorCodeEnum.NOT_LOGIN);
L53|        }
L54|        String userId = TodoTextUtil.trimWhitespace(headers.get(TodoConstants.HEADER_USER_ID));
        // 问题：userId 直接取自请求头 X-User-Id，未校验网关签名/票据（auth.verify.enabled 默认 false 且只判票据非空）
L70|        String tenantId = TodoTextUtil.trimWhitespace(headers.get(TodoConstants.HEADER_TENANT_ID));
L74|        return new OperatorContext(userId, userName, tenantId, parseRoles(headers.get(TodoConstants.HEADER_USER_ROLES)));
        // 问题：roles 取自 X-User-Roles，客户端可自行追加 "TODO_CREATE"/"*"，绕过 R06 垂直权限
```

配套：`src/main/java/com/antdigital/todo/service/impl/TodoServiceImpl.java:201-210`（`checkPermission` 的唯一判定源就是上述 `roles`）。**Spec 证据**：design R06「通过统一权限平台配置『待办-创建』权限点，服务端以注解 + 权限点方式校验」、§6.4.2.2「实现。通过统一权限平台配置…无权限返回 TODO_0006」，§6.4.2.3「默认全链路统一拦截器校验登录态…无白名单接口」。

### P0-2 `G17.1`/`G15.3`/`G12.2` — 无功能总开关与灰度

`src/main/java/com/antdigital/todo/service/impl/TodoServiceImpl.java:104-110`

片段范围：`src/main/java/com/antdigital/todo/service/impl/TodoServiceImpl.java:104-110`

```java
L104|    public TodoCreateResult createTodo(TodoCreateRequest request, OperatorContext operator) {
L105|        validateOperator(operator);
L106|        checkPermission(operator);
L107|        todoRateLimiter.acquire(operator.getUserId());
        // 问题：缺少 todo.create.enabled 总开关与 todo.create.gray.tenants 灰度判定，故障时只能回滚发布包
L109|        ValidatedTodo validated = todoValidator.validateAndNormalize(request);
```

配套：`src/main/resources/application.yml:29-32` 的 `todo.create` 下仅有 `rate-limit-per-minute`，无 `enabled`/`gray`。**Spec 证据**：design §7.2「灰度开关：配置中心 `todo.create.gray.tenants`（租户尾号列表）+ `todo.create.enabled`（总开关）」；§7.3「一级应急（优先，无需回滚）：配置中心关闭 `todo.create.enabled` 总开关」。

### P0-3 `G16.1` — 无指标埋点

`src/main/java/com/antdigital/todo/service/impl/TodoServiceImpl.java:143-147`

片段范围：`src/main/java/com/antdigital/todo/service/impl/TodoServiceImpl.java:143-147`

```java
L143|            auditCreateQuietly(validated.getRequestId(), entity.getTodoNo(), operator.getUserId());
L144|            LOGGER.info("todo create success, requestId={}, todoNo={}, creatorId={}, cost={}ms",
L145|                    validated.getRequestId(), entity.getTodoNo(), operator.getUserId(),
L146|                    System.currentTimeMillis() - startMillis);
        // 问题：success/cost/idempotent/rate_limited/no_degrade 仅有日志文本，无 todo_create_total 等指标，
        //       系分 §6.5 的「成功率 <99%」「P99 >500ms」「TODO_0008 >20 次/分」告警无数据源
L147|            return toResult(entity, false);
```

**Spec 证据**：design §6.5 埋点清单（`todo_create_total`/`todo_create_cost_ms`/`todo_create_idempotent_hit_total`/`todo_create_rate_limited_total`/`todo_no_degrade_total`）及 5 条告警规则；§7.1 依赖与业务埋点。

### P1-1 `S8.1`/`G14.2` — tenantId 缺失静默回落

`src/main/java/com/antdigital/todo/integration/impl/GatewayAuthIntegrationImpl.java:70-73`

片段范围：`src/main/java/com/antdigital/todo/integration/impl/GatewayAuthIntegrationImpl.java:70-73`

```java
L70|        String tenantId = TodoTextUtil.trimWhitespace(headers.get(TodoConstants.HEADER_TENANT_ID));
L71|        if (tenantId.isEmpty()) {
L72|            tenantId = TodoConstants.DEFAULT_TENANT_ID;   // 问题：R05 要求上下文缺失 → TODO_0005，此处静默写入租户 "0"
L73|        }
```

**Spec 证据**：design R05「`creatorId`/`creatorName`/`tenantId` 只取网关注入的登录上下文…若上下文缺失 → TODO_0005」、§6.4.2.1「`tenant_id` 由网关签名透传，业务层不得跨租户写入」。

### P1-2 `G3.2` — 审计投递在事务内

`src/main/java/com/antdigital/todo/service/impl/TodoServiceImpl.java:102-103,143`

片段范围：`src/main/java/com/antdigital/todo/service/impl/TodoServiceImpl.java:102-103,143`

```java
L102|    @Override
L103|    @Transactional(rollbackFor = Exception.class)
L104|    public TodoCreateResult createTodo(TodoCreateRequest request, OperatorContext operator) {
...
L143|            auditCreateQuietly(validated.getRequestId(), entity.getTodoNo(), operator.getUserId());
        // 问题：审计在事务提交前发出；若此后事务回滚（如外层调用方抛异常），审计仍记录「创建成功」
```

**Spec 证据**：design §5.1.3.1「审计与限流均在**事务外**/异步执行，避免长事务」；R10「创建成功必须写审计日志」。

### P1-3 `G2.2`/`G9.3` — 幂等键冲突依赖 DB 错误文案解析

`src/main/java/com/antdigital/todo/dao/manager/TodoItemManager.java:76-89`

片段范围：`src/main/java/com/antdigital/todo/dao/manager/TodoItemManager.java:76-89`

```java
L76|    private String resolveConstraintName(DuplicateKeyException e) {
L77|        Throwable cause = e.getMostSpecificCause();
L78|        String message = cause == null ? null : cause.getMessage();
L79|        if (message == null) {
L80|            return "";                     // 问题：解析失败返回空串
L81|        }
L82|        if (message.contains(TodoDuplicateKeyException.UK_REQUEST_ID)) {
L83|            return TodoDuplicateKeyException.UK_REQUEST_ID;
L84|        }
        ...
L88|        return "";
L89|    }
```

配套：`src/main/java/com/antdigital/todo/service/impl/TodoServiceImpl.java:124-137` —— `constraintName=""` 时 `isRequestIdConflict` 为 false，走「编号冲突」分支 `continue` 重试；第二次插入同一 `requestId` 再次冲突，循环耗尽后抛 `TODO_0007`，而**系分要求返回首次结果（`idempotent=true`）**。**Spec 证据**：design §5.1.3.1 异常场景表「唯一键冲突（并发重复提交）→ 捕获后按 `request_id` 重查并返回首次结果，`idempotent=true`，不向用户报错」。

### P1-4 `G1.1`/`G2` — 雪花取号多实例同 ID + 6 位取模碰撞

`src/main/java/com/antdigital/todo/service/component/SnowflakeSequenceProvider.java:56-58` 与 `TodoNoGeneratorImpl.java:69-71`

片段范围：`src/main/java/com/antdigital/todo/service/component/SnowflakeSequenceProvider.java:56-58`

```java
L56|    public SnowflakeSequenceProvider() {
L57|        this(0L, 0L);      // 问题：所有实例默认 workerId=0、datacenterId=0，多副本同毫秒会生成完全相同的序列
L58|    }
```

片段范围：`src/main/java/com/antdigital/todo/service/component/TodoNoGeneratorImpl.java:69-71`

```java
L69|        long sequence = sequenceProvider.nextSequence(dateKey);
L70|        long suffix = Math.abs(sequence % TodoConstants.TODO_NO_SEQ_MODULUS);   // 问题：再压缩到 10^6，多实例碰撞概率被放大
L71|        return TodoConstants.TODO_NO_PREFIX + dateKey + padLeft(String.valueOf(suffix));
```

**Spec 证据**：design §5.1 支撑层「ID 生成…保证多实例下不重复」；S04「依赖 Redis INCR…不可用时降级为雪花后缀；不保证唯一，唯一性由 `uk_todo_item_no` 兜底」+ R09 仅重试 1 次 → 高并发下用户可见 `TODO_0007`。

### P1-5 `G8.1`/`G16.2` — 审计降级无告警

`src/main/java/com/antdigital/todo/integration/impl/AuditIntegrationImpl.java:74-80`

片段范围：`src/main/java/com/antdigital/todo/integration/impl/AuditIntegrationImpl.java:74-80`

```java
L74|    public void recordCreate(String requestId, String todoNo, String operatorId, boolean success) {
L75|        String event = buildEvent(requestId, todoNo, operatorId, success);
L76|        boolean offered = eventQueue.offer(event);
L77|        if (!offered) {
L78|            LOGGER.error("audit queue full, degrade to local log. event={}", event);
        // 问题：仅本地日志，无告警计数/指标，R10 要求的「监控告警」未落地
L79|        }
L80|    }
```

**Spec 证据**：design R10「审计失败不阻断创建…降级为 ERROR 级本地日志 **+ 监控告警**」；§6.5「审计投递失败 > 50 次/分钟 → P3 告警」。

### P1-6 功能偏差 — 限流单实例口径

`src/main/java/com/antdigital/todo/service/component/TodoRateLimiter.java:40-47`

片段范围：`src/main/java/com/antdigital/todo/service/component/TodoRateLimiter.java:40-47`

```java
L40|    private final Map<String, Deque<Long>> timestamps = new ConcurrentHashMap<>();
L41|
L42|    /**
L43|     * 默认构造：30 次/分钟，系统时钟。
L44|     */
L45|    public TodoRateLimiter() {
L46|        this(TodoConstants.DEFAULT_RATE_LIMIT_PER_MINUTE, System::currentTimeMillis);
        // 问题：进程内计数器，N 副本部署时单用户实际额度 ≈ 30×N，且未读配置项
L47|    }
```

**Spec 证据**：design R11「单用户创建频率限制：默认 30 次/分钟（可配置）」；§6.3「限流：单用户 30 次/分钟（令牌桶，本地 + 网关双层）」。

### P2-1 `M016`/`G14.3` — 默认时区

`src/main/java/com/antdigital/todo/service/impl/TodoServiceImpl.java:220`

片段范围：`src/main/java/com/antdigital/todo/service/impl/TodoServiceImpl.java:219-220`

```java
L219|    private TodoItemDO buildEntity(ValidatedTodo validated, OperatorContext operator) {
L220|        LocalDateTime now = LocalDateTime.now();   // 问题：依赖 JVM 默认时区，部署时区变更会导致 gmt_create 漂移
```

配套 `src/main/java/com/antdigital/todo/service/component/TodoNoGeneratorImpl.java:47`（`LocalDate::now`，影响 `todo_no` 日期段跨零点归属）。**Spec 证据**：design §3 时间字段统一 datetime；pre 扫 `M016` 命中。

### P2-2 `G10.1` — 未知异常映射混淆

`src/main/java/com/antdigital/todo/controller/TodoExceptionHandler.java:123-127`

片段范围：`src/main/java/com/antdigital/todo/controller/TodoExceptionHandler.java:123-127`

```java
L123|    @ExceptionHandler(Exception.class)
L124|    public ResponseEntity<ApiResult<Void>> handleUnexpected(Exception e) {
L125|        LOGGER.error("todo unexpected error", e);
L126|        return buildError(TodoErrorCodeEnum.STORAGE_ERROR);   // 问题：非存储类异常（NPE/序列化等）也返回 TODO_0008，排障与重试语义混淆
L127|    }
```

### P2-3 — 配置项未接线

`src/main/resources/application.yml:29-32` / `src/main/java/com/antdigital/todo/service/component/TodoRateLimiter.java:45-47`

```yaml
L29| todo:
L30|   create:
L31|     # 单用户每分钟创建次数上限（R11）
L32|     rate-limit-per-minute: 30
```

片段范围：`src/main/java/com/antdigital/todo/service/component/TodoRateLimiter.java:45-47`

```java
L45|    public TodoRateLimiter() {
L46|        this(TodoConstants.DEFAULT_RATE_LIMIT_PER_MINUTE, System::currentTimeMillis);
L47|    }   // 问题：未使用 @Value("${todo.create.rate-limit-per-minute:30}")，改配置不生效
```

### P2-4 — `data.id` 类型与系分不一致

`src/main/java/com/antdigital/todo/model/vo/TodoCreateVO.java:16` / `TodoController.java:85`

片段范围：`src/main/java/com/antdigital/todo/controller/TodoController.java:84-85`

```java
L84|        // 主键以 String 返回，避免前端 Number 精度丢失
L85|        vo.setId(result.getId() == null ? null : String.valueOf(result.getId()));
        // 问题：W01 出参表定义 data.id 为 Long，实现为 String，契约与文档不一致（需二选一修订）
```

### P2-5 `G6.1` — 限流器 Map 无淘汰

`src/main/java/com/antdigital/todo/service/component/TodoRateLimiter.java:40,66-68`

片段范围：`src/main/java/com/antdigital/todo/service/component/TodoRateLimiter.java:66-68`

```java
L66|    public void acquire(String userId) {
L67|        long now = clock.getAsLong();
L68|        Deque<Long> queue = timestamps.computeIfAbsent(userId, key -> new ArrayDeque<>());
        // 问题：仅裁剪单个队列内部时间戳，userId 条目永不移除；用户量大时 Map 无界增长
```

### P2-6 — 审计关闭丢弃剩余事件

`src/main/java/com/antdigital/todo/integration/impl/AuditIntegrationImpl.java:86-95`

片段范围：`src/main/java/com/antdigital/todo/integration/impl/AuditIntegrationImpl.java:85-95`

```java
L85|    @PreDestroy
L86|    public void shutdown() {
L87|        running.set(false);
L88|        consumer.interrupt();     // 问题：interrupt 后 consume 立即 return，队列中剩余事件被静默丢弃且无日志/计数
L89|        try {
L90|            consumer.join(TimeUnit.SECONDS.toMillis(SHUTDOWN_WAIT_SECONDS));
```

### P2-7 `A3.4` — 行宽超限

`src/main/java/com/antdigital/todo/integration/impl/GatewayAuthIntegrationImpl.java:74`

片段范围：`src/main/java/com/antdigital/todo/integration/impl/GatewayAuthIntegrationImpl.java:74`

```java
L74|        return new OperatorContext(userId, userName, tenantId, parseRoles(headers.get(TodoConstants.HEADER_USER_ROLES)));
        // 121 字符 > 120，建议按参数换行
```

配套：`src/test/java/com/antdigital/todo/service/component/TodoValidatorTest.java:67`（121）、`:133`（134）。

### P2-8 — 死常量

`src/main/java/com/antdigital/todo/common/constant/TodoConstants.java:153-166`

片段范围：`src/main/java/com/antdigital/todo/common/constant/TodoConstants.java:153-166`

```java
L153|    /**
L154|     * 雪花算法工作节点 ID 位数
L155|     */
L156|    public static final int SNOWFLAKE_WORKER_ID_BITS = 5;      // 问题：全仓库无引用
...
L164|    /**
L165|     * 待办编号降级后缀长度（取雪花 ID 后 6 位，与序号位数对齐）
L166|     */
L166|    public static final int TODO_NO_FALLBACK_SUFFIX_LENGTH = 6;   // 问题：全仓库无引用（降级路径未实现）
```

### P2-9 `G11.2` — 测试缺口

`src/test/java/com/antdigital/todo/service/impl/TodoServiceImplTest.java:136-154`

片段范围：`src/test/java/com/antdigital/todo/service/impl/TodoServiceImplTest.java:136-154`

```java
L136|    @Test
L137|    @DisplayName("并发重复提交：唯一键冲突后重查并返回首次结果")
L138|    void should_returnFirstResult_when_duplicateRequestIdRace() {
        // 问题：仅覆盖「索引名可识别」的理想路径；未覆盖 constraintName=""/无法识别时
        //       应仍返回首次结果而非 TODO_0007（对应 P1-3 的缺口），也无跨租户隔离用例
L144|        todoItemMapper.missSelectOnce = true;
```

配套：`src/test/java/com/antdigital/todo/controller/TodoControllerTest.java` 仅 3 个用例，未覆盖 `/create` 缺 `requestId` 时 `@Valid` → `resolveFieldCode` 的错误码映射。

### P2-10 — 雪花忙等自旋

`src/main/java/com/antdigital/todo/service/component/SnowflakeSequenceProvider.java:111-118`

片段范围：`src/main/java/com/antdigital/todo/service/component/SnowflakeSequenceProvider.java:111-118`

```java
L111|    private long waitUntil(long targetTimestamp) {
L112|        long timestamp = System.currentTimeMillis();
L113|        while (timestamp < targetTimestamp) {
L114|            Thread.yield();          // 问题：时钟回拨期间纯自旋忙等，占用 CPU；建议短 sleep 或回拨阈值快速失败
L115|            timestamp = System.currentTimeMillis();
L116|        }
```

---

## 8. 修复任务列表

> 供后续改代码逐项执行与核销。

### P0

- [ ] **P0** `GatewayAuthIntegrationImpl.java:50-75` + `TodoServiceImpl.java:201-210` — 接入网关签名/票据真实校验（`auth.verify.enabled` 默认 true），身份取自校验结果；权限判定改为对接权限平台权限点（R06），不再信任客户端可控的 `X-User-Id`/`X-User-Roles`。
- [ ] **P0** `TodoServiceImpl.java:104-110` — 新增 `todo.create.enabled` 总开关与 `todo.create.gray.tenants` 灰度判定，未命中返回「功能未开放」，并在 `application.yml` 补齐配置项。
- [ ] **P0** `TodoServiceImpl.java:143-147` — 补齐 `todo_create_total`/`todo_create_cost_ms`/`todo_create_idempotent_hit_total`/`todo_create_rate_limited_total`/`todo_no_degrade_total` 指标埋点与告警接入。

### P1

- [ ] **P1** `GatewayAuthIntegrationImpl.java:70-73` — 删除 `tenantId` 静默回落 `"0"`，缺失时按 R05 抛 `TODO_0005`。
- [ ] **P1** `TodoServiceImpl.java:143` — 将审计投递移出 `@Transactional`（事务提交后事件回调或 `TransactionSynchronization`），避免回滚仍记成功审计。
- [ ] **P1** `TodoItemManager.java:76-89` — 消除对 DB 错误文案的依赖：改为插入前按 `request_id` 二次确认，或冲突后无条件重查 `request_id`，解析失败时仍返回首次结果。
- [ ] **P1** `SnowflakeSequenceProvider.java:56-58` — 去掉 `(0L, 0L)` 默认构造，改由配置/环境注入唯一 `workerId`/`datacenterId`；`TodoNoGeneratorImpl.java:70` 的 6 位取模改为更高基数（如 8 位）或直接用号段保证不碰撞。
- [ ] **P1** `AuditIntegrationImpl.java:74-80` — 队列满/投递失败接入告警计数（对接 P0 的指标体系），满足 R10「ERROR 日志 + 监控告警」。
- [ ] **P1** `TodoRateLimiter.java:40-47` — 明确双层限流落点：要么接入集中式限流（Redis/网关）保证 30 次/分钟全局限额，要么在 `docs/modules/todo/README.md` 显著标注「本服务仅单实例限流，集群限额由网关保证」并给出网关配置项。

### P2（可选）

- [ ] **P2** `TodoServiceImpl.java:220`、`TodoNoGeneratorImpl.java:47` — 显式指定时区（如 `ZoneId.of("Asia/Shanghai")` 或统一 UTC），消除 `M016`。
- [ ] **P2** `TodoExceptionHandler.java:123-127` — 兜底异常改用独立错误码或 `UNKNOWN_ERROR` 语义，避免与 `TODO_0008` 存储异常混淆。
- [ ] **P2** `TodoRateLimiter.java:45-47` — 用 `@Value("${todo.create.rate-limit-per-minute:30}")` 接线配置项。
- [ ] **P2** `TodoCreateVO.java:16`、`TodoController.java:85` — 与系分 W01 出参表对齐 `data.id` 类型（保留 String 则同步修订 `design.md` 出参表）。
- [ ] **P2** `TodoRateLimiter.java:40,66-68` — 为 `timestamps` 增加空闲用户条目淘汰（定期清理或 Caffeine 过期缓存）。
- [ ] **P2** `AuditIntegrationImpl.java:85-95` — shutdown 前排空/记录剩余事件数量，丢弃时输出 ERROR 并计数。
- [ ] **P2** `GatewayAuthIntegrationImpl.java:74`、`TodoValidatorTest.java:67`、`TodoValidatorTest.java:133` — 按 120 字符换行。
- [ ] **P2** `TodoConstants.java:156,166` — 删除未引用常量 `SNOWFLAKE_WORKER_ID_BITS`、`TODO_NO_FALLBACK_SUFFIX_LENGTH`（或补齐降级实现）。
- [ ] **P2** `TodoServiceImplTest.java:136-154` — 补充「索引名不可识别仍返回首次结果」、跨租户隔离、`/create` 缺 `requestId` 错误码映射用例。
- [ ] **P2** `SnowflakeSequenceProvider.java:111-118` — `waitUntil` 改为短 `Thread.sleep` 等待，并设置时钟回拨告警阈值。

---

## 9. 验证与残留风险

| 验证项 | 状态 | 说明 |
|--------|:----:|------|
| `scan-all-rules.sh`（52/222 条可程序化规则） | ✅ 已执行 | 10 条命中，7 条 `G16.2` 复核为误报，3 条有效（`M016`、`A3.4`×2） |
| 逐 ID 人工核销（G1–G17 / S1–S10 / B001–B081 / M001–M027 / I001–I010） | ✅ 已执行 | 见 checklist §4.1–§4.3，`⬜` 计数为 0 |
| 编译 / 单测执行 | ⚠️ **未执行** | 本沙箱 **无 JDK / Maven / Gradle**（`java`/`javac`/`mvn`/`gradle` 均 MISSING，`/usr/lib/jvm`、`/tmp/*jdk*` 为空），无法运行 `mvn test`。**本报告的可靠性结论为静态审查结论，未获运行时证据支撑**；`impl.md` 声明的「84 用例通过」为编码节点自述，本轮未复验 |
| SQL / MyBatis 一致性 | ✅ 静态核对 | `TodoItemMapper.xml` 字段与 `todo_item.sql` 全部对齐；`#{}` 全参数化；`useGeneratedKeys` 回填 `id` |
| 残留风险 | — | ①无运行时验证，`TodoItemManager` 冲突解析在真实 MySQL 驱动下的行为（含 `constraintName=""`）只能静态推断；②`@Transactional` 内重试取号依赖「InnoDB 唯一键冲突不中止事务」这一前提，未经真实 DB 验证；③限流与雪花取号的并发表现未经压测 |

