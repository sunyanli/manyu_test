# 代码评审报告（Code Review Report）

- **审查对象**：HEAD commit `574aa4a`（`[auto-dev] 编码实现`，master 相对上一次提交 `2268f1f` 的 **43 个文件 / +2852 行**，其中 34 个 `.java` 文件）。
- **审查分支**：`AI/task-DEV-eecb99b0-9b79-11f1-a438-852be3c2a082-39bc093a-4aa8-4616-a4e9-f018634c99a2`
- **需求**：生成一个 3 秒视频，内容是"小猫喵喵叫"。
- **审查技能**：dtazziboot-java-code-review（precheck → Step1 执行队列 → Step2 功能 → Step3 可读性 → Step4 可靠性/安全/Bug 模式 → Step5 自定义）。
- **审查日期**：2026-09-17

---

## §1 审查范围

| 类别 | 文件 | 数量 | 处理 |
|------|------|------|------|
| Java 主代码 | `src/main/java/com/antdigital/video/**` | 32 | 逐文件 LLM 复核 |
| Java 测试 | `src/test/java/com/antdigital/video/**` | 2 | 逐文件 LLM 复核 |
| 数据访问 XML | `src/main/resources/mapper/*.xml`（×3） | 3 | 非 Java，作可靠性/安全证据引用（跳过 LLM 单文件复核） |
| 数据库脚本 | `src/main/resources/db/schema.sql` | 1 | 非 Java，作可靠性证据引用 |
| 配置 | `src/main/resources/application.yml`、`pom.xml` | 2 | 非 Java，作安全/依赖证据引用 |
| 设计/实现文档 | `design.md`、`impl.md`、`docs/**`（×2） | 4 | Step2 REQ 抽取依据 |

> 预检结论（precheck）：工作树干净；仓库为 Java 项目（34 个 `.java` 文件，Spring Boot 3.3.5 / Java 21 / MyBatis）。

## §2 问题计数

| 级别 | 数量 | 说明 |
|------|------|------|
| P0 / Blocker | **2** | 均来自安全清单（S8.1 认证与租户隔离、S9.1 硬编码密钥） |
| P1 / Major | 9 | 并发幂等、事务边界、任务号唯一性、时区、响应一致、异常可观测性、HTTP 语义、结果 URL 授权、枚举校验 | 
| P2 / Minor | 4 项（8 处行宽） | 代码风格（行宽×8）、类型映射不一致、冗余 DTO、异常映射 |
| Bug 模式（B/M/I） | 0 | scan 高危子集 52/222 规则未命中 B/M/I 高风险模式 |

> 机械扫描 `scan-all-rules.sh` 输出 12 条（P0×3、P1×1、P2×8）。其中 3 条 `[P0] G16.2 CatchWithoutLogging` 经 LLM 逐行复核后**降级**：`VideoTaskServiceImpl.java:243`（重试 catch 已 `logger.warn`）、`:320`（`sign` 重抛非吞异常）、`VideoTaskServiceImplTest.java:278`（测试断言路径）。G16.2 在 reliability-checklist 中的等级锚点为 **P1**，脚本打印 `[P0]` 属标注偏差；且均非 G16.4（空 catch / printStackTrace）的真 P0。最终 blocker 以人工复核为准。

## §3 Step2 功能检查（REQ 映射）

| REQ | 需求点 | 结论 | 证据 |
|-----|--------|------|------|
| F01 | 创建任务 + 幂等（R01/R02） | ⚠️ | `VideoTaskServiceImpl.java:76-118`；`format/resolution/aspectRatio` 无枚举校验，仅默认值兜底 |
| F02 | 详情查询 + 租户隔离（R06） | ⚠️ | `:126-137`；租户头 `X-Tenant-Id` 用户可控（见 P0 #1） |
| F03 | 结果/预览 | ✅ | `:198-200, :132-135`；`completeTask` 合并 resultUrl |
| F04 | 列表 / 取消 | ✅ | `:141-166`；非终态可取消 |
| F05 | 引擎回调（R04/R05/R07） | ⚠️ | `:170-228`；验签存在，幂等为读后写竞态（见 P1 #1） |
| F06 | 规格默认值 | ⚠️ | `VideoConsts.java`；默认 720p/16:9/mp4，无枚举白名单校验 |

