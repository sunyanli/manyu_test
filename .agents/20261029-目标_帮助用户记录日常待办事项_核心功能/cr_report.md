# Code Review Report

> **Change** `todo-create`（新增待办事项 · 修复后复审 Round 2） · **分支/Commit** `task/DEV-966dcd0a-7905-11f1-9649-3b4281182f10-27fdbcad-6a5f-4d77-a539-94db5a84337e` / `4b2f55c`（范围 `0625c2a..HEAD`：编码 `009d45d` → 评审 `af9c663` → 修复 `4b2f55c`） · **日期** `2026-10-29` · **审查者** AI
>
> **规则**：等级 **P0 / P1 / P2**；G/S 以 checklist 行内定义为准；Bug 模式 Blocker→P0、Major→P1、Info→P2。已**先**运行 `scan-all-rules.sh`（输出并入 §5），再出 LLM 结论。每条问题含 `ID + path:line` + 代码片段。
>
> **驱动清单**：`.agents/20261029-目标_帮助用户记录日常待办事项_核心功能/2026-10-29-todo-create-cr-checklist-round2.md`（45 个 Java 文件逐文件核销，`⬜` = 0）
>
> **验证证据**：`mvn -o -B test` → **Tests run: 111, Failures: 0, Errors: 0, Skipped: 0**（9 个测试类，surefire 报告实测，见 §7.3）；`src` 全量行宽（CJK 按 2 计）**0 处 > 120**、**0 处 Tab**。HTTP 运行时冒烟因离线仓库缺插件依赖被阻断，已降级（见 §7.3）。

---

## 1. 审查范围

| 项 | 值 |
|----|-----|
| `.java` 文件数 | `45`（主代码 36 / 测试 9） |
| 变更行数（全量 `0625c2a..HEAD`） | `+3196 / -0` |
| 修复轮增量（`009d45d..HEAD`） | `+1827 / -258`（34 文件） |
| 非 Java 变更 | `pom.xml`、`src/main/resources/application.yml`、`resources/mapper/TodoItemMapper.xml`、`resources/db/todo_item.sql`、`docs/*`、`.agents/*` |

| 类/接口 | 路径 | 角色 |
|---------|------|------|
| `TodoApplication` | `src/main/java/com/antdigital/todo/TodoApplication.java` | 启动装配（`@MapperScan`） |
| `TodoController` | `.../controller/TodoController.java` | W01 入口 `POST /api/todo/item/create` |
| `TodoExceptionHandler` | `.../controller/TodoExceptionHandler.java` | 异常 → 错误码 + HTTP 状态 |
| `TodoService` / `TodoServiceImpl` | `.../service/` | S01 创建编排 / S02 幂等查询 |
| `TodoValidator` / `TodoTextUtil` | `.../service/component/`、`.../common/util/` | S03 校验与规范化（R01~R04） |
| `TodoNoGenerator(Impl)` / `TodoSequenceProvider` / `SnowflakeSequenceProvider` | `.../service/component/` | S04 取号（R09，降级路径） |
| `TodoCreateGate` / `TodoRateLimiter` / `TodoMetrics` | `.../service/component/` | 开关灰度（§7.2/§7.3）、限流（R11）、埋点（§6.5） |
| `AuthIntegration` / `GatewayAuthIntegrationImpl` / `GatewaySignatureVerifier` | `.../integration/` | I01 身份与租户注入、网关签名校验（R05/R06） |
| `AuditIntegration(Impl)` | `.../integration/` | I02 审计异步投递（R10） |
| `TodoItemMapper` / `TodoItemManager` / `TodoItemDO` | `.../dao/` | 持久化 + 唯一键语义转换 |
| DTO/VO/枚举/异常/常量 | `model/**`、`common/**` | 契约与常量 |
| 9 个测试类 | `src/test/java/com/antdigital/todo/` | 单测（111 用例） |

---

## 2. 问题计数

| P0 | P1 | P2 |
|----|----|-----|
| 0 | 6 | 11 |

> **`blocker_count = 0`**（无阻塞项）。
> 预扫 `scan-all-rules.sh` 原始命中 14 条（全为 `G16.2 CatchWithoutLogging`），逐条复核 **14/14 误报**（catch 内均有 `LOGGER.error/warn` 并转换异常或计数告警），不计入上表。上表为 LLM 复核后的最终结论。

---

## 3. Step 2 — 功能（REQ）

