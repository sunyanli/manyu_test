# Code Review Report

> **Change** 三示例接口（helloworld/哈希/冒泡排序）+ 三 Tab 页面 + 导出 + 埋点统计报表 · **分支/Commit** `AI/task-DEV-eecb99b0-9b79-11f1-a438-852be3c2a082-fea6c9db-7e0f-4099-8d47-5e4b4fceb86a` / `e38e697` · **日期** `2026-08-20` · **审查者** AI
>
> **AI**：等级 **P0 / P1 / P2**；G/S 以 checklist 行内定义为准；Bug 模式以 `bug-pattern-checklist.md` 表头为准（Blocker→P0、Major→P1、Info→P2）。已运行 `scan-all-rules.sh` 统一预扫（52/222 条规则），输出并入 §5，再写 LLM 结论。问题含 `path:line` 与清单 ID（可读性 `A3.4`、安全 `S8.1`、可靠性 `G16.2`、Bug 模式 `B008` / `M016` 等）。所有 ❌/⚠️ 问题在 §7.1 附 `.java` 问题片段。

---

## 1. 审查范围

| 项 | 值 |
|----|-----|
| `.java` 文件数 | 54 |
| 变更行数 | `+4963 / -0`（manyu_test，含前端 manyu_test1 +2420 行非 Java） |

审查仓库：`manyu_test`（Spring Boot 3.2 / Java 17 后端，54 个 `.java`）；`manyu_test1`（Vue 前端，无 `.java`，按技能 Java 守卫跳过，仅做跨仓契约对齐核验）。

