# Code Review Report

> **Change** `todo-create`（新增待办事项最小闭环） · **分支/Commit** `AI/task-DEV-9d10e310-…-9b6893e6` / `e6f46f1`（变更范围 `dd91a6a..e6f46f1`） · **日期** `2026-09-24` · **审查者** AI（技能：dtazziboot-java-code-review v1.1.0，SDD 模式）
>
> 流程：知识库检索（manyu商业版预发：弱相关命中 1 篇；百事测试项目：已检索、未命中）→ `scan-all-rules.sh` 预扫（52/222 条，No findings）→ LLM 逐文件 Step 2→3→4→5。详细核销见 `.agents/cr/2026-09-24-todo-create-cr-checklist.md`。

---

## 1. 审查范围

| 项 | 值 |
|----|-----|
| `.java` 文件数 | 12（main 11 + test 1） |
| 变更行数 | +2036 / -0（含文档与资源文件；Java 约 +571 行） |

| 类/接口 | 路径 | 角色 |
|---------|------|------|
| TodoApplication | src/main/java/com/manyu/todo/TodoApplication.java | 启动类 |
| TodoController | src/main/java/com/manyu/todo/api/controller/TodoController.java | REST 入口 POST /api/todos/create |
| TodoService / TodoServiceImpl | src/main/java/com/manyu/todo/service/… | 业务校验与落库 |
| TodoMapper (+XML) | src/main/java/com/manyu/todo/dao/mapper/… | MyBatis DAO |
| TodoCreateRequest / TodoDO / TodoVO | src/main/java/com/manyu/todo/model/… | DTO/实体/视图对象 |
| Result / BizException / GlobalExceptionHandler | src/main/java/com/manyu/todo/common/… | 统一响应与异常 |
| TodoServiceImplTest | src/test/java/com/manyu/todo/service/impl/TodoServiceImplTest.java | 单元测试（6 例） |

---

## 2. 问题计数

| P0 | P1 | P2 |
|----|----|-----|
| 9 | 3 | 3 |

---

## 3. Step 2 — 功能（REQ）

| REQ | Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|-----|----------|------|----------|----------|------|
| REQ-1 | 合法 name+description 创建并返回含自增 id 的 VO | ✅ | requirement 原文；ARCHITECTURE.md §应用概览 | TodoServiceImpl.java:31-43；Test:34-57 | 功能闭环完整，id 回填、trim、写库参数均有测试 |
| REQ-2 | name 空白→A0001；超 64 字符→A0002 | ✅ | README.md §API 错误码表 | TodoServiceImpl.java:49-55；Test:69-92 | |
| REQ-3 | description 选填，超 512→A0003 | ✅ | README.md §API 错误码表 | TodoServiceImpl.java:56-59；Test:95-106 | |
| REQ-4 | 请求体缺失/非法 JSON→A0400/A0001 | ❌ **P0** | README.md:52-53「A0400 请求体校验失败」「A0001 … 请求为空」 | GlobalExceptionHandler.java:36-39 | `@RequestBody`（required=true）body 缺失/JSON 非法抛 `HttpMessageNotReadableException`，未单独处理，落入 `Exception` 兜底返回 **B0001「系统繁忙」**，文档承诺的错误码行为不可达 |
| REQ-5 | 响应 `gmtCreate` 为 ISO-8601 字符串 | ❌ **P0** | README.md:44 响应示例 `"gmtCreate": "2026-09-24T12:00:00.000+08:00"` | TodoVO.java:3,22；application.yml（无 jackson 配置） | `java.util.Date` 在 Spring Boot 2.7 默认序列化为 **epoch 毫秒数**，与文档示例不符，前端按文档对接会解析失败 |

---

## 4. Step 3 — 可读性检查

| 结果 | 说明 |
|------|------|
| ⚠️（1 项 P2） | **A2.4** `TodoController.java:7-12`：import 组内非 ASCII 字典序（`javax.validation.Valid` 位于 `org.springframework.*` 之后）。其余 A1/A3–A7 全部合规：命名、K&R、4 空格缩进、行宽、Javadoc、`@Override` 均符合规范 |

---

## 5. Step 4 — 可靠性检查