## §4 Step3 可读性检查

- 命名、import、结构、Javadoc：✅ 达标。
- **A3.4 行宽超限**（>120 字符，P2）共 8 处：`VideoTaskMapper.java:18`；`VideoTaskServiceImpl.java:86,120,182,200,202,208`；`VideoTaskServiceImplTest.java:225`。
- A5.2 空 catch：✅ 无空 catch / 无 `printStackTrace`（`swap` 命中的 catch 均重抛或记日志）。

## §5 Step4 可靠性 / 安全 / Bug 模式检查

**可靠性（LLM 复核后定级）**

- **G1.1 幂等并发竞态（P1）**：`handleEngineCallback` 先 `selectByEngineJobId` 判断 `callbackStatus=RECEIVED`，再无条件 `updateCallbackStatus ... WHERE id=#{id}`（无 `AND callback_status != 'RECEIVED'` 条件），并发重复回调可双写/状态错乱。
- **G3.2 事务包含外部 I/O（P1）**：`createVideoTask` 的 `@Transactional` 包裹 `submitToEngine`（3 次重试的外部提交）；重试耗尽后 `updateError(...FAILED...)` 随后 `throw BusinessException` → 运行时异常触发回滚，FAILED 标记/结果被一并回滚，违反设计 R03（应 `REQUIRES_NEW` 或先提交再异步改造）。
- **M016 / G14.4 时区（P1）**：`generateTaskNo()` 使用 `LocalDateTime.now()`（VM 默认时区）拼接秒级时间戳 + `AtomicLong % 10000`；多实例部署或进程重启后同秒复用序列 → 触碰 `uk_video_task_task_no` 唯一约束 → 500。

**安全（LLM 复核后定级）**

- **S8.1 认证与资源归属（P0 #1）**：对外接口仅取 `X-Tenant-Id` 请求头作为租户身份，全程无认证/授权；任意调用方可伪造租户头读取/取消他租户任务（违反 R06 租户隔离，等价命中 G14.2 租户查询）。
- **S9.1 硬编码密钥（P0 #2）**：回调签名密钥默认值 `callback-secret` 直接写在源码 `@Value("${video.callback.secret:callback-secret}")`，`application.yml` 未覆盖；生产环境若未显式配置则回调签名可被伪造，任意注入成功结果 URL（违反 R04）。
- S9.2 日志敏感信息：✅ 未打印密钥/敏感报文。

**Bug 模式**：B001–B081 / M001–M027 / I001–I010 无高风险命中。

## §6 Step5 自定义扩展检查

- U1.1 入参校验：✅ 两个创建接口 Controller 均使用 `@Valid`。
- U2.* 业务红线：**N/A（未启用自定义规则）**。

## §7 结论

本次提交为全新增量的 Spring Boot 视频生成服务骨架，功能面覆盖 F01–F06 主链路，测试补充到位（`VideoTaskServiceImplTest` 覆盖状态机/重试/幂等）。但存在 **2 个 P0 安全阻塞项** —— 接口缺少认证与租户隔离防护、回调密钥硬编码默认值 —— 当前代码若直接上线，任意调用方可跨租户读取数据并伪造引擎成功回调，必须在本轮收口前修复。另有 7 项 P1（并发幂等、事务边界、响应一致性、时区、异常可观测性、HTTP 语义、结果 URL 授权）与若干 P2 待处理。

### §7.1 问题片段（必填）

**P0 #1 · G14.2 / S8.1 · 接口缺少认证，租户头用户可控 · `VideoTaskController.java:40-43`**
```
L40|    public Object create(@RequestHeader("X-Tenant-Id") String tenantId,
L41|            @Valid @RequestBody CreateVideoTaskRequest request) {
L42|        Long taskId = videoTaskService.createVideoTask(tenantId, toCommand(request));
L43|        return Responses.createSuccess(taskId, null, "CREATED");
```