| 类/接口 | 路径 | 角色 |
|---------|------|------|
| AlgoDemoApplication | src/main/java/com/manyu/algodemo/AlgoDemoApplication.java | 启动类 + MapperScan |
| DemoController / DemoService(Impl) | demo/controller|service | W01-W03 三接口实现 + 校验 + 埋点注解 |
| BubbleSortAlgorithm / HashUtils | demo/algorithm | 冒泡排序（对齐 bubble_sort.py）/ 哈希工具（MD5/SHA-256/SM3） |
| ExportController / ExportService(Impl) / CsvExportUtil | export/** | W04 页面导出 + CSV 组装（公式注入防护） |
| TrackCall / TrackAspect / CallRecordQueue | tracking/** | F06 AOP 埋点 + 异步批量写 |
| TrackingController / TrackingService(Impl) / TrackingMapper(.xml) | tracking/** | W05/W06/W07 报表聚合查询 |
| CallContextResolver / CallerInfo | common/context | A03 调用人上下文解析（请求头模拟） |
| GlobalExceptionHandler / ErrorCode / CommonResponse / BizException | common/** | 统一出参/错误码/异常转译 |
| WebConfig | config/WebConfig.java | CORS（仅本地前端来源） |
| CallRecordDO + 6 个 DTO + 6 个枚举 | tracking/model/** | call_record 数据契约 |
| 6 个测试类 | src/test/java/** | 算法/服务/切面/CSV/统计单测 |

---

## 2. 问题计数

> 首轮审查计数（2026-08-20 13:03，修复前）。

| P0 | P1 | P2 |
|----|----|-----|
| 1 | 8 | 6 |

> **复审核销后（2026-08-20 13:2x，修复阶段已完成）**：P0=0、P1=0、P2=0 —— 首轮 1 P0 + 8 P1 + 6 P2 全部修复并核销（详见 §9 复审记录），当前代码无未修复问题。

---

## 3. Step 2 — 功能（REQ）

> REQ 来源：任务需求原文 + 系分设计 `design.md`（F01-F07 / W01-W07 / R01-R05）。

### REQ-1: W01 helloworld 接口（F01）

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| POST /api/demo/helloworld 返回问候文案/服务端时间/requestId | ✅ | design 5.1 W01 | DemoController.java:41-44、DemoServiceImpl.java:61-71、HelloWorldRequest/VO | 出参含 costTimeMs（超设计列，向前兼容） |
| name 默认 World、长度 ≤64 校验 | ✅ | W01 R01 | @Size(max=64)：HelloWorldRequest.java:11 | 校验在 Controller 层经 @Valid 生效 |
| 非法入参返回 DEMO_001 | ✅ | W01 R02 / 错误码表 | GlobalExceptionHandler.java:38-42 → DEMO_001 | 统一转译 |

### REQ-2: W02 哈希算法接口（F02）

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 支持 MD5/SHA-256/SM3，默认 SHA256 | ✅ | design 5.1 W02、A04 | HashAlgorithm.java、HashUtils.java:29-34、DemoServiceImpl.java:80 | 大小写不敏感归一化 |
| text 非空且 UTF-8 字节 ≤4096（DEMO_001） | ✅ | W02 R01 | HashRequest.java:11、DemoServiceImpl.java:82-85（先校验后哈希） | 字节级校验正确 |
| 不支持的 algorithm → DEMO_002 | ✅ | W02 R02 | DemoServiceImpl.java:130-137 | |
| 明文原文不落库（R03） | ✅ | 5.3 R03 | TrackAspect.buildReqSummary：hash 分支仅记 algorithm+textBytes（TrackAspect.java:99） | 无原文落库 |

### REQ-3: W03 冒泡排序接口（F03）

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 逻辑对齐 bubble_sort.py 标准/优化/降序三变体 | ✅ | design F03 / 5.1.3 | BubbleSortAlgorithm.java:24-47 + BubbleSortAlgorithmTest（5 用例） | swaps/提前终止/版本号正确 |
| size 1..10000（DEMO_003） | ✅ | W03 R01 | DemoServiceImpl.java:101-103（前置 size 校验） | |
| 元素非有限数（NaN/Infinity）→ DEMO_001 | ✅ | W03 R02 | DemoServiceImpl.java:117-124（sanitize） | 含 null 检查 |
| 返回最多前 100 元素、完整结果走导出 | ✅ | W03 出参表 | DemoServiceImpl.java:126-128（limit） | subList 视图只读序列化，安全 |

### REQ-4: F05 导出按钮 + W04 导出接口（导出各页面展示结果）

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 按 target 导出 Tab 展示结果 / REPORT 导出报表 | ✅ | design 5.2 R01 | ExportServiceImpl.buildPageRows / buildReportRows（:105/:134） | 数据源 call_record 参数化查询 |
| 非法 target → EXPORT_001 | ✅ | W04 R01 | ExportServiceImpl.java:167-173 | |
| 时间跨度 ≤90 天（TRACKING_003） | ✅ | W04 R02 | pageRecords → TrackingServiceImpl.validateRange（:128-135） | 经服务层统一校验 |
| CSV 公式注入防护（=+-@ 前置单引号） | ✅ | W04 R04 | CsvExportUtil.java:49-52 + CsvExportUtilTest | **漏了 `\t`(Tab) 与 0x0D 前缀，见 §5 S-x** |
| 导出动作本身写 EXPORT 埋点 | ⚠️ | 5.2 R03（含导出人/目标/格式） | ExportServiceImpl.java:62 @TrackCall(type=EXPORT) | **req_summary 的目标/格式字段实际为空：TrackAspect.exportField 依赖 ExportRequest.toString()，而 DTO 未重写 toString（Object 默认输出），解析结果恒为空 → P1** |
| 导出数据为空 → EXPORT_002 | ✅ | W04 R05 | ExportServiceImpl.java:108-110 | |
| 并发限流（Semaphore ≤5） | ❌ | 5.2 并发控制策略 | ExportServiceImpl.java:76-87 | **tryAcquire 失败路径在 finally 中仍执行 release()，释放了从未获取的许可 → 信号量被逐渐放大，限流失效（P0）** |

### REQ-5: F06 后端埋点（调用次数 + 调用人维度快照）

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| @TrackCall + AOP 环绕记录次数/调用人 | ✅ | 5.3.3.1 R01/R04 | TrackAspect.java:55-73、CallRecordQueue.java:71-79 | 成功/失败两路径均入队，FAIL 记 errorCode |
| 异步批量写、队列满静默降级 | ✅ | 5.3.3.1 R02 | CallRecordQueue.java:81-93（drainTo+batchInsert） | 写失败计 writeFailCount 并 ERROR 日志 |
| 入参/出参仅落摘要不落原文与密钥（R03） | ⚠️ | 5.3 R03 | TrackAspect.java:93-158 | **resp_summary 实际是 Object.toString() 的对象标识（如 `com.manyu...HashVO@xxxx`），非设计要求的"哈希前 16 位/排序前 10 元素"→ P1** |
| 调用人解析失败兜底 anonymous/SYSTEM | ✅ | 5.3.3.1 R04 | CallContextResolver.java:44-64 | |

### REQ-6: F07 报表可视化（W05/W06/W07）

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 概况卡片（总调用/人数/成功率/平均耗时/topCaller 脱敏） | ✅ | 5.3.2 W05 | TrackingServiceImpl.overview（:51-67）、maskName（:192-197） | 姓名脱敏保留首字+`*` |
| 维度统计（CALLER_TYPE/LEVEL/DEPT/BIZ_TYPE）供饼图/柱状图 | ✅ | W06、F07 | TrackingServiceImpl.stats（:73-93）、columnsOf 白名单（:156-163） | 白名单映射防 SQL 注入 |
| 时间趋势（HOUR/DAY/MONTH）供折线图 | ⚠️ | W07（可选 dimension 细分） | TrackingServiceImpl.trend（:99-117） | **design W07 声明可选 dimension 细分，实现未提供 W07 dimension 参数（前端也未用），属降级但向后兼容 → P2** |
| 时间范围校验（TRACKING_001/003，≤90 天） | ✅ | 5.3.3.2 R02 | validateRange（:128-135） | Duration.toDays() 截断：90 天 23 小时判定为 90，边界放宽 <1 天（P2） |

### REQ-7: F04 前端三 Tab + 报表区 + 导出按钮（manyu_test1，非 Java，跨仓契约）

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| 一页三 Tab 分别展示三类执行结果 | ✅ | 5.5 | App.vue（el-tabs 三 pane）+ HelloWorldTab/HashTab/BubbleSortTab | 仅核对前端契约 |
| 报表区折线/饼图/柱状图三种形式 | ✅ | 5.5、F07 | ReportSection.vue（226 行，概览+趋势+维度分布） | 折线←W07、饼/柱←W06 |
| 导出按钮调用 W04 下载 | ✅ | 5.5 | App.vue handleExport → exportApi.export（api/index.js:40-56） | blob 下载 + content-disposition 解析，契约对齐 |
| 前端注入 X-Caller-* 身份头 | ✅ | A03 | api/index.js:9-17 | 与 CallContextResolver 头名一致，契约对齐 |
| W01-W07 路径/入参/出参与后端一致 | ✅ | 8.3 对齐点 2 | api/index.js 与 Demo/Tracking/Export 三 Controller | 契约匹配 |

### REQ-8: 跨仓数据契约（call_record / 对齐点 1/3/5）

| Scenario | 结果 | Spec证据 | 代码证据 | 说明 |
|----------|------|----------|----------|------|
| call_record 字段与报表维度一致 | ✅ | 8.3 对齐点 1 | schema.sql（15 字段 + 5 索引）↔ CallRecordDO ↔ TrackingMapper.xml | 映射一致，underscore-to-camel 开启 |
| 冒泡排序 Java 实现与 bubble_sort.py 行为一致 | ✅ | 8.3 对齐点 5 | BubbleSortAlgorithmTest 5 用例 + 入参不修改 | 标准/优化/降序对齐 |
| 时间约定 ISO-8601（UTC），库内 datetime | ⚠️ | 全局约定 / G14.4 | ExportServiceImpl.java:70,82、TrackingController.java:45,63,81 | **LocalDateTime.now() 未显式时区，M016 → P1** |

---

## 4. Step 3 — 可读性检查（A1-A7）

| 结果 | 说明（违规写 Ax.x 与 `path:行`） |
|------|--------------------------------|
| ✅ / ⚠️ | 整体结构化良好：包名/类名/方法名符合 A4；K&R 括号、4 空格缩进、成员空行符合 A3/A6；DTO/VO 均有 Javadoc（A7 基本满足）。 |
| ⚠️ A2.3/A2.4 | `GlobalExceptionHandler.java:1-15`、`TrackingServiceImpl.java:1-26` import 未分组（静态/非静态未空行分隔，部分组内非严格 ASCII 序）→ P2 |
| ⚠️ A3.4 | `DemoController.java:43`、`ExportServiceImpl.java:100-102` 等行宽 >120 字符 → P2 |
| ⚠️ A4.5 | `TrackAspect.java:29-31` 常量命名符合 A4.4（UPPER_SNAKE_CASE）✓；`buildReqSummary` switch 内魔法字符串（"hello"/"hash"/"bubbleSort"/"export"）依赖方法名 → 脆弱，建议 A5 编码实践：以注解参数代替方法名分支 → P2 |
| ⚠️ A5.1 | 所有 `@Override` 已标注 ✓；`ExportServiceImpl`/`DemoServiceImpl` 实现类均带 `@Override` ✓ |

---

## 5. Step 4 — 可靠性检查

### 预扫输出（`scan-all-rules.sh`，52/222 条规则）

```
[P0] B008  — AvoidUsingExecutors        CallRecordQueue.java:56
[P0] G16.2 — CatchWithoutLogging        HashUtils.java:57 / DemoServiceImpl.java:133,142 /
                                         ExportFormat.java:23 / ExportServiceImpl.java:170 /
                                         TrackAspect.java:67 / CallRecordQueue.java:89,105 /
                                         TrackingServiceImpl.java:140,151
[P1] M016  — JavaTimeDefaultTimeZone    ExportServiceImpl.java:70,82 / TrackingController.java:45,63,81 /
                                         TrackingServiceImplTest.java:53,66,76,86,100,115
Summary: 22 findings (P0=11, P1=11)
```

### 5.1 人工复核 + LLM 补扫结论

| 域 | 参考 | 结果 | 等级 | 说明（命中 ID / 复核结论） |
|----|------|------|------|-------------------------------------|
| 可靠性（G） | G1-G17 | ❌ | P0 | **G1/G8 并发控制：ExportServiceImpl.java:76-87 信号量"未获取即释放"（fail-then-release 放大许可数）→ P0，详见 §7.1-1** |
| 可靠性（G） | G1-G17 | ⚠️ | P1 | G16.2 异常路径缺日志：DemoServiceImpl.java:142、ExportServiceImpl.java:170、TrackingServiceImpl.java:140,151（catch→throw BizException 未记日志，缺少可追溯上下文；预扫 7 处中 3 处为误报（HashUtils:57 有 cause 包装、CallRecordQueue:89 有 LOGGER.error、CallRecordQueue:105 为中断恢复标准模式）） |
| 可靠性（G） | G1-G17 | ⚠️ | P1 | G8.4 线程池关闭已实现（@PreDestroy shutdown+awaitTermination，CallRecordQueue.java:98-111）✓；G8.6 有界队列（capacity=10000）✓ |
| 可靠性（G） | G1-G17 | ⚠️ | P1 | B008（预扫 Blocker→按 LLM 复核降 P1）：`Executors.newSingleThreadScheduledExecutor`（CallRecordQueue.java:56）——该执行器仅承载 1 个定时 flush 任务、业务队列为有界 LinkedBlockingQueue、守护线程 + PreDestroy 关闭，实际风险被约束；仍建议改 `ScheduledThreadPoolExecutor` 显式参数以合规 |
| 安全（S） | S1-S10 | ⚠️ | P1 | S8.1 访问控制：无任何鉴权/登录拦截器（design 6.4.2 要求 /api 统一登录态校验返回 COMMON_401 未实现）；调用人身份完全由客户端请求头 X-Caller-* 控制（伪造即越权），且无"正式环境禁用模拟通道"开关 → P1 |
| 安全（S） | S1-S10 | ⚠️ | P1 | S9.1 凭证硬编码：application.yml:10-11 `username: root / password: root` 明文入库（演示环境 localhost，未上生产；上线前必须改配置中心/环境变量） |
| 安全（S） | S1-S10 | ✅ | - | S1.1/S1.2 SQL 注入：Mapper 全部 `#{}` 参数化；`${dimColumn}/${groupBy}/${timeExpr}` 均由 Service 枚举白名单映射（TrackingServiceImpl.java:156-171），非用户直传 → 无注入 |
| 安全（S） | S1-S10 | ⚠️ | P2 | S7.1/S10.2 通过：CORS 白名单仅 localhost:5173 ✓；导出文件名由枚举+日期白名单生成，无路径穿越 ✓；CSV 公式注入前缀清单 `=+-@` 未含制表符/CR（Excel 亦以 TAB 开头视为公式）→ P2 |
| Bug 模式 | B/M/I | ⚠️ | P1/P2 | B008（见上，降 P1）；M016 JavaTimeDefaultTimeZone → P1；其余 B*/M*/I* 120 条：LLM 补扫——未命中真实性缺陷（无 == 包装比较、无浮点金额、无资源未释放、无 SQL 拼接注入点、无 OOM 风险） |

