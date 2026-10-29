# Code Review Report

> **Change** `todo-create`（新增待办事项） · **分支/Commit** `task/DEV-966dcd0a-7905-11f1-9649-3b4281182f10-75a6d105-1e54-442f-a3d4-a6fda5a9c6ad` / `61a3d8a` · **日期** 2026-10-29 · **审查者** AI
>
> **AI**：等级 **P0 / P1 / P2**；G/S 以 checklist 行内定义为准；Bug 模式以 `bug-pattern-checklist.md` 表头为准（Blocker→P0、Major→P1、Info→P2）。已先运行 `scan-all-rules.sh`（52/222 条）并将要点并入 §5，再写 LLM 结论。
>
> **配套清单**：`.agents/20261029-目标_帮助用户记录日常待办事项_核心功能/2026-10-29-todo-create-cr-checklist.md`（Step 1–5 全量核销源）
>
> **验证基线**：`mvn -o test` → `Tests run: 64, Failures: 0, Errors: 0`，`BUILD SUCCESS`（JDK 17 + Maven 3.9.9，离线仓库）。

---

## 1. 审查范围

| 项 | 值 |
|----|-----|
| `.java` 文件数 | `30`（主代码 23 + 测试 7） |
| 变更行数 | `+2293 / -0`（含 `pom.xml`、`application.yml`、SQL、`design.md`、`impl.md` 等非 Java 文件，已跳过代码审查） |
| 功能范围 | 最小闭环「新增待办事项」：W01 `POST /api/todo/item/create` |

| 类/接口 | 路径 | 角色 |
|---------|------|------|
| `TodoApplication` | `src/main/java/com/antdigital/todo/TodoApplication.java` | 启动类、`@MapperScan` |
| `TodoItemController` | `src/main/java/com/antdigital/todo/item/controller/TodoItemController.java` | W01 接口入口 |
| `TodoItemService` / `TodoItemServiceImpl` | `src/main/java/com/antdigital/todo/item/service/`、`.../service/impl/` | S01 业务规则 R02~R08、R10 |
| `TodoItemMapper` / `TodoItemDO` | `src/main/java/com/antdigital/todo/item/mapper/`、`.../model/` | S02 单表写入 |
| `CreateTodoRequest` / `CreateTodoResponse` | `src/main/java/com/antdigital/todo/item/model/` | 出入参 |
| `TodoTextNormalizer` | `src/main/java/com/antdigital/todo/item/util/` | R05/R06 规范化与非法字符 |
| `LoginInterceptor` | `src/main/java/com/antdigital/todo/common/interceptor/LoginInterceptor.java` | R01/R09/R11/R12/R13 |
| `RateLimitInterceptor` / `TokenBucketRateLimiter` | `.../common/interceptor/`、`.../common/ratelimit/` | 用户级限流（设计 §5.2.3 C03） |
| `GlobalExceptionHandler` / `BizException` | `src/main/java/com/antdigital/todo/common/exception/` | 统一异常映射 |
| `ApiResponse` / `ResultCode` | `src/main/java/com/antdigital/todo/common/api/` | 统一出参与错误码 |
| `OperatorContext` / `OperatorContextHolder` | `src/main/java/com/antdigital/todo/common/context/` | 登录态 ThreadLocal 上下文 |
| `TodoCreateProperties` / `TrustedGatewayProperties` / `WebMvcConfig` | `src/main/java/com/antdigital/todo/common/` | 开关/灰度/限流/白名单配置与拦截器注册 |

---

## 2. 问题计数

| P0 | P1 | P2 |
|----|----|-----|
| 2 | 3 | 7 |

> **blocker_count = 2**（P0-1、P0-2）。§8 中 P0 任务为 3 条，其中 1 条是上述两个 P0 的**回归用例锁定**，不额外计入阻塞缺陷数。

---

## 3. Step 2 — 功能（REQ）

