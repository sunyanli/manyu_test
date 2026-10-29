# Code Review Checklist

> **Change** `todo-create`（新增待办事项） · **分支** `task/DEV-966dcd0a-7905-11f1-9649-3b4281182f10-75a6d105-1e54-442f-a3d4-a6fda5a9c6ad` / **Commit** `61a3d8a` · **日期** `2026-10-29`
>
> **AI**：唯一进度源；状态仅用 `⬜` `✅` `❌` `⚠️` `N/A`。
> **完成标准**：所有核销项必须从 `⬜` 变为其他状态；`N/A` 需写原因。
>
> **执行顺序（强制）**：已先执行 `references/script/scan-all-rules.sh src/main/java/com/antdigital/todo src/test/java/com/antdigital/todo`（52/222 条规则），输出见附录 A；随后 LLM 逐文件补全脚本未覆盖项。

---

## Step 1 — 执行队列（产物 A）

> 由 `git diff --name-only HEAD~1` 展开；共 30 个 `.java`（23 主代码 + 7 测试）。非 Java 文件（`pom.xml`、`application.yml`、`db/todo_item.sql`、`schema-h2.sql`）**跳过**，仅在 Step 2 功能核对中作为证据引用。

| # | 文件（仓库相对路径） | 归属原因 | Step2 | Step3 | G1 | G2 | G3 | G4 | G5 | G6 | G7 | G8 | G9 | G10 | G11 | G12 | G13 | G14 | G15 | G16 | G17 | S1 | S2 | S3 | S4 | S5 | S6 | S7 | S8 | S9 | S10 | 总状态 |
|---|----------------------|----------|-------|-------|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|----|--------|
| 1 | `src/main/java/com/antdigital/todo/TodoApplication.java` | 启动类 / @MapperScan（REQ-1） | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ | ✅ | N/A | ✅ | N/A | N/A | ✅ | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ 已审 |
| 2 | `src/main/java/com/antdigital/todo/common/api/ApiResponse.java` | 统一出参 {result,msg,data}（REQ-8） | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ | ✅ | N/A | ✅ | N/A | N/A | ✅ | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ 已审 |
| 3 | `src/main/java/com/antdigital/todo/common/api/ResultCode.java` | 错误码 TODO_001~007（REQ-8） | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ | ✅ | N/A | ✅ | N/A | N/A | ✅ | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ 已审 |
| 4 | `src/main/java/com/antdigital/todo/common/config/TodoCreateProperties.java` | 开关/灰度/限流配置（REQ-5） | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ | ✅ | N/A | ✅ | ⚠️ | N/A | ✅ | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ⚠️ 已审有问题 |
| 5 | `src/main/java/com/antdigital/todo/common/config/WebMvcConfig.java` | 拦截器注册（REQ-4/REQ-5） | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ | ✅ | ✅ | ✅ | N/A | N/A | ✅ | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ 已审 |
| 6 | `src/main/java/com/antdigital/todo/common/context/OperatorContext.java` | 登录态上下文载体（REQ-3） | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ | ✅ | N/A | ✅ | N/A | N/A | ✅ | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ 已审 |
| 7 | `src/main/java/com/antdigital/todo/common/context/OperatorContextHolder.java` | ThreadLocal 持有器（REQ-3） | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️ | N/A | ✅ | ✅ | N/A | ✅ | ✅ | N/A | ✅ | N/A | N/A | ✅ | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ⚠️ 已审有问题 |
| 8 | `src/main/java/com/antdigital/todo/common/exception/BizException.java` | 业务异常（REQ-8） | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ | ✅ | N/A | ✅ | N/A | N/A | ✅ | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ 已审 |
| 9 | `src/main/java/com/antdigital/todo/common/exception/GlobalExceptionHandler.java` | 统一异常映射（REQ-8） | ✅ | ❌ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ | ✅ | N/A | ✅ | N/A | N/A | ✅ | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ⚠️ 已审有问题 |
| 10 | `src/main/java/com/antdigital/todo/common/interceptor/LoginInterceptor.java` | R01/R09/R11/R12/R13 登录态与开关（REQ-4/REQ-5） | ⚠️ | ✅ | N/A | ✅ | N/A | N/A | N/A | N/A | N/A | ❌ | N/A | ✅ | ✅ | N/A | ✅ | ⚠️ | N/A | ⚠️ | N/A | N/A | ✅ | N/A | N/A | N/A | ✅ | N/A | ❌ | ✅ | N/A | ⚠️ 已审有问题 |
| 11 | `src/main/java/com/antdigital/todo/common/interceptor/RateLimitInterceptor.java` | 用户级限流（设计 5.2.3 C03） | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ | ✅ | N/A | ⚠️ | N/A | N/A | ✅ | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ⚠️ 已审有问题 |
| 12 | `src/main/java/com/antdigital/todo/common/ratelimit/TokenBucketRateLimiter.java` | 进程内令牌桶（设计 5.2.3 C03） | ✅ | ⚠️ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ | ✅ | N/A | ✅ | N/A | N/A | ✅ | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ⚠️ 已审有问题 |
| 13 | `src/main/java/com/antdigital/todo/common/security/TrustedGatewayProperties.java` | 网关白名单配置（R11） | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ | ✅ | N/A | ✅ | N/A | N/A | ✅ | N/A | N/A | N/A | ✅ | N/A | ⚠️ | ✅ | N/A | ⚠️ 已审有问题 |
| 14 | `src/main/java/com/antdigital/todo/item/constant/TodoConstants.java` | 常量（R02/R03、Header、时区） | ✅ | ⚠️ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ | ✅ | N/A | ✅ | N/A | N/A | ✅ | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ⚠️ 已审有问题 |
| 15 | `src/main/java/com/antdigital/todo/item/controller/TodoItemController.java` | W01 接口入口（REQ-1） | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ | ✅ | N/A | ✅ | N/A | N/A | ✅ | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ 已审 |
| 16 | `src/main/java/com/antdigital/todo/item/enums/TodoStatus.java` | 状态枚举（R08） | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ | ✅ | N/A | ✅ | N/A | N/A | ✅ | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ 已审 |
| 17 | `src/main/java/com/antdigital/todo/item/mapper/TodoItemMapper.java` | 单表写入（REQ-1） | ✅ | ✅ | N/A | N/A | N/A | ✅ | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ | ✅ | N/A | ✅ | N/A | ✅ | ✅ | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ 已审 |
| 18 | `src/main/java/com/antdigital/todo/item/model/CreateTodoRequest.java` | 入参模型（R02/R03） | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ | ✅ | N/A | ✅ | N/A | N/A | ✅ | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ 已审 |
| 19 | `src/main/java/com/antdigital/todo/item/model/CreateTodoResponse.java` | 出参模型（REQ-1） | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ | ✅ | N/A | ✅ | N/A | N/A | ✅ | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ 已审 |
| 20 | `src/main/java/com/antdigital/todo/item/model/TodoItemDO.java` | 持久化实体（表结构 5.1.1.1） | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ | ✅ | N/A | ✅ | N/A | N/A | ✅ | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ 已审 |
| 21 | `src/main/java/com/antdigital/todo/item/service/TodoItemService.java` | 服务接口 S01（REQ-1） | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ | ✅ | N/A | ✅ | N/A | N/A | ✅ | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ 已审 |
| 22 | `src/main/java/com/antdigital/todo/item/service/impl/TodoItemServiceImpl.java` | R02~R08/R10 业务实现（REQ-1/2/3/6） | ✅ | ✅ | N/A | ✅ | ✅ | ✅ | N/A | N/A | N/A | ✅ | ✅ | ✅ | ✅ | N/A | ✅ | ✅ | N/A | ⚠️ | N/A | ✅ | ✅ | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ⚠️ 已审有问题 |
| 23 | `src/main/java/com/antdigital/todo/item/util/TodoTextNormalizer.java` | R05/R06 规范化与非法字符（REQ-2） | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ | ✅ | N/A | ✅ | N/A | N/A | ✅ | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ 已审 |
| 24 | `src/test/java/com/antdigital/todo/common/interceptor/LoginInterceptorTest.java` | 单测：R11/R12/R13/R09 | N/A(测试) | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ | ✅ | N/A | ✅ | N/A | N/A | ✅ | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ 已审 |
| 25 | `src/test/java/com/antdigital/todo/common/interceptor/RateLimitInterceptorTest.java` | 单测：限流 | N/A(测试) | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ | ✅ | N/A | ✅ | N/A | N/A | ✅ | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ 已审 |
| 26 | `src/test/java/com/antdigital/todo/common/ratelimit/TokenBucketRateLimiterTest.java` | 单测：令牌桶 | N/A(测试) | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ | ✅ | N/A | ✅ | N/A | N/A | ✅ | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ 已审 |
| 27 | `src/test/java/com/antdigital/todo/item/controller/TodoCreateIntegrationTest.java` | 集成测试：真实 INSERT（REQ-1/REQ-3） | ✅ | ⚠️ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ | ✅ | N/A | ✅ | N/A | N/A | ✅ | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ⚠️ 已审有问题 |
| 28 | `src/test/java/com/antdigital/todo/item/controller/TodoItemControllerTest.java` | 切片测试：出参与异常映射（REQ-8） | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ | ✅ | N/A | ✅ | N/A | N/A | ✅ | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ 已审 |
| 29 | `src/test/java/com/antdigital/todo/item/service/impl/TodoItemServiceImplTest.java` | 单测：R02~R08/R10 | ✅ | ✅ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | ✅ | ⚠️ | N/A | ✅ | ✅ | N/A | ✅ | N/A | N/A | ✅ | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ⚠️ 已审有问题 |
| 30 | `src/test/java/com/antdigital/todo/item/util/TodoTextNormalizerTest.java` | 单测：R05/R06 | ✅ | ⚠️ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ✅ | ✅ | N/A | ✅ | N/A | N/A | ✅ | N/A | N/A | N/A | ✅ | N/A | ✅ | ✅ | N/A | ⚠️ 已审有问题 |

