# 问题修复报告（Fix Report）

> **Change** `todo-create`（新增待办事项） · **阶段** review / 任务节点「问题修复」
> **输入** `cr_report.md`（P0×2、P1×3、P2×7） · **日期** 2026-10-29 · **执行** AiWork
> **规范依据** `dtazziboot-java-coding-standards`（G8.5 / S8.1 / G16.1 / G16.2 / G17.1 / A2.4 / A3.4 / G8.1 / G11.2）
> **验证基线** `mvn -o test` → `Tests run: 88, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`

---

## 1. 修复总览

| 编号 | 等级 | 结论 | 主要落点 |
|------|------|------|----------|
| P0-1 | P0 | ✅ 已修复 | `LoginInterceptor`（拒绝路径 `clear()`）+ 新增 `TraceIdFilter`（过滤器兜底清理） |
| P0-2 | P0 | ✅ 已修复 | `LoginInterceptor.checkTrustedGateway` + 新增 `GatewayIpMatcher` + `application.yml` 显式白名单 |
| P1-1 | P1 | ✅ 已修复 | 新增 `TodoCreateMetrics`（`MeterRegistry`）+ `micrometer-registry-prometheus` + 访问日志 |
| P1-2 | P1 | ✅ 已修复 | 新增 `TraceContext` / `TraceIdFilter`（生成 + MDC + 响应头回写 + 日志串联） |
| P1-3 | P1 | ✅ 已修复 | 新增 `TodoCreateSettings`（运行期绑定，改配置免重启） |
| P2-1 | P2 | ✅ 已修复 | `GlobalExceptionHandler` import 按 ASCII 字典序重排 |
| P2-2 | P2 | ✅ 已修复 | 4 处 >120 列行宽折行 |
| P2-3 | P2 | ✅ 已修复 | 删除死常量 `INSERT_TIMEOUT_MS` |
| P2-4 | P2 | ✅ 已修复 | `TokenBucketRateLimiter` 空闲桶周期淘汰 |
| P2-5 | P2 | ✅ 已修复 | 补拒绝路径上下文清理断言、超长 Header、emoji 落库用例 |
| P2-6 | P2 | ✅ 已修复 | 连接串 `characterEncoding=UTF-8` + `connectionCollation=utf8mb4_general_ci` |
| P2-7 | P2 | ✅ 已修复 | `gray-enabled` 默认 `true`（按 0,1,2 起步灰度） |

---

## 2. P0 修复明细

### P0-1 ThreadLocal 拒绝路径未清理（`G8.5`）

- **根因**：`preHandle` 先 `OperatorContextHolder.set(...)`，随后 `checkSwitchAndGray` 抛 `TODO_006/TODO_007`；Spring MVC 仅在 `preHandle` 返回 `true` 后登记 `afterCompletion`，异常路径不回调，ThreadLocal 残留。
- **修复**：
  1. 调整顺序：**全部校验通过后才 `set` 上下文**，校验阶段不再有"已写入待清理"的状态；
  2. `preHandle` 用 `try/catch(BizException|RuntimeException)` 收口，`handleRejected(...)` 内显式 `OperatorContextHolder.clear()` 并记录访问日志；
  3. `afterCompletion` 改为 `try { 访问日志 } finally { clear() }`；
  4. 新增 `TraceIdFilter`（`OncePerRequestFilter`），`finally` 中清理 `TraceContext`/MDC，杜绝拦截器异常路径的 MDC 残留。
- **证据**：`LoginInterceptor.java:53-92,150-170`；用例 `LoginInterceptorTest#should_throwTodo007_andClearContext_when_switchOff`、`#should_throwTodo006_andClearContext_when_tenantSuffixNotInGrayList`（断言 `OperatorContextHolder.get()` 为 `null`）；端到端 `TodoCreateIntegrationTest#should_rejectAndClearContext_when_switchOffAtRuntime`（真实容器 + 真实拦截器链）。

### P0-2 网关白名单 fail-open（`S8.1` / `R11`）

- **根因**：`!trustedIps.isEmpty() && ...` 使空列表等价于关闭校验，且默认配置即空列表 → 任一可达端口调用方可伪造 `X-User-Id`/`X-Tenant-Id`。
- **修复**：
  1. `checkTrustedGateway` 改为 **fail-closed**：白名单为空 → `LOGGER.error` 安全告警 + `TODO_002`；
  2. 新增 `GatewayIpMatcher` 支持精确 IP 与 IPv4 CIDR（非法/越界表达式一律不匹配，避免配置错误被当成放行）；
  3. `application.yml` 默认值改为 `${TRUSTED_GATEWAY_IPS:127.0.0.1}`，生产由环境变量显式注入；`TrustedGatewayProperties` 注释同步 fail-closed 语义。