### REQ-1: 新增待办主流程（F01）

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| Given 合法登录态与 `title`/`description`，When `POST /api/todo/item/create`，Then 落库 1 条并返回 `result=OK`、`data.id/title/status/gmtCreate` | ✅ | design §4.1 W01、§5.1.2 出参表 | `TodoItemController.java:39-46`；`TodoCreateIntegrationTest.java:49-81`（真实 INSERT 回查） | 主键回填、`status=0`、`gmtCreate` 格式均验证通过 |

### REQ-2: 入参校验与文本规范化（F02）

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 名称空白 → TODO_001「事项名称不能为空」；名称 >100 → TODO_001；描述 >500 → TODO_001；含控制字符 → TODO_005 | ✅ | design §5.1.3.1 R02~R06 | `TodoItemServiceImpl.java:66-86`、`TodoTextNormalizer.java:25-76`；`TodoItemServiceImplTest.java:105/120/133/151/164` | 均不落库（`verify(..., never())`） |

### REQ-3: 创建人与租户归属识别（F03）

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| `tenant_id`/`creator_id` 只取自 `OperatorContext`；`status` 固定 0；`creator_name` 缺失以 userId 兜底 | ✅ | design R07/R08、§6.4.2.1 | `TodoItemServiceImpl.java:88-108`；`TodoCreateIntegrationTest.java:73-78` | Body 无同名参数入口，无法覆盖归属 |

### REQ-4: 登录态与来源校验（R11/R12/R13）

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 登录 Header 缺失/超长 → TODO_002；`tenantId` 参数不一致 → TODO_003；来源 IP 不在白名单 → TODO_002 | ⚠️ | design §5.2.3 R11/R12/R13、§6.4.2.3 | `LoginInterceptor.java:42-67`；`LoginInterceptorTest.java:81/94/108` | R12/R13 达成；**R11 在 `trusted-gateway-ips` 为空时整体跳过**（默认配置即为空）→ 见 **P0-2** |

### REQ-5: 开关与灰度（R09）

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 总开关关闭 → TODO_007；灰度未命中 → TODO_006；命中 → 放行 | ✅ | design §7.2、§7.3、R09 | `LoginInterceptor.java:85-96`；`LoginInterceptorTest.java:123/137/152` | 语义正确；但拒绝路径上下文未清理 → 见 **P0-1** |

### REQ-6: 落库失败统一降级（R10）

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 持久化异常 / 影响行数≠1 → TODO_004，不暴露 SQL 细节 | ✅ | design R10、§5.2.3 C02 | `TodoItemServiceImpl.java:57-62, 110-119`；`TodoItemServiceImplTest.java:198-208` | `DataAccessException` 捕获后记 ERROR 含 `tenantId/creatorId` + 异常栈 |

### REQ-7: 链路追踪与可监控（`X-Trace-Id`、指标、访问日志）

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| Header 表：「`X-Trace-Id` 缺失时应用生成」 | ❌ | design §4.1 入参 Header 表 | `LoginInterceptor.java:55` 仅读取打印；无生成逻辑、无 MDC | 见 **P1-2** |
| 指标：QPS、P99 RT、错误码分布（TODO_001~007 分桶）、DB insert 耗时、连接池活跃数 | ❌ | design §6.5 指标表、§7.1 可监控 | `pom.xml` 未引入 `micrometer-registry-*`，全仓无 `MeterRegistry`/`@Timed`；仅 `spring-boot-starter-actuator` 默认 JVM/HTTP 指标 | 见 **P1-1** |
| 访问日志（traceId、userId、tenantId、耗时、结果码） | ❌ | design §6.5 日志行 | 无访问日志实现（无 `HandlerInterceptor.afterCompletion` 耗时打点、无 AOP） | 见 **P1-1** |