- `N/A` 语义：该 G/S 类别在本文件范围内无对应代码（如无 MQ、无缓存、无调度、无 SQL 拼接）。
- 收口核对：30 行全部为非 `⬜` 状态。

---

## Step 2 — 功能（产物 B）

| REQ | Scenario | Spec证据（原文/章节） | 关联文件 | 状态 | 代码证据（文件/测试/接口） |
|-----|----------|----------------------|----------|------|----------------------------|
| REQ-1 | Given 合法登录态与 `title`/`description`，When `POST /api/todo/item/create`，Then 落库 1 条并返回 `result=OK`、`data.id/title/status/gmtCreate` | design §4.1 W01、§5.1.2、F01 | `TodoItemController.java`、`TodoItemServiceImpl.java`、`TodoItemMapper.java` | ✅ | `TodoCreateIntegrationTest.should_persistTodoItem_when_createSucceeds:49`（真实 INSERT 回查，`mvn -o test` 通过） |
| REQ-2 | Given 名称空白/超长、描述超长、含控制字符，When 提交，Then 返回 TODO_001 具体文案 / TODO_005，且不落库 | design §5.1.3.1 R02~R06 | `CreateTodoRequest.java`、`TodoTextNormalizer.java`、`TodoItemServiceImpl.java` | ✅ | `TodoItemServiceImplTest:105/120/133/151/164`、`TodoTextNormalizerTest:19-63` |
| REQ-3 | Given 网关透传 Header，When 创建，Then `tenant_id/creator_id` 只取登录态、`status` 固定 0、`creator_name` 缺失以 userId 兜底 | design §5.1.3.1 R07/R08 | `LoginInterceptor.java`、`TodoItemServiceImpl.java`、`TodoItemDO.java` | ✅ | `TodoItemServiceImplTest:73-80`、`TodoCreateIntegrationTest:73-78` |
| REQ-4 | Given 无登录态/非受信来源/`tenantId` 不一致，When 请求 `POST /api/todo/item/create`，Then TODO_002 / TODO_003，不进入业务层 | design §5.2.3 R11/R12/R13、§6.4.2.3 | `LoginInterceptor.java`、`WebMvcConfig.java`、`TrustedGatewayProperties.java` | ⚠️ | R12/R13 有实现与单测（`LoginInterceptorTest:81/94/108`）；**R11 在 `trusted-gateway-ips` 为空时整体跳过**（`LoginInterceptor.java:43-48` + `application.yml:45`）→ P0-2 |
| REQ-5 | Given 总开关关闭 / 灰度未命中，When 请求，Then TODO_007 / TODO_006 | design §7.2、§7.3、R09 | `LoginInterceptor.java`、`TodoCreateProperties.java` | ✅ | `LoginInterceptorTest:123/137`；但拒绝路径 ThreadLocal 未清理 → 见 P0-1 |
| REQ-6 | Given 持久化异常 / 影响行数≠1，When 写库，Then 统一 TODO_004 且不暴露 SQL | design R10、§5.2.3 C02 | `TodoItemServiceImpl.java`、`GlobalExceptionHandler.java` | ✅ | `TodoItemServiceImplTest:198`、`insertRecord:110-119` 记录 `LOGGER.error` 含 tenantId/creatorId |
| REQ-7 | Given 请求无 `X-Trace-Id`，When 处理，Then 应用生成 traceId 并串入访问日志/指标（含 QPS、P99、错误码分桶） | design §4.1 入参 Header 表、§6.5、§7.1 | `LoginInterceptor.java`、`TodoItemServiceImpl.java`、`pom.xml` | ❌ | 代码仅读取 `X-Trace-Id` 打印（`LoginInterceptor.java:55`），无生成、无 MDC、无访问日志、无 Micrometer 指标 → P1-1/P1-2 |
| REQ-8 | Given 各类失败，When 返回，Then `{result,msg,data}` 结构、错误码 TODO_001~007 与 `data=null` | design §5 全局约定、§5.1.1.2 | `ApiResponse.java`、`ResultCode.java`、`GlobalExceptionHandler.java` | ✅ | `TodoItemControllerTest:85/99/110/126` |
| REQ-9 | Given 生产建表，When 执行 `db/todo_item.sql`，Then 字段/默认值/索引与 design §5.1.1.1 一致 | design §5.1.1.1 | `src/main/resources/db/todo_item.sql` | ✅ | 9 字段 + `PRIMARY` + `idx_todo_item_tenant_creator(tenant_id,creator_id,gmt_create)` 与设计逐列一致 |