| REQ | Scenario | Spec证据 | 代码证据 | 结果 |
|-----|----------|----------|----------|------|
| REQ-1 | Given 合法入参与可信身份，When 调用 W01，Then 落库一条 `todo_item` 并返回 `id/todoNo/status=0/gmtCreate` | design §4.1 W01、§5.1.2、§5.1.3.1 时序图 | `TodoController.java:68-74`、`TodoServiceImpl.java:149-194`、`TodoItemMapper.xml:54-63`；`TodoServiceImplTest#should_createTodo_when_validRequest` | ✅ |
| REQ-2 | Given 名称/描述/requestId，When 校验，Then trim(含全角)+NFC+剔除控制字符后名称 1~128、描述 0~512、requestId 字母数字`-_`且 ≤64，**超长不截断** | design R01~R04、A05「按字符计，非字节」、S03 | `TodoValidator.java:35-106`、`TodoTextUtil.java:46-123`；`TodoValidatorTest` 22 例、`TodoTextUtilTest` 12 例 | ✅ |
| REQ-3 | Given 同一 `tenantId+requestId` 重复提交（含并发），Then 返回首次结果且 `idempotent=true`，不向用户报错 | design R07、§5.1.3.1 异常场景表「唯一键冲突 → 重查返回首次结果」 | `TodoServiceImpl.java:157-181`；`should_returnFirstResult_when_sameRequestIdSubmittedTwice`、`..._duplicateRequestIdRace`、`..._duplicateRequestIdRaceAndConstraintUnknown` | ⚠️ 并发 ≥3 同 `requestId` 存在锁升级死锁（**P1-1**） |
| REQ-4 | Given 创建成功，When 事务提交后，Then 异步写审计（requestId/todoNo/operatorId），失败不阻断 | design R10、§4.4 I02、§5.1.3.1「审计在事务外/异步」 | `TodoServiceImpl.java:311-339`（`afterCommit`）、`AuditIntegrationImpl.java:88-96`；`should_stillSucceed_when_auditThrows` | ✅ |
| REQ-5 | Given 网关注入身份/租户/权限，When 创建，Then 只取可信上下文，缺失 TODO_0005，无权限点 TODO_0006 | design R05/R06、§6.4.2.1~6.4.2.3 | `GatewayAuthIntegrationImpl.java:65-83`、`TodoServiceImpl.java:253-276`；`TodoControllerTest` 7 例、`GatewaySignatureVerifierTest` 7 例 | ✅（头部大小写健壮性见 **P1-2**） |
| REQ-6 | Given 客户端传 `status`/`is_deleted`/`source`，Then 服务端固定 `status=0`/`is_deleted=0`，`source` 缺省 `web` 且「不接受前端指定」 | design R08 | `TodoServiceImpl.java:294-298`（status/deleted 强制）；`TodoValidator.java:100-106`（source 取客户端值） | ⚠️ spec 自身矛盾：W01 入参表允许 `source`，R08 禁止 → **P1-6** |
| REQ-7 | Given 编号冲突，Then 重试 1 次取新号；仍冲突 TODO_0007；编号格式 `TD+yyyyMMdd+6位` | design R09、A09、S04 | `TodoServiceImpl.java:166-193`、`TodoNoGeneratorImpl.java:77-85`；`should_retryAndSucceed_when_todoNoConflictOnce`、`should_throwTodoNoConflict_when_todoNoAlwaysConflicts`、`TodoNoGeneratorImplTest` 3 例 | ✅ |
| REQ-8 | Given 单用户 1 分钟超 30 次，Then TODO_0009 | design R11、§6.3 | `TodoRateLimiter.java:52,73-84`；`should_throwRateLimited_when_exceedsLimit` | ✅（性能 P2-5） |
| REQ-9 | Given 总开关关闭或租户未命中灰度，Then TODO_0010（503）且不落库 | design §7.2、§7.3 | `TodoCreateGate.java:68-81`、`TodoServiceImpl.java:151`、`application.yml:34-42`；`TodoCreateGateTest` 4 例 + `TodoServiceImplTest` 3 例 | ✅（口径 P2-7） |
| REQ-10 | Given 创建链路，When 成功/失败/幂等/限流/降级，Then 上报 §6.5 五项指标支撑 5 条告警 | design §6.5、§7.1 | `TodoMetrics.java:114-198`、`TodoServiceImpl.java:127-139`、`pom.xml:49-57`、`application.yml:49-56` | ⚠️ `todo_no_degrade_total` 恒计数致告警失效 → **P1-3** |
| REQ-11 | Given 建表脚本，Then 字段/唯一索引/查询索引与 §5.1.1.1 一致 | design §5.1.1.1 | `todo_item.sql:5-27`、`TodoItemDO.java:16-206`（索引名与 `TodoDuplicateKeyException` 常量一致） | ✅（软删语义 P2-4） |
| REQ-12 | Given 创建成功，Then `data.id` 为 String、`gmtCreate` 为 `yyyy-MM-dd HH:mm:ss`、`code=OK/msg=SUCCESS` | design §5.1.2 W01 出参表（修订版 `data.id` String） | `TodoController.java:82-94`、`ApiResult.java:56-69`；`should_returnSuccessResult_when_validRequest` | ✅ |
| REQ-13 | Given 日志/审计输出，Then 不含 `itemName`/`itemDesc` 原文，敏感信息脱敏且截断 | design §6.4.3.2 | `TodoServiceImpl.java:160-190`（仅 requestId/todoNo/creatorId）、`TodoItemDO.java:192-205`、`DesensitizeUtil.java:58-66` | ✅ |

---

## 4. Step 3 — 可读性检查

| 结果 | 说明（违规写 Ax.x 与 `path:行`） |
|------|--------------------------------|
| ⚠️ | **A2.4**（P2-11）：`java.*` import 组排在 `org.*` 之后，未按完整名 ASCII 升序 —— `TodoServiceImpl.java:23-30`、`TodoCreateGate.java:6-14`、`GatewaySignatureVerifier.java:7-19`、`TodoMetrics.java:5-8`（全仓库一致，统一整改） |
| ✅ | **A1** 文件名=顶层类名、UTF-8、全量 `src` 0 处 Tab |
| ✅ | **A3** K&R 大括号、4 空格缩进、行宽 ≤120（显示宽度复核 0 处超限，修复轮 P2-7 已闭环）、成员间空行 |
| ✅ | **A4** 包全小写、类 UpperCamel、方法 lowerCamel、常量 UPPER_SNAKE、测试类 `XxxTest`（`DEFAULT_RATE_LIMIT_PER_MINUTE` 属未引用常量，见 P2-6） |
| ✅ | **A5** `@Override` 齐备（`TodoServiceImpl.java:124,204,317`、`AuditIntegrationImpl.java:88,101`）、无空 catch、无 `finalize()` |
| ✅ | **A6** `switch` 均有 `default`（`TodoExceptionHandler.java:155-174`）、long 字面量大写 `L`（`TodoConstants.java:73,158,163`）、修饰符顺序正确 |
| ✅ | **A7** public 类/方法 Javadoc 完整，`@param → @return → @throws` 顺序正确，自解释 getter 省略符合 A7.3 |

---

## 5. Step 4 — 可靠性检查

> **预扫（先执行）**：`bash references/script/scan-all-rules.sh src/main/java/com/antdigital/todo src/test/java/com/antdigital/todo src/main/resources/mapper/TodoItemMapper.xml src/main/resources/db/todo_item.sql`
> 输出：`14 findings (P0=14, P1=0, P2=0) | 52/222 rules scanned`，全部 `G16.2 CatchWithoutLogging`。
> **复核：14/14 误报** —— `TodoItemManager.java:73`、`AuditIntegrationImpl.java:106/130/138`、`GatewaySignatureVerifier.java:148/166`、`SnowflakeSequenceProvider.java:142`、`TodoServiceImpl.java:132/135/170/182/334`、`TodoControllerTest.java:185`、`GatewaySignatureVerifierTest.java:135` 的 catch 块内均记录 `LOGGER.error/warn` 并转换为业务异常或计数告警（脚本按 catch 后首行正则匹配，未识别日志语句形态）。

