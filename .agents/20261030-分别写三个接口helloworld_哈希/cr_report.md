# Code Review Report

> **Change** 接口演示与统计报表（HelloWorld + 哈希 + 冒泡排序） · **分支** `task/DEV-966dcd0a-7905-11f1-9649-3b4281182f10-ca9eb1f2-c398-41fa-9d0b-9aae5790d5b1` · **日期** 2026-10-30 · **审查者** AI (dtazziboot-java-code-review v1.1.0)
>
> **等级 P0 / P1 / P2**；G/S 以 checklist 行内定义为准；Bug 模式以 `bug-pattern-checklist.md` 表头为准（Blocker→P0、Major→P1、Info→P2）。已运行 `scan-all-rules.sh` 并并入 §5。

---

## 1. 审查范围

| 项 | 值 |
|----|-----|
| 仓库 | `manyu_test` (后端 15 Java) + `manyu_test1` (前端 1 HTML) |
| `.java` 文件数 | 15 |
| 变更行数 | `+1717` (20 files in manyu_test), `+388` (1 file in manyu_test1) |

| 类/接口 | 路径 | 角色 |
|---------|------|------|
| `DemoApplication` | `src/main/java/.../DemoApplication.java` | 启动类 |
| `CommonResponse` | `src/main/java/.../common/CommonResponse.java` | 统一响应体 |
| `CorsConfig` | `src/main/java/.../common/CorsConfig.java` | CORS 配置 |
| `DemoException` | `src/main/java/.../common/DemoException.java` | 业务异常 |
| `DemoController` | `src/main/java/.../controller/DemoController.java` | REST 控制器 |
| `ApiCallRecordMapper` | `src/main/java/.../dao/mapper/ApiCallRecordMapper.java` | MyBatis Mapper |
| `ApiCallRecordDO` | `src/main/java/.../model/ApiCallRecordDO.java` | 埋点实体 |
| `HashRequest` | `src/main/java/.../request/HashRequest.java` | 哈希请求体 |
| `SortRequest` | `src/main/java/.../request/SortRequest.java` | 排序请求体 |
| `DemoResultVO` | `src/main/java/.../response/DemoResultVO.java` | 演示结果 VO |
| `StatsResultVO` | `src/main/java/.../response/StatsResultVO.java` | 统计结果 VO |
| `DemoService` | `src/main/java/.../service/DemoService.java` | 演示服务接口 |
| `ApiCallRecordService` | `src/main/java/.../service/ApiCallRecordService.java` | 埋点服务接口 |
| `DemoServiceImpl` | `src/main/java/.../service/impl/DemoServiceImpl.java` | 演示服务实现 |
| `ApiCallRecordServiceImpl` | `src/main/java/.../service/impl/ApiCallRecordServiceImpl.java` | 埋点服务实现 |

非 Java 文件（跳过 Step 3–5 逐文件审查，仅做契约/配置检查）：
- `pom.xml`
- `application.yml`
- `ApiCallRecordMapper.xml`
- `schema.sql`
- `[manyu_test1] src/index.html`

---

## 2. 问题计数

| P0 | P1 | P2 |
|----|----|-----|
| 1 | 5 | 6 |

---

## 3. Step 2 — 功能（REQ）

> 来源：design.md §1.5 需求功能清单、§5 功能模块设计。

### REQ-F01: HelloWorld 示例接口

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| GET /api/demo/hello 无参返回消息+时间戳 | ✅ | design.md L57–58: "F01 HelloWorld 示例接口，GET，自动返回消息+时间戳" | DemoController.java:48–55; DemoServiceImpl.java:43–55 | 接口路径、方法、响应体符合 |
| 调用后触发埋点 | ✅ | design.md R01: "调用即埋点" | DemoServiceImpl.java:50 `recordTrack("hello", ...)` | 异步埋点已实现 |
| 响应格式 {code, msg, data} | ✅ | design.md §5.1.2 W01 出参 | CommonResponse.java:23–28 | 统一响应体 |

### REQ-F02: 哈希算法接口