- **证据**：`LoginInterceptor.java:94-113`、`GatewayIpMatcher.java`、`application.yml:43-47`；用例 `LoginInterceptorTest#should_throwTodo002_when_trustedIpListEmpty`、`#should_pass_when_sourceIpMatchesCidr`、`GatewayIpMatcherTest`（5 例）、端到端 `TodoCreateIntegrationTest#should_returnTodo002_when_sourceIpNotTrusted`。
- **发布注意**：白名单未配置时接口对全部来源返回 `TODO_002`，属预期；发布单必须携带 `TRUSTED_GATEWAY_IPS`。

---

## 3. P1 修复明细

### P1-1 指标与访问日志缺失（`G16.1`）

- 新增 `TodoCreateMetrics`：`todo.create.requests{result=OK|TODO_00x}`（调用量/成功率/错误码分布）、`todo.create.cost`（成功耗时 → P99 RT）。
- 埋点位置：`TodoItemServiceImpl.createTodo` 成功出口记 `recordSuccess(cost)`；`GlobalExceptionHandler.fail(...)` 统一记失败分桶（覆盖校验、业务、兜底异常；拦截器抛出的 `BizException` 同样经该处理器）。
- `pom.xml` 增加 `micrometer-registry-prometheus`（版本由 `spring-boot-dependencies` 管理），`management.endpoints.web.exposure.include: health,metrics,prometheus`；连接池活跃数由 actuator 自动装配的 HikariCP 指标覆盖。
- 访问日志：`LoginInterceptor.afterCompletion` 输出 `traceId/uri/userId/tenantId/result/costMs`，拒绝路径由 `handleRejected` 输出（`userId/tenantId` 取自请求属性，避免日志字段为空）。
- **证据**：`TodoCreateMetrics.java`、`TodoItemServiceImpl.java:56-66`、`GlobalExceptionHandler.java:88-92`、`application.yml:21-29`；用例 `TodoCreateMetricsTest`（2 例）、`TodoItemServiceImplTest#should_returnCreatedTodo_when_requestIsValid`（`verify(recordSuccess)`）、`TodoCreateIntegrationTest#should_recordMetrics_when_createSucceeds`。

### P1-2 traceId 未生成 / 未入 MDC（`G16.2`）

- 新增 `TraceContext`：合法 Header（`[A-Za-z0-9_-]{1,64}`）透传，缺失或非法（含换行注入、超长）时生成 UUID 并写 MDC。
- 新增 `TraceIdFilter`（`@Order(HIGHEST_PRECEDENCE)`）：入口初始化、响应头回写 `X-Trace-Id`、`finally` 清理。
- `GlobalExceptionHandler` 全部分支日志与 `LoginInterceptor` 拒绝日志均输出 `traceId`；`logging.pattern.level` 追加 `[traceId:%X{traceId:-}]`。
- **证据**：`TraceContext.java`、`TraceIdFilter.java`、`application.yml:49-51`；用例 `TraceContextTest`（4 例，含日志注入与超长拒绝）、`TodoCreateIntegrationTest#should_generateTraceId_when_headerAbsent`。

### P1-3 开关/灰度不支持热更新（`G17.1`）

- 新增 `TodoCreateSettings`：每次读取都用 `Binder` 实时解析 `Environment`，缺失时回落 `TodoCreateProperties`；未接 Spring 容器（纯单测）时 `environment == null` 等价直读 properties。
- `LoginInterceptor`、`RateLimitInterceptor` 改为依赖 `TodoCreateSettings`，`enabled`/`gray-*`/`rate-limit-qps` 改值即生效。
- **证据**：`TodoCreateSettings.java`；用例 `TodoCreateSettingsTest`（3 例，含"改值后免重启生效"）、`TodoCreateIntegrationTest#should_rejectAndClearContext_when_switchOffAtRuntime`（向 `Environment` 注入 `todo.create.enabled=false` 后请求立即返回 `TODO_007`）。

---

## 4. P2 修复明细

