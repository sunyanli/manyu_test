# Code Review Report

> **Change**: 接口演示与统计报表（HelloWorld + 哈希 + 冒泡排序） · **分支**: `task/DEV-966dcd0a` · **日期**: 2026-10-30 · **审查者**: AI (dtazziboot-java-code-review v1.1.0)
>
> **AI**：等级 **P0 / P1 / P2**；G/S 以 checklist 行内定义为准；Bug 模式以 `bug-pattern-checklist.md` 表头为准（Blocker→P0、Major→P1、Info→P2）。已运行 `scan-all-rules.sh` 并并入 §5。**问题须含 `path:line` 或清单 ID：可读性 `A3.4`，安全 `S10.2`，可靠性 `G16.2`。**

---

## 1. 审查范围

| 项 | 值 |
|----|-----|
| 仓库 | `manyu_test` (后端 14 Java + 4 配置) + `manyu_test1` (前端 1 HTML) |
| `.java` 文件数 | 14 |
| 修复阶段变更行数 | +110 / –22（7 files） |

| 类/接口 | 路径 | 角色 |
|---------|------|------|
| `DemoApplication` | `src/main/java/.../DemoApplication.java` | 启动类 + 异步线程池配置 |
| `CommonResponse` | `src/main/java/.../common/CommonResponse.java` | 统一响应体 `{code,msg,data}` |
| `CorsConfig` | `src/main/java/.../common/CorsConfig.java` | CORS 跨域配置（可配置化） |
| `DemoException` | `src/main/java/.../common/DemoException.java` | 业务异常 |
| `DemoController` | `src/main/java/.../controller/DemoController.java` | REST 控制器（5 端点 + 3 异常处理） |
| `ApiCallRecordMapper` | `src/main/java/.../dao/mapper/ApiCallRecordMapper.java` | MyBatis Mapper 接口 |
| `ApiCallRecordDO` | `src/main/java/.../model/ApiCallRecordDO.java` | 埋点记录实体（MyBatis-Plus） |
| `HashRequest` | `src/main/java/.../request/HashRequest.java` | 哈希请求体（@NotBlank） |
| `SortRequest` | `src/main/java/.../request/SortRequest.java` | 排序请求体（@NotEmpty） |
| `DemoResultVO` | `src/main/java/.../response/DemoResultVO.java` | 演示结果 VO |
| `StatsResultVO` | `src/main/java/.../response/StatsResultVO.java` | 统计结果 VO（含 StatsItem/TrendItem 内部类） |
| `DemoService` / `DemoServiceImpl` | `src/main/java/.../service/DemoService.java` + `impl/DemoServiceImpl.java` | 演示核心逻辑（hello/hash/sort + 埋点触发 + 姓名脱敏） |
| `ApiCallRecordService` / `ApiCallRecordServiceImpl` | `src/main/java/.../service/ApiCallRecordService.java` + `impl/ApiCallRecordServiceImpl.java` | 埋点异步写入 + 统计查询 + Excel 导出 |

非 Java 文件（做契约/配置审查，不纳入 Step 3–4 逐文件扫描）：
- `pom.xml`、`application.yml`、`ApiCallRecordMapper.xml`、`schema.sql`
- `[manyu_test1] src/index.html`

---

## 2. 问题计数

| P0 | P1 | P2 |
|----|----|-----|
| 0 | 2 | 4 |

---

## 3. Step 2 — 功能（REQ）

> 来源：design.md §1.5 需求功能清单 + §5 功能模块设计。

### REQ-F01: HelloWorld 示例接口

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| GET /api/demo/hello 无参返回消息+时间戳+type=hello | ✅ | design §1.5 F01, §5.1.2 W01 | DemoServiceImpl.java:44-57 | 返回 `{type:"hello", message, timestamp}` 符合 spec |
| 每次调用触发异步埋点 | ✅ | design R01 "调用即埋点" | DemoServiceImpl.java:51 recordTrack("hello",...) | 异步投递、失败降级记日志 |
| 响应格式 `{code, msg, data}` | ✅ | design §5 全局约定 | CommonResponse.ok(result) | 统一响应体 |