| 域 | 参考 | 结果 | 等级 | 说明 |
|----|------|------|------|------|
| 可靠性 | reliability-checklist.md G1–G17 | ❌ | P0 | **G2.1** 写接口无幂等键（TodoController.java:35-38，重复提交重复落库）；**G9.1/G9.2** DB 调用未设超时且异常不分三态（application.yml:8、GlobalExceptionHandler.java:36-39）；**G16.1** 核心链路无指标埋点；**G17.1/G17.2** 无功能开关与降级预案。P2：**G11.2** 单测未覆盖 `name==null` 分支与 gmtCreate 回填 |
| 安全 | security-checklist.md S1–S10 | ❌ | P0 | **S9.1** 数据库 root/root 凭证硬编码提交仓库（application.yml:9-10）；**S8.1** 全部接口无鉴权。P1：**S8.3** 自增 ID 直接对外；**S9.3** 未配置 HTTPS（需确认网关终结）；**S10.1** 无 CSRF 防护（token 鉴权可豁免，需架构确认）。✅：SQL 全 `#{}` 参数化、日志无敏感信息、CORS 未放开 |
| Bug 模式 | bug-pattern-checklist.md B/M/I（120 条） | ⚠️ | P2 | 预扫 `scan-all-rules.sh`：**No findings（52/222）**。LLM 补扫命中 **I004**：`TodoDO.java:21`、`TodoVO.java:22` 使用 `java.util.Date`（建议 `LocalDateTime`，可一并解决 REQ-5）。其余 119 条 N/A 或 ✅（B055/B079/B080/M020/M026 等相关项均合规） |

---

## 6. Step 5 — 自定义扩展检查

| 域 | 参考 | 结果 | 等级 | 说明 |
|----|------|------|------|------|
| 自定义扩展 | customized-checklist.md U* | ✅ | — | N/A(未启用自定义规则)；示例项 U1.1（Controller 入参 `@Valid`）已满足 |

---

## 7. 结论

- **合并建议**：**阻止合并**（P0 未清零）。
- **P0**：
  1. **S9.1** `application.yml:9-10` — 数据库凭证 root/root 硬编码入库（上线前必修，并轮换凭证）。
  2. **S8.1** — 接口无任何鉴权，可匿名写库。
  3. **G9.2/G9.1** — DB 无 connectTimeout/socketTimeout，慢 SQL 可挂满 Tomcat 线程，且异常不分三态。
  4. **REQ-4** — 请求体缺失/非法 JSON 返回 B0001 而非文档承诺的 A0400/A0001（补 `HttpMessageNotReadableException` 处理器）。
  5. **REQ-5** — `gmtCreate` 实际序列化为 epoch 毫秒，与 README 契约不符（改 LocalDateTime 或补 Jackson 配置）。
  6. **G2.1** — 创建接口无幂等键，重复提交重复落库。
  7. **G16.1** — 核心链路无监控埋点。
  8. **G17.1** — 无功能开关支持紧急关闭。
  9. **G17.2** — 无降级预案。
- **P1**：S8.3（自增 ID 可预测）、S9.3（HTTPS 需网关确认）、S10.1（CSRF 需架构确认）。
- **P2**：A2.4（import 顺序）、I004（java.util.Date）、G11.2（单测边界补充）。
- **一句话**：功能最小闭环实现完整、分层清晰、单测与编码规范质量较高（脚本预扫零命中），但**安全基线（凭证泄露、无鉴权）与生产可靠性（超时/幂等/监控/应急）尚未达标**，须完成 P0 修复后方可合并上线。

---

## 7.1 问题片段（必填）

### P0

- **P0** `S9.1` `src/main/resources/application.yml:7-11` — 数据库 root 凭证硬编码并提交仓库，任何人可从代码库获取生产同款凭证。片段范围：`src/main/resources/application.yml:7-11`。**N/A(非 Java)**：

```yaml
L 7|  datasource:
L 8|    url: jdbc:mysql://127.0.0.1:3306/todo_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
L 9|    username: root
L10|    password: root
L11|    driver-class-name: com.mysql.cj.jdbc.Driver
```