### REQ-8: 统一出参与错误码

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 失败返回 `{result,msg,data=null}`；TODO_001~007 与设计一致 | ✅ | design §5 全局约定、§5.1.1.2 | `ApiResponse.java:30-54`、`ResultCode.java:13-34`、`GlobalExceptionHandler.java:30-71`；`TodoItemControllerTest.java:85/99/110/126` | 文案「事项名称不能为空」等与设计原文一致 |

### REQ-9: 建表脚本

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 9 字段类型/默认值、`PRIMARY`、`idx_todo_item_tenant_creator(tenant_id,creator_id,gmt_create)` | ✅ | design §5.1.1.1 | `src/main/resources/db/todo_item.sql:2-14` | 与设计逐列一致；无外键、无触发器 |

---

## 4. Step 3 — 可读性检查

> 无 Java：**N/A**。（本次有 Java，全量执行）

| 结果 | 说明（违规写 Ax.x 与 `path:行`） |
|------|--------------------------------|
| ⚠️ | **A2.4**：`GlobalExceptionHandler.java:7-12` 中 `org.springframework.http.converter.HttpMessageNotReadableException` 位于 `org.springframework.validation.*`、`org.springframework.web.bind.*` 之后，非 ASCII 字典序 → **P2-1** |
| ⚠️ | **A3.4**：4 处行宽 >120：`TodoConstants.java:40`(122)、`TokenBucketRateLimiter.java:9`(125)、`TodoCreateIntegrationTest.java:25`(126)、`TodoTextNormalizerTest.java:34`(127) → **P2-2** |
| ✅ | A1 文件格式（命名/UTF-8/无 Tab）、A3 其余项（K&R、4 空格）、A4 命名、A5 编码实践（`@Override` 齐全、无空 catch）、A6 元素样式、A7 Javadoc（public 类与方法齐全，getter 省略符合 A7.3） |

---

## 5. Step 4 — 可靠性检查

| 域 | 参考 | 结果 | 等级 | 说明（列命中 ID 或「已扫无命中」） |
|----|------|------|------|-------------------------------------|
| 可靠性 | `reliability-checklist.md` G1–G17 | ❌ | P0 | `G8.5` ThreadLocal 拒绝路径未清理（`LoginInterceptor.java:69-78`）→ **P0-1**；`G16.1` 无指标/访问日志 → **P1-1**；`G16.2` 异常日志无 traceId → **P1-2**；`G17.1` 开关不热更新 → **P1-3**；`G8.1` 令牌桶无淘汰 → **P2-4**；`G11.2` 漏测拒绝路径 → **P2-5** |
| 安全 | `security-checklist.md` S1–S10 | ❌ | P0 | `S8.1` 来源网关校验 fail-open（`LoginInterceptor.java:43-48` + `application.yml:45`）→ **P0-2**；其余 S1/S2/S6/S9/S10 已扫无命中（无 SQL 拼接、无富文本、无多态反序列化、凭证走环境变量、无 CORS 通配） |
| Bug 模式 | `bug-pattern-checklist.md` B/M/I（120） | ✅ | — | 预扫 `scan-all-rules.sh`：`[P0] G16.2 — CatchWithoutLogging: TodoItemServiceImpl.java:113`，**人工复核为误报**（catch 内已 `LOGGER.error(..., ex)`）。逐条核销 B001–B081 / M001–M027 / I001–I010 无 Blocker/Major 命中 |

**注**：`G16.1` 在清单中定级 P0，本报告记为 **P1**——`actuator` 已提供默认 JVM/HTTP 指标，缺的是设计要求的业务维度埋点与访问日志，属可观测性缺口而非功能不可用；其余 P0/P1 与清单定级一致。

---

## 6. Step 5 — 自定义扩展检查

| 域 | 参考 | 结果 | 等级 | 说明 |
|----|------|------|------|------|
| 自定义扩展 | `customized-checklist.md` U* | ✅ | — | `U1.1` Controller 入参使用 `@Valid @RequestBody`（`TodoItemController.java:40`）；`U2.*` 为空节，`N/A(未启用自定义规则)` |