---

## Step 3 — 可读性检查（产物 C）

| ID | 检查项 | 状态 | 备注（命中写 `path:line`） |
|----|--------|------|----------------------------|
| A1 | 源文件格式 | ✅ | 30 个文件均为 `类名.java`、UTF-8、无 Tab、无 CRLF |
| A2 | 源文件结构/import 顺序 | ❌ | A2.4：`GlobalExceptionHandler.java:7-12` 中 `org.springframework.http.converter.HttpMessageNotReadableException` 排在 `org.springframework.web.bind.*` 之后，非字典序 → P2-1 |
| A3 | 代码样式 | ⚠️ | K&R 大括号、4 空格缩进合规；A3.4 行宽超 120：`TodoConstants.java:40`(122)、`TokenBucketRateLimiter.java:9`(125)、`TodoCreateIntegrationTest.java:25`(126)、`TodoTextNormalizerTest.java:34`(127) → P2-2 |
| A4 | 命名规范 | ✅ | 包全小写；`TodoItemDO`/`CreateTodoRequest` 驼峰；`TITLE_MAX_LEN` 常量大写；测试类 `XxxTest` 与被测类同名 |
| A5 | 编码实践 | ✅ | 所有接口实现方法均带 `@Override`（`TodoItemServiceImpl:40`、`LoginInterceptor:40/74`、`WebMvcConfig:31`）；无空 catch |
| A6 | 特定元素样式 | ✅ | 无 switch、无数组声明、无 `l` 后缀 long 字面量；`1_000_000_000L` 用法正确 |
| A7 | Javadoc 规范 | ✅ | public 类/方法均有 Javadoc（`ApiResponse`、`TodoTextNormalizer`、`ResultCode` 等）；getter/setter 省略符合 A7.3 |

---

## Step 4 — 可靠性检查（产物 D）

### 4.1 Bug 模式（`bug-pattern-checklist.md`）

> 预扫：`scan-all-rules.sh` 报 `[P0] G16.2 — CatchWithoutLogging: TodoItemServiceImpl.java:113`；**人工复核为误报**——该 `catch` 块内 `LOGGER.error(..., ex)` 已记录日志（见附录 A 复核结论）。