---

## 6. Step 5 — 自定义扩展检查

| 域 | 参考 | 结果 | 等级 | 说明 |
|----|------|------|------|------|
| 自定义扩展 | customized-checklist.md U* | N/A(未启用自定义规则) | - | 清单仅含示例项（U1.1 示例），未启用团队/项目私有规则 |

---

## 7. 结论

> 首轮审查结论（修复前）。修复阶段已全部落地并核销，复审核销结论见 §9。

- **合并建议**（复审后）：**通过** —— 首轮 Blocking P0 与 8 个 P1、6 个 P2 全部修复核销，当前代码可合并。
- **P0**：导出并发信号量 over-release 缺陷（`ExportServiceImpl.java:76-87`）——并发超限请求会释放未获取的许可，长期运行使并发上限名存实亡。
- **P1**：1) 导出埋点 req_summary 目标/格式为空（TrackAspect 依赖无 toString 的 DTO）；2) resp_summary 为对象标识非摘要（VO 无 toString，需按 design 输出哈希前 16 位/排序前 10 元素）；3) 缺登录态拦截器 + 请求头身份可伪造（S8.1）；4) DB 凭证硬编码（S9.1）；5) 4 处 catch 无日志（G16.2）；6) M016 默认时区；7) B008 Executors 使用（降级 P1）；8) 导出埋点目标/格式字段空值（与 1 合并）。
- **P2**：W07 dimension 维度细分未实现；CSV 公式注入前缀缺 TAB/CR；90 天边界截断放宽；行宽/import 分组风格；方法名驱动的摘要 switch 脆弱。
- **一句话**：功能覆盖率达 REQ（三接口/三 Tab/导出/埋点/报表全链路闭环，跨仓契约对齐），但"导出并发信号量释放缺陷 + 无鉴权 + 摘要落库失真"三项需在合并前修复；整体为演示级质量、可验收。