| 编号 | 处理方式 | 位置 |
|------|----------|------|
| P2-1 import 顺序 | `org.springframework.http.converter.*` 前移至 `validation.*` 之前 | `GlobalExceptionHandler.java:9` |
| P2-2 行宽 | `TodoConstants`、`TokenBucketRateLimiter`、`TodoCreateIntegrationTest`、`TodoTextNormalizerTest` 全部折行至 ≤120；新增/改动文件同样复核通过 | 见各文件 |
| P2-3 死常量 | 删除 `INSERT_TIMEOUT_MS`（超时由 `default-statement-timeout` 控制），新增实际使用的 `NANOS_PER_MILLI`、`ATTR_RESULT_CODE` | `TodoConstants.java:39-45` |
| P2-4 桶无淘汰 | `TokenBucketRateLimiter` 增加空闲淘汰：60s 扫描一次、300s 未访问即移除；构造器可注入阈值供测试 | `TokenBucketRateLimiter.java:20-70` |
| P2-5 测试缺口 | 新增上下文清理、空白名单、超长 Header、emoji 落库、指标、淘汰、traceId、灰度配置等用例（64 → 88） | 见 §5 |
| P2-6 字符集 | `characterEncoding=UTF-8&connectionCollation=utf8mb4_general_ci`，与 `utf8mb4` 表对齐 | `application.yml:8` |
| P2-7 默认全量 | `gray-enabled: true` + `TodoCreateProperties` 默认值同步为 `true` | `application.yml:36`、`TodoCreateProperties.java:20` |

---

## 5. 变更清单

### 新增（主代码 5）

- `common/trace/TraceContext.java`、`common/trace/TraceIdFilter.java`
- `common/metrics/TodoCreateMetrics.java`
- `common/security/GatewayIpMatcher.java`
- `common/config/TodoCreateSettings.java`

### 修改（主代码 7）

- `common/interceptor/LoginInterceptor.java`（校验顺序、fail-closed、traceId、访问日志、拒绝路径清理）
- `common/interceptor/RateLimitInterceptor.java`（改用 `TodoCreateSettings`）
- `common/exception/GlobalExceptionHandler.java`（import 顺序、traceId、失败指标、结果码属性）
- `common/ratelimit/TokenBucketRateLimiter.java`（空闲淘汰）
- `common/config/TodoCreateProperties.java`（灰度默认开启）
- `common/security/TrustedGatewayProperties.java`（fail-closed 语义注释）
- `item/constant/TodoConstants.java`、`item/service/impl/TodoItemServiceImpl.java`（常量整改、成功指标埋点）
- `pom.xml`（prometheus registry）、`src/main/resources/application.yml`、`src/test/resources/application-test.yml`

### 新增测试（4 类）

- `common/trace/TraceContextTest`（4）、`common/metrics/TodoCreateMetricsTest`（2）、`common/security/GatewayIpMatcherTest`（5）、`common/config/TodoCreateSettingsTest`（3）

### 扩充测试

- `LoginInterceptorTest`（8 → 12）、`TokenBucketRateLimiterTest`（3 → 4）、`TodoCreateIntegrationTest`（2 → 7）、`TodoItemServiceImplTest`（补 `recordSuccess` 断言）、`TodoItemControllerTest`（适配 `GlobalExceptionHandler` 构造）、`RateLimitInterceptorTest`（适配新入参）

---

## 6. 验证结果

```
mvn -o test
Tests run: 88, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- 新增回归用例直接锁定两个 P0：拒绝路径 `OperatorContextHolder.get() == null`、空白名单 `TODO_002` 且不落库。
- 端到端用例运行在真实 Spring 容器 + 真实 MyBatis-Plus + H2 上，覆盖 emoji 4 字节落库、traceId 响应头、运行期开关关闭、指标累计。
- 仅执行 `mvn -o test`；未执行 `mvn clean install`（改动不涉及打包流程，离线仓库无新增外部依赖需求）。

---

## 7. 残留说明与后续建议

1. **生产白名单必须配置**：`TRUSTED_GATEWAY_IPS` 未注入时所有 `/api/**` 返回 `TODO_002`，发布单需同步配置；建议发布后以 `127.0.0.1` 之外的真实网关 CIDR 验证一次连通性。
2. **访问日志范围**：当前访问日志由 `LoginInterceptor.afterCompletion` 输出，非 `/api/**` 路径（如 actuator）不记录；如需全站访问日志建议下沉到统一网关日志。
3. **限流精度**：仍为实例级近似限流，多副本下阈值为 `单实例 QPS × 副本数`，与设计一致；如需全局精确限流需引入集中式令牌桶。
4. **未处理的非阻塞项**：`cr_report` 中 §7 结论之外的观察项（如分布式追踪链路接入）建议随监控平台建设统一推进。