| ID | 状态 | 备注（命中写 `path:line`；预扫可粘贴脚本摘要） |
|----|------|--------------------------------------------------|
| B001 | N/A | 变更内无该缺陷模式对应代码 |
| B002 | N/A | 变更内无该缺陷模式对应代码 |
| B003 | N/A | 变更内无该缺陷模式对应代码 |
| B004 | N/A | 变更内无该缺陷模式对应代码 |
| B005 | N/A | 变更内无该缺陷模式对应代码 |
| B006 | ✅ | 测试统一使用 AssertJ `assertThat(...).isEqualTo(expected)`，无 JUnit `assertEquals` 参数顺序风险 |
| B007 | N/A | 变更内无该缺陷模式对应代码 |
| B008 | N/A | 变更内无该缺陷模式对应代码 |
| B009 | N/A | 变更内无该缺陷模式对应代码 |
| B010 | N/A | 变更内无该缺陷模式对应代码 |
| B011 | N/A | 变更内无该缺陷模式对应代码 |
| B012 | N/A | 变更内无该缺陷模式对应代码 |
| B013 | N/A | 变更内无该缺陷模式对应代码 |
| B014 | N/A | 变更内无该缺陷模式对应代码 |
| B015 | N/A | 变更内无该缺陷模式对应代码 |
| B016 | N/A | 变更内无该缺陷模式对应代码 |
| B017 | N/A | 变更内无该缺陷模式对应代码 |
| B018 | N/A | 变更内无该缺陷模式对应代码 |
| B019 | N/A | 变更内无该缺陷模式对应代码 |
| B020 | N/A | 变更内无该缺陷模式对应代码 |
| B021 | N/A | 变更内无该缺陷模式对应代码 |
| B022 | ✅ | 无 `SimpleDateFormat`，统一 `DateTimeFormatter`（`TodoConstants:47`） |
| B023 | N/A | 变更内无该缺陷模式对应代码 |
| B024 | N/A | 变更内无该缺陷模式对应代码 |
| B025 | N/A | 变更内无该缺陷模式对应代码 |
| B026 | ✅ | 无 `x.equals(null)` 写法 |
| B027 | N/A | 变更内无该缺陷模式对应代码 |
| B028 | N/A | 变更内无该缺陷模式对应代码 |
| B029 | N/A | 变更内无该缺陷模式对应代码 |
| B030 | ✅ | 无浮点 `==` 比较；令牌桶仅 `tokens < permits` 比较 |
| B031 | N/A | 变更内无该缺陷模式对应代码 |
| B032 | N/A | 变更内无该缺陷模式对应代码 |
| B033 | N/A | 变更内无该缺陷模式对应代码 |
| B034 | N/A | 变更内无该缺陷模式对应代码 |
| B035 | N/A | 变更内无该缺陷模式对应代码 |
| B036 | N/A | 变更内无该缺陷模式对应代码 |
| B037 | N/A | 变更内无该缺陷模式对应代码 |
| B038 | N/A | 变更内无该缺陷模式对应代码 |
| B039 | N/A | 变更内无该缺陷模式对应代码 |
| B040 | N/A | 变更内无该缺陷模式对应代码 |
| B041 | N/A | 变更内无该缺陷模式对应代码 |
| B042 | N/A | 变更内无该缺陷模式对应代码 |
| B043 | N/A | 变更内无该缺陷模式对应代码 |
| B044 | N/A | 变更内无该缺陷模式对应代码 |
| B045 | N/A | 变更内无该缺陷模式对应代码 |
| B046 | ✅ | 无 while 循环（`TodoTextNormalizer` 为 for 循环且步进正确） |
| B047 | N/A | 变更内无该缺陷模式对应代码 |
| B048 | N/A | 变更内无该缺陷模式对应代码 |
| B049 | ✅ | 格式串 `yyyy-MM-dd HH:mm:ss` 使用小写 `dd`（`TodoConstants:48`） |
| B050 | ✅ | 24 小时制 `HH`，未搭配 `a`（`TodoConstants:48`） |
| B051 | N/A | 变更内无该缺陷模式对应代码 |
| B052 | ✅ | 格式串使用 `yyyy` 而非 `YYYY`（`TodoConstants:48`） |
| B053 | N/A | 变更内无该缺陷模式对应代码 |
| B054 | N/A | 变更内无该缺陷模式对应代码 |
| B055 | ✅ | Mockito 用法正确：`when(...).thenReturn/thenThrow/thenAnswer`、`verify(mock).method(...)`（`TodoItemServiceImplTest:55-99`、`TodoItemControllerTest:68-94`） |
| B056 | N/A | 变更内无该缺陷模式对应代码 |
| B057 | N/A | 变更内无该缺陷模式对应代码 |
| B058 | N/A | 变更内无该缺陷模式对应代码 |
| B059 | N/A | 变更内无该缺陷模式对应代码 |
| B060 | N/A | 变更内无该缺陷模式对应代码 |
| B061 | N/A | 变更内无该缺陷模式对应代码 |
| B062 | N/A | 变更内无该缺陷模式对应代码 |
| B063 | N/A | 变更内无该缺陷模式对应代码 |
| B064 | N/A | 变更内无该缺陷模式对应代码 |
| B065 | N/A | 变更内无该缺陷模式对应代码 |
| B066 | N/A | 变更内无该缺陷模式对应代码 |
| B067 | N/A | 变更内无该缺陷模式对应代码 |
| B068 | N/A | 变更内无该缺陷模式对应代码 |
| B069 | N/A | 变更内无该缺陷模式对应代码 |
| B070 | N/A | 变更内无该缺陷模式对应代码 |
| B071 | N/A | 变更内无该缺陷模式对应代码 |
| B072 | N/A | 变更内无该缺陷模式对应代码 |
| B073 | N/A | 变更内无该缺陷模式对应代码 |
| B074 | N/A | 变更内无该缺陷模式对应代码 |
| B075 | ✅ | for 循环方向与边界一致（`TodoTextNormalizer:31`、`LoginInterceptorTest:40`） |
| B076 | N/A | 变更内无该缺陷模式对应代码 |
| B077 | N/A | 变更内无该缺陷模式对应代码 |
| B078 | N/A | 变更内无该缺陷模式对应代码 |
| B079 | N/A | 变更内无该缺陷模式对应代码 |
| B080 | ✅ | 所有测试方法均含断言或 verify（抽查 7 个测试类） |
| B081 | N/A | 变更内无该缺陷模式对应代码 |
| M001 | N/A | 变更内无该缺陷模式对应代码 |
| M002 | N/A | 变更内无该缺陷模式对应代码 |
| M003 | N/A | 变更内无该缺陷模式对应代码 |
| M004 | ✅ | 无 `printStackTrace()`；异常统一 `LOGGER.error(..., ex)` |
| M005 | N/A | 变更内无该缺陷模式对应代码 |
| M006 | N/A | 变更内无该缺陷模式对应代码 |
| M007 | ✅ | 无空 catch；`TodoItemServiceImpl:113-118` 有日志 + 转换 |
| M008 | N/A | 变更内无该缺陷模式对应代码 |
| M009 | N/A | 变更内无该缺陷模式对应代码 |
| M010 | N/A | 变更内无该缺陷模式对应代码 |
| M011 | N/A | 变更内无该缺陷模式对应代码 |
| M012 | N/A | 变更内无该缺陷模式对应代码 |
| M013 | N/A | 变更内无该缺陷模式对应代码 |
| M014 | N/A | 变更内无该缺陷模式对应代码 |
| M015 | N/A | 变更内无该缺陷模式对应代码 |
| M016 | ✅ | 无默认时区依赖：`ZonedDateTime.now(TodoConstants.ZONE_UTC8)`（`TodoItemServiceImpl:89`） |
| M017 | N/A | 变更内无该缺陷模式对应代码 |
| M018 | N/A | 变更内无该缺陷模式对应代码 |
| M019 | N/A | 变更内无该缺陷模式对应代码 |
| M020 | ✅ | 所有重写方法均有 `@Override` |
| M021 | N/A | 变更内无该缺陷模式对应代码 |
| M022 | N/A | 变更内无该缺陷模式对应代码 |
| M023 | N/A | 变更内无该缺陷模式对应代码 |
| M024 | N/A | 变更内无该缺陷模式对应代码 |
| M025 | N/A | 变更内无该缺陷模式对应代码 |
| M026 | N/A | 变更内无该缺陷模式对应代码 |
| M027 | ✅ | `OperatorContextHolder:10` 为 `private static final ThreadLocal` |
| I001 | ✅ | 异常断言覆盖错误码，不仅判断类型（`TodoItemServiceImplTest:110-114`） |
| I002 | N/A | 变更内无该缺陷模式对应代码 / 仅统计类规则 |
| I003 | N/A | 变更内无该缺陷模式对应代码 / 仅统计类规则 |
| I004 | ✅ | 未使用 `java.util.Date` |
| I005 | N/A | 变更内无该缺陷模式对应代码 / 仅统计类规则 |
| I006 | N/A | 变更内无该缺陷模式对应代码 / 仅统计类规则 |
| I007 | N/A | 变更内无该缺陷模式对应代码 / 仅统计类规则 |
| I008 | N/A | 变更内无该缺陷模式对应代码 / 仅统计类规则 |
| I009 | N/A | 变更内无该缺陷模式对应代码 / 仅统计类规则 |
| I010 | N/A | 变更内无该缺陷模式对应代码 / 仅统计类规则 |