| Scenario | 结果 | ✅/❌/⚠️ | Spec证据 | 代码证据 | 说明 |
|----------|------|---------|----------|----------|------|
| POST /api/demo/hash, 支持 MD5/SHA-1/SHA-256 | ✅ | design.md L57–58, F02 | DemoController.java:60–68; DemoServiceImpl.java:58–91 | 三种算法均已实现 |
| input 为空返回 DEMO_011 | ✅ | design.md R02 | DemoServiceImpl.java:61–63 | 空校验 |
| 不支持的算法返回 DEMO_010 | ✅ | design.md R03 | DemoServiceImpl.java:72–73 | 白名单校验 |
| algorithm 不传时默认 SHA-256 | ✅ | design.md F02 备注 | DemoServiceImpl.java:66–68 | 默认值处理 |

### REQ-F03: 冒泡排序接口

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| POST /api/demo/sort, 支持 asc/desc | ✅ | design.md F03 | DemoController.java:73–81; DemoServiceImpl.java:93–130 | |
| numbers 为空返回 DEMO_020 | ✅ | design.md R04 | DemoServiceImpl.java:96–98 | |
| order 非法返回 DEMO_021 | ✅ | design.md R05 | DemoServiceImpl.java:106–108 | |
| 返回交换次数 swapCount | ✅ | design.md §5.1.2 W03 | DemoServiceImpl.java:121,171–190 | bubbleSort 含交换计数 |
| 排序前拷贝数组（不修改入参） | ✅ | design.md L522 "拷贝后再排序" | DemoServiceImpl.java:111–112 | `mapToInt` 创建新数组 |

### REQ-F04: 前端 Tab 页面

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 3 个 Tab: HelloWorld / 哈希 / 冒泡排序 | ✅ | design.md F04 | index.html: HelloWorldTab / HashTab / SortTab | Tabs 组件已实现 |

### REQ-F05: 导出按钮 + 导出接口

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| GET /api/demo/export?type=hello/hash/sort | ✅ | design.md F05 | DemoController.java:86–105; ApiCallRecordServiceImpl.java:122–175 | |
| type 非法返回 DEMO_030 | ✅ | design.md R14 | ApiCallRecordServiceImpl.java:124–126 | |
| 返回 xlsx 文件流 | ✅ | design.md W04 | ResponseEntity + XSSFWorkbook | |
| 前端导出按钮 + Blob 下载 | ✅ | design.md F05 | index.html: downloadExport() | |

### REQ-F06: 调用埋点

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 每次接口调用异步写入埋点表 | ✅ | design.md R10 | DemoServiceImpl.java:196–213 → ApiCallRecordServiceImpl.java:50–57 `@Async` | |
| 调用人从 Header 获取 | ✅ | design.md R11 | DemoController.java:50–51 `X-User-Id`/`X-User-Name` | |
| 取不到时记 anonymous | ✅ | design.md R11 | DemoServiceImpl.java:200 | `userId != null ? userId : "anonymous"` |
| 写入失败降级记日志 | ✅ | design.md 异常场景 | ApiCallRecordServiceImpl.java:55–57 有 `logger.error` | ✅ 已正确记录日志 |

### REQ-F07: 调用统计查询

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| GET /api/demo/stats?dimension=personType/personLevel/personDept | ✅ | design.md F07 | DemoController.java:110–118; ApiCallRecordServiceImpl.java:60–119 | |
| dimension 非法返回 DEMO_040 | ✅ | design.md R12 | ApiCallRecordServiceImpl.java:63–65 | 白名单校验 |
| 默认近 7 天 | ✅ | design.md R13 | ApiCallRecordServiceImpl.java:70–75 | `minusDays(7)` |
| 分桶 + 日趋势 | ✅ | design.md W05 出参 | statsByDimension + dailyTrend | |

### REQ-F08: 前端报表可视化

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 折线图(趋势) + 饼图(占比) + 柱状图(对比) | ✅ | design.md R16 | index.html: LineChart/PieChart/BarChart | Recharts 组件 |
| 维度选择器 + 日期范围 | ✅ | design.md R17 | index.html: ChartsSection | Select + DatePicker |
| 数据为空展示空状态 | ✅ | design.md 异常场景 | index.html: Empty 组件 | |