---

## 7. 结论

- **合并建议**：**阻止合并**（2 个 P0 必须修复后复审）
- **P0**：
  1. `LoginInterceptor` 在写入 `OperatorContextHolder` 之后、开关/灰度校验抛异常时，`afterCompletion` 不会执行，`ThreadLocal` 残留（已实测复现）。
  2. 受信网关 IP 白名单默认为空且代码对空列表**整体跳过校验**，任何可达端口的调用方都能伪造 `X-User-Id`/`X-Tenant-Id` 代表他人创建，与设计 R11、§6.4.2.1 直接冲突。
- **P1**：
  1. 设计要求的指标埋点与访问日志未实现。
  2. `X-Trace-Id` 仅透传，缺失时不生成、未入 MDC，异常日志无法串联排障。
  3. 总开关/灰度/限流阈值均为 `@ConfigurationProperties` 静态绑定，无配置中心刷新，与「热更新、应急无需发版」不符。
- **P2**：import 顺序、4 处行宽、`INSERT_TIMEOUT_MS` 死常量、令牌桶无淘汰、单测漏测项、连接字符集与 utf8mb4 一致性风险、默认配置直接全量放开。
- **一句话**：主流程、校验、归属与错误码实现扎实且 64 个用例全绿，但**身份信任边界（网关白名单 fail-open）与 ThreadLocal 生命周期**两处 P0 使其当前不具备上线条件。

---

## 7.1 问题片段（必填）

### P0-1 · `G8.5` · `src/main/java/com/antdigital/todo/common/interceptor/LoginInterceptor.java:69-78`

- **等级**：P0（阻塞）
- **规则**：`G8.5`（ThreadLocal 在线程池场景 `finally` 未 `remove()`），关联 `M027`（ThreadLocal 应为 static——本处已合规）
- **问题**：`preHandle` 先 `set` 上下文，随后 `checkSwitchAndGray(tenantId)` 可能抛 `TODO_007`/`TODO_006`。Spring MVC 仅在 `preHandle` **返回 true** 后才登记 `afterCompletion` 回调，因此异常路径下 `clear()` 永不执行，`OperatorContext` 残留在 Tomcat 工作线程上。
- **实测证据**（临时探针，已删除）：`todo.create.enabled=false` 时发起请求 → 响应 `result=TODO_007`，随后同线程读取 `OperatorContextHolder.get()` 输出 `PROBE_RESULT_LEAKED=true; userId=U9999`。
- 片段范围：`src/main/java/com/antdigital/todo/common/interceptor/LoginInterceptor.java:69-78`

```java
L69|        OperatorContextHolder.set(new OperatorContext(userId, userName, tenantId));
L70|        checkSwitchAndGray(tenantId);   // 问题：此处抛出 TODO_006/TODO_007 后 afterCompletion 不会回调
L71|        return true;
L72|    }
L73|
L74|    @Override
L75|    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
L76|                               Object handler, Exception ex) {
L77|        OperatorContextHolder.clear();  // 仅正常放行/后置异常路径可达
L78|    }
```

### P0-2 · `S8.1` / `R11` · `src/main/java/com/antdigital/todo/common/interceptor/LoginInterceptor.java:43-48`

- **等级**：P0（阻塞）
- **规则**：`S8.1`（接口已接入鉴权，资源操作校验归属权）；功能性不符 R11（P0）
- **Spec 证据**：design §5.2.3 R11「来源 IP 必须在受信网关白名单内，否则视为未授权访问 → TODO_002，记安全日志」；§6.4.2.1「用户无法以他人身份创建记录」；§2.2「应用只信任网关来源」。
- **代码证据**：条件 `!trustedIps.isEmpty() && ...` 使**空列表等价于完全关闭校验**；`src/main/resources/application.yml:45` 默认即为 `trusted-gateway-ips: []`，且无 profile 区分生产配置。
- **后果**：任何能访问应用端口的调用方自带 `X-User-Id`/`X-Tenant-Id` 即可为任意用户/租户创建待办，直接击穿 F03 归属识别与水平权限约束；同时 `G14.2`（租户值来自不可信来源）同源命中。
- 片段范围：`src/main/java/com/antdigital/todo/common/interceptor/LoginInterceptor.java:42-52`