| 域 | 参考 | 结果 | 等级 | 说明（命中 ID + 定位） |
|----|------|------|------|----------------------|
| 可靠性 | `reliability-checklist.md` G1–G18 | ⚠️ | P1 | `G1.1` 幂等回查 `FOR UPDATE` 锁升级死锁（`TodoServiceImpl.java:170-172` + `TodoItemMapper.xml:43-52`）；`G8.2` 线程池/信号量隔离缺失（`TodoServiceImpl.java:124-126`、`application.yml` 无相关项）；`G16.1` 降级指标恒计数（`SnowflakeSequenceProvider.java:125`）；其余 G 项已扫无命中，明细见 checklist §4.2 |
| 安全 | `security-checklist.md` S1–S10 | ⚠️ | P1 | `S8.1` 签名不绑定 method/path/body 且无 nonce，300s 内可重放（`GatewaySignatureVerifier.java:109-137`）；请求头大小写不敏感获取缺失（`GatewayAuthIntegrationImpl.java:69-77`）；密钥配置化 + fail-closed 已达标（`GatewaySignatureVerifier.java:113-117`）；S1.1 全 `#{}`、S6.2 无多态、S9.2 日志脱敏通过；S2~S5/S7/S10 本变更不涉及 `N/A` |
| Bug 模式 | `bug-pattern-checklist.md` B/M/I（120 条） | ✅ | — | 预扫 + LLM 逐条核销 120 条未发现命中：`B076`（`@Transactional` public）、`M016`（显式时区）、`M020`（`@Override`）、`B053/B080`（异常断言齐全）、`B008`（未用 `Executors`）、`B055`（未用 Mockito）均专项通过；逐条状态见 checklist §4.1 |
| 数据库设计 | design §5.1.1.1 / db 规范 | ⚠️ | P2 | 唯一索引 `uk_todo_item_req(tenant_id, request_id)` 不含 `is_deleted`，而幂等查询过滤 `is_deleted = 0`：启用逻辑删除后同 `requestId` 重建会被误报 TODO_0007（`todo_item.sql:22` + `TodoItemMapper.xml:47-49`） |

---

## 6. Step 5 — 自定义扩展检查

| 域 | 参考 | 结果 | 等级 | 说明 |
|----|------|------|------|------|
| 自定义扩展 | `customized-checklist.md` U* | ✅ / N/A | — | `U1.1` Controller 入参使用 `@Valid` + `@NotBlank`（`TodoController.java:69`、`TodoCreateRequest.java:21-28`）；`U1.2~U2.3` 为示例项/空节 → `N/A(未启用自定义规则)` |

---

## 7. 结论

- **合并建议**：**修复后合并**（无 P0；6 项 P1 建议合并前修复，11 项 P2 可选）
- **P0**：无。上轮 P0-1（身份可伪造）、P0-2（无开关灰度）、P0-3（无指标）均已闭环，复核证据见 §7.2。

- **P1**：
  1. **P1-1** `G1.1`/`G9.3` `TodoServiceImpl.java:170` — 唯一键冲突后在已持共享锁的事务内做 `FOR UPDATE` 回查，两个并发同 `requestId` 事务形成锁升级死锁，最终返回 TODO_0011/500，违背 R07「返回首次结果」。
  2. **P1-2** `S8.1`/`G10` `GatewayAuthIntegrationImpl.java:69` — 请求头按大小写精确查找；网关下发小写头（HTTP/2 强制小写）时 `userId`/`tenantId` 恒空 → 全量 TODO_0005，单测用精确大小写 `HashMap` 无法暴露。
  3. **P1-3** `G16.1`/`G13.1` `SnowflakeSequenceProvider.java:125` — 唯一 provider 恒 `isDegraded()==true`，`todo_no_degrade_total` 恒等于创建量，§6.5「>10 次/分钟」告警永久触发，真实降级不可辨识。
  4. **P1-4** `S8.1` `GatewaySignatureVerifier.java:131` — 签名不绑定 method/path/body 摘要且无 nonce，300s 窗口内可重放任意报文。
  5. **P1-5** `G8.2` `TodoServiceImpl.java:126` — 系分 §6.3 要求的「创建接口独立线程池/信号量（默认 50 并发）」未实现，DB 变慢将占满容器工作线程。
  6. **P1-6** `R08`/REQ-6 `TodoValidator.java:100` — `source` 由客户端指定并落库，与 R08「不接受前端指定」冲突（spec W01 入参表与 R08 自相矛盾，需澄清统一）。

- **P2**：①成功指标/日志先于提交（`TodoServiceImpl.java:130`）；②非存储异常误记 `TODO_0008` 码（`:136`）；③指标每请求动态 `register`（`TodoMetrics.java:190`）；④唯一索引与软删查询错配（`TodoItemMapper.xml:49`）；⑤限流器请求路径 O(n) 全量扫描（`TodoRateLimiter.java:91`）；⑥死常量 `DEFAULT_RATE_LIMIT_PER_MINUTE`（`TodoConstants.java:68`）；⑦开关与灰度共用错误码/指标（`TodoCreateGate.java:68`）；⑧签名校验可配置关闭无生产防护（`application.yml:29`）；⑨重复请求头只留一值（`TodoController.java:70`）；⑩审计停机中断后统计偏差（`AuditIntegrationImpl.java:110`）；⑪import 分组顺序（A2.4）。

- **一句话**：主链路与安全基线（签名 fail-closed、幂等唯一索引、开关灰度、审计事务后投递、指标埋点）已达标、111 个单测全绿、无阻塞缺陷；剩余风险集中在「并发幂等的死锁路径」「网关契约健壮性/抗重放」「可监控口径失真」，均可局部修复，建议按 §8 清单收尾后合并。

### 7.2 修复轮（Round 1 → Round 2）逐项复核