---

## 4. Step 3 — 可读性检查

> 对照 `readability-checklist.md` A1–A7。

| ID | 检查项 | 状态 | 备注 |
|----|--------|------|------|
| A1 | 源文件格式 | ✅ | 文件名=类名、UTF-8 编码 |
| A2 | 源文件结构/import 顺序 | ⚠️ P2 | **A2.2**: 3 处通配符 import。<br>`DemoController.java:17` `import org.springframework.web.bind.annotation.*;`<br>`ApiCallRecordServiceImpl.java:8` `import org.apache.poi.ss.usermodel.*;`<br>`ApiCallRecordServiceImpl.java:22` `import java.util.*;` |
| A3 | 代码样式 | ⚠️ P2 | **A3.4**: 5 处行宽超 120 字符。<br>`DemoController.java:93` (文件名拼接，~130 字符)<br>`DemoController.java:102` (CONTENT_DISPOSITION header, ~135 字符)<br>`DemoController.java:103` (contentType, ~122 字符)<br>`DemoController.java:133` (异常处理器参数类型全限定名, ~130 字符)<br>`ApiCallRecordServiceImpl.java:155` (callTime 格式化, ~122 字符) |
| A4 | 命名规范 | ✅ | 类名 UpperCamelCase、方法 lowerCamelCase、常量 UPPER_SNAKE_CASE |
| A5 | 编码实践 | ✅ | `@Override` 正确使用、无空 catch（见 Step 4 例外） |
| A6 | 特定元素样式 | ✅ | 数组方括号在类型侧、修饰符顺序正确、long 无字面量 |
| A7 | Javadoc | ✅ | public 类和方法均有 Javadoc |

---

## 5. Step 4 — 可靠性检查

### 5.1 自动化预扫结果（scan-all-rules.sh）

```
=== 预扫摘要 (20 findings) ===
[P0] G16.2 CatchWithoutLogging: DemoController.java:97
[P0] G16.2 CatchWithoutLogging: ApiCallRecordServiceImpl.java:171
[P0] G16.2 CatchWithoutLogging: ApiCallRecordServiceImpl.java:55
[P0] G16.2 CatchWithoutLogging: DemoServiceImpl.java:162
[P0] G16.2 CatchWithoutLogging: DemoServiceImpl.java:210
[P1] M016 JavaTimeDefaultTimeZone: DemoController.java:93, ApiCallRecordServiceImpl.java:71,74, DemoServiceImpl.java:205,207,208
[P1] S10.2 CorsWildcard: CorsConfig.java:19
[P2] A2.2 WildcardImport: DemoController.java:17, ApiCallRecordServiceImpl.java:8,22
[P2] A3.4 LineWidthExceeded: DemoController.java:93,102,103,133; ApiCallRecordServiceImpl.java:155
```

> **人工复核**：5 处 G16.2 中，4 处为预扫误报（catch 块内已有 `logger.error(...)` 日志），仅在 DemoController.java:97 为真正空 catch。M016 6 处命中均为 `LocalDateTime.now()` 未指定 ZoneId。

### 5.2 可靠性（reliability-checklist.md）