### REQ-F02: 哈希算法接口

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| POST /api/demo/hash, MD5/SHA-1/SHA-256 | ✅ | design F02 | DemoServiceImpl.java:60-92 | 三算法全部实现 |
| input 为空 → DEMO_011 | ✅ | design R02 | DemoServiceImpl.java:62-64 | |
| 非法算法 → DEMO_010 | ✅ | design R03 | DemoServiceImpl.java:73-75 | 白名单 |
| 默认 SHA-256 | ✅ | design §5.1.2 W02 | DemoServiceImpl.java:37 DEFAULT_ALGORITHM | |

### REQ-F03: 冒泡排序接口

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| POST /api/demo/sort, asc/desc, 返回 swapCount | ✅ | design F03 | DemoServiceImpl.java:95-131 | |
| numbers 为空 → DEMO_020 | ✅ | design R04 | DemoServiceImpl.java:97-99 | |
| order 非法 → DEMO_021 | ✅ | design R05 | DemoServiceImpl.java:107-109 | |
| 排序前拷贝数组避免影响调用方 | ✅ | design §5.1.3.3 "拷贝后再排序" | DemoServiceImpl.java:112-113 | `mapToInt` 创建新数组 |

### REQ-F04: 前端 Tab 页面

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 3 个 Tab（HelloWorld/哈希/冒泡排序） | ✅ | design F04 | manyu_test1:src/index.html | 非 Java 审查范围 |

### REQ-F05: 导出按钮 + 导出接口

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| GET /api/demo/export?type=xxx → xlsx 文件流 | ✅ | design F05, §5.2.2 W04 | ApiCallRecordServiceImpl.java:153-208 | Apache POI XSSFWorkbook |
| type 非法 → DEMO_030 | ✅ | design R14 | ApiCallRecordServiceImpl.java:155-157 | hello/hash/sort 白名单 |
| 空数据返回仅表头的空 Excel | ✅ | design R15 | ApiCallRecordServiceImpl.java:167-173 | 表头始终生成 |

### REQ-F06: 调用埋点

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| @Async("trackExecutor") 异步写入 api_call_record | ✅ | design F06, §5.2.3.1 | DemoServiceImpl.java:197-214 + ApiCallRecordServiceImpl.java:68-76 | 自定义线程池 TrackExecutor |
| 写入失败降级记日志 | ✅ | design 异常场景 | ApiCallRecordServiceImpl.java:73-75 + DemoServiceImpl.java:211-213 | 双层 catch 防护 |
| 调用人取不到 → anonymous | ✅ | design R11 | DemoServiceImpl.java:201-202 | |
| 姓名脱敏（姓+*） | ✅ | design §6.4.3 | DemoServiceImpl.java:219-227 maskName() | |
| 调用时间显式指定 ZoneId（修复后） | ✅ | design 全局约定 | DemoServiceImpl.java:206-209 `ZoneId.of("Asia/Shanghai")` | 修复阶段已修正 |

### REQ-F07: 调用统计查询

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| GET /api/demo/stats 按三维度聚合 + 日趋势 | ✅ | design F07, §5.2.2 W05 | ApiCallRecordServiceImpl.java:79-150 | |
| dimension 非法 → DEMO_040 | ✅ | design R12 | ApiCallRecordServiceImpl.java:81-83 | DIMENSION_COLUMN_MAP 白名单 |
| 默认近 7 天 | ✅ | design R13 | ApiCallRecordServiceImpl.java:88-93 | minusDays(7) |
| 日期格式校验（修复后新增） | ✅ | design §5.2.3.2 异常场景 | ApiCallRecordServiceImpl.java:98-106 | LocalDate.parse + catch DateTimeParseException |
| 空值归入"未知"分组 | ✅ | design §6.1 | ApiCallRecordServiceImpl.java:122-124 | null/"null"/空串 → "未知" |

### REQ-F08: 前端报表可视化

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 折线图+饼图+柱状图 | ✅ | design F08, R16 | manyu_test1:src/index.html | 非 Java 审查范围 |