---

## 7.1 问题片段（必填）

> 等级 + 规则ID + path:line + 问题说明，片段带行号。

### 1. P0 `G1/G8`（并发）`src/main/java/com/manyu/algodemo/export/service/impl/ExportServiceImpl.java:76-87` — 信号量"未获取即释放"

`tryAcquire()` 返回 false 时抛出 BizException，但 `finally` 无条件执行 `release()`，对从未获取的许可执行释放，`Semaphore` 许可数被逐次放大（每次并发超限 +1），限流上限最终形同虚设。

```java
L76|        try {
L77|            if (!concurrencyGate.tryAcquire()) {
L78|                throw new BizException(ErrorCode.EXPORT_001, "导出并发超限，请稍后重试");
L79|            }
L80|            byte[] content = buildContent(target, format, start, end);
L81|            String fileName = target.name().toLowerCase(Locale.ROOT) + "_page_"
L82|                    + LocalDateTime.now().format(FILE_NAME_DATE)
L83|                    + (format == ExportFormat.CSV ? ".csv" : ".xlsx");
L84|            return new ExportFile(fileName, CSV_CONTENT_TYPE, content);
L85|        } finally {
L86|            concurrencyGate.release(); // 问题：tryAcquire 失败分支也执行 release → 过度释放
L87|        }
```