| ID | 状态 | 备注 |
|----|------|------|
| G1.1–G1.4 | N/A | 无并发写冲突（埋点 append-only，演示接口无状态） |
| G2.1–G2.3 | N/A | 无重试/消息消费场景 |
| G3.1–G3.2 | N/A | 无事务标注、无分布式事务 |
| G4.1–G4.3 | ✅ | SQL 简单无子查询；索引使用 `#{}` 预编译；分页有 LIMIT |
| G5.1 | N/A | 无 MQ |
| G6.1–G6.2 | N/A | 无缓存 |
| G7.1–G7.2 | N/A | 无调度任务 |
| G8.1 | ✅ | 异常路径均有日志（除 DemoController.java:97） |
| G8.2 | N/A | 无核心链路强依赖 |
| G8.3 | ✅ | Excel 导出使用 try-with-resources (ApiCallRecordServiceImpl.java:130) |
| G8.4 | ⚠️ P1 | `@EnableAsync` 使用默认线程池，未配置自定义 ThreadPoolTaskExecutor 的 shutdown 策略 |
| G8.5 | N/A | 无 ThreadLocal 使用 |
| G8.6 | ⚠️ P1 | `@Async` 默认使用 `SimpleAsyncTaskExecutor`（无界线程池），建议显式配置 `ThreadPoolTaskExecutor` |
| G9.1–G9.3 | N/A | 无外部 RPC/HTTP 调用（组织架构 Client 为预留接口） |
| G10.1–G10.2 | ✅ | 响应体字段含义清晰，`code`/`msg`/`data` 结构统一 |
| G11.1–G11.2 | N/A | 无单元测试文件（impl.md 标注 Maven 不可用，已跳过） |
| G11.3 | ⚠️ P1 | stats 接口 `startDate`/`endDate` 无格式校验与范围校验（start>end 未拦截）—— `ApiCallRecordServiceImpl.java:70–78` |
| G11.4 | ✅ | 无金额/浮点运算 |
| G12.1–G12.2 | N/A | 无资损场景 |
| G13.1 | ✅ | 日志级别正确：业务异常 WARN、系统错误 ERROR、正常 INFO |
| G14.1–G14.4 | ⚠️ P1 | **M016**: `LocalDateTime.now()` 未指定 ZoneId（5 处）—— 虽为演示项目但建议显式指定时区 |
| G15.1 | ✅ | schema.sql 为 CREATE TABLE IF NOT EXISTS + 新增列有 DEFAULT |
| G15.2–G15.3 | N/A | 新增功能，无旧接口兼容问题 |
| G16.1 | ✅ | 埋点本身即监控数据；统计接口可观测 |
| G16.2 | ⚠️ P1 | 预扫评估 5 处中 4 处为误报（均有日志），实际仅 DemoController.java:97 缺日志（标 P0 见 G16.4） |
| G16.3 | ✅ | 级别合理 |
| G16.4 | ❌ **P0** | `DemoController.java:97` — catch 块吞异常（无日志、无补救），违反空 catch 禁令 |
| G17.1–G17.3 | ✅ | 埋点降级已有日志兜底 |

### 5.3 安全（security-checklist.md）

| ID | 状态 | 备注 |
|----|------|------|
| S1.1 | ✅ | INSERT/SELECT 均使用 `#{}` 预编译 |
| S1.2 | ⚠️ P1 | `ApiCallRecordMapper.xml:30,33` 使用 `${column}` 动态列名（`statsByDimension` SQL）—— Java 层 `DIMENSION_COLUMN_MAP` 白名单映射提供了防护，但仍需注意 |
| S1.3 | ✅ | 无 LIKE/IN 动态拼接 |
| S2.1–S2.3 | N/A | 后台接口返回 JSON，无 HTML 输出 |
| S3.1–S3.3 | N/A | 无外部 URL 请求 |
| S4.1–S4.2 | N/A | 无命令执行 |
| S5.1–S5.2 | N/A | 无 XML 解析 |
| S6.1–S6.3 | N/A | 无反序列化定制 |
| S7.1–S7.3 | N/A | 无文件上传；导出为内存生成 |
| S8.1 | ✅ | 登录态通过统一拦截器；调用人从 Header 获取 |
| S8.2 | ✅ | 增删改均为 POST，GET 仅用于查询/导出 |
| S8.3 | N/A | 无用户可控 ID |
| S8.4 | N/A | 无自定义 Cookie |
| S9.1 | ✅ | 无硬编码密钥 |
| S9.2 | ✅ | 日志中 callerName 已脱敏（`maskName`） |
| S9.3 | N/A | 演示项目 |
| S9.4 | N/A | 无随机数生成 |
| S10.1 | N/A | 无 CSRF Token |
| S10.2 | ❌ **P1** | `CorsConfig.java:19` — `allowedOriginPatterns("*")` 允许任意来源，生产环境需限定白名单 |
| S10.3 | N/A | 无 URL 跳转 |