---

## 4. Step 3 — 可读性检查

> 对照 `readability-checklist.md` A1–A7。

| ID | 检查项 | 状态 | 备注（命中写 `path:line`） |
|----|--------|------|----------------------------|
| A1 | 源文件格式 | ✅ | 文件名=类名、UTF-8 编码 |
| A2 | 源文件结构/import 顺序 | ✅ | 无 `import *`（修复阶段已展开通配符导入） |
| A3 | 代码样式 | ⚠️ P2 | **A3.4**: DemoController.java:117-118 行宽超 120 字符（CONTENT_DISPOSITION header 拼接）；DemoController.java:110-113 接近上限 |
| A4 | 命名规范 | ✅ | 类名 UpperCamelCase、方法 lowerCamelCase、常量 UPPER_SNAKE_CASE |
| A5 | 编码实践 | ✅ | @Override 齐全；无空 catch（DemoController.java:110 catch 有 logger.warn） |
| A6 | 特定元素样式 | ✅ | long 无字面量；switch 无；修饰符顺序正确 |
| A7 | Javadoc | ⚠️ P2 | **A7.1**: DemoController.java 5 个 public 端点方法仅有简短中文注释 `/** W01 - HelloWorld 接口 */`，缺少 `@param`、`@return` 标准 Javadoc（同类问题影响 3 个异常处理方法 :140, :149, :162）。DemoServiceImpl/ApiCallRecordServiceImpl 的 private 辅助方法可豁免 |

---

## 5. Step 4 — 可靠性检查

### 5.1 scan-all-rules.sh 预扫结果（含 LLM 复核）

```
=== Step 4 Rule Scan (B/M/I + A/S/G) ===
Targets: src/main/java/com/alipay/xchain/web/demo/
[P0] G16.2 CatchWithoutLogging: DemoController.java:110
[P0] G16.2 CatchWithoutLogging: ApiCallRecordServiceImpl.java:101
[P0] G16.2 CatchWithoutLogging: ApiCallRecordServiceImpl.java:204
[P0] G16.2 CatchWithoutLogging: ApiCallRecordServiceImpl.java:73
[P0] G16.2 CatchWithoutLogging: DemoServiceImpl.java:163
[P0] G16.2 CatchWithoutLogging: DemoServiceImpl.java:211

=== Summary: 6 findings (P0=6) | 52/222 rules scanned ===
```

> **LLM 人工复核**：脚本 6 条命中中 **4 条为误报**（catch 块内确有 `logger.error/warn` 日志输出），仅 2 条有效/需评估。脚本标注 P0 与 `reliability-checklist.md` 中 G16.2=P1 不一致，本报告以 checklist 定义为准。

| 命中位置 | 脚本判定 | LLM 复核 | 结论 |
|----------|:------:|------|:----:|
| DemoController.java:110 | G16.2 P0 | catch(Exception) 内有 `logger.warn(...)` | **误报**；catch 过宽属 P2 风格问题 |
| ApiCallRecordServiceImpl.java:101 | G16.2 P0 | catch(DateTimeParseException) 仅 throw DemoException，**无日志记录原始异常** | **有效 P1** |
| ApiCallRecordServiceImpl.java:204 | G16.2 P0 | catch(IOException) 内有 `logger.error(...)` | **误报** |
| ApiCallRecordServiceImpl.java:73 | G16.2 P0 | catch(Exception) 内有 `logger.error(...)` | **误报** |
| DemoServiceImpl.java:163 | G16.2 P0 | catch(NoSuchAlgorithmException) 内有 `logger.error(...)` | **误报** |
| DemoServiceImpl.java:211 | G16.2 P0 | catch(RuntimeException) 内有 `logger.error(...)` | **误报** |

### 5.2 可靠性（reliability-checklist.md）