```java
L42|        // R11：来源 IP 必须在受信网关白名单内（白名单为空时由网络层保证）
L43|        List<String> trustedIps = gatewayProperties.getTrustedGatewayIps();
L44|        if (!trustedIps.isEmpty() && !trustedIps.contains(request.getRemoteAddr())) {
L45|            LOGGER.warn("拒绝非受信网关来源请求, remoteAddr={}, uri={}",
L46|                    request.getRemoteAddr(), request.getRequestURI());
L47|            throw new BizException(ResultCode.TODO_002, "未登录或登录态失效");
L48|        }   // 问题：trustedIps 为空（默认配置）时跳过校验，X-User-Id/X-Tenant-Id 可被任意伪造
L49|
L50|        // R12：登录态 Header 必须非空且长度受限
L51|        String userId = trimToNull(request.getHeader(TodoConstants.HEADER_USER_ID));
L52|        String tenantId = trimToNull(request.getHeader(TodoConstants.HEADER_TENANT_ID));
```

### P1-1 · `G16.1` · `src/main/java/com/antdigital/todo/item/service/impl/TodoItemServiceImpl.java:57-63`（全链路缺失）

- **等级**：P1
- **规则**：`G16.1`（核心链路关键指标埋点：成功率/耗时/调用量）；REQ-7 ❌
- **说明**：`pom.xml` 仅引入 `spring-boot-starter-actuator`，未引入任何 Micrometer registry，也无 `@Timed`/`MeterRegistry`/访问日志实现。设计 §6.5 要求的「错误码分布 TODO_001~007 分桶、DB insert 耗时、连接池活跃数」与 §7.1「Controller 入口/Service 出口埋点」均无法观测，上线后告警规则（错误率 >1%、P99 >500ms）无数据源。
- 片段范围：`src/main/java/com/antdigital/todo/item/service/impl/TodoItemServiceImpl.java:56-63`

```java
L56|        TodoItemDO record = buildRecord(title, description, operator);
L57|        int affectedRows = insertRecord(record);   // 问题：无耗时/成功失败指标埋点
L58|        if (affectedRows != EXPECTED_AFFECTED_ROWS) {
L59|            LOGGER.error("待办事项落库影响行数异常, affectedRows={}, tenantId={}, creatorId={}",
L60|                    affectedRows, operator.getTenantId(), operator.getUserId());
L61|            throw new BizException(ResultCode.TODO_004);
L62|        }
L63|        return buildResponse(record);
```

### P1-2 · `G16.2` / REQ-7 · `src/main/java/com/antdigital/todo/common/interceptor/LoginInterceptor.java:54-56`

- **等级**：P1
- **规则**：`G16.2`（异常路径日志须含可追溯上下文 traceId/bizId）
- **说明**：设计 §4.1 要求「`X-Trace-Id` 缺失时应用生成」，§6.5 要求访问/异常日志带 traceId。实现仅在拒绝日志里回读 Header，缺失时为 `null`（单测日志可见 `traceId=null`），未生成、未放入 MDC，`TodoItemServiceImpl`/`GlobalExceptionHandler` 的异常日志因此无法与单次请求串联。`impl.md` §6 已自述该边界，仍未闭合设计。
- 片段范围：`src/main/java/com/antdigital/todo/common/interceptor/LoginInterceptor.java:53-57`