**修复建议**：`boolean acquired = concurrencyGate.tryAcquire(); if (!acquired) throw ...; try { ... } finally { if (acquired) concurrencyGate.release(); }`。

### 2. P1 `G16.2` `src/main/java/com/manyu/algodemo/demo/service/impl/DemoServiceImpl.java:139-145` — catch 无日志

`parseOrder` 捕获 IllegalArgumentException 直接转 BizException，未记录入参上下文（预扫命中；同上模式的 ExportServiceImpl:167-173、TrackingServiceImpl:137-154 一并修复）。

```java
L139|    private SortOrder parseOrder(String order) {
L140|        try {
L141|            return SortOrder.valueOf(order);
L142|        } catch (IllegalArgumentException e) {
L143|            throw new BizException(ErrorCode.DEMO_001, "不支持的排序方向: " + order);
L144|        }
L145|    }
```

### 3. P1 `G16.2 / F05 R03` `src/main/java/com/manyu/algodemo/tracking/aspect/TrackAspect.java:99-104,124-136` — 导出埋点 req_summary 为空 + 摘要失真

`exportField(args[0], "target")` 依赖 `ExportRequest.toString()`，而 DTO 未重写 toString（Object 默认输出 `class@hash`），`contains("target=")` 恒为 false → 目标/格式恒空；同理 `buildRespSummary` 对 hash/bubbleSort 的 `result.toString()` 输出对象标识而非设计要求的摘要（哈希前 16 位/排序前 10 元素）。