### 4.2 可靠性（`reliability-checklist.md`）

> 核销范围以 `reliability-checklist.md` 实际定义的 **52 个 G ID**（G1.1–G18.3）为准；`cr-checklist-template.md` 中多出的 `G8.7`、`G10.3` 两份清单均无规则正文，不予核销（`N/A(参考清单未定义)`）。S 侧 30 个 ID 全部核销。

| ID | 状态 | 备注 |
|----|------|------|
| G1.1 | N/A | 无事务内先读后写；仅单条 insert |
| G1.2 | N/A | 无加锁更新 |
| G1.3 | N/A | 无乐观锁 |
| G1.4 | N/A | 无多资源加锁 |
| G2.1 | ✅ | 设计 A06 明确本期不做服务端幂等，重复提交产生多条为可接受结果（有意为之，非遗漏） |
| G2.2 | N/A | 无重试/MQ 重投 |
| G2.3 | N/A | 无幂等键约定 |
| G3.1 | N/A | 无跨库分布式事务 |
| G3.2 | ✅ | 无 `@Transactional`；单表单条 insert 自动提交，`TodoItemServiceImpl:110-119` |
| G4.1 | N/A | 无手写 SQL/存储过程 |
| G4.2 | N/A | 无 WHERE 条件（仅 BaseMapper.insert） |
| G4.3 | N/A | 本期无列表查询 |
| G5.1 | N/A | 无 MQ |
| G6.1 | N/A | 无缓存 |
| G6.2 | N/A | 无缓存 |
| G7.1 | N/A | 无调度任务 |
| G7.2 | N/A | 无调度任务 |
| G8.1 | ✅ | 无吞异常；`insertRecord` 捕获后记 ERROR 并转 TODO_004。另 P2 备注：`TokenBucketRateLimiter:15` buckets 无淘汰策略，长期运行条目数随用户数增长 → P2-3 |
| G8.2 | ✅ | DB 为唯一强依赖，`connection-timeout: 1000`、`default-statement-timeout: 3`（`application.yml:14,19`） |
| G8.3 | N/A | 无显式 I/O 流/连接/锁资源 |
| G8.4 | N/A | 未自建线程池 |
| G8.5 | ❌ | `LoginInterceptor.java:69` 先 set，`checkSwitchAndGray:70` 可抛 TODO_007/TODO_006；`afterCompletion` 在 preHandle 抛异常时不会被调用 → 上下文残留（实测 `PROBE_RESULT_LEAKED=true; userId=U9999`）→ P0-1 |
| G8.6 | N/A | 未使用 `Executors` |
| G9.1 | ✅ | DB 异常统一 TODO_004，不区分细节也不泄露 SQL（设计要求） |
| G9.2 | ✅ | 连接超时 1s、语句超时 3s（`application.yml:14,19`；`TodoConstants:41` 常量与配置对齐） |
| G9.3 | N/A | 无重试逻辑 |
| G10.1 | ✅ | `data=null` 仅表示失败；成功分支 data 必非空 |
| G10.2 | N/A | 新增接口，无契约变更 |
| G11.1 | ✅ | 新增 64 个用例中 43 个为本次新增，均有断言；`mvn -o test` 全绿 |
| G11.2 | ⚠️ | 边界覆盖较全（空/100/101/501/控制字符/影响行数≠1），但**未覆盖开关关闭后上下文应清理**，导致 P0-1 未被发现 → P2-5 |
| G11.3 | ✅ | `createTodo` 对 `request == null || operator == null` 有防御（`TodoItemServiceImpl:42-44`）；Controller 对上下文 null 兜底 TODO_002 |
| G11.4 | N/A | 无金额/数值运算 |
| G12.1 | N/A | 非资金场景 |
| G12.2 | ✅ | `todo.create.enabled` 总开关可止血（设计 §7.3） |
| G13.1 | ✅ | 业务异常 WARN、系统异常 ERROR（`GlobalExceptionHandler:57,69`） |
| G14.1 | N/A | 无金额 |
| G14.2 | ⚠️ | `tenant_id` 取自 Header；Header 来源可信性依赖 R11 白名单，白名单为空时放行 → 与 P0-2 同源（`LoginInterceptor.java:43-52`） |
| G14.3 | ✅ | 统一 `Asia/Shanghai` 写库与展示（`TodoConstants:44`、`TodoItemServiceImpl:89`、`application.yml:8` serverTimezone 一致），符合设计 §5 全局约定 |
| G14.4 | ✅ | `DateTimeFormatter` 用于格式化 `LocalDateTime`，无时区隐含依赖 |
| G15.1 | ✅ | 新增独立表，无存量表变更 |
| G15.2 | N/A | 无旧接口 |
| G15.3 | ✅ | 新能力由 `todo.create.enabled` + 灰度尾号控制（`LoginInterceptor:85-96`） |
| G16.1 | ⚠️ | 核心链路无 Micrometer 指标、无访问日志（设计 §6.5/§7.1 要求 QPS/P99/错误码分桶/insert 耗时）→ P1-1（清单定级 P0，本报告按 P1 记录，理由见报告 §5 备注） |
| G16.2 | ⚠️ | 异常日志含 tenantId/creatorId 与异常栈，但不含 traceId（`X-Trace-Id` 仅透传、未生成/未 MDC）→ P1-2 |
| G16.3 | ✅ | 级别正确：业务 WARN、系统 ERROR、限流/来源拒绝 WARN |
| G16.4 | ✅ | 无空 catch / printStackTrace |
| G17.1 | ⚠️ | 开关存在，但 `@ConfigurationProperties` 无 `@RefreshScope`/配置中心监听，改值需重启，与设计 §7.3「热更新、应急无需发版」不符 → P1-3 |
| G17.2 | ✅ | 创建失败语义明确（TODO_004），无需降级兜底（设计 §2.2） |
| G17.3 | ✅ | 仅新增表，回滚保留表不 DROP（设计 §7.3），无数据变更脚本 |
| G18.1 | N/A | 无密码/凭证处理 |
| G18.2 | ✅ | 日志不打印 description 全文（`TodoItemServiceImpl:59,115` 仅打印 tenantId/creatorId） |
| G18.3 | N/A | 无敏感数据存储 |

### 4.3 安全（`security-checklist.md`）

| ID | 状态 | 备注 |
|----|------|------|
| S1.1 | ✅ | 无自定义 SQL，仅 MyBatis-Plus `BaseMapper.insert` 预编译；无 `${}` |
| S1.2 | N/A | 无动态 order by / 表名 |
| S1.3 | N/A | 无 like / in 查询 |
| S2.1 | ✅ | 接口返回 JSON，无 HTML 渲染；前端渲染转义由设计 §6.4.3.2 承担 |
| S2.2 | N/A | 无富文本，描述按纯文本存储 |
| S2.3 | N/A | 无模板引擎 |
| S3.1 | N/A | 无外部 URL 请求 |
| S3.2 | N/A | 无 302 |
| S3.3 | N/A | 无外部 HTTP 调用 |
| S4.1 | N/A | 无命令执行 |
| S4.2 | N/A | 无文件操作 |
| S5.1 | N/A | 无 XML 解析 |
| S5.2 | N/A | 无 XPath |
| S6.1 | ✅ | 仅 Jackson 反序列化到固定 DTO，无多态类型 |
| S6.2 | ✅ | 未使用 `@JsonTypeInfo` |
| S6.3 | N/A | 无敏感字段 |
| S7.1 | N/A | 无文件上传 |
| S7.2 | N/A | 无文件路径 |
| S7.3 | N/A | 无文件存储 |
| S8.1 | ❌ | `/api/**` 已接入登录态校验，但**来源网关校验 fail-open**：`trusted-gateway-ips` 为空即跳过（`LoginInterceptor.java:43-48`，默认 `application.yml:45` 为空）→ 任何可达应用端口的调用方可自造 `X-User-Id`/`X-Tenant-Id` 代表他人创建 → P0-2 |
| S8.2 | ✅ | 增操作使用 POST（`TodoItemController:39`） |
| S8.3 | N/A | 主键自增，无资源 ID 查询接口 |
| S8.4 | N/A | 无 Cookie 写入 |
| S9.1 | ✅ | DB 账号密码来自环境变量占位（`application.yml:9-10`），未硬编码真实凭证 |
| S9.2 | ✅ | 日志输出 tenantId/creatorId，不打印描述全文（设计 §6.4.3.2 允许） |
| S9.3 | N/A | 传输层由网关 HTTPS 承担，设计 §2.2 |
| S9.4 | N/A | 无随机数使用（令牌桶使用 `System.nanoTime`） |
| S10.1 | N/A | 无会话/Cookie，鉴权由网关 Header 承担 |
| S10.2 | ✅ | 未开启 CORS 通配 |
| S10.3 | N/A | 无跳转 |

---

## Step 5 — 自定义扩展检查（产物 E）

| ID | 状态 | 备注 |
|----|------|------|
| U1.1 | ✅ | Controller 入参使用 `@Valid @RequestBody`（`TodoItemController:40`） |
| U1.2 | N/A | 示例项之外无更多团队规则（`customized-checklist.md` 仅含示例项） |
| U1.3 | N/A | 同上 |
| U2.1 | N/A | `customized-checklist.md` U2 无规则内容，`N/A(未启用自定义规则)` |
| U2.2 | N/A | 同上 |
| U2.3 | N/A | 同上 |

---

## 终检（防漏检）

- [x] 执行队列中每个文件 `Step2`、`Step3`、**S1–S10 / G1–G17** 各列均非 `⬜`
- [x] Step 2 的每个 REQ/Scenario 均非 `⬜`（REQ-4 ⚠️、REQ-7 ❌ 已记入报告）
- [x] Step 3 的 A1–A7 均非 `⬜`（A2 ❌）
- [x] Step 4 全部 G/S 与 B001–B081 / M001–M027 / I001–I010 ID 均非 `⬜`
- [x] Step 5 全部 U* ID 均非 `⬜`
- [x] 所有 `❌/⚠️` 已写入报告，且包含 `ID + path:line`

---

## 附录 A — `scan-all-rules.sh` 预扫输出（原文）

```text
=== Step 4 Rule Scan (B/M/I + A/S/G) ===
Targets: src/main/java/com/antdigital/todo src/test/java/com/antdigital/todo
Engine:  ripgrep

[P0] G16.2 — CatchWithoutLogging: src/main/java/com/antdigital/todo/item/service/impl/TodoItemServiceImpl.java:113

=== Summary: 1 findings (P0=1, P1=0, P2=0) | 52/222 rules scanned ===
```

**复核结论**：误报。`TodoItemServiceImpl.java:113-117` 的 `catch (DataAccessException ex)` 块内已调用
`LOGGER.error("待办事项写库失败, tenantId={}, creatorId={}", ..., ex)`，满足 G16.2/G16.4；
脚本基于「catch 与日志不在同一行/最近 2 行」的正则未命中，人工核销为 ✅。

---

## Round 2 — 修复后复审（Commit `00bc14d`，2026-10-29）

> **目的**：核销 Round 1 的 P0×2 / P1×3 / P2×7，并复核修复引入的新风险。
> **范围**：`git diff --name-only HEAD~1` → 24 个 `.java`（主代码 13 + 测试 11），逐文件重跑 Step 2→3→4→5。
> **验证**：`JAVA_HOME=zulu17 mvn -o test` → `Tests run: 88, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

### R2 Step 1 — 复审执行队列（逐文件，禁止 glob）

| # | 文件（仓库相对路径） | 归属原因 | Step2 | Step3 | Step4（G/S/B/M/I） | 总状态 |
|---|----------------------|----------|-------|-------|--------------------|--------|
| 1 | `src/main/java/com/antdigital/todo/common/config/TodoCreateProperties.java` | P1-3 灰度默认值 | ✅ | ✅ | ✅ | ✅ 已审 |
| 2 | `src/main/java/com/antdigital/todo/common/config/TodoCreateSettings.java` | 新增：运行期配置读取（G17.1） | ✅ | ✅ | ⚠️ P2-4 | ⚠️ 已审有问题 |
| 3 | `src/main/java/com/antdigital/todo/common/exception/GlobalExceptionHandler.java` | 失败指标 + traceId + import 序 | ✅ | ✅ | ⚠️ P2-2 | ⚠️ 已审有问题 |
| 4 | `src/main/java/com/antdigital/todo/common/interceptor/LoginInterceptor.java` | P0-1 / P0-2 主修复点 | ✅ | ✅ | ⚠️ P2-3 | ⚠️ 已审有问题 |
| 5 | `src/main/java/com/antdigital/todo/common/interceptor/RateLimitInterceptor.java` | 改用 `TodoCreateSettings` | ✅ | ✅ | ✅ | ✅ 已审 |
| 6 | `src/main/java/com/antdigital/todo/common/metrics/TodoCreateMetrics.java` | 新增：G16.1 指标 | ✅ | ✅ | ✅ | ✅ 已审 |
| 7 | `src/main/java/com/antdigital/todo/common/ratelimit/TokenBucketRateLimiter.java` | P2-4 空闲淘汰 | ✅ | ✅ | ✅ | ✅ 已审 |
| 8 | `src/main/java/com/antdigital/todo/common/security/GatewayIpMatcher.java` | 新增：S8.1 IP/CIDR 匹配 | ✅ | ✅ | ⚠️ P2-1 / P2-5 | ⚠️ 已审有问题 |
| 9 | `src/main/java/com/antdigital/todo/common/security/TrustedGatewayProperties.java` | fail-closed 语义 | ✅ | ✅ | ✅ | ✅ 已审 |
| 10 | `src/main/java/com/antdigital/todo/common/trace/TraceContext.java` | 新增：G16.2 traceId | ✅ | ✅ | ✅ | ✅ 已审 |
| 11 | `src/main/java/com/antdigital/todo/common/trace/TraceIdFilter.java` | 新增：G8.5 过滤器兜底清理 | ✅ | ✅ | ✅ | ✅ 已审 |
| 12 | `src/main/java/com/antdigital/todo/item/constant/TodoConstants.java` | P2-2 行宽 / P2-3 死常量 | ✅ | ✅ | ✅ | ✅ 已审 |
| 13 | `src/main/java/com/antdigital/todo/item/service/impl/TodoItemServiceImpl.java` | G16.1 成功埋点 | ✅ | ✅ | ⚠️ P2-2 | ⚠️ 已审有问题 |
| 14 | `src/test/java/com/antdigital/todo/common/config/TodoCreateSettingsTest.java` | 热更新用例 | ✅ | ✅ | ✅ | ✅ 已审 |
| 15 | `src/test/java/com/antdigital/todo/common/interceptor/LoginInterceptorTest.java` | P0 回归锁定用例 | ✅ | ✅ | ✅ | ✅ 已审 |
| 16 | `src/test/java/com/antdigital/todo/common/interceptor/RateLimitInterceptorTest.java` | 适配新构造 | ✅ | ✅ | ✅ | ✅ 已审 |
| 17 | `src/test/java/com/antdigital/todo/common/metrics/TodoCreateMetricsTest.java` | 指标分桶用例 | ✅ | ✅ | ✅ | ✅ 已审 |
| 18 | `src/test/java/com/antdigital/todo/common/ratelimit/TokenBucketRateLimiterTest.java` | 淘汰用例 | ✅ | ✅ | ✅ | ✅ 已审 |
| 19 | `src/test/java/com/antdigital/todo/common/security/GatewayIpMatcherTest.java` | fail-closed 用例 | ✅ | ✅ | ✅ | ✅ 已审 |
| 20 | `src/test/java/com/antdigital/todo/common/trace/TraceContextTest.java` | 防注入/清理用例 | ✅ | ✅ | ✅ | ✅ 已审 |
| 21 | `src/test/java/com/antdigital/todo/item/controller/TodoCreateIntegrationTest.java` | 端到端回归 | ✅ | ✅ | ✅ | ✅ 已审 |
| 22 | `src/test/java/com/antdigital/todo/item/controller/TodoItemControllerTest.java` | 适配新构造 | ✅ | ✅ | ✅ | ✅ 已审 |
| 23 | `src/test/java/com/antdigital/todo/item/service/impl/TodoItemServiceImplTest.java` | `recordSuccess` 断言 | ✅ | ✅ | ✅ | ✅ 已审 |
| 24 | `src/test/java/com/antdigital/todo/item/util/TodoTextNormalizerTest.java` | 行宽整改 | ✅ | ✅ | ✅ | ✅ 已审 |

**跳过（非 Java，仅作证据）**：`pom.xml`（Prometheus 依赖 / 元信息 → P2-6）、`src/main/resources/application.yml`（白名单、指标端点、日志 pattern、灰度默认值）、`src/test/resources/application-test.yml`（测试白名单回环放行）、`fix_report.md`。

### R2 Step 2 — Round 1 问题核销

| 上轮 | 状态 | 本轮证据 |
|------|------|----------|
| P0-1 `G8.5` ThreadLocal 残留 | ✅ 已闭合 | `LoginInterceptor.java:63-81,155-159`；`TraceIdFilter.java:33-37`；3 个拒绝路径断言 + 1 个端到端用例 |
| P0-2 `S8.1`/R11 fail-open | ✅ 已闭合 | `LoginInterceptor.java:99-113`；`GatewayIpMatcher.java:34-58`；`application.yml:46`；6 个单测 + 1 个端到端 |
| P1-1 `G16.1` 指标/访问日志 | ✅ 已闭合 | `TodoCreateMetrics.java:44-59`；`GlobalExceptionHandler.java:92-96`；`LoginInterceptor.java:166-176` |
| P1-2 `G16.2` traceId | ✅ 已闭合 | `TraceContext.java:37-62`；`TraceIdFilter.java:31-37`；`application.yml:48-51` |
| P1-3 `G17.1` 热更新 | ✅ 已闭合 | `TodoCreateSettings.java:40-92`；端到端运行期开关用例 |
| P2-1 import 序 | ✅ 已修复 | `GlobalExceptionHandler.java:10-15` |
| P2-2 行宽 | ✅ 已修复 | 全量复测 24 个变更文件无 >120 列 |
| P2-3 死常量 | ✅ 已修复 | `INSERT_TIMEOUT_MS`/`DEFAULT_STATUS` 全仓 0 引用 |
| P2-4 令牌桶无淘汰 | ✅ 已修复 | `TokenBucketRateLimiter.java:16-32,72-82`（60s 扫描 / 300s 空闲淘汰） |
| P2-5 测试缺口 | ✅ 已修复 | 64 → 88 例；含上下文清理、空白名单、超长 Header、emoji 落库 |
| P2-6 字符集 | ✅ 已修复 | `application.yml:8`（`characterEncoding=UTF-8` + `connectionCollation=utf8mb4_general_ci`） |
| P2-7 默认全量 | ✅ 已修复 | `application.yml:36` + `TodoCreateProperties.java:20`（`gray-enabled=true`） |

### R2 Step 3 — 可读性

| ID | 状态 | 备注 |
|----|------|------|
| A1 | ✅ | 命名/UTF-8/无 Tab |
| A2 | ✅ | import 按 ASCII 序（上轮 A2.4 已整改，`GlobalExceptionHandler.java:10-15`） |
| A3 | ✅ | 4 空格/K&R；行宽复测无 >120（被审 24 文件） |
| A4 | ✅ | 命名规范一致 |
| A5 | ✅ | `@Override` 齐全；无空 catch、无 `System.out`、无 `printStackTrace` |
| A6 | ✅ | 元素样式一致 |
| A7 | ✅ | 新增 5 个类均有类/方法级 Javadoc |

### R2 Step 4 — 可靠性 / 安全 / Bug 模式（仅列有变化的 ID）

| ID | 状态 | 备注 |
|----|------|------|
| G8.5 | ✅ | ThreadLocal 三条路径全清理（`preHandle` 拒绝、`afterCompletion`、`TraceIdFilter.finally`） |
| G11.2 | ⚠️ | `X-User-Name` 超长用例缺失，对应 P2-3 |
| G16.1 | ✅ | 成功量/耗时/错误码分桶落地；DB insert 独立耗时未单列 → P2-2 |
| G16.2 | ⚠️ | 预扫 5 条：4 条误报，`GatewayIpMatcher.java:105` 真命中 → P2-1（P2 降级，因结果仍 fail-closed） |
| G16.3 | ✅ | 业务异常 WARN、系统异常 ERROR、访问日志 INFO |
| G17.1 | ✅ | 运行期 `Binder` 实时读取，改值即生效（P2-4 为性能建议） |
| G17.2 | ✅ | 开关关闭 → TODO_007 兜底，不写库 |
| G14.2 | ✅ | 租户仅取自 `OperatorContext`，且来源受白名单约束 |
| G15.1 | ✅ | 本轮无表结构变更 |
| S8.1 | ⚠️ | 业务链路闭合；管理端点无鉴权 → **P1-1** |
| S9.1 | ✅ | 库凭证走 `${DB_USERNAME}/${DB_PASSWORD}`；白名单走 `${TRUSTED_GATEWAY_IPS}` |
| S9.2 | ✅ | 日志不输出描述全文；`creator_id` 为内部 ID |
| S1.1–S1.3 / S2.* / S3.* / S4.* / S5.* / S6.* / S7.* / S10.* | N/A | 无 SQL 拼接、无富文本、无外部 URL/命令/XML/文件上传、无 CORS 通配 |
| B001–B081 / M001–M027 / I001–I010 | ✅ | 逐条核销无 Blocker/Major 命中 |

### R2 Step 5 — 自定义扩展

| ID | 状态 | 备注 |
|----|------|------|
| U1.1 | ✅ | `@Valid @RequestBody`（`TodoItemController.java:40`） |
| U1.2 / U1.3 / U2.* | N/A | `N/A(未启用自定义规则)` |

### R2 附录 B — `scan-all-rules.sh` 预扫输出（Round 2 原文）

```text
=== Step 4 Rule Scan (B/M/I + A/S/G) ===
Targets: src/main/java/com/antdigital/todo src/test/java/com/antdigital/todo
Engine:  ripgrep