```java
L53|        if (!isValidOperatorField(userId) || !isValidOperatorField(tenantId)) {
L54|            LOGGER.warn("登录态缺失或非法, uri={}, traceId={}",
L55|                    request.getRequestURI(), request.getHeader(TodoConstants.HEADER_TRACE_ID)); // 问题：缺失时为 null，未生成
L56|            throw new BizException(ResultCode.TODO_002, "未登录或登录态失效");
L57|        }
```

### P1-3 · `G17.1` · `src/main/java/com/antdigital/todo/common/config/TodoCreateProperties.java:13-17`

- **等级**：P1
- **规则**：`G17.1`（功能开关支持紧急关闭，不需重启/发布）
- **Spec 证据**：design §7.3「主开关 `todo.create.enabled`（配置中心动态生效）」「配置项（库连接、灰度尾号、开关、限流阈值）均支持热更新，应急时无需发版」。
- **代码证据**：`@ConfigurationProperties` 无 `@RefreshScope`，无 `@EventListener(EnvironmentChangeEvent)`/Nacos/Apollo 监听；`WebMvcConfig` 通过构造器注入单例。改配置只能重启实例，`TODO_007` 止血路径实际上要发版，与设计声明不一致。
- 片段范围：`src/main/java/com/antdigital/todo/common/config/TodoCreateProperties.java:13-26`

```java
L13|@ConfigurationProperties(prefix = "todo.create")
L14|public class TodoCreateProperties {
L15|
L16|    /** 总开关，关闭后接口直接返回 TODO_007。 */
L17|    private boolean enabled = true;   // 问题：无 @RefreshScope/配置中心监听，改值需重启
```

### P2-1 · `A2.4` · `src/main/java/com/antdigital/todo/common/exception/GlobalExceptionHandler.java:7-12`

- **等级**：P2
- **说明**：import 未按完整引入名 ASCII 字典序排列（`http.converter` 应最前）。
- 片段范围：`src/main/java/com/antdigital/todo/common/exception/GlobalExceptionHandler.java:7-12`

```java
L7|import org.springframework.validation.BindException;
L8|import org.springframework.validation.FieldError;
L9|import org.springframework.web.bind.MethodArgumentNotValidException;
L10|import org.springframework.web.bind.annotation.ExceptionHandler;
L11|import org.springframework.web.bind.annotation.RestControllerAdvice;
L12|import org.springframework.http.converter.HttpMessageNotReadableException;  // 问题：应位于第 7 行之前
```

### P2-2 · `A3.4` · `src/main/java/com/antdigital/todo/item/constant/TodoConstants.java:40-41`

- **等级**：P2
- **说明**：行宽 122 > 120（另见 `TokenBucketRateLimiter.java:9`=125、`TodoCreateIntegrationTest.java:25`=126、`TodoTextNormalizerTest.java:34`=127）。
- 片段范围：`src/main/java/com/antdigital/todo/item/constant/TodoConstants.java:40-41`

```java
L40|    /** 单条 INSERT 超时时间（毫秒），与 application.yml 中 default-statement-timeout=3 秒保持一致。 */
L41|    public static final int INSERT_TIMEOUT_MS = 3000;
```

### P2-3 · `B004`-类比（未用常量）· `src/main/java/com/antdigital/todo/item/constant/TodoConstants.java:41`

- **等级**：P2
- **说明**：`INSERT_TIMEOUT_MS` 全仓无引用（`rg INSERT_TIMEOUT_MS` 仅命中定义行），设计 §5.1.1.2 列出的 `DEFAULT_STATUS` 也未落地（改用 `TodoStatus.TODO`）。死常量与文档不一致，易误导后续维护者以为超时由该常量控制。
- 片段范围：`src/main/java/com/antdigital/todo/item/constant/TodoConstants.java:40-44`

```java
L40|    /** 单条 INSERT 超时时间（毫秒），与 application.yml 中 default-statement-timeout=3 秒保持一致。 */
L41|    public static final int INSERT_TIMEOUT_MS = 3000;   // 问题：无任何引用
L42|
L43|    /** 展示与存储统一使用 UTC+8。 */
L44|    public static final ZoneId ZONE_UTC8 = ZoneId.of("Asia/Shanghai");
```