### 5.4 Bug 模式（bug-pattern-checklist.md）

> 预扫脚本未报告 B*/M*/I* 命中（除 M016 跨分类已纳入 §5.2 G14）。LLM 逐条核销结果：

| ID 范围 | 状态 | 备注 |
|---------|------|------|
| B001–B081 (Blocker 81条) | ✅ | 无命中：无字面量解析、无数组误用、无线程池反模式（除 G8.6 已覆盖）、无 equals 反模式、无无界递归等 |
| M001–M027 (Major 27条) | ✅ | M016 已在 G14 核销；其余无命中 |
| I001–I010 (Info 10条) | ✅ | 无命中 |

---

## 6. Step 5 — 自定义扩展检查

| ID | 检查项 | 状态 | 备注 |
|----|--------|------|------|
| U1.1 | Controller 入参使用 `@Valid` 校验 | ✅ | `DemoController.java:62,75` 已加 `@Valid` |

> 其余 U* 条目：`N/A(未启用自定义规则)` — `customized-checklist.md` 仅含 U1.1 示例项。

---

## 7. 仓间对齐检查

| 对齐点 | [manyu_test] 后端 | [manyu_test1] 前端 | 结论 |
|--------|------|------|:----:|
| 接口路径 | `/api/demo/hello\|hash\|sort\|export\|stats` | `API_BASE + path` | ✅ |
| 响应格式 | `CommonResponse<T>` `{code, msg, data}` | `data.code === 'OK'` 判断 | ✅ |
| 统计维度枚举 | `personType/personLevel/personDept`（白名单） | `<Select.Option>` 三选一 | ✅ |
| 导出 | `application/vnd.openxmlformats...sheet` 文件流 | `fetch → blob → download` | ✅ |
| 埋点 | 后端 `@Async` 异步写入 | 前端零埋点代码 | ✅ |
| 调用人来源 | `X-User-Id` / `X-User-Name` Header | 请求头自动携带 | ✅ |

---

## 8. 结论

- **合并建议**：修复 P0 后合并
- **P0**：1. DemoController.java:97 — 空 catch 吞异常，需加日志
- **P1**：
  1. CorsConfig.java:19 — CORS 全通配，生产需限定
  2. ApiCallRecordMapper.xml:30,33 — `${column}` 动态列名（已有白名单防护，风险可控）
  3. ApiCallRecordServiceImpl.java:70–78 — 缺少 startDate/endDate 格式与范围校验
  4. DemoServiceImpl.java 等 — LocalDateTime.now() 未指定 ZoneId（5 处）
  5. @Async 使用默认线程池，建议显式配置 ThreadPoolTaskExecutor
- **P2**：通配符 import（3 处）、行宽超限（5 处）、SortRequest 冗余注解、导出硬编码 limit、catch Exception 过宽

- **一句话**：功能覆盖度完整（8 个 REQ 全部满足），仓间契约对齐良好，存在 1 个阻塞级空 catch 和若干安全/可靠性改进项，修复后可合并。

---

## 7.1 问题片段（必填）

### P0 问题

- **P0** `G16.4` `src/main/java/com/alipay/xchain/web/demo/controller/DemoController.java:97` — catch 块吞异常，无日志、无告警，不符合空 catch 禁令。
  片段范围：`DemoController.java:93-99`

```java
L93|        String fileName = "api_demo_" + type + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
L94|        String encodedFileName;
L95|        try {
L96|            encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()).replace("+", "%20");
L97|        } catch (Exception e) {
L98|            encodedFileName = "export.xlsx";
L99|        }
```

### P1 问题

- **P1** `S10.2` `src/main/java/com/alipay/xchain/web/demo/common/CorsConfig.java:19` — `allowedOriginPatterns("*")` 允许任意来源跨域请求。
  片段范围：`CorsConfig.java:17-23`

```java
L17|    public void addCorsMappings(CorsRegistry registry) {
L18|        registry.addMapping("/api/**")
L19|                .allowedOriginPatterns("*")
L20|                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
L21|                .allowedHeaders("*")
L22|                .allowCredentials(true)
L23|                .maxAge(3600);
```