[P0] G16.2 — CatchWithoutLogging: src/main/java/com/antdigital/todo/common/interceptor/LoginInterceptor.java:75
[P0] G16.2 — CatchWithoutLogging: src/main/java/com/antdigital/todo/common/interceptor/LoginInterceptor.java:78
[P0] G16.2 — CatchWithoutLogging: src/main/java/com/antdigital/todo/common/security/GatewayIpMatcher.java:105
[P0] G16.2 — CatchWithoutLogging: src/main/java/com/antdigital/todo/common/security/GatewayIpMatcher.java:66
[P0] G16.2 — CatchWithoutLogging: src/main/java/com/antdigital/todo/item/service/impl/TodoItemServiceImpl.java:119

=== Summary: 5 findings (P0=5, P1=0, P2=0) | 52/222 rules scanned ===
```

**复核结论**：`LoginInterceptor.java:75/78`（→`handleRejected` 内 clear + 访问日志）、`GatewayIpMatcher.java:66`（紧邻 `LOGGER.warn`）、`TodoItemServiceImpl.java:119`（`LOGGER.error(..., ex)`）为**误报**；`GatewayIpMatcher.java:105` 为**真命中**，但结果仍是 fail-closed（拒绝而非放行），按影响面降级为 **P2-1**。

### R2 终检

- [x] 复审队列 24 个文件 Step2/Step3/Step4 均非 `⬜`
- [x] Round 1 的 12 项（P0×2 / P1×3 / P2×7）逐项核销完毕
- [x] 预扫输出已并入 Step 4 并逐条人工复核
- [x] 全部 `❌/⚠️` 已写入 `cr_report.md` §3–§7，且含 ID + `path:line`
- [x] 报告末尾含 §8 修复任务列表