### P2-4 · `G8.1` · `src/main/java/com/antdigital/todo/common/ratelimit/TokenBucketRateLimiter.java:15-31`

- **等级**：P2
- **说明**：`buckets` 按 userId 无界增长且无淘汰/过期，长期运行下 Map 条目数随历史用户数单调增长（内部用户量有限，故非 P0/P1）；同时冷用户桶以满容量初始化，理论突发上界为 `qps × 1s`，与设计「实例级近似限流」一致。
- 片段范围：`src/main/java/com/antdigital/todo/common/ratelimit/TokenBucketRateLimiter.java:15-31`

```java
L15|    private final Map<String, Bucket> buckets = new ConcurrentHashMap<String, Bucket>();
L16|
L17|    /**
L18|     * 尝试获取一个令牌。
...
L25|    public boolean tryAcquire(String key, int permits, int qps) {
L26|        if (qps <= 0) {
L27|            return true;
L28|        }
L29|        // 单桶容量取 1 秒的令牌量，允许不超过 1 秒的突发
L30|        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(qps));  // 问题：无淘汰策略
L31|        return bucket.tryAcquire(permits, qps);
```

### P2-5 · `G11.2` · `src/test/java/com/antdigital/todo/common/interceptor/LoginInterceptorTest.java:121-133`

- **等级**：P2
- **说明**：开关/灰度两个用例只断言异常码，未断言「拒绝后 `OperatorContextHolder.get()` 应为 null」，因此 P0-1 未被现有 64 个用例发现；同理缺 `X-User-Id` 长度 >64、emoji 实际落库两类边界用例。
- 片段范围：`src/test/java/com/antdigital/todo/common/interceptor/LoginInterceptorTest.java:121-133`

```java
L121|    @Test
L122|    @DisplayName("总开关关闭：抛 TODO_007")
L123|    void should_throwTodo007_when_switchOff() {
L124|        // Arrange
L125|        createProperties.setEnabled(false);
L126|        MockHttpServletRequest request = buildRequest("U1001", "张三", "T01");
L127|
L128|        // Act & Assert
L129|        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
L130|                .isInstanceOf(BizException.class)
L131|                .extracting(ex -> ((BizException) ex).getResultCode())
L132|                .isEqualTo(ResultCode.TODO_007);   // 问题：未断言上下文已清理
L133|    }
```

### P2-6 · 字符集一致性 · `src/main/resources/application.yml:8`

- **等级**：P2
- **说明**：表为 `utf8mb4`（`db/todo_item.sql:14`），连接串使用 `characterEncoding=utf8`。Connector/J 8.x 通常将 Java UTF-8 映射为 utf8mb4，但该映射依赖驱动版本与 `connectionCollation`，未显式声明时存在 emoji（4 字节）写入失败或乱码风险；测试侧 H2 无法复现。建议改 `characterEncoding=UTF-8` 并补 emoji 落库用例。
- 片段范围：`src/main/resources/application.yml:8-11`

```yaml
L8|    url: jdbc:mysql://127.0.0.1:3306/ant_todo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
L9|    username: ${DB_USERNAME:todo}
L10|    password: ${DB_PASSWORD:todo}
L11|    driver-class-name: com.mysql.cj.jdbc.Driver
```

### P2-7 · 默认配置风险 · `src/main/resources/application.yml:31-40`

- **等级**：P2
- **说明**：`enabled: true` + `gray-enabled: false` 意味着按当前配置发布即为**全量开放**，而设计 §7.2 要求「上线初期置为 `0,1,2` 观察后逐步放开」。建议 `gray-enabled` 默认 `true`，由发布单显式放开。
- 片段范围：`src/main/resources/application.yml:31-40`