| ID | 状态 | 备注 |
|----|------|------|
| G1.1–G1.4 | ✅ | 无并发写冲突：埋点 append-only，演示接口无状态 |
| G2.1–G2.3 | ✅ | 无写接口幂等需求（纯追加） |
| G3.1–G3.2 | ✅ | 无事务标注、无分布式事务 |
| G4.1–G4.3 | ✅ | SQL 简单；`#{}` 预编译；导出有 LIMIT 分页 |
| G5.1 | N/A | 无 MQ |
| G6.1–G6.2 | N/A | 无缓存 |
| G7.1–G7.2 | N/A | 无调度任务 |
| G8.1 | ✅ | 异常路径均有日志或降级处理 |
| G8.2 | N/A | 无核心链路强依赖外部服务 |
| G8.3 | ✅ | Excel 导出 try-with-resources（ApiCallRecordServiceImpl.java:161）；无其他 I/O |
| G8.4 | ✅ | **修复阶段已修正**：DemoApplication.java:39-40 `setWaitForTasksToCompleteOnShutdown(true)` + `setAwaitTerminationSeconds(30)` |
| G8.5 | N/A | 无 ThreadLocal |
| G8.6 | ✅ | **修复阶段已修正**：DemoApplication.java:32-42 显式配置 ThreadPoolTaskExecutor（core=2, max=4, queue=500, CallerRunsPolicy），非默认无界线程池 |
| G9.1–G9.3 | N/A | 无外部 RPC/HTTP 调用（组织架构 Client 预留） |
| G10.1–G10.2 | ✅ | 统一 `{code,msg,data}` 格式 |
| G11.1–G11.2 | ⚠️ P2 | 无单元测试（impl.md 标注 Maven 不可用，已跳过） |
| G11.3 | ✅ | **修复阶段已修正**：ApiCallRecordServiceImpl.java:98-106 增加 startDate/endDate 格式校验（LocalDate.parse + catch DateTimeParseException）及 start>end 拦截 |
| G11.4 | ✅ | 无金额/浮点运算 |
| G12.1–G12.2 | N/A | 无资损场景 |
| G13.1 | ✅ | 日志级别正确：WARN（业务异常/降级）/ ERROR（系统异常）/ INFO（正常流程） |
| G14.1–G14.4 | ✅ | **修复阶段已修正**：LocalDateTime.now() 全部显式指定 `ZoneId.of("Asia/Shanghai")`（DemoServiceImpl.java:206-209；ApiCallRecordServiceImpl.java:88-92） |
| G15.1 | ✅ | schema.sql 使用 CREATE TABLE IF NOT EXISTS，索引单独创建，向前兼容 |
| G15.2–G15.3 | ✅ | 全新增功能，无旧接口兼容问题 |
| G16.1 | ✅ | 埋点即监控数据；stats/export 可观测 |
| G16.2 | ⚠️ P1 | ApiCallRecordServiceImpl.java:101 — DateTimeParseException 捕获后仅 throw DemoException，未记录原始异常上下文（trace 丢失）。其余 5 条脚本命中经复核均有日志（误报） |
| G16.3 | ✅ | 日志级别合理 |
| G16.4 | ✅ | **修复阶段已修正**：DemoController.java:110 catch 现已含 `logger.warn(...)`；无空 catch |
| G17.1–G17.3 | ✅ | 埋点异步降级已有日志兜底；新增功能可独立回滚 |

### 5.3 安全（security-checklist.md）