- **P0** `S8.1` `src/main/java/com/manyu/todo/api/controller/TodoController.java:19-21` — 接口无任何鉴权层（无 Spring Security、无登录态校验、无网关鉴权约定），匿名请求可直接写库。

```java
L19|@RestController
L20|@RequestMapping("/api/todos")
L21|public class TodoController {
L22|
L23|    private final TodoService todoService;
L24|
L25|    public TodoController(TodoService todoService) {
L26|        this.todoService = todoService;
L27|    }
```

- **P0** `G2.1` `src/main/java/com/manyu/todo/api/controller/TodoController.java:35-38` — 写接口无幂等键/防重复提交，双击或网络重发会创建多条重复待办。

```java
L35|    @PostMapping("/create")
L36|    public Result<TodoVO> createTodo(@Valid @RequestBody TodoCreateRequest request) {
L37|        return Result.success(todoService.createTodo(request));
L38|    }
```

- **P0** `G9.2` `src/main/resources/application.yml:8` — JDBC URL 未设置 `connectTimeout/socketTimeout`，HikariCP 亦无显式配置，DB 慢/挂时请求线程被无限占用。片段见上方 S9.1 片段 L8 行。**N/A(非 Java)**。
- **P0** `REQ-4` `src/main/java/com/manyu/todo/common/exception/GlobalExceptionHandler.java:36-40` — `HttpMessageNotReadableException`（body 缺失/JSON 非法）落入兜底处理器，返回 B0001「系统繁忙」而非文档承诺的 A0400/A0001。

```java
L36|    @ExceptionHandler(Exception.class)
L37|    public Result<Void> handleUnexpectedException(Exception e) {
L38|        LOGGER.error("系统异常, errorMessage: {}", e.getMessage(), e);
L39|        return Result.fail("B0001", "系统繁忙，请稍后再试");
L40|    }
```

- **P0** `REQ-5` `src/main/java/com/manyu/todo/model/vo/TodoVO.java:20-24` — `gmtCreate` 为 `java.util.Date` 且无 Jackson 格式化配置，实际响应为 epoch 毫秒，与 README.md:44 的 ISO-8601 示例契约不符。

```java
L20|    /** 创建时间 */
L21|    private Date gmtCreate;
L22|
L23|    public Long getId() {
L24|        return id;
L25|    }
```

（注：字段声明位于 L21-22；`import java.util.Date` 位于 L3。）

- **P0** `G16.1` `src/main/java/com/manyu/todo/api/controller/TodoController.java:35-38` — 核心创建链路无任何指标埋点（调用量/成功率/耗时），线上不可监控、不可告警。片段同 G2.1 片段。缺失性证据：全工程无 Micrometer/Prometheus 依赖（pom.xml）。
- **P0** `G17.1` / `G17.2` — 无功能开关与降级预案（缺失性证据：pom.xml 无开关组件、TodoServiceImpl.java:31-43 无降级分支）。**N/A(非 Java)**（缺失性问题，无可引用的问题代码行；修复动作见 §8）。

### P1

- **P1** `S8.3` `src/main/java/com/manyu/todo/model/vo/TodoVO.java:12-13` — 自增 Long ID 直接对外暴露，可预测可遍历；后续开放查询/删除接口前建议引入不可预测 ID。

```java
L12|    /** 待办事项 ID */
L13|    private Long id;
L14|
L15|    /** 事项名称 */
L16|    private String name;
```

- **P1** `S9.3` `src/main/resources/application.yml:1-2` — 应用层为 HTTP 明文，未配置/声明 TLS（若由网关终结需部署确认）。**N/A(非 Java)**：

```yaml
L1|server:
L2|  port: 8080
```

- **P1** `S10.1` `src/main/java/com/manyu/todo/api/controller/TodoController.java:35-36` — 增改操作无 CSRF 防护；若最终采用 Header Token 鉴权（非 Cookie 会话）可豁免，需架构层面确认。

```java
L35|    @PostMapping("/create")
L36|    public Result<TodoVO> createTodo(@Valid @RequestBody TodoCreateRequest request) {
```

### P2

- **P2** `A2.4` `src/main/java/com/manyu/todo/api/controller/TodoController.java:7-12` — import 组内非 ASCII 字典序（`javax` 应排在 `org` 之前）。

```java
L 7|import org.springframework.web.bind.annotation.PostMapping;
L 8|import org.springframework.web.bind.annotation.RequestBody;
L 9|import org.springframework.web.bind.annotation.RequestMapping;
L10|import org.springframework.web.bind.annotation.RestController;
L11|
L12|import javax.validation.Valid;
```

- **P2** `I004` `src/main/java/com/manyu/todo/model/entity/TodoDO.java:20-24` — 使用 `java.util.Date`，建议 `java.time.LocalDateTime`（与 REQ-5 修复合并处理）。

```java
L20|    /** 创建时间 */
L21|    private Date gmtCreate;
L22|
L23|    /** 修改时间 */
L24|    private Date gmtModified;
```

- **P2** `G11.2` `src/test/java/com/manyu/todo/service/impl/TodoServiceImplTest.java:69-79` — name 校验仅覆盖空白分支，未覆盖 `name == null` 分支（TodoServiceImpl.java:50 的 null 判断）；建议补充 `request.setName(null)` 用例。

```java
L69|    void should_throwException_when_nameIsBlank() {
L70|        // Arrange (Given)
L71|        TodoCreateRequest request = new TodoCreateRequest();
L72|        request.setName("   ");
```

---

## 8. 修复任务列表

### P0

- [ ] **P0** `src/main/resources/application.yml:9-10`（S9.1）— 将数据库凭证改为环境变量/配置中心注入，移除仓库中的 root/root，并轮换已泄露凭证
- [ ] **P0** `src/main/resources/application.yml:8`（G9.2）— 在 JDBC URL 追加 `connectTimeout=1000&socketTimeout=3000` 并显式配置 HikariCP 连接池参数
- [ ] **P0** `S8.1` — 为 `/api/todos/**` 接入统一鉴权（网关鉴权或 Spring Security 登录态校验），拒绝匿名写库
- [ ] **P0** `src/main/java/com/manyu/todo/common/exception/GlobalExceptionHandler.java:36`（REQ-4）— 新增 `HttpMessageNotReadableException` 处理器返回 A0400，使错误码行为与 docs/modules/todo/README.md 一致
- [ ] **P0** `src/main/java/com/manyu/todo/model/vo/TodoVO.java:21`（REQ-5/I004）— 将 gmtCreate 改为 `LocalDateTime` 或增加 Jackson ISO-8601 格式化配置，使响应与 README.md:44 契约一致
- [ ] **P0** `src/main/java/com/manyu/todo/api/controller/TodoController.java:35`（G2.1）— 为创建接口增加幂等控制（客户端请求 ID + DB 唯一约束或 Redis 去重）
- [ ] **P0** `G16.1` — 为创建链路接入指标埋点（调用量/成功率/耗时）并配置告警
- [ ] **P0** `G17.1` — 为创建入口增加可动态关闭的功能开关
- [ ] **P0** `G17.2` — 补充 DB 故障场景降级预案（限流/兜底响应）并写入 docs/modules/todo/README.md

### P1

- [ ] **P1** `src/main/java/com/manyu/todo/model/vo/TodoVO.java:13`（S8.3）— 评估对外 ID 改为 UUID/加密 ID（开放查询接口前完成）
- [ ] **P1** `src/main/resources/application.yml:1-2`（S9.3）— 确认 TLS 由网关/SLB 终结并在部署文档中声明，否则开启 HTTPS
- [ ] **P1** `S10.1` — 确认鉴权方案为 Header Token 时在架构文档中豁免 CSRF，否则补 CSRF 防护

### P2（可选）

- [ ] **P2** `src/main/java/com/manyu/todo/api/controller/TodoController.java:7-12`（A2.4）— 调整 import 顺序，`javax.validation.Valid` 移至 `org.springframework.*` 之前
- [ ] **P2** `src/main/java/com/manyu/todo/model/entity/TodoDO.java:21`（I004）— 与 REQ-5 修复合并，统一改用 `java.time` 类型
- [ ] **P2** `src/test/java/com/manyu/todo/service/impl/TodoServiceImplTest.java:69`（G11.2）— 补充 `name == null` 分支单测