```yaml
L31|todo:
L32|  create:
L33|    # 总开关，关闭后新增接口直接返回 TODO_007
L34|    enabled: true
L35|    # 按租户尾号灰度，关闭表示全量
L36|    gray-enabled: false      # 问题：默认全量，与设计「先灰度 0,1,2」不一致
L37|    gray-tenant-suffix:
L38|      - "0"
L39|      - "1"
L40|      - "2"
```

---

## 8. 修复任务列表

> 供后续改代码时逐项执行与核销；与 §3–§7 中 ❌/⚠️ 及结论中的可执行项一一对应。

### P0

- [ ] **P0** `LoginInterceptor.java:69-71` — 在 `preHandle` 内用 `try { checkSwitchAndGray(...) } catch (BizException e) { OperatorContextHolder.clear(); throw e; }` 包裹，或改为校验通过后再 `set` 上下文，确保任何抛出路径都清理 ThreadLocal。
- [ ] **P0** `LoginInterceptor.java:43-48` — 将网关来源校验改为 fail-closed：白名单为空时拒绝并记安全日志（或启动期校验非空），同时把 `application.yml:45` 的白名单改为环境实际网关 IP/CIDR 配置项，满足 R11。
- [ ] **P0**（P0-1/P0-2 回归锁定）`LoginInterceptorTest.java:121-133` — 为开关关闭/灰度未命中两个用例补断言 `assertThat(OperatorContextHolder.get()).isNull()`，并为「白名单为空 → TODO_002」补用例，锁定上述两个修复。

### P1

- [ ] **P1** `TodoItemServiceImpl.java:57-63` — 接入 Micrometer：`MeterRegistry` 记录创建调用量/成功率/耗时与错误码分桶，按设计 §6.5 暴露 `TODO_001~007` 维度。
- [ ] **P1** `LoginInterceptor.java:54-56` — 缺失 `X-Trace-Id` 时生成 UUID 并写入 MDC，`afterCompletion` 中清理；在 `GlobalExceptionHandler` 与 Service 异常日志中输出 traceId。
- [ ] **P1** `LoginInterceptor.java:41-72` — 增加访问日志（traceId、userId、tenantId、耗时、result）覆盖成功与失败分支（设计 §6.5/§7.1）。
- [ ] **P1** `TodoCreateProperties.java:13-17` — 为 `todo.create.*` 增加配置中心刷新能力（`@RefreshScope` 或 `EnvironmentChangeEvent` 监听 + 可变持有器），使 `enabled`/`gray-*`/`rate-limit-qps` 改值无需重启。

### P2（可选）

- [ ] **P2** `GlobalExceptionHandler.java:7-12` — 按 ASCII 字典序重排 import（`org.springframework.http.*` 提前）。
- [ ] **P2** `TodoConstants.java:40` — 折行至 ≤120 字符；同步处理 `TokenBucketRateLimiter.java:9`、`TodoCreateIntegrationTest.java:25`、`TodoTextNormalizerTest.java:34`。
- [ ] **P2** `TodoConstants.java:41` — 删除未使用的 `INSERT_TIMEOUT_MS`，或改为在数据源/语句超时处实际引用。
- [ ] **P2** `TokenBucketRateLimiter.java:15` — 为 `buckets` 增加空闲淘汰（如定时清理或 Caffeine `expireAfterAccess`），避免 Map 无界增长。
- [ ] **P2** `LoginInterceptorTest.java:121` — 补 `X-User-Id` 长度 >64 与 emoji 名称落库用例，提高边界覆盖率（G11.2）。
- [ ] **P2** `application.yml:8` — 连接串改 `characterEncoding=UTF-8` 并显式 `connectionCollation=utf8mb4_general_ci`，与 `utf8mb4` 表对齐。
- [ ] **P2** `application.yml:36` — 将 `gray-enabled` 默认值改为 `true`，发布时按设计 §7.2 以 `0,1,2` 起步灰度。