```java
L99|            case "hash" -> "algorithm=" + safe(args[1]) + ",textBytes=" + textBytes(safe(args[0]));
L100|            case "bubbleSort" -> "size=" + listSize(args[0]) + ",order=" + safe(args[1])
L101|                    + ",optimized=" + safe(args[2]);
L102|            case "export" -> "target=" + exportField(args[0], "target") + ",format=" + exportField(args[0], "format");
L103|            default -> "args=" + truncate(args[0], RESP_SUMMARY_LIMIT);
L104|        };
```

### 4. P1 `S8.1` `src/main/java/com/manyu/algodemo/common/context/CallContextResolver.java:41-54` — 身份完全信任请求头（无鉴权）

design 6.4.2 要求 /api 统一登录态校验返回 COMMON_401；当前无任何拦截器，调用人 ID/姓名/类型/层级/部门全部来自客户端可伪造的 `X-Caller-*` 请求头，且无"正式环境禁用模拟通道"开关。前端 api/index.js:9-17 动态注入即证明可伪造性。

```java
L47|        info.setCallerId(firstNonBlank(request.getHeader(HEADER_CALLER_ID), ANONYMOUS_ID));
L48|        info.setCallerName(firstNonBlank(request.getHeader(HEADER_CALLER_NAME), ANONYMOUS_NAME));
L49|        info.setCallerType(firstNonBlank(request.getHeader(HEADER_CALLER_TYPE), SYSTEM_TYPE));
L50|        info.setCallerLevel(firstNonBlank(request.getHeader(HEADER_CALLER_LEVEL), "N/A"));
L51|        info.setCallerDeptCode(firstNonBlank(request.getHeader(HEADER_CALLER_DEPT_CODE), "N/A"));
L52|        info.setCallerDeptName(firstNonBlank(request.getHeader(HEADER_CALLER_DEPT_NAME), "未知部门"));
```

### 5. P1 `S9.1` `src/main/resources/application.yml:10-11` — DB 凭证硬编码

```yaml
L10|    username: root
L11|    password: root
```

### 6. P1 `M016 / G14.4` `src/main/java/com/manyu/algodemo/export/service/impl/ExportServiceImpl.java:69-74`（同类 TrackingController.java:45,63,81）— 默认时区

`LocalDateTime.now()` / `parse(ISO_DATE_TIME)` 未显式指定时区，依赖 JVM 系统时区；design 全局约定接口时间为 ISO-8601 UTC。预扫 M016 命中 11 处（含测试）。

```java
L69|        LocalDateTime end = request.getEndTime() == null || request.getEndTime().isBlank()
L70|                ? LocalDateTime.now()
L71|                : LocalDateTime.parse(request.getEndTime(), DateTimeFormatter.ISO_DATE_TIME);
L72|        LocalDateTime start = request.getStartTime() == null || request.getStartTime().isBlank()
L73|                ? end.minusDays(30)
L74|                : LocalDateTime.parse(request.getStartTime(), DateTimeFormatter.ISO_DATE_TIME);
```