| 上轮问题 | 复核证据 | 结论 |
|----------|----------|------|
| P0-1 身份/权限信任客户端头 | `GatewaySignatureVerifier.java:109-137`（HMAC-SHA256 + 时间戳容差 + `MessageDigest.isEqual` 常量时间比较）；`GatewayAuthIntegrationImpl.java:69-82`（先取头→再验签→才构造上下文）；`GatewaySignatureVerifier.java:113-117` 密钥缺失 fail-closed；`X-User-Roles` 已删除 | ✅ 闭环（健壮性见 P1-2/P1-4） |
| P0-2 无开关/灰度 | `TodoCreateGate.java:68-81`（总开关 + 尾号白名单）；`application.yml:34-42`；`TodoServiceImpl.java:151`（身份后、权限前）；TODO_0010 独立错误码 + 503 | ✅ 闭环（口径见 P2-7） |
| P0-3 无指标 | `TodoMetrics.java:22-198`（5 项主指标 + 灰度/审计降级）；`pom.xml:49-57`；`application.yml:49-56` 暴露 `prometheus` | ✅ 闭环（口径见 P1-3/P2-1/P2-3） |
| P1-1 租户静默回落 | `GatewayAuthIntegrationImpl.java:71-75`、`TodoServiceImpl.java:253-260`：userId/tenantId 任一缺失即 TODO_0005，无默认回落；`TodoControllerTest#should_throwNotLogin_when_tenantHeaderMissing`、`should_throwNotLogin_when_tenantIdBlank` | ✅ 闭环 |
| P1-2 审计在事务内 | `TodoServiceImpl.java:311-322` `TransactionSynchronizationManager.registerSynchronization(...afterCommit)`，无事务上下文直投 | ✅ 闭环 |
| P1-3 冲突依赖错误文案 | `TodoServiceImpl.java:170-181`：先无条件按 `(tenant, requestId)` 加锁回查，命中即返回首次结果，索引名降级为日志诊断；`should_returnFirstResult_when_duplicateRequestIdRaceAndConstraintUnknown` 覆盖 | ✅ 功能闭环（并发死锁见 P1-1） |
| P1-4 雪花节点硬编码 | `SnowflakeSequenceProvider.java:70-84` 配置注入 + 越界 `IllegalArgumentException` + 0/0 WARN；`application.yml:43-46` | ✅ 闭环 |
| P1-5 审计降级无告警 | `AuditIntegrationImpl.java:92-95,113-117,133-136,138-142`：队列满/停机/中断/消费异常均计数 + ERROR | ✅ 闭环 |
| P1-6 限流配置未接线 | `TodoRateLimiter.java:51-54` `@Value("${todo.create.rate-limit-per-minute:30}")`；`docs/modules/todo/README.md:89-94` 限流口径说明 | ✅ 闭环（性能见 P2-5） |
| P2-1 默认时区 | `TodoConstants.java:138-143` `BIZ_ZONE`；`TodoServiceImpl.java:287`、`TodoNoGeneratorImpl.java:54` 显式时区 | ✅ 闭环 |
| P2-2 未知异常误映射 | `TodoErrorCodeEnum.java:67` `TODO_0011`；`TodoExceptionHandler.java:123-127` | ✅ 闭环（指标侧见 P2-2） |
| P2-3 限流配置未生效 | 同 P1-6 | ✅ 闭环 |
| P2-4 `data.id` 契约不一致 | `TodoCreateVO.java:16` String + `design.md` W01 出参表已改为 String | ✅ 闭环 |
| P2-5 限流器 Map 无界 | `TodoRateLimiter.java:91-104` 阈值触发回收 + `timestamps.remove(key, queue)` | ✅ 闭环（性能见 P2-5 本轮） |
| P2-6 停机丢弃无计数 | `AuditIntegrationImpl.java:101-118` 先停拉取→join→超时中断→残留计数 + ERROR | ✅ 闭环（偏差见 P2-10） |
| P2-7 行宽超限 | 显示宽度复核：`src` 全量 0 处 > 120 | ✅ 闭环 |
| P2-8 死常量 | `SNOWFLAKE_WORKER_ID_BITS` / `TODO_NO_FALLBACK_SUFFIX_LENGTH` / `DEFAULT_TENANT_ID` 已删除 | ⚠️ 部分闭环：`DEFAULT_RATE_LIMIT_PER_MINUTE` 仍为死常量（P2-6） |
| P2-9 测试缺口 | 新增 3 个测试类 + 5 个测试类扩充，111 用例全绿 | ✅ 闭环 |
| P2-10 雪花忙等 | `SnowflakeSequenceProvider.java:135-149` 短 `sleep` + 回拨超 5s 快速失败 | ✅ 闭环 |

### 7.3 验证与降级说明

| 验证项 | 命令 | 结果 |
|--------|------|------|
| 单测（真实执行） | `JAVA_HOME=/opt/toolchain/zulu17.56.15-ca-jdk17.0.14-linux_x64 mvn -o -B test` | **BUILD SUCCESS**；surefire 9 个测试类：`SortUtilTest` 21、`TodoTextUtilTest` 12、`TodoControllerTest` 7、`TodoItemManagerTest` 5、`GatewaySignatureVerifierTest` 7、`TodoCreateGateTest` 4、`TodoNoGeneratorImplTest` 9、`TodoValidatorTest` 22、`TodoServiceImplTest` 24 → 合计 **111 / 0 / 0 / 0** |
| 规则预扫 | `scan-all-rules.sh`（52/222 条） | 14 命中，复核 14 误报 |
| 行宽 / Tab | Python 显示宽度统计（CJK=2）+ awk Tab 扫描 | 0 处 > 120、0 处 Tab |
| HTTP 运行时冒烟 | `mvn -o spring-boot:run` / `dependency:build-classpath` | **未执行成功**：离线仓库缺少 `spring-boot-maven-plugin:2.7.18` 运行期依赖（`spring-boot-loader-tools`、`spring-boot-buildpack-platform`、`maven-shade-plugin`）且 `dependency` 插件不可用 → 降级为「单测 + 静态审查」，**未做真实 HTTP 探活** |
| SQL / MyBatis 一致性 | 静态核对 | 字段与 `todo_item.sql` 全对齐、全 `#{}`、`useGeneratedKeys` 回填 `id`、索引名与 `TodoDuplicateKeyException` 常量一致 |

> **未覆盖风险**（不阻塞本次结论，建议预发补验）：①真实 MySQL 下 `FOR UPDATE` 与 200 并发同 `requestId` 的幂等表现（P1-1 的修复验证依赖此项）；②网关侧签名载荷、请求头大小写的字节级一致性（P1-2/P1-4）；③`/actuator/prometheus` 实际抓取与告警阈值（P1-3）；④50 并发隔离落地后的容量表现（P1-5）。

---

## 7.1 问题片段（必填）

### P1-1 `G1.1`/`G9.3` — 幂等回查使用加锁读，并发同 `requestId` 存在锁升级死锁

**P1** `G1.1` `src/main/java/com/antdigital/todo/service/impl/TodoServiceImpl.java:170` — 插入因唯一键失败后，在**同一事务**内对同一唯一索引记录执行 `SELECT ... FOR UPDATE`。失败的 INSERT 已在唯一索引记录上持 S 锁，两个并发事务同时进入该分支即互相等待 S→X 升级，InnoDB 判定死锁回滚其一；异常被 `catch (RuntimeException)` 记为 `TODO_0008` 指标后原样抛出，最终返回 **TODO_0011 / 500**，违反 R07「重复请求返回首次结果」。
片段范围：`src/main/java/com/antdigital/todo/service/impl/TodoServiceImpl.java:166-184`

```java
L166|for (int attempt = 0; attempt <= TodoConstants.TODO_NO_CONFLICT_RETRY_TIMES; attempt++) {
L167|    TodoItemDO entity = buildEntity(validated, operator);
L168|    try {
L169|        todoItemManager.insert(entity);
L170|    } catch (TodoDuplicateKeyException e) {
L171|        // 索引名可能因驱动/版本差异无法解析，先无条件做幂等回查，避免误把幂等冲突当编号冲突
L172|        TodoItemDO concurrent = selectByRequestIdForUpdate(validated.getRequestId(), operator);
L173|        if (concurrent != null) {
L174|            todoMetrics.recordIdempotentHit();
L175|            LOGGER.info("todo create idempotent hit by duplicate key, requestId={}, todoNo={}, constraint={}",
L176|                    validated.getRequestId(), concurrent.getTodoNo(), e.getConstraintName());
L177|            return toResult(concurrent, true);
L178|        }
L179|        LOGGER.warn("todo item unique key conflict, retry with new no. todoNo={}, attempt={}, constraint={}",
L180|                entity.getTodoNo(), attempt, e.getConstraintName());
L181|        continue;
L182|    } catch (RuntimeException e) {
L183|        LOGGER.error("todo create storage error, requestId={}", validated.getRequestId(), e);
L184|        throw new TodoBizException(TodoErrorCodeEnum.STORAGE_ERROR);
```

片段范围：`src/main/resources/mapper/TodoItemMapper.xml:43-52`

```xml
L43|<select id="selectByRequestIdForUpdate" resultMap="TodoItemResultMap">
L44|    SELECT
L45|    <include refid="BaseColumns"/>
L46|    FROM todo_item
L47|    WHERE tenant_id = #{tenantId}
L48|    AND request_id = #{requestId}
L49|    AND is_deleted = 0
L50|    LIMIT 1
L51|    FOR UPDATE
L52|</select>
```

> **Spec 证据**：design §5.1.3.1 R07「同一 `tenantId + requestId` 只允许创建一条待办，重复请求返回首次结果且 `idempotent=true`」；异常场景表「唯一键冲突（并发重复提交）→ 捕获后按 `request_id` 重查并返回首次结果，不向用户报错」。
> **修复方向**：① 回查改为 `REQUIRES_NEW` 事务或独立普通读（去掉 `FOR UPDATE`）；② 单独捕获 `DeadlockLoserDataAccessException`/`CannotAcquireLockException` 后回读重试；③ 冲突为 `uk_todo_item_req` 时直接按 `requestId` 回读首次结果，不再重试取号。

### P1-2 `S8.1`/`G10` — 请求头按大小写精确查找，小写头将导致全量 401

**P1** `S8.1` `src/main/java/com/antdigital/todo/integration/impl/GatewayAuthIntegrationImpl.java:69` — HTTP 头名大小写不敏感，但 `@RequestHeader Map<String,String>` 保留容器原始大小写（非 `HttpHeaders` 的大小写不敏感实现），代码以 `"X-User-Id"` 等精确 key 取值。若网关下发 `x-user-id`（HTTP/2 强制小写、部分网关默认小写），`userId`/`tenantId` 恒空 → 所有创建请求 **TODO_0005**，功能整体不可用；单测用精确大小写 `HashMap` 无法暴露该问题。
片段范围：`src/main/java/com/antdigital/todo/integration/impl/GatewayAuthIntegrationImpl.java:65-78`

```java
L65|public OperatorContext resolveOperator(Map<String, String> headers) {
L66|    if (headers == null || headers.isEmpty()) {
L67|        throw new TodoBizException(TodoErrorCodeEnum.NOT_LOGIN);
L68|    }
L69|    String userId = TodoTextUtil.trimWhitespace(headers.get(TodoConstants.HEADER_USER_ID));
L70|    String tenantId = TodoTextUtil.trimWhitespace(headers.get(TodoConstants.HEADER_TENANT_ID));
L71|    if (userId.isEmpty() || tenantId.isEmpty()) {
L72|        LOGGER.error("gateway identity header missing, userIdPresent={}, tenantIdPresent={}",
L73|                !userId.isEmpty(), !tenantId.isEmpty());
L74|        throw new TodoBizException(TodoErrorCodeEnum.NOT_LOGIN);
L75|    }
L76|    String rawUserName = TodoTextUtil.trimWhitespace(headers.get(TodoConstants.HEADER_USER_NAME));
L77|    List<String> permissions = parsePermissions(headers.get(TodoConstants.HEADER_USER_PERMISSIONS));
L78|    signatureVerifier.verify(headers, buildCanonicalPayload(userId, rawUserName, tenantId, permissions));
```

> **修复方向**：Controller 改用 `HttpHeaders`（大小写不敏感）或以 `LinkedCaseInsensitiveMap` 归一后再解析；补一条「全小写请求头」用例。

### P1-3 `G16.1`/`G13.1` — 取号降级指标恒计数，§6.5 告警永久触发

**P1** `G16.1` `src/main/java/com/antdigital/todo/service/component/SnowflakeSequenceProvider.java:125` — 唯一注册的 `TodoSequenceProvider` 实现恒返回 `isDegraded()==true`，`TodoNoGeneratorImpl` 每次取号都 `recordNoDegrade()`，`todo_no_degrade_total` 恒等于创建量；README/系分 §6.5 的「编号降级事件 > 10 次/分钟 → P2 告警」在全量流量下持续触发，真实降级（Redis 故障）反而无法辨识。
片段范围：`src/main/java/com/antdigital/todo/service/component/TodoNoGeneratorImpl.java:77-85`

```java
L77|public String nextTodoNo() {
L78|    String dateKey = DATE_FORMATTER.format(dateSupplier.get());
L79|    long sequence = sequenceProvider.nextSequence(dateKey);
L80|    if (sequenceProvider.isDegraded()) {
L81|        todoMetrics.recordNoDegrade();
L82|    }
L83|    long suffix = Math.abs(sequence % TodoConstants.TODO_NO_SEQ_MODULUS);
L84|    return TodoConstants.TODO_NO_PREFIX + dateKey + padLeft(String.valueOf(suffix));
L85|}
```

片段范围：`src/main/java/com/antdigital/todo/service/component/SnowflakeSequenceProvider.java:119-127`

```java
L119|/**
L120| * 本实现为取号降级路径，需上报 {@code todo_no_degrade_total}。
L121| *
L122| * @return 恒为 true
L123| */
L124|@Override
L125|public boolean isDegraded() {
L126|    return true;
L127|}
```

> **Spec 证据**：design §6.5「`todo_no_degrade_total`（编号降级次数）」「编号降级事件 > 10 次/分钟 → P2 告警（提示 Redis 异常）」。
> **修复方向**：区分「当前生效实现」与「运行时降级」——默认雪花实现不计降级；或改为 `todo_no_source_total{provider=...}`，仅实际切走 Redis 时计入降级。

### P1-4 `S8.1` — 网关签名未绑定 method/path/报文，窗口内可重放

**P1** `S8.1` `src/main/java/com/antdigital/todo/integration/support/GatewaySignatureVerifier.java:131` — 签名明文仅含 `timestamp + userId + userName + tenantId + permissions`，不含 HTTP 方法、路径与请求体摘要，且仅靠 300s 时间戳窗口防重放、无 nonce 去重。头部一旦泄露（日志、代理、TLS 终结侧），攻击者可在窗口内以该用户身份提交任意待办内容。
片段范围：`src/main/java/com/antdigital/todo/integration/support/GatewaySignatureVerifier.java:125-136`

```java
L125|long timestamp = parseTimestamp(timestampText);
L126|long now = clock.getAsLong();
L127|if (Math.abs(now - timestamp) > toleranceMillis) {
L128|    LOGGER.warn("gateway signature timestamp out of tolerance, deviation={}ms", now - timestamp);
L129|    throw new TodoBizException(TodoErrorCodeEnum.NOT_LOGIN);
L130|}
L131|String expected = sign(secret, timestampText + PAYLOAD_SEPARATOR + canonicalPayload);
L132|if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
L133|        signature.toLowerCase().getBytes(StandardCharsets.UTF_8))) {
L134|    LOGGER.warn("gateway signature mismatch, reject request");
L135|    throw new TodoBizException(TodoErrorCodeEnum.NOT_LOGIN);
L136|}
```

> **修复方向**：载荷加入 `method + path + sha256(body)`（与网关同口径）；缩短容差（如 60s）或引入 `X-Gateway-Nonce` + 短 TTL 去重缓存。

### P1-5 `G8.2` — 系分要求的创建接口线程池/信号量隔离未实现

**P1** `G8.2` `src/main/java/com/antdigital/todo/service/impl/TodoServiceImpl.java:126` — 系分 §6.3 明确「创建接口独立线程池/信号量（默认 50 并发），防止慢 SQL 拖垮整个应用」，代码与 `application.yml` 均无相应配置，创建链路直接运行在容器工作线程上；DB 变慢时全部 Tomcat 线程被占满，影响同实例其他请求。
片段范围：`src/main/java/com/antdigital/todo/service/impl/TodoServiceImpl.java:124-131`

```java
L124|@Override
L125|@Transactional(rollbackFor = Exception.class)
L126|public TodoCreateResult createTodo(TodoCreateRequest request, OperatorContext operator) {
L127|    long startMillis = System.currentTimeMillis();
L128|    try {
L129|        TodoCreateResult result = doCreate(request, operator);
L130|        todoMetrics.recordCreateSuccess(System.currentTimeMillis() - startMillis);
L131|        return result;
```

> **Spec 证据**：design §6.3「隔离：创建接口独立线程池/信号量（默认 50 并发），防止慢 SQL 拖垮整个应用」。
> **修复方向**：为创建链路加信号量/Bulkhead（默认 50 并发），超限快速失败并埋点；同步 `application.yml` 与模块文档配置表。

### P1-6 `R08`/REQ-6 — `source` 接受客户端指定，与 R08 冲突

**P1** `R08` `src/main/java/com/antdigital/todo/service/component/TodoValidator.java:100` — R08 要求 `status`/`is_deleted`/`source` 均「不接受前端指定，服务端覆盖为固定值」，实现却按 W01 入参表接受 `source` 并落库（白名单 web/app/api）。spec 自身矛盾（W01 入参表含 `source`，R08 禁止），代码取了宽口径，需澄清并统一文档与实现。
片段范围：`src/main/java/com/antdigital/todo/service/component/TodoValidator.java:94-106`

```java
L94|/**
L95| * 解析创建来源，缺省为 web，取值非法时报参数错误。
L96| *
L97| * @param source 原始来源
L98| * @return 规范化后的来源编码
L99| */
L100|private String resolveSource(String source) {
L101|    String trimmed = TodoTextUtil.trimWhitespace(source);
L102|    if (trimmed.isEmpty()) {
L103|        return TodoConstants.DEFAULT_SOURCE;
L104|    }
L105|    return TodoSourceEnum.of(trimmed).getCode();
L106|}
```

> **修复方向**：二选一并统一文档/实现——① 保留 W01 口径，修订 R08 为「`status`/`is_deleted` 不接受前端指定；`source` 允许白名单值，缺省 web」；② 严格按 R08，服务端固定 `web` 并删除入参字段。

### P2-1 `G16.1` — 成功指标/成功日志先于事务提交

**P2** `G16.1` `src/main/java/com/antdigital/todo/service/impl/TodoServiceImpl.java:130` — 成功计数与「todo create success」日志在 `@Transactional` 方法返回前执行；若提交阶段失败（连接中断、提交时暴露的死锁回滚），指标与日志仍记成功，成功率虚高。

片段范围：`src/main/java/com/antdigital/todo/service/impl/TodoServiceImpl.java:127-134`

```java
L127|long startMillis = System.currentTimeMillis();
L128|try {
L129|    TodoCreateResult result = doCreate(request, operator);
L130|    todoMetrics.recordCreateSuccess(System.currentTimeMillis() - startMillis);
L131|    return result;
L132|} catch (TodoBizException e) {
L133|    todoMetrics.recordCreateFailure(e.getCode(), System.currentTimeMillis() - startMillis);
L134|    throw e;
```

> **修复方向**：与审计一致，成功计数/成功日志放到 `afterCommit` 回调。

### P2-2 `G16.1` — 非存储异常统一记 `TODO_0008`，与响应码 TODO_0011 不一致

**P2** `G16.1` `src/main/java/com/antdigital/todo/service/impl/TodoServiceImpl.java:136` — 任何 `RuntimeException`（含并发死锁、NPE）均计入 `code=TODO_0008`，而全局处理器对非业务异常返回 **TODO_0011**，导致 §6.5「TODO_0008 1 分钟 > 20 次」告警口径失真。

片段范围：`src/main/java/com/antdigital/todo/service/impl/TodoServiceImpl.java:135-139`

```java
L135|} catch (RuntimeException e) {
L136|    todoMetrics.recordCreateFailure(TodoErrorCodeEnum.STORAGE_ERROR.getCode(),
L137|            System.currentTimeMillis() - startMillis);
L138|    throw e;
L139|}
```

### P2-3 `G16.1` — 指标每次请求动态 `register`

**P2** `G16.1` `src/main/java/com/antdigital/todo/service/component/TodoMetrics.java:190` — 每次创建都 `Counter.builder(...).register(...)`，热路径重复注册表查找；建议按 code 预注册或缓存 `Counter`。

片段范围：`src/main/java/com/antdigital/todo/service/component/TodoMetrics.java:189-198`

```java
L189|private void recordCreate(String code, long costMillis) {
L190|    Counter.builder(METRIC_CREATE_TOTAL)
L191|            .description("待办创建调用总数")
L192|            .tag(TAG_CODE, code)
L193|            .register(meterRegistry)
L194|            .increment();
L195|    if (costMillis >= 0) {
L196|        createCostSummary.record(costMillis);
L197|    }
L198|}
```

### P2-4 `G4.2` — 唯一索引不含 `is_deleted`，幂等查询却过滤软删

**P2** `G4.2` `src/main/resources/mapper/TodoItemMapper.xml:49` + `src/main/resources/db/todo_item.sql:22` — `uk_todo_item_req(tenant_id, request_id)` 不区分删除位，而幂等查询附加 `is_deleted = 0`。日后启用逻辑删除时，同 `requestId` 重新提交会命中唯一键冲突 → 加锁回查因 `is_deleted=0` 查不到 → 重试耗尽返回误导性的 **TODO_0007**（应提示「已存在/已删除」）。

片段范围：`src/main/resources/mapper/TodoItemMapper.xml:43-52`

```xml
L43|<select id="selectByRequestIdForUpdate" resultMap="TodoItemResultMap">
L44|    SELECT
L45|    <include refid="BaseColumns"/>
L46|    FROM todo_item
L47|    WHERE tenant_id = #{tenantId}
L48|    AND request_id = #{requestId}
L49|    AND is_deleted = 0
L50|    LIMIT 1
L51|    FOR UPDATE
L52|</select>
```

> **修复方向**：回查不过滤 `is_deleted`（命中软删记录给出明确错误码），或将删除位纳入唯一索引并同步系分 §5.1.1.1。

### P2-5 `G13`（性能） — 限流器在高基数下每次请求全量扫描

**P2** `G13.1` `src/main/java/com/antdigital/todo/service/component/TodoRateLimiter.java:91` — 用户条目数 ≥ 1024 后，**每次** `acquire` 都遍历全部用户队列（O(n) 且逐个 `synchronized`），用户量大或被刷时会把限流器自身变成瓶颈。

片段范围：`src/main/java/com/antdigital/todo/service/component/TodoRateLimiter.java:86-104`

```java
L86|/**
L87| * 回收已完全滑出窗口的用户条目：仅在用户数超过阈值时触发，避免每次请求全量遍历。
L88| *
L89| * @param now 当前毫秒
L90| */
L91|private void evictIdleUsers(long now) {
L92|    if (timestamps.size() < TodoConstants.RATE_LIMIT_IDLE_SCAN_THRESHOLD) {
L93|        return;
L94|    }
L95|    for (Map.Entry<String, Deque<Long>> entry : timestamps.entrySet()) {
L96|        Deque<Long> queue = entry.getValue();
L97|        synchronized (queue) {
L98|            evictExpired(queue, now);
L99|            if (queue.isEmpty()) {
L100|                timestamps.remove(entry.getKey(), queue);
L101|            }
L102|        }
L103|    }
L104|}
```

> **修复方向**：改为定时清理任务或分片轮询（每次只扫 1/N），避免请求路径上的全量遍历。

### P2-6 `A4.4` — 死常量 `DEFAULT_RATE_LIMIT_PER_MINUTE`

**P2** `A4.4` `src/main/java/com/antdigital/todo/common/constant/TodoConstants.java:68` — 该常量全仓库无引用（`@Value("${todo.create.rate-limit-per-minute:30}")` 直接写字面量 30），属上轮 P2-8「死常量」整改遗漏。

片段范围：`src/main/java/com/antdigital/todo/common/constant/TodoConstants.java:65-68`

```java
L65|/**
L66| * 单用户默认每分钟创建次数上限（R11）
L67| */
L68|public static final int DEFAULT_RATE_LIMIT_PER_MINUTE = 30;
```

### P2-7 `G13.1` — 总开关关闭与灰度未命中共用错误码与指标

**P2** `G13.1` `src/main/java/com/antdigital/todo/service/component/TodoCreateGate.java:68` — 两种语义（应急关停 / 灰度未放量）都抛 `FEATURE_DISABLED` 且累加同一 `todo_create_gray_blocked_total`，灰度期与应急演练时无法从指标区分拦截原因。

片段范围：`src/main/java/com/antdigital/todo/service/component/TodoCreateGate.java:68-81`

```java
L68|public void check(String tenantId) {
L69|    if (!createEnabled) {
L70|        LOGGER.warn("todo create feature disabled by switch");
L71|        throw new TodoBizException(TodoErrorCodeEnum.FEATURE_DISABLED);
L72|    }
L73|    if (grayTails.isEmpty()) {
L74|        return;
L75|    }
L76|    String tail = resolveTenantTail(tenantId);
L77|    if (!grayTails.contains(tail)) {
L78|        LOGGER.info("tenant not in gray scope, tenantTail={}", tail);
L79|        throw new TodoBizException(TodoErrorCodeEnum.FEATURE_DISABLED);
L80|    }
L81|}
```

> **修复方向**：指标加 `reason=switch|gray` 标签（或拆两个计数器），日志同步带上原因。

### P2-8 `S9`/配置 — 签名校验可被配置关闭且无生产防护

**P2** `S9` `src/main/java/com/antdigital/todo/integration/support/GatewaySignatureVerifier.java:110` + `src/main/resources/application.yml:29` — `auth.gateway.verify.enabled=false` 时直接 `return`，身份头退化为「客户端可信」，仅靠文档约定「生产禁止关闭」；误配置将静默放开越权写入。

片段范围：`src/main/java/com/antdigital/todo/integration/support/GatewaySignatureVerifier.java:109-117`

```java
L109|public void verify(Map<String, String> headers, String canonicalPayload) {
L110|    if (!verifyEnabled) {
L111|        return;
L112|    }
L113|    String secret = TodoTextUtil.trimWhitespace(signSecret);
L114|    if (secret.isEmpty()) {
L115|        LOGGER.error("gateway sign secret is not configured, reject request to avoid trusting client headers");
L116|        throw new TodoBizException(TodoErrorCodeEnum.NOT_LOGIN);
L117|    }
```

> **修复方向**：`prod` profile 下 `verifyEnabled=false` 直接启动失败，或将该开关移出配置中心可写范围。

### P2-9 `G10.1` — 重复请求头只保留一个值

**P2** `G10.1` `src/main/java/com/antdigital/todo/controller/TodoController.java:70` — `@RequestHeader Map<String,String>` 在头名重复时只能保留一个值，若网关以多值形式下发权限头，可能丢掉 `TODO_CREATE` 导致误判 TODO_0006。

片段范围：`src/main/java/com/antdigital/todo/controller/TodoController.java:68-74`

```java
L68|@PostMapping("/create")
L69|public ApiResult<TodoCreateVO> create(@Valid @RequestBody TodoCreateRequest request,
L70|                                      @RequestHeader Map<String, String> headers) {
L71|    OperatorContext operator = authIntegration.resolveOperator(headers);
L72|    TodoCreateResult result = todoService.createTodo(request, operator);
L73|    return ApiResult.success(toVO(result));
L74|}
```

> **修复方向**：改用 `HttpHeaders` 或显式声明头参数，并在文档明确「权限头单值、逗号分隔」契约。

### P2-10 `G8.4` — 审计停机中断后未再 join 即统计残留

**P2** `G8.4` `src/main/java/com/antdigital/todo/integration/impl/AuditIntegrationImpl.java:110` — `interrupt()` 后立即读 `eventQueue.size()` 计数，消费线程可能仍在收尾，残留数与实际丢弃数存在偏差，弱化停机丢审计的可观测性。

片段范围：`src/main/java/com/antdigital/todo/integration/impl/AuditIntegrationImpl.java:101-118`

```java
L101|@PreDestroy
L102|public void shutdown() {
L103|    running.set(false);
L104|    try {
L105|        consumer.join(TimeUnit.SECONDS.toMillis(SHUTDOWN_WAIT_SECONDS));
L106|    } catch (InterruptedException e) {
L107|        Thread.currentThread().interrupt();
L108|        LOGGER.error("interrupted while waiting audit consumer shutdown", e);
L109|    }
L110|    if (consumer.isAlive()) {
L111|        consumer.interrupt();
L112|    }
L113|    int remaining = eventQueue.size();
L114|    if (remaining > 0) {
L115|        todoMetrics.recordAuditDegrade();
L116|        LOGGER.error("audit queue not drained on shutdown, dropped={}", remaining);
L117|    }
L118|}
```

### P2-11 `A2.4` — import 分组顺序（`java.*` 置于 `org.*` 之后）

**P2** `A2.4` `src/main/java/com/antdigital/todo/service/impl/TodoServiceImpl.java:23` — 非静态 import 未按完整名 ASCII 升序，`java.*` 排在 `org.*` 之后（全仓库一致），影响统一格式化与工具校验。

片段范围：`src/main/java/com/antdigital/todo/service/impl/TodoServiceImpl.java:23-30`

```java
L23|import org.slf4j.Logger;
L24|import org.slf4j.LoggerFactory;
L25|import org.springframework.transaction.support.TransactionSynchronization;
L26|import org.springframework.transaction.support.TransactionSynchronizationManager;
L27|
L28|import java.time.LocalDateTime;
```

---

## 8. 修复任务列表

> 供后续改代码逐项执行与核销；与 §3–§7 的 `⚠️`/P1/P2 一一对应，完成改 `- [x]`。

### P0

- 无待修复项。

### P1

- [ ] **P1** `src/main/java/com/antdigital/todo/service/impl/TodoServiceImpl.java:170` — `G1.1` 消除幂等回查的锁升级死锁：冲突为 `uk_todo_item_req` 时改用独立事务/非加锁回读（或捕获死锁异常后回读重试），保证 R07「重复提交返回首次结果」
- [ ] **P1** `src/main/java/com/antdigital/todo/integration/impl/GatewayAuthIntegrationImpl.java:69` — `S8.1`/`G10` 请求头解析改为大小写不敏感（`HttpHeaders`/`LinkedCaseInsensitiveMap`），并补「全小写请求头」单测
- [ ] **P1** `src/main/java/com/antdigital/todo/service/component/SnowflakeSequenceProvider.java:125` — `G16.1` 修正取号降级指标口径（默认雪花实现不计降级，或改 `provider` 维度计数），恢复 §6.5 告警有效性
- [ ] **P1** `src/main/java/com/antdigital/todo/integration/support/GatewaySignatureVerifier.java:131` — `S8.1` 签名载荷绑定 method+path+body 摘要，并缩短时间戳容差或引入 nonce 防重放
- [ ] **P1** `src/main/java/com/antdigital/todo/service/impl/TodoServiceImpl.java:126` — `G8.2` 为创建链路补信号量/线程池隔离（默认 50 并发、超限快速失败），同步 `application.yml` 与模块文档
- [ ] **P1** `src/main/java/com/antdigital/todo/service/component/TodoValidator.java:100` — `R08` 澄清并统一 `source` 口径（修订 R08 或服务端固定 `web`），使 spec 与实现一致

### P2（可选）

- [ ] **P2** `src/main/java/com/antdigital/todo/service/impl/TodoServiceImpl.java:130` — `G16.1` 成功指标与成功日志移至 `afterCommit`，避免提交失败仍计成功
- [ ] **P2** `src/main/java/com/antdigital/todo/service/impl/TodoServiceImpl.java:136` — `G16.1` 失败指标按异常类型映射（存储异常 TODO_0008 / 其他 TODO_0011），与响应码一致
- [ ] **P2** `src/main/java/com/antdigital/todo/service/component/TodoMetrics.java:190` — `G16.1` 预注册/缓存 `Counter`，去掉请求路径上的 `register`
- [ ] **P2** `src/main/resources/mapper/TodoItemMapper.xml:49` — `G4.2` 幂等回查去掉 `is_deleted = 0` 或与唯一索引语义对齐，避免逻辑删除后误报 TODO_0007
- [ ] **P2** `src/main/java/com/antdigital/todo/service/component/TodoRateLimiter.java:91` — `G13` 空闲条目清理改为定时/分片执行，消除请求路径 O(n) 扫描
- [ ] **P2** `src/main/java/com/antdigital/todo/common/constant/TodoConstants.java:68` — `A4.4` 删除死常量 `DEFAULT_RATE_LIMIT_PER_MINUTE` 或改由配置绑定引用
- [ ] **P2** `src/main/java/com/antdigital/todo/service/component/TodoCreateGate.java:68` — `G13.1` 开关拦截与灰度拦截在指标/日志上区分原因
- [ ] **P2** `src/main/resources/application.yml:29` — `S9` 生产环境强制签名校验（`prod` 关闭即启动失败）
- [ ] **P2** `src/main/java/com/antdigital/todo/controller/TodoController.java:70` — `G10.1` 改用 `HttpHeaders` 或显式头参数，避免重复头丢值
- [ ] **P2** `src/main/java/com/antdigital/todo/integration/impl/AuditIntegrationImpl.java:110` — `G8.4` 中断后再 join/二次统计，保证停机丢弃计数准确
- [ ] **P2** `src/main/java/com/antdigital/todo/service/impl/TodoServiceImpl.java:23` — `A2.4` 按完整名 ASCII 升序调整 import 分组（全仓库统一执行）