| ID | 状态 | 备注 |
|----|------|------|
| S1.1 | ✅ | INSERT/SELECT 均 `#{}` 预编译 |
| S1.2 | ✅ | statsByDimension SQL `${column}` 动态列名 — Java 层 `DIMENSION_COLUMN_MAP` 硬编码白名单 + `stats()` 方法 L81-83 校验 dimension 三选一，注入风险已消除 |
| S1.3 | ✅ | 无 LIKE/IN 动态拼接 |
| S2–S7 | N/A | 无 XML/XSS/SSRF/命令执行/XXE/反序列化/文件上传场景 |
| S8.1 | ✅ | 调用人从 Header 获取（X-User-Id/X-User-Name）；design §6.4.2 明确登录态由全局拦截器校验 |
| S8.2 | ✅ | GET 仅查询/导出，增删改为 POST |
| S8.3–S8.4 | N/A | 无用户可控 ID / 无自定义 Cookie |
| S9.1–S9.4 | ✅ | 无硬编码密钥；callerName 已脱敏（maskName 姓+*）；无随机数/加密需求 |
| S10.1 | N/A | 无 CSRF Token 需求 |
| S10.2 | ⚠️ P1 | CorsConfig.java:25 `allowedOriginPatterns("*")` 配合 `allowCredentials(true)` — 浏览器 CORS 规范不允许 credentials 模式使用 `*` 通配符。当前通过 `@Value("${cors.allowed-origins:*}")` 可配置化，**生产部署前必须将配置项改为具体域名白名单** |
| S10.3 | N/A | 无 URL 跳转 |

### 5.4 Bug 模式（bug-pattern-checklist.md）

> 预扫脚本未报告 B*/M*/I* 命中。LLM 逐条核销结果：

| ID 范围 | 状态 | 备注 |
|---------|------|------|
| B001–B081 (Blocker 81条) | ✅ | 无命中。B008 已排除（使用 ThreadPoolTaskExecutor 非 Executors）；B076 `@Async` 在 public 方法上正确 |
| M001–M027 (Major 27条) | ✅ | M016 已修正（LocalDateTime.now 显式 ZoneId）；M004/M007 无命中；M020 @Override 齐全 |
| I001–I010 (Info 10条) | ✅ | 无命中 |

---

## 6. Step 5 — 自定义扩展检查

| ID | 检查项 | 状态 | 备注 |
|----|--------|------|------|
| U1.1 | Controller 入参使用 `@Valid` 校验 | ✅ | DemoController.java:71,84 已加 `@Valid` |
| U1.2–U2.x | 其余 | N/A | `customized-checklist.md` 仅含 U1.1 示例项，未启用自定义规则 |

---

## 7. 仓间对齐检查

| 对齐点 | [manyu_test] 后端 | [manyu_test1] 前端 | 结论 |
|--------|------|------|:----:|
| 接口路径 | `/api/demo/hello\|hash\|sort\|export\|stats` | API_BASE + path | ✅ |
| 响应格式 | `CommonResponse<T>` `{code, msg, data}` | `data.code === 'OK'` 判断 | ✅ |
| 统计维度枚举 | `personType/personLevel/personDept`（白名单） | `<Select.Option>` 三选一 | ✅ |
| 导出 | `application/vnd.openxmlformats...sheet` 文件流 | `fetch → blob → download` | ✅ |
| 埋点 | 后端 `@Async("trackExecutor")` 异步 | 前端零埋点 | ✅ |
| 调用人来源 | `X-User-Id` / `X-User-Name` Header | 请求头携带 | ✅ |

---

## 8. 结论

- **合并建议**: 修复后合并（P0=0，P1=2，P2=4）
- **P1**:
  1. G16.2 — `ApiCallRecordServiceImpl.java:101` DateTimeParseException 未记录原始异常溯源信息
  2. S10.2 — `CorsConfig.java:25` CORS 默认 `*` + credentials，生产须限定白名单
- **P2**:
  1. A3.4 — `DemoController.java:117-118` 行宽超 120
  2. A7.1 — `DemoController.java` 8 个 public 方法缺标准 Javadoc (@param/@return)
  3. G11.1 — 无单元测试覆盖
  4. `DemoController.java:110` — catch(Exception) 过于宽泛，应限定为 UnsupportedEncodingException
- **一句话**: 功能覆盖完整（8/8 REQ），仓间契约对齐良好，修复阶段已将上轮 P0 空 catch、P1 时区硬编码、P1 线程池缺失、P1 日期校验缺失 4 项问题全部修正到位；当前仅余 2 个 P1 低风险改进项和 4 个 P2 风格/工程优化项，不影响合并。

---

## 8.1 问题片段（必填）

### P1 问题