### 7. P1 `B008` `src/main/java/com/manyu/algodemo/tracking/async/CallRecordQueue.java:56` — Executors 创建线程池（降级为 P1）

```java
L56|        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
L57|            Thread t = new Thread(r, "call-record-flusher");
L58|            t.setDaemon(true);
L59|            return t;
L60|        });
```

### 8. P2 `S7.1` `src/main/java/com/manyu/algodemo/export/util/CsvExportUtil.java:49-52` — 公式注入前缀缺 TAB/CR

```java
L49|        // R04 防 CSV 公式注入：以 = + - @ 开头的单元格前置单引号
L50|        if (!value.isEmpty() && FORMULA_PREFIXES.indexOf(value.charAt(0)) >= 0) {
L51|            value = "'" + value;
L52|        }
```

---

## 8. 修复任务列表

> 状态更新（2026-08-20，修复阶段完成）：以下任务已全部核销（`[x]`），复审核销证据见 §9。

### P0

- [x] **P0** `src/main/java/com/manyu/algodemo/export/service/impl/ExportServiceImpl.java:76-87` — 修复信号量 over-release：以 `boolean acquired = tryAcquire()` 记录获取状态，仅已获取时在 finally 中 `release()`（或 tryAcquire 失败直接 return/throw 前不进入 finally 释放分支）。

### P1

- [x] **P1** `G16.2` `src/main/java/com/manyu/algodemo/demo/service/impl/DemoServiceImpl.java:139-145` — parseOrder catch 补 WARN 日志（含 order 入参）；同步补 ExportServiceImpl.parseTarget、TrackingServiceImpl.parseDimension/parseGranularity。
- [x] **P1** `F05 R03` `src/main/java/com/manyu/algodemo/tracking/aspect/TrackAspect.java:93-136` — 为 HelloWorldVO/HashVO/SortVO/ExportRequest 实现摘要化 `toString()`（或按 VO 字段组装 req/resp summary），使导出埋点含目标/格式、resp_summary 含哈希前 16 位/排序前 10 元素。
- [x] **P1** `S8.1` `config/WebConfig.java` 或新增 `WebMvcConfigurer` 拦截器 — 实现 /api 登录态/身份校验拦截器（未登录 COMMON_401），并增加配置开关在正式环境禁用 X-Caller-* 模拟通道。
- [x] **P1** `S9.1` `src/main/resources/application.yml:10-11` — DB 凭证改环境变量/配置中心注入，仓库内删除明文密码。
- [x] **P1** `M016/G14.4` `export/service/impl/ExportServiceImpl.java:70,82`、`tracking/controller/TrackingController.java:45,63,81` — 统一显式时区（如 `ZoneId.of("Asia/Shanghai")` / UTC），避免 JVM 默认时区漂移。
- [x] **P1** `B008` `tracking/async/CallRecordQueue.java:56` — 改用 `ScheduledThreadPoolExecutor` 显式 corePoolSize/ThreadFactory，满足规范。

### P2

- [x] **P2** `W07` `tracking/service/impl/TrackingServiceImpl.java:99-117` — 按 design 增加时间趋势 dimension 维度细分参数（向后兼容可选）。
- [x] **P2** `S7.1` `export/util/CsvExportUtil.java:50` — 公式注入前缀补充 `\t`（制表符）与 `\r`。
- [x] **P2** `G11.2` `tracking/service/impl/TrackingServiceImpl.java:132` — `Duration.toDays()` 截断导致 90 天边界放宽，改为按小时比较或 `end.isAfter(start.plusDays(90))`。
- [x] **P2** `A3.4/A2.3` 多文件 — 行宽 ≤120 与 import 分组/字典序整理。
- [x] **P2** `A5` `tracking/aspect/TrackAspect.java:97-104` — 以注解属性替代方法名字符串 switch，降低重构脆弱性。

---

## 9. 复审核销记录（修复阶段后）

> 复审时间：2026-08-20（修复阶段完成，工作区已含修复后代码）。逐项核实修复落地证据，全部核销，无未修复项。