**P0 #1 · `OpenapiVideoTaskController.java:33-36`**
```
L33|    public Object create(@RequestHeader("X-Tenant-Id") String tenantId,
L34|            @Valid @RequestBody CreateVideoTaskRequest request) {
L35|        Long taskId = videoTaskService.createVideoTask(tenantId, toCommand(request));
L36|        return Responses.createSuccess(taskId, null, "CREATED");
```

**P0 #2 · S9.1 · 回调密钥硬编码默认值 · `VideoTaskServiceImpl.java:63-64`**
```
L63|            @Value("${video.engine.type:default}") String engineType,
L64|            @Value("${video.callback.secret:callback-secret}") String callbackSecret) {
L65|        this.videoTaskMapper = videoTaskMapper;
```

**P1 · G1.1 · 回调幂等读后写竞态 · `AiEngineJobMapper.xml:36-40`（非 Java）**
```
N/A(非 Java) — updateCallbackStatus 无条件 SET callback_status=#{callbackStatus} WHERE id=#{id}，缺当前状态条件
```

**P1 · G3.2 · 事务内外部 I/O，失败回滚丢失 FAILED 标记 · `VideoTaskServiceImpl.java:230-250`**
```
L230|    private String submitToEngine(VideoTaskDO task) {
...
L243|            } catch (RuntimeException ex) {
L244|                last = ex;
L249|        videoTaskMapper.updateError(task.getId(), TaskStatusEnum.FAILED.name(), ErrorCodes.VT_007, "引擎提交失败");
L250|        throw new BusinessException(ErrorCodes.VT_007, "引擎提交失败", last == null ? "视频生成引擎不可用" : last.getMessage());
```

**P1 · 时区 M016/G14.4 · 任务号生成无时区、序列内存态 · `VideoTaskServiceImpl.java:293-297`**
```
L293|    private String generateTaskNo() {
L294|        String time = LocalDateTime.now().format(TASK_NO_TIME);
L295|        long seq = taskNoSequence.incrementAndGet() % 10000L;
L296|        return VideoConsts.TASK_NO_PREFIX + time + String.format("%04d", seq);
```

**P1 · 响应一致 · 返回 taskNo=null 且状态写死 CREATED · `VideoTaskController.java:39-43`**
```
L39|    @PostMapping
L40|    public Object create(@RequestHeader("X-Tenant-Id") String tenantId,
L41|            @Valid @RequestBody CreateVideoTaskRequest request) {
L42|        Long taskId = videoTaskService.createVideoTask(tenantId, toCommand(request));
L43|        return Responses.createSuccess(taskId, null, "CREATED");
```

**P1 · 响应一致 · `OpenapiVideoTaskController.java:32-36`**
```
L32|    @PostMapping
L33|    public Object create(@RequestHeader("X-Tenant-Id") String tenantId,
L34|            @Valid @RequestBody CreateVideoTaskRequest request) {
L35|        Long taskId = videoTaskService.createVideoTask(tenantId, toCommand(request));
L36|        return Responses.createSuccess(taskId, null, "CREATED");
```

**P1 · G16.2 · sign() 捕获异常重抛但无日志上下文 · `VideoTaskServiceImpl.java:311-322`**
```
L313|            MessageDigest digest = MessageDigest.getInstance("SHA-256");
L314|            byte[] hash = digest.digest((callbackSecret + raw).getBytes(StandardCharsets.UTF_8));
...
L320|        } catch (Exception ex) {
L321|            throw new IllegalStateException("签名算法不可用", ex);
L322|        }
```

**P1 · 业务/校验异常映射 HTTP 200 · `GlobalExceptionHandler.java:23-35`**
```
L23|    @ExceptionHandler(BusinessException.class)
L24|    @ResponseStatus(HttpStatus.OK)
L25|    public ApiResponse<Void> handleBusinessException(BusinessException ex) {
...
L30|    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
L31|    @ResponseStatus(HttpStatus.OK)
L32|    public ApiResponse<Void> handleValidationException(Exception ex) {
```

**P1 · R10 结果 URL 未授权/未签名 · `DefaultVideoStorageService.java:16-21`**
```
L16|    @Override
L17|    public String buildAccessUrl(String objectKey) {
L18|        if (objectKey == null || objectKey.isBlank()) {
L19|            throw new IllegalArgumentException("对象 key 不能为空");
L20|        }
L21|        return baseUrl + "/videos/" + objectKey;
```

**P2 · A3.4 · 行宽超限代表（其余 7 处见 §4）· `VideoTaskServiceImpl.java:86`**
```
L85|            if (existed != null) {
L86|                logger.info("命中幂等键, tenantId: {}, idempotencyKey: {}, taskId: {}", tenantId, idempotencyKey, existed.getId());
L87|                return existed.getId();
```

**P2 · 类型映射不一致 · `AiEngineJobMapper.xml:11-12`（非 Java）**
```
N/A(非 Java) — resultMap raw_payload jdbcType="VARCHAR" 与 schema.sql:48 raw_payload TEXT 不一致
```

**P2 · 冗余 DTO · `VideoTaskQuery.java`（非缺陷代码片段，纯类型存在）**
```
N/A(非缺陷代码) — 未在任何 Controller/Service 签名中使用，可删除
```

## §8 修复任务列表

P0（2）：
- [ ] P0: 认证与租户隔离（S8.1/G14.2）— 为 `VideoTaskController.java:40` 与 `OpenapiVideoTaskController.java:33` 引入真实鉴权，禁止直接信任 `X-Tenant-Id` 请求头。
- [ ] P0: 回调密钥硬编码（S9.1）— `VideoTaskServiceImpl.java:64` 移除默认值 `callback-secret`，改为仅从环境变量注入且缺失即启动失败。

P1（9）：
- [ ] P1: HTTP 语义（HTTP 200 冲突）— `GlobalExceptionHandler.java:24,31` 业务/校验异常改为 4xx 状态码。
- [ ] P1: 结果 URL 授权（R10）— `DefaultVideoStorageService.java:21` 改为带签名及租户限定的访问 URL。
- [ ] P1: 事务边界（G3.2）— `VideoTaskServiceImpl.java:75,230-250` 将外部引擎提交移出事务，或 FAILED 落库用 `REQUIRES_NEW`。
- [ ] P1: 任务号唯一性（M016/G14.4）— `VideoTaskServiceImpl.java:293-297` 改用数据库序列/雪花 ID 或 UUID，避免多实例/重启冲突。
- [ ] P1: 时区（G14.4）— `VideoTaskServiceImpl.java:294` 使用 `ZoneId`/UTC 显式时区。
- [ ] P1: 回调幂等竞态（G1.1）— `AiEngineJobMapper.xml:36-40` 增加 `AND callback_status != 'RECEIVED'` 条件并校验影响行数。
- [ ] P1: 异常可观测性（G16.2）— `VideoTaskServiceImpl.java:243,320` 记录完整堆栈（日志带 `ex` 异常对象）。
- [ ] P1: 响应一致性 — `VideoTaskController.java:43` 与 `OpenapiVideoTaskController.java:36` 返回真实 `taskNo` 与提交后状态（如 `SUBMITTED`）。
- [ ] P1: 枚举白名单校验 — 对 `format/resolution/aspectRatio/durationSec` 增加合法值校验。

P2（4）：
- [ ] P2: X-Tenant-Id 缺失避免 500 — 全局处理器捕获 `MissingRequestHeaderException` 并返回 400。
- [ ] P2: MyBatis 类型映射不一致 — `AiEngineJobMapper.xml:12` raw_payload 改为 `jdbcType="LONGVARCHAR"` 或 CLOB 映射。
- [ ] P2: 行宽规范（A3.4）— 折行处理 8 处超 120 字符（路径见 §4）。
- [ ] P2: 冗余 DTO — 删除未使用的 `VideoTaskQuery.java`。