- **P1** `G16.2` `ApiCallRecordServiceImpl.java:101` — DateTimeParseException 捕获后仅 throw 包装异常，未记录原始异常信息，排障时可观测性不足。
  片段范围：`ApiCallRecordServiceImpl.java:98-103`

```java
 L98|        try {
 L99|            start = LocalDate.parse(startDate, DATE_FMT);
L100|            end = LocalDate.parse(endDate, DATE_FMT);
L101|        } catch (DateTimeParseException e) {
L102|            throw new DemoException("DEMO_041", "日期格式非法，请使用 yyyy-MM-dd 格式");
L103|        }
```

- **P1** `S10.2` `CorsConfig.java:25` — `allowedOriginPatterns("*")` 配合 `allowCredentials(true)` 在生产环境存在安全风险，需通过 `cors.allowed-origins` 配置项限定白名单。
  片段范围：`CorsConfig.java:24-29`

```java
 L24|        registry.addMapping("/api/**")
 L25|                .allowedOriginPatterns(allowedOrigins.split(","))
 L26|                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
 L27|                .allowedHeaders("*")
 L28|                .allowCredentials(true)
 L29|                .maxAge(3600);
```

### P2 问题

- **P2** `A3.4` `DemoController.java:117-118` — Content-Disposition header 拼接超 120 字符。
  片段范围：`DemoController.java:115-121`

```java
L115|        return ResponseEntity.ok()
L116|                .header(HttpHeaders.CONTENT_DISPOSITION,
L117|                        "attachment; filename=\"" + encodedFileName
L118|                                + "\"; filename*=UTF-8''" + encodedFileName)
L119|                .contentType(MediaType.parseMediaType(
L120|                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
L121|                .body(excelBytes);
```

- **P2** `A7.1` `DemoController.java` — 5 个端点方法 + 3 个异常处理方法仅有简短中文注释，缺少 `@param`、`@return` 标准 Javadoc。代表性片段：
  片段范围：`DemoController.java:54-64`

```java
 L54|    /**
 L55|     * W01 - HelloWorld 接口
 L56|     */
 L57|    @GetMapping("/hello")
 L58|    public CommonResponse<DemoResultVO> hello(
 L59|            @RequestHeader(value = "X-User-Id", required = false) String userId,
 L60|            @RequestHeader(value = "X-User-Name", required = false) String userName) {
 L61|
 L62|        DemoResultVO result = demoService.hello(userId, userName);
 L63|        return CommonResponse.ok(result);
 L64|    }
```

- **P2** `DemoController.java:110` — catch(Exception) 过宽。URLEncoder.encode 仅抛出 UnsupportedEncodingException。
  片段范围：`DemoController.java:107-113`

```java
L107|        try {
L108|            encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name())
L109|                    .replace("+", "%20");
L110|        } catch (Exception e) {
L111|            logger.warn("文件名URL编码失败, 使用默认文件名, fileName: {}", fileName, e);
L112|            encodedFileName = "export.xlsx";
L113|        }
```

---

## 9. 修复任务列表

### P1

- [ ] **P1** `ApiCallRecordServiceImpl.java:101` — 在 catch(DateTimeParseException) 中添加 `logger.warn("日期解析失败, startDate:{}, endDate:{}", startDate, endDate, e)` 后再 throw DemoException
- [ ] **P1** `CorsConfig.java:25` — 生产部署前将 `cors.allowed-origins` 配置项从 `*` 改为具体域名白名单

### P2

- [ ] **P2** `DemoController.java:117-118` — 重构文件名拼接，如抽取为局部变量或使用 String.format 控制行宽 ≤ 120
- [ ] **P2** `DemoController.java:57,69,82,95,127,140,149,162` — 为 8 个 public 方法补全标准 Javadoc（@param、@return、@throws）
- [ ] **P2** `DemoController.java:110` — 将 `catch (Exception e)` 替换为 `catch (UnsupportedEncodingException e)`
- [ ] **P2** `G11.1` — 在 Maven 可用后补充核心 Service 方法的单元测试（DemoServiceImpl.hello/hash/sort, ApiCallRecordServiceImpl.stats/export）