- **P1** `S1.2` `src/main/resources/mapper/ApiCallRecordMapper.xml:30,33` — `${column}` 动态列名拼接（Java 层 `DIMENSION_COLUMN_MAP` 白名单已提供防护）。
  片段范围：`ApiCallRecordMapper.xml:29-35`

```xml
L29|    <select id="statsByDimension" resultType="java.util.HashMap">
L30|        SELECT ${column} AS name, COUNT(*) AS callCount
L31|        FROM api_call_record
L32|        WHERE call_time BETWEEN #{startTime} AND #{endTime}
L33|        AND ${column} IS NOT NULL AND ${column} != ''
L34|        GROUP BY ${column}
L35|        ORDER BY callCount DESC
```

- **P1** `G11.3` `src/main/java/com/alipay/xchain/web/demo/service/impl/ApiCallRecordServiceImpl.java:70-78` — stats 接口未校验 startDate/endDate 日期格式合法性及 start > end 非法范围。
  片段范围：`ApiCallRecordServiceImpl.java:69-78`

```java
L69|        // 默认时间窗：近 7 天
L70|        if (startDate == null || startDate.isEmpty()) {
L71|            startDate = LocalDate.now().minusDays(7).format(DATE_FMT);
L72|        }
L73|        if (endDate == null || endDate.isEmpty()) {
L74|            endDate = LocalDate.now().format(DATE_FMT);
L75|        }
L76|
L77|        String startTime = startDate + " 00:00:00";
L78|        String endTime = endDate + " 23:59:59";
```

- **P1** `M016` `src/main/java/com/alipay/xchain/web/demo/service/impl/DemoServiceImpl.java:205-208` — `LocalDateTime.now()` 未指定 ZoneId（同类问题共 5 处）。
  片段范围：`DemoServiceImpl.java:204-211`

```java
L204|            record.setPersonDept("");
L205|            record.setCallTime(java.time.LocalDateTime.now());
L206|            record.setResultStatus(status);
L207|            record.setGmtCreate(java.time.LocalDateTime.now());
L208|            record.setGmtModified(java.time.LocalDateTime.now());
L209|            apiCallRecordService.record(record);
L210|        } catch (Exception e) {
L211|            logger.error("埋点记录失败, apiName: {}, userId: {}", apiName, userId, e);
```

---

## 8. 修复任务列表

### P0

- [ ] **P0** `DemoController.java:97` — 在 catch 块中增加 `logger.warn("文件名编码失败, 使用默认文件名", e)` 日志

### P1

- [ ] **P1** `CorsConfig.java:19` — 将 `allowedOriginPatterns("*")` 改为限定白名单（开发环境可用配置项注入）
- [ ] **P1** `ApiCallRecordServiceImpl.java:70-78` — 增加 startDate/endDate 格式校验（try-catch LocalDate.parse）及 start > end 合法性检查
- [ ] **P1** `DemoServiceImpl.java:205,207,208` 等 5 处 — `LocalDateTime.now()` 改为 `LocalDateTime.now(ZoneId.of("Asia/Shanghai"))` 或注入 Clock
- [ ] **P1** `DemoApplication.java` — 显式配置 `ThreadPoolTaskExecutor` 替换 `@Async` 默认无界线程池

### P2

- [ ] **P2** `DemoController.java:17` — 展开 `import org.springframework.web.bind.annotation.*` 为具体注解导入
- [ ] **P2** `ApiCallRecordServiceImpl.java:8,22` — 展开通配符 import
- [ ] **P2** `DemoController.java:93,102,103,133` 等 — 长行换行（≤120 字符）
- [ ] **P2** `SortRequest.java:17` — 移除冗余 `@NotNull`（`@NotEmpty` 已隐含）
- [ ] **P2** `ApiCallRecordServiceImpl.java:128` — 导出 limit 1000 改为可配置项
- [ ] **P2** `DemoServiceImpl.java:210` — catch `Exception` 改为更具体的异常类型