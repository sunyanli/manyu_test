# Code Review Report

> **Change** `todo-create`（新增待办事项，最小闭环） · **分支** `task/DEV-966dcd0a-7905-11f1-9649-3b4281182f10-75a6d105-1e54-442f-a3d4-a6fda5a9c6ad` / **Commit** `00bc14d` · **日期** 2026-10-29 · **审查者** AI
>
> **AI**：等级 **P0 / P1 / P2**；G/S 以 checklist 行内定义为准；Bug 模式以 `bug-pattern-checklist.md` 表头为准（Blocker→P0、Major→P1、Info→P2）。已先运行 `scan-all-rules.sh`（52/222 条）并将要点并入 §5，再写 LLM 结论。
>
> **复审轮次**：第 2 轮（**修复后复审**）。上轮基于 `61a3d8a`，结论为 P0×2 / P1×3 / P2×7、阻止合并；本轮针对 `问题修复` 提交 `00bc14d`（`+1294 / -76`）逐项核销，并复核修复引入的新风险。
>
> **配套清单**：`.agents/20261029-目标_帮助用户记录日常待办事项_核心功能/2026-10-29-todo-create-cr-checklist.md`（Step 1–5 全量核销源 + 附录 A/B 预扫输出 + Round 2 复审记录）
>
> **验证基线**：`JAVA_HOME=zulu17 mvn -o test` → `Tests run: 88, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`（本轮实测，耗时 17.5s）。

---

## 1. 审查范围

| 项 | 值 |
|----|-----|
| `.java` 文件数 | `24`（主代码 13 + 测试 11），均在本轮 `git diff HEAD~1 --name-only` 中 |
| 变更行数 | `+1294 / -76` |
| 非 Java 变更（跳过码审，仅作证据） | `pom.xml`、`src/main/resources/application.yml`、`src/test/resources/application-test.yml`、`fix_report.md` |
| 功能范围 | 最小闭环「新增待办事项」：W01 `POST /api/todo/item/create` + 横切能力（登录态/网关来源、开关灰度、限流、traceId、指标、访问日志） |

| # | 类/接口 | 路径 | 角色 |
|---|---------|------|------|
| 1 | `TodoCreateProperties` | `src/main/java/com/antdigital/todo/common/config/TodoCreateProperties.java` | 开关/灰度/限流静态绑定（兜底源） |
| 2 | `TodoCreateSettings` | `src/main/java/com/antdigital/todo/common/config/TodoCreateSettings.java` | **新增**：运行期实时读取配置（G17.1） |
| 3 | `GlobalExceptionHandler` | `src/main/java/com/antdigital/todo/common/exception/GlobalExceptionHandler.java` | 统一异常映射 + 失败指标 + traceId |
| 4 | `LoginInterceptor` | `src/main/java/com/antdigital/todo/common/interceptor/LoginInterceptor.java` | R01/R09/R11/R12/R13 + 访问日志 |
| 5 | `RateLimitInterceptor` | `src/main/java/com/antdigital/todo/common/interceptor/RateLimitInterceptor.java` | 用户级限流（设计 C03） |
| 6 | `TodoCreateMetrics` | `src/main/java/com/antdigital/todo/common/metrics/TodoCreateMetrics.java` | **新增**：Micrometer 业务指标（G16.1） |
| 7 | `TokenBucketRateLimiter` | `src/main/java/com/antdigital/todo/common/ratelimit/TokenBucketRateLimiter.java` | 令牌桶 + 空闲淘汰 |
| 8 | `GatewayIpMatcher` | `src/main/java/com/antdigital/todo/common/security/GatewayIpMatcher.java` | **新增**：白名单精确 IP / IPv4 CIDR 匹配（S8.1） |
| 9 | `TrustedGatewayProperties` | `src/main/java/com/antdigital/todo/common/security/TrustedGatewayProperties.java` | 白名单配置（fail-closed 语义） |
| 10 | `TraceContext` | `src/main/java/com/antdigital/todo/common/trace/TraceContext.java` | **新增**：traceId ThreadLocal + MDC（G16.2） |
| 11 | `TraceIdFilter` | `src/main/java/com/antdigital/todo/common/trace/TraceIdFilter.java` | **新增**：入口生成/透传 + `finally` 清理（G8.5） |
| 12 | `TodoConstants` | `src/main/java/com/antdigital/todo/item/constant/TodoConstants.java` | 常量整改（删除死常量、新增换算/属性键） |
| 13 | `TodoItemServiceImpl` | `src/main/java/com/antdigital/todo/item/service/impl/TodoItemServiceImpl.java` | R02~R08/R10 + 成功指标埋点 |
| 14 | `TodoCreateSettingsTest` | `src/test/java/com/antdigital/todo/common/config/TodoCreateSettingsTest.java` | 热更新/回落（3 例） |
| 15 | `LoginInterceptorTest` | `src/test/java/com/antdigital/todo/common/interceptor/LoginInterceptorTest.java` | 拒绝路径清理、空白名单、CIDR、超长 Header（12 例） |
| 16 | `RateLimitInterceptorTest` | `src/test/java/com/antdigital/todo/common/interceptor/RateLimitInterceptorTest.java` | 限流/无上下文放行（2 例） |
| 17 | `TodoCreateMetricsTest` | `src/test/java/com/antdigital/todo/common/metrics/TodoCreateMetricsTest.java` | 成功/失败分桶（2 例） |
| 18 | `TokenBucketRateLimiterTest` | `src/test/java/com/antdigital/todo/common/ratelimit/TokenBucketRateLimiterTest.java` | 限流 + 空闲淘汰（4 例） |
| 19 | `GatewayIpMatcherTest` | `src/test/java/com/antdigital/todo/common/security/GatewayIpMatcherTest.java` | 精确/CIDR/非法表达式 fail-closed（5 例） |
| 20 | `TraceContextTest` | `src/test/java/com/antdigital/todo/common/trace/TraceContextTest.java` | 生成/透传/防注入/清理（4 例） |
| 21 | `TodoCreateIntegrationTest` | `src/test/java/com/antdigital/todo/item/controller/TodoCreateIntegrationTest.java` | 真实容器 + H2 端到端（7 例） |
| 22 | `TodoItemControllerTest` | `src/test/java/com/antdigital/todo/item/controller/TodoItemControllerTest.java` | 出参/错误码（适配新构造） |
| 23 | `TodoItemServiceImplTest` | `src/test/java/com/antdigital/todo/item/service/impl/TodoItemServiceImplTest.java` | 业务规则 + `recordSuccess` 断言 |
| 24 | `TodoTextNormalizerTest` | `src/test/java/com/antdigital/todo/item/util/TodoTextNormalizerTest.java` | 规范化/非法字符（行宽整改） |

---

## 2. 问题计数

| P0 | P1 | P2 |
|----|----|-----|
| 0 | 1 | 6 |

> **blocker_count = 0**。上轮 2 个 P0（ThreadLocal 残留、网关白名单 fail-open）经代码复核 + 新增回归用例 + 本轮实测全绿，均判定 **已闭合**；本轮新增问题无 P0。

---

## 3. Step 2 — 功能（REQ）

### 3.1 上轮 P0/P1 修复核销（本轮重点）

| 上轮编号 | 结论 | 代码证据 | 测试证据 |
|----------|------|----------|----------|
| P0-1 `G8.5` ThreadLocal 拒绝路径残留 | ✅ 已闭合 | `LoginInterceptor.java:63-74`（全部校验通过后才 `set`）+ `:75-81`（`catch` → `handleRejected`）+ `:155-159`（拒绝即 `clear()`）+ `TraceIdFilter.java:33-37`（`finally` 清 MDC） | `LoginInterceptorTest#should_throwTodo007_andClearContext_when_switchOff`、`#should_throwTodo006_andClearContext_when_tenantSuffixNotInGrayList`、`#should_throwTodo002_when_trustedIpListEmpty`（均断言 `OperatorContextHolder.get()` 为 `null`）；`TodoCreateIntegrationTest#should_rejectAndClearContext_when_switchOffAtRuntime`（真实容器） |
| P0-2 `S8.1`/R11 白名单 fail-open | ✅ 已闭合 | `LoginInterceptor.java:99-113`（空 → ERROR 安全告警 + `TODO_002`；非命中 → WARN + `TODO_002`），`GatewayIpMatcher.java:34-58`（空/非法一律不匹配），`application.yml:46`（`${TRUSTED_GATEWAY_IPS:127.0.0.1}`） | `LoginInterceptorTest#should_throwTodo002_when_trustedIpListEmpty`、`#should_throwTodo002_when_sourceIpNotTrusted`、`#should_pass_when_sourceIpMatchesCidr`、`GatewayIpMatcherTest`（5 例）、`TodoCreateIntegrationTest#should_returnTodo002_when_sourceIpNotTrusted`（且校验不落库） |
| P1-1 `G16.1` 指标/访问日志缺失 | ✅ 已闭合（残留 P2-2） | `TodoCreateMetrics.java:44-59`、`TodoItemServiceImpl.java:68`、`GlobalExceptionHandler.java:92-96`、`LoginInterceptor.java:166-176`、`pom.xml:54-57`、`application.yml:21-29` | `TodoCreateMetricsTest`（2 例）、`TodoItemServiceImplTest`（`verify(recordSuccess)`）、`TodoCreateIntegrationTest#should_recordMetrics_when_createSucceeds` |
| P1-2 `G16.2`/REQ-7 traceId 未生成 | ✅ 已闭合 | `TraceContext.java:37-45`（合法透传 / 非法或缺失生成 UUID）、`:59-62`（ThreadLocal+MDC 双清）、`TraceIdFilter.java:31-37`（回写响应头）、`application.yml:48-51`（日志 pattern 带 `%X{traceId:-}`） | `TraceContextTest`（4 例，含换行注入与 65 字符拒绝）、`TodoCreateIntegrationTest#should_generateTraceId_when_headerAbsent`（响应头 32 位） |
| P1-3 `G17.1` 开关/灰度不支持热更新 | ✅ 已闭合 | `TodoCreateSettings.java:40-92`（每次读取实时 `Binder` 解析，缺失回落 properties）；`LoginInterceptor`/`RateLimitInterceptor` 改为依赖该读取器 | `TodoCreateSettingsTest#should_reflectChangedConfiguration_withoutRestart`、`TodoCreateIntegrationTest#should_rejectAndClearContext_when_switchOffAtRuntime`（运行期注入 `todo.create.enabled=false` 后立即 `TODO_007`） |

### 3.2 需求功能点

#### REQ-1: 新增待办主流程（F01）

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| Given 合法登录态与 `title`/`description`，When `POST /api/todo/item/create`，Then 落库 1 条并返回 `result=OK`、`data.id/title/status/gmtCreate` | ✅ | design §4.1 W01、§5.1.2 出参表 | `TodoItemController.java:39-46`；`TodoCreateIntegrationTest.java:60-98`（真实 INSERT 回查 7 个字段） | 修复未改动主流程，回归通过 |

#### REQ-2: 入参校验与文本规范化（F02）

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 名称空白/超 100 字符 → TODO_001；描述超 500 → TODO_001；控制字符 → TODO_005 | ✅ | design §5.1.3.1 R02~R06 | `TodoItemServiceImpl.java:52-92`、`TodoTextNormalizer.java:25-76`、`CreateTodoRequest.java:19-25` | 规范化后按码点计数，emoji 计 1 字符 |

#### REQ-3: 创建人与租户归属识别（F03）

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| `tenant_id`/`creator_id` 只取自 `OperatorContext`；`status` 固定 0；`creator_name` 缺失以 userId 兜底 | ✅ | design R07/R08、§6.4.2.1 | `LoginInterceptor.java:58-74`；`TodoItemServiceImpl.java:94-114`；`TodoCreateIntegrationTest.java:86-92` | Body 无同名入参入口；修复后归属注入时机更安全（校验通过后） |

#### REQ-4: 登录态与来源校验（R11/R12/R13）

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 登录 Header 缺失/超长 → TODO_002；`tenantId` 参数不一致 → TODO_003；来源 IP 不在白名单 → TODO_002 | ✅ | design §5.2.3 R11/R12/R13、§6.4.2.1 | `LoginInterceptor.java:63-68,99-121,123-128`；`LoginInterceptorTest.java:104-166` | **上轮 P0-2 已闭合**：空列表不再等价放行；白名单支持 `10.0.0.0/24` |

#### REQ-5: 开关与灰度（R09）

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 总开关关闭 → TODO_007；灰度未命中 → TODO_006；命中 → 放行；改配置免重启 | ✅ | design §7.2、§7.3、R09 | `LoginInterceptor.java:130-146`、`TodoCreateProperties.java:16-23`、`application.yml:31-42` | 校验顺序前移，异常路径不再残留上下文；运行期改动即时生效 |

#### REQ-6: 落库失败统一降级（R10）

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 持久化异常 / 影响行数≠1 → TODO_004，不暴露 SQL 细节 | ✅ | design R10、§5.2.3 C02 | `TodoItemServiceImpl.java:63-67,116-125` | catch 内 ERROR 带 `tenantId/creatorId` + 异常栈 |

#### REQ-7: 链路追踪与可监控（`X-Trace-Id`、指标、访问日志）

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| `X-Trace-Id` 缺失时应用生成，并回写响应头、进入日志 | ✅ | design §4.1 入参 Header 表、§6.5 日志行 | `TraceContext.java:37-45`、`TraceIdFilter.java:31-37`、`application.yml:48-51` | 上轮 P1-2 闭合 |
| 指标：QPS、P99 RT、错误码分布（TODO_001~007 分桶）、连接池活跃数 | ✅ | design §6.5 指标表 | `TodoCreateMetrics.java:44-59`、`GlobalExceptionHandler.java:92-96`、`application.yml:21-29` | 连接池活跃数由 Hikari 自动指标覆盖；**「DB insert 耗时」未独立打点**（记的是整段 service 耗时）→ P2-2 |
| 访问日志（traceId、userId、tenantId、耗时、结果码） | ✅ | design §6.5 日志行、§7.1 | `LoginInterceptor.java:84-92,155-176` | 成功与拒绝分支均输出，拒绝分支从请求属性兜底取值 |
| 统计：每日新增待办条数（按租户聚合） | ⚠️ | design §6.5「统计」行 | 无专用实现 | 可由 `todo_item` 按 `tenant_id + gmt_create` 聚合直接得出，本期最小闭环可接受 → 并入 P2-2 说明，不单列 |

#### REQ-8: 统一出参与错误码

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 失败返回 `{result,msg,data=null}`；TODO_001~007 与设计一致 | ✅ | design §5 全局约定、§5.1.1.2、§5.2.3 错误码表 | `ApiResponse.java:30-54`、`ResultCode.java:13-34`、`GlobalExceptionHandler.java:45-96`、`RateLimitInterceptor.java:48`（限流 → TODO_004，与设计 §5.2.3「超限返回 TODO_004」一致） | 文案与设计原文逐条一致 |

#### REQ-9: 建表脚本

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 9 字段类型/默认值、`PRIMARY`、`idx_todo_item_tenant_creator(tenant_id,creator_id,gmt_create)` | ✅ | design §5.1.1.1 | `src/main/resources/db/todo_item.sql:2-15` | 本轮无表结构变更（G15.1 向前兼容） |

---

## 4. Step 3 — 可读性检查

> 本次有 Java，全量执行 A1–A7。

| 结果 | 说明（违规写 Ax.x 与 `path:行`） |
|------|--------------------------------|
| ✅ | **A2.4 已整改**：`GlobalExceptionHandler.java:10-15` import 现按完整引入名 ASCII 序（`http.converter` 在 `validation` 之前） |
| ✅ | **A3.4 已整改**：变更内 24 个 `.java` 逐行复测，无 >120 列（全仓仅 `src/test/java/com/antdigital/sort/SortUtilTest.java:19` 超宽，属既有排序模块、非本次变更，不计入） |
| ✅ | A1 文件格式（命名/UTF-8/无 Tab）、A3 其余项（K&R、4 空格）、A4 命名、A5 编码实践（`@Override` 齐全、无空 catch、无 `System.out`/`printStackTrace`）、A6 元素样式、A7 Javadoc（新增 5 个类均有类级/方法级注释，getter 省略符合 A7.3） |

---

## 5. Step 4 — 可靠性检查

> **预扫（强制）**：`bash references/script/scan-all-rules.sh src/main/java/com/antdigital/todo src/test/java/com/antdigital/todo` → `5 findings (P0=5) | 52/222 rules scanned`，全部为 `G16.2 — CatchWithoutLogging`。逐条人工复核结果见下表末尾「预扫复核」。

| 域 | 参考 | 结果 | 等级 | 说明（列命中 ID 或「已扫无命中」） |
|----|------|------|------|-------------------------------------|
| 可靠性 | `reliability-checklist.md` G1–G17 | ⚠️ | P2 | `G8.5` **已闭合**（`LoginInterceptor.java:75-81,155-159` + `TraceIdFilter.java:33-37`）；`G16.1` ✅ 指标/访问日志落地；`G16.2` ⚠️ `GatewayIpMatcher.java:105-107` IPv4 解析失败静默返回 `-1`（P2-1）；`G17.1` ✅ 运行期配置读取；`G8.1` ✅ 令牌桶空闲淘汰；`G11.2` ⚠️ 缺 `X-User-Name` 超长用例（并入 P2-3） |
| 安全 | `security-checklist.md` S1–S10 | ⚠️ | P1 | `S8.1` **业务链路已闭合**（fail-closed + 回归用例）；但新增的 Prometheus/actuator 端点被两个拦截器显式排除且无任何鉴权 → **P1-1**。其余 S1/S2/S3/S4/S5/S6/S7/S9/S10 已扫无命中（无 `${}` 拼接、无富文本、无外部 URL/命令执行/XML/文件上传、凭证走环境变量、无 CORS 通配） |
| Bug 模式 | `bug-pattern-checklist.md` B/M/I（120） | ✅ | — | 预扫 5 条 `G16.2` 全部落在 catch 行；逐条核销 B001–B081 / M001–M027 / I001–I010 无 Blocker/Major 命中（`M027` ThreadLocal 为 `static`、`B012` 无 `==` 比较字符串、`B004` 无魔法值散落） |

**预扫复核（5 条逐一）**

| 命中 | 判定 | 依据 |
|------|------|------|
| `LoginInterceptor.java:75` | 误报 | `catch (BizException ex)` 内调用 `handleRejected(...)`（`:155-159` 内 `clear()` + INFO 访问日志）后原样抛出 |
| `LoginInterceptor.java:78` | 误报 | 同上，`RuntimeException` 分支最终由 `GlobalExceptionHandler.java:86-90` 以 ERROR + 栈输出 |
| `GatewayIpMatcher.java:66` | 误报 | `catch (NumberFormatException ex)` 紧邻 `LOGGER.warn("受信网关白名单 CIDR 前缀非法, pattern={}")` |
| `GatewayIpMatcher.java:105` | **真命中（降级 P2）** | `toIpv4Value` 解析失败直接 `return -1L`，无日志 → 见 P2-1；因结果仍是 fail-closed（拒绝而非放行），且配置错误可在启动/联调阶段暴露，按 **P2** 记录（清单 G16.2 行内为 P1，此处依据影响面降级并留痕） |
| `TodoItemServiceImpl.java:119` | 误报 | catch 内 `LOGGER.error(..., ex)` 已记录且含归属上下文 |

---

## 6. Step 5 — 自定义扩展检查

| 域 | 参考 | 结果 | 等级 | 说明 |
|----|------|------|------|------|
| 自定义扩展 | `customized-checklist.md` U* | ✅ | — | `U1.1` Controller 入参使用 `@Valid @RequestBody`（`TodoItemController.java:40`）；`U2.*` 无具体规则，`N/A(未启用自定义规则)` |

---

## 7. 结论

- **合并建议**：**修复后合并**（P0 已清零；仅 1 项 P1 属可观测性端点暴露，建议随本次发布单一起整改后合并）
- **P0**：无。上轮 P0-1（ThreadLocal 残留）、P0-2（网关白名单 fail-open）均已闭合，并有单测 + 端到端用例锁定。
- **P1**：
  1. `/api/actuator/**` 被登录/限流拦截器整体排除，`metrics`、`prometheus` 端点无鉴权暴露，成为 fail-closed 主链路之外的唯一免校验入口（内部容量、错误码分布、Hikari/JVM 指标可被任意可达方读取）。
- **P2**：
  1. `GatewayIpMatcher` IPv4 解析失败静默（排障盲区）。
  2. `todo.create.cost` 记的是整段 service 耗时而非「DB insert 耗时」；`todo.create.requests` 由全局异常处理器统一打点，非 create 接口的失败也会计入（当前仅一个接口，无实际偏差）；「每日新增按租户聚合」统计未落地（可由表数据聚合）。
  3. `X-User-Name` 未做长度/字符校验，超 128 字符会退化为 `TODO_004`「系统繁忙」。
  4. `TodoCreateSettings` 每请求执行 4 次属性绑定，建议进程内缓存 + 变更事件失效。
  5. 网关白名单仅支持 IPv4，IPv6 来源一律拒绝（fail-closed，但若网关走 IPv6 将全量拒服）。
  6. `pom.xml` 的 `name/description` 仍为「排序算法核心模块（冒泡排序三变体）」，与应用实际能力不符。
- **一句话**：两个 P0 的修复方向正确且有回归用例/端到端用例锁定，88 个用例实测全绿，主流程、身份边界、可观测性与应急开关均已达设计基线；剩余风险集中在**新增 actuator 端点的鉴权边界**与少量可维护性细节。

---

## 7.1 问题片段（必填）

### P1-1 · `S8.1` · `src/main/java/com/antdigital/todo/common/config/WebMvcConfig.java:32-41`

- **等级**：P1（合并前应修复）
- **规则**：`S8.1`（接口鉴权边界），关联 design §6.4.2「应用只信任网关来源」
- **问题**：登录态与限流拦截器同时 `excludePathPatterns("/api/actuator/**")`，而 `application.yml:21-29` 把 `health,metrics,prometheus` 暴露在同一容器端口 `/api/actuator` 下，`pom.xml` 未引入 `spring-boot-starter-security`，也没有独立 management 端口。结果：主链路 `/api/**` 已 fail-closed，但 `/api/actuator/prometheus`、`/api/actuator/metrics` 对任何能连通应用端口的调用方完全开放，可读取 `todo.create.requests{result=...}` 错误码分布、耗时分布、连接池/JVM 指标。
- **证据性质**：静态证据（路由 + 排除规则 + 依赖清单）；未做运行时探测（启动需外部 MySQL）。建议修复后用 `curl -s localhost:8080/api/actuator/prometheus`（不带任何 Header）复核应返回 401/404。
- 片段范围：`src/main/java/com/antdigital/todo/common/config/WebMvcConfig.java:32-41`

```java
L32|    public void addInterceptors(InterceptorRegistry registry) {
L33|        registry.addInterceptor(loginInterceptor)
L34|                .addPathPatterns(API_PATTERN)
L35|                .excludePathPatterns("/api/actuator/**")   // 问题：管理端点绕过登录态/来源校验
L36|                .order(1);
L37|        registry.addInterceptor(rateLimitInterceptor)
L38|                .addPathPatterns(API_PATTERN)
L39|                .excludePathPatterns("/api/actuator/**")
L40|                .order(2);
L41|    }
```

```yaml
L21|management:
L22|  endpoints:
L23|    web:
L24|      base-path: /api/actuator          # 与业务同端口同名空间
L25|      exposure:
L26|        include: health,metrics,prometheus   # prometheus/metrics 对外可读，无鉴权
```

### P2-1 · `G16.2` · `src/main/java/com/antdigital/todo/common/security/GatewayIpMatcher.java:101-107`

- **等级**：P2（参考）
- **规则**：`G16.2`（异常/失败路径缺少可追溯日志）
- **问题**：`toIpv4Value` 解析失败一律 `return -1L`，无任何日志。白名单写错（如 `10.0.0.300`、`10.0.0`）时全部请求被 `TODO_002` 拒绝，运维只能看到「全量拒绝」而无法定位是哪条白名单非法。
- 片段范围：`src/main/java/com/antdigital/todo/common/security/GatewayIpMatcher.java:100-107`

```java
L100|        for (String segment : segments) {
L101|            int octet;
L102|            try {
L103|                octet = Integer.parseInt(segment);
L104|            } catch (NumberFormatException ex) {
L105|                return -1L;   // 问题：静默失败，非法白名单/来源 IP 无日志可查
L106|            }
L107|            if (octet < 0 || octet > 255) {
```

### P2-2 · `G16.1` / REQ-7 · `src/main/java/com/antdigital/todo/item/service/impl/TodoItemServiceImpl.java:66-69`

- **等级**：P2（参考）
- **说明**：设计 §6.5 要求「DB insert 耗时」单列指标，实现记录的是 `createTodo` 全段耗时（含规范化、校验），insert 变慢与校验变慢无法区分；`todo.create.requests` 在 `GlobalExceptionHandler.fail` 中对所有异常统一打点（当前仅一个接口，无实际偏差，接口扩容后需按 uri 区分）。
- 片段范围：`src/main/java/com/antdigital/todo/item/service/impl/TodoItemServiceImpl.java:61-69`

```java
L61|        TodoItemDO record = buildRecord(title, description, operator);
L62|        int affectedRows = insertRecord(record);      // 未计时：insert 真实耗时未单独埋点
L63|        if (affectedRows != EXPECTED_AFFECTED_ROWS) {
L64|            LOGGER.error("待办事项落库影响行数异常, affectedRows={}, tenantId={}, creatorId={}",
L65|                    affectedRows, operator.getTenantId(), operator.getUserId());
L66|            throw new BizException(ResultCode.TODO_004);
L67|        }
L68|        todoCreateMetrics.recordSuccess(System.nanoTime() - startNanos);   // 记的是整段 service 耗时
```

### P2-3 · `G11.2` · `src/main/java/com/antdigital/todo/common/interceptor/LoginInterceptor.java:72-74`

- **等级**：P2（参考）
- **说明**：`X-User-Id`/`X-Tenant-Id` 受 `OPERATOR_FIELD_MAX_LEN=64` 约束（`LoginInterceptor.java:186-188`），而 `X-User-Name` 直接取值写入 `creator_name varchar(128)`（`db/todo_item.sql:9`）。超长展示名会以数据库异常形式落到 `TODO_004`「系统繁忙」，而非可解释的 `TODO_002`；同时无非法字符过滤。设计未对该 Header 约定长度，故按 P2 记录，建议复用 `OPERATOR_FIELD_MAX_LEN` 或按列宽截断。
- 片段范围：`src/main/java/com/antdigital/todo/common/interceptor/LoginInterceptor.java:71-74`

```java
L71|
L72|            String userName = trimToNull(request.getHeader(TodoConstants.HEADER_USER_NAME));
L73|            OperatorContextHolder.set(new OperatorContext(userId, userName == null ? userId : userName, tenantId));
L74|            return true;   // 问题：userName 未做长度/字符校验，超 128 字符将在 INSERT 阶段报错
```

### P2-4 · `G17.1`-补充 · `src/main/java/com/antdigital/todo/common/config/TodoCreateSettings.java:50-55`

- **等级**：P2（参考）
- **说明**：为满足热更新改为每次请求实时绑定，单次创建请求触发 4 次 `Binder.bind`（`isCreateEnabled`/`isGrayEnabled`/`grayTenantSuffix`/`rateLimitQps`）。当前 50 QPS/实例的量级可接受，但建议加 1s 级本地缓存并在 `EnvironmentChangeEvent` 时失效，避免属性解析占用请求线程。
- 片段范围：`src/main/java/com/antdigital/todo/common/config/TodoCreateSettings.java:50-55`

```java
L50|    public boolean isCreateEnabled() {
L51|        if (binder == null) {
L52|            return properties.isEnabled();
L53|        }
L54|        return binder.bind(KEY_ENABLED, Boolean.class).orElseGet(properties::isEnabled);   // 每请求解析一次
L55|    }
```

### P2-5 · 可用性 · `src/main/java/com/antdigital/todo/common/security/GatewayIpMatcher.java:34-58`

- **等级**：P2（参考）
- **说明**：匹配器只实现 IPv4（`toIpv4Value` 按 `.` 分 4 段），IPv6 来源（如 `::1`、容器双栈环境）一律不匹配 → fail-closed 拒绝。方向安全，但若网关回源走 IPv6，会表现为「白名单已配置仍全量 `TODO_002`」。建议发布单注明只支持 IPv4，或后续支持 IPv6/CIDR6。
- 片段范围：`src/main/java/com/antdigital/todo/common/security/GatewayIpMatcher.java:34-44`

```java
L34|    public static boolean matches(List<String> patterns, String ip) {
L35|        if (patterns == null || patterns.isEmpty() || ip == null) {
L36|            return false;
L37|        }
L38|        for (String pattern : patterns) {
L39|            if (matchesSingle(pattern, ip)) {   // 仅 IPv4：IPv6 来源恒为 false
L40|                return true;
L41|            }
L42|        }
L43|        return false;
L44|    }
```

### P2-6 · 工程一致性 · `pom.xml:11-12`

- **等级**：P2（参考）
- **说明**：`name`/`description` 仍为「sort-core / 排序算法核心模块（冒泡排序三变体）」。artifactId 保留可解释（design A01：在 `sort-core` 仓库内新增 Web 能力），但描述与当前交付能力不符，易误导构建产物与后续维护者。
- 片段范围：`pom.xml:11-12` → **N/A(非 Java)**，原文如下

```xml
L11|    <name>sort-core</name>
L12|    <description>排序算法核心模块（冒泡排序三变体）</description>
```

---

## 8. 修复任务列表

> 与 §3–§7 中 ❌/⚠️ 及结论中的可执行项一一对应；无 P0 待办。

### P0

- 无待修复项（上轮 P0-1、P0-2 已闭合，回归用例见 §3.1）。

### P1

- [ ] **P1** `WebMvcConfig.java:33-40` + `application.yml:21-29` — 收敛管理端点为可鉴权：优先 `management.server.port` 独立端口（仅内网）并把 `exposure.include` 收缩为 `health,prometheus`；若必须同端口，则为 `/api/actuator/**` 保留来源白名单校验（`GatewayIpMatcher`）而非整体排除，并在发布单补一条「不带 Header 访问 `/api/actuator/prometheus` 应被拒绝」的验收步骤。

### P1（可选加固，随 P1 一并处理）

- [ ] **P1** `LoginInterceptor.java:72-73` — `X-User-Name` 复用 `OPERATOR_FIELD_MAX_LEN`/`creator_name` 列宽校验，超限回退为 `X-User-Id`。

### P2（可选）

- [ ] **P2** `GatewayIpMatcher.java:105-107` — 解析失败分支补 `LOGGER.warn("非法 IP 表达式, value={}")`，消除 fail-closed 排障盲区。
- [ ] **P2** `TodoItemServiceImpl.java:62` — 为 `insertRecord` 单独计时并写入 `todo.create.db.insert.cost`，与设计 §6.5「DB insert 耗时」对齐。
- [ ] **P2** `GlobalExceptionHandler.java:94` — 指标加 `uri` 标签或在 `TodoItemController`/Service 层打点，避免接口扩容后非创建接口的失败污染 `todo.create.requests`。
- [ ] **P2** `TodoCreateSettings.java:50-92` — 增加 1s 级本地缓存 + `EnvironmentChangeEvent` 失效，降低每请求 4 次属性绑定开销。
- [ ] **P2** `GatewayIpMatcher.java:34-58` — 明确「仅支持 IPv4」并在发布单标注；如需 IPv6 回源再扩展 CIDR6。
- [ ] **P2** `pom.xml:11-12` — 更新 `name`/`description` 为「todo 待办事项服务（含排序模块）」，保持构建产物元信息准确。