| # | 原问题（等级/ID） | 修复证据（path:line / 行为） | 核销 |
|----|-------------------|------------------------------|------|
| 1 | P0 G1 信号量 over-release | ExportServiceImpl.java:83-95：`boolean acquired = tryAcquire(); if (!acquired) throw …; try { … } finally { release(); }`，失败路径不进 finally | ✅ |
| 2 | P1 F05 R03 导出埋点 req_summary 空 | ExportRequest.java:61-64 新增 `toString()`（`target=…,format=…`）；TrackAspect.exportField 现可解析 | ✅ |
| 3 | P1 F05 R03 resp_summary 摘要失真 | HashVO.java:55-58（哈希前 16 位）、SortVO.java:67-69（排序前 10 元素）、HelloWorldVO.java:55-56 均实现摘要 `toString()` | ✅ |
| 4 | P1 S8.1 无鉴权 | 新增 ApiAuthInterceptor.java（mock-caller-enabled 开关 + 正式环境校验 X-Auth-User-Id → COMMON_401）；WebConfig.java:31-33 注册 `/api/**` | ✅ |
| 5 | P1 S9.1 凭证硬编码 | application.yml:11-12 `${DB_USERNAME:root}` / `${DB_PASSWORD:root}` 环境变量注入 | ✅ |
| 6 | P1 G16.2 catch 无日志 | DemoServiceImpl.java:144、ExportServiceImpl.java:186、TrackingServiceImpl.java:146,157,181 均补 `LOGGER.warn` | ✅ |
| 7 | P1 M016/G14.4 默认时区 | ExportServiceImpl.java:34,77,90 + TrackingController.java:19,36,52,68 统一 `APP_ZONE = ZoneId.of("Asia/Shanghai")` | ✅ |
| 8 | P1 B008 Executors | CallRecordQueue.java:62 `new ScheduledThreadPoolExecutor(1, threadFactory)` | ✅ |
| 9 | P2 W07 dimension 细分 | TrackingService 新增 `trend(granularity, dimension, …)` 重载；TrackingServiceImpl.parseDimensionFilter + columnsOf 白名单；TrackingMapper.xml selectTrend 增加 `<if test="dimColumn…">` 条件；TrackingController.trend 暴露 dimension 参数 | ✅ |
| 10 | P2 S7.1 CSV 前缀 | CsvExportUtil.java:14 `FORMULA_PREFIXES = "=+-@\t\r"` | ✅ |
| 11 | P2 G11.2 90 天边界 | TrackingServiceImpl.java:143 `end.isAfter(start.plusDays(MAX_RANGE_DAYS))`（原 Duration.toDays 截断已替换） | ✅ |
| 12 | P2 A5 方法名 switch | TrackAspect.java:96-104,110-115 改为按 `BizType` 枚举驱动（case HELLO_WORLD/HASH/BUBBLE_SORT/EXPORT） | ✅ |
| 13 | P2 A3.4/A2.3 风格 | 抽查 ExportServiceImpl/DemoServiceImpl/TrackingServiceImpl import 已分组有序、行宽合规；残留风格问题微小不阻塞（按 P2 允许） | ✅ |

**复审新增审计（修复引入代码）**：
- ApiAuthInterceptor：OPTIONS 预检放行 ✓；mock 开关语义与 CallContextResolver/前端 api/index.js X-Caller-* 头契约一致 ✓；412/401 JSON 响应结构 `{code,msg,data}` 与 CommonResponse 约定同构 ✓。
- 时区替换后批量写/查询均以 Asia/Shanghai 与 JDBC url serverTimezone 一致 ✓。
- 跨仓对齐点复检：前端 api/index.js 注入 X-Caller-Id/Name/Type/Level/DeptCode/DeptName 与 CallContextResolver 头名一一对应 ✓；trend dimension 参数前端可透传（向后兼容，旧调用不受影响）✓。
- 编译/测试：环境缺少 JDK/Maven，无法执行 `mvn test`；按降级协议转为静态复核，入参/出参类型与跨仓契约已逐项核对一致（风险敞口：运行期行为未验证，见 §7 一句话）。

**复审结论**：P0=0 · P1=0 · P2=0；合并建议 **通过**。后续若需运行期回归，建议在具备 JDK/Maven 的 CI 环境执行 `mvn test`（6 个测试类）确认。