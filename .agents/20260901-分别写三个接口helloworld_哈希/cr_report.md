# 代码评审报告 — 算法展示与监控子系统

> **评审日期**: 2026-09-01
> **评审范围**: manyu_test (后端 Java) + manyu_test1 (前端 React)
> **系分文档**: `.agents/20260901-分别写三个接口helloworld_哈希/design.md`
> **实现文档**: `.agents/20260901-分别写三个接口helloworld_哈希/impl.md`

---

## Project Profile

| 项目 | 内容 |
|------|------|
| State | `FOUND_AND_USED` |
| Source | `REVIEW.md` (root) |
| Notes | 项目级评审门禁已覆盖跨仓契约、数据完整性、异常处理、测试四个维度，与当前变更匹配 |

---

## Lane Verdict Table

| Lane | Verdict | Notes |
|------|---------|-------|
| Align | `REJECT` | API 契约漂移：`durationMs` vs `duration_ms`；R10 90天限制未实现 |
| Design | `REJECT` | 埋点/报表/导出均为 stub 硬编码，未集成数据库；缺少 CORS 配置；前端未发送用户身份 Header |
| Trim | `APPROVE_WITH_COMMENTS` | 存在未使用字段和冗余异常处理，非阻塞 |
| Verify | `REJECT` | TrackingService / ExportService / ReportController 零测试覆盖 |
| Cause | `NOT_RUN` | 本次为全新功能开发，非 Bug 修复 |

---

## Blocking Findings (CRITICAL + HIGH)

### CRITICAL

#### [CRITICAL] [DESIGN] [BOUNDARY-LEAK] TrackingServiceImpl 未写入数据库
- **路径**: `manyu_test/src/main/java/com/example/demo/tracking/service/impl/TrackingServiceImpl.java:23-33`
- **证据**: `recordCall()` 仅执行 `logger.info(...)`，未调用任何 Mapper/DAO 向 `api_call_log` 表写入数据。`schema.sql` 已定义 `api_call_log` 表结构，`application.yml` 已配置 MyBatis-Plus 和 MySQL 数据源，但 `TrackingServiceImpl` 未注入任何 Mapper。
- **影响**: 所有埋点数据静默丢失，报表和导出功能无数据源。
- **建议**: 创建 `ApiCallLogMapper` / `ApiCallLog` Entity，在 `TrackingServiceImpl` 中注入并调用 `mapper.insert(entity)`。

#### [CRITICAL] [DESIGN] [WRONG-LAYER] ReportController 返回硬编码 Mock 数据
- **路径**: `manyu_test/src/main/java/com/example/demo/tracking/controller/ReportController.java:52-57` (callStats), `:85-88` (dimensionStats)
- **证据**: 两个方法均返回固定数组（如 `series.add(new SeriesPoint("2026-09-01", 15))`），代码注释明确标注"实际应查询数据库"。未调用任何 Mapper 或 Service 查询 `api_call_log` 表。
- **影响**: 报表功能完全不可用，无法反映真实调用数据。
- **建议**: 在 `TrackingService` 中新增 `queryCallStats()` / `queryDimensionStats()` 方法，实现数据库聚合查询；Controller 调用 Service 而非构造 Mock 数据。

#### [CRITICAL] [DESIGN] [WRONG-LAYER] ExportServiceImpl 返回硬编码 Mock 数据
- **路径**: `manyu_test/src/main/java/com/example/demo/export/service/impl/ExportServiceImpl.java:66-71`
- **证据**: 仅写入一条固定示例数据行（`"sample_user"`, `"2026-09-01 12:00:00"`），未查询 `api_call_log` 表。`export_record` 表也未写入。
- **影响**: 导出功能完全不可用，只能下载含一条固定数据的 Excel。
- **建议**: 注入 `ApiCallLogMapper`，按 `exportType` 和时间范围查询数据库，将结果写入 Excel。

---

### HIGH

#### [HIGH] [ALIGN] [API-CONTRACT] BubbleSortVO JSON 字段名与设计文档不一致
- **路径**: `manyu_test/src/main/java/com/example/demo/algorithm/model/vo/BubbleSortVO.java:22`
- **证据**: Java 字段 `durationMs` → Jackson 默认序列化为 `"durationMs"`。设计文档 §5.1.2 W03 出参定义为 `duration_ms`（蛇形命名）。前端代码未直接引用此字段（通过 JSON.stringify 展示），但若后续前端按设计文档解析 `duration_ms` 将获取 `undefined`。
- **建议**: 添加 `@JsonProperty("duration_ms")` 注解，或在 `application.yml` 中配置 `spring.jackson.property-naming-strategy: SNAKE_CASE`。

#### [HIGH] [ALIGN] [CLAIM-DRIFT] 时间范围 90 天限制未实现
- **路径**: `manyu_test/src/main/java/com/example/demo/tracking/controller/ReportController.java:108-117`
- **证据**: 设计文档 R10 明确要求"时间范围不能超过 90 天"，但 `validateTimeRange()` 仅校验了 null 和 start > end，未计算日期差并校验 90 天上限。
- **建议**: 在 `validateTimeRange()` 中解析日期并计算差值，超过 90 天时抛出 `BusinessException(TRK_001, "时间范围不能超过90天")`。

#### [HIGH] [DESIGN] [BOUNDARY-LEAK] 缺少 CORS 跨域配置
- **路径**: `manyu_test` 项目全局
- **证据**: 无 `@CrossOrigin` 注解、无 `CorsFilter` Bean、无 `WebMvcConfigurer.addCorsMappings()` 配置。前端 `API_BASE = 'http://localhost:8080'`（`manyu_test1/src/AlgorithmDashboard.js:4`），React 开发服务器默认运行在 `localhost:3000`，浏览器将因同源策略阻止所有 API 请求。
- **建议**: 添加 `CorsConfig` 配置类，允许 `localhost:3000` 来源的跨域请求。

#### [HIGH] [DESIGN] [OBSERVABILITY-GAP] 前端未发送用户身份 Header
- **路径**: `manyu_test1/src/AlgorithmDashboard.js:15,44,86` (所有 fetch 调用)
- **证据**: 后端 `AlgorithmController.getUserId()` 从 `X-User-Id` Header 读取用户 ID（默认 `"anonymous"`）。前端所有 `fetch()` 调用均未设置 `X-User-Id` Header。所有埋点记录的 `user_id` 将始终为 `"anonymous"`。
- **建议**: 前端在 `fetch` 的 `headers` 中添加 `'X-User-Id': '<从登录态获取>'`。或在后端实现统一的 `Filter/Interceptor` 从 Session/Token 解析用户 ID。

---

## Advisory Findings (WARNING)

#### [WARNING] [ALIGN] [CONFIG-CONTRACT] 应急开关命名不一致
- **路径**: `manyu_test/src/main/resources/application.yml:20`
- **证据**: 设计文档 §7.3 定义开关名为 `track.enabled`，实际配置为 `tracking.enabled`。`TrackingServiceImpl` 读取 `${tracking.enabled:true}`。
- **建议**: 统一为 `track.enabled` 或更新设计文档。

#### [WARNING] [DESIGN] [WRONG-LAYER] 异常处理模式不一致
- **路径**: `AlgorithmController.java:47-50` / `ReportController.java:62-68` / `ExportController.java:57-64`
- **证据**: AlgorithmController 捕获后 re-throw（依赖 GlobalExceptionHandler）；ReportController 捕获后直接 `return ApiResponse.error()`；ExportController 捕获后返回 `ResponseEntity.ok(ApiResponse.error())`。三种模式各异。
- **建议**: 统一由 GlobalExceptionHandler 处理，Controller 不捕获异常（除非有特殊降级逻辑）。

#### [WARNING] [DESIGN] [OBSERVABILITY-GAP] AlgorithmController 异常处理与 R08 冲突
- **路径**: `manyu_test/src/main/java/com/example/demo/algorithm/controller/AlgorithmController.java:47-50`
- **证据**: `trackingService.recordCall()` 与 `ApiResponse.success()` 在同一 try 块中。设计 R08 要求"埋点失败不影响主流程"。当前 TrackingServiceImpl 内部有 try-catch 兜底，但若未来 TrackingService 实现变更抛异常，主流程将被中断。
- **建议**: 将 `trackingService.recordCall()` 移到 try-catch 外部，或使用 `try { recordCall() } catch { logger.warn }` 隔离。

#### [WARNING] [TRIM] 未使用的请求字段
- `CallStatsRequest.dimensionValue` (manyu_test/.../CallStatsRequest.java:27) — 声明但未使用
- `CallStatsRequest.granularity` (manyu_test/.../CallStatsRequest.java:21) — 声明但未使用
- `DimensionStatsRequest.chartType` (manyu_test/.../DimensionStatsRequest.java:25) — 仅记录日志，不区分查询逻辑
- **建议**: 删除未使用字段，或在实现中利用它们。

#### [WARNING] [TRIM] AlgorithmController 冗余 catch-rethrow
- **路径**: `manyu_test/src/main/java/com/example/demo/algorithm/controller/AlgorithmController.java:47-50, 62-65, 77-80`
- **证据**: 三个方法均 catch Exception → log → re-throw。GlobalExceptionHandler 已统一处理所有异常，此 catch 块仅增加日志噪音。
- **建议**: 删除 try-catch 块，由 GlobalExceptionHandler 统一处理。

#### [WARNING] [VERIFY] [TEST-GAP] 缺少 Service/Controller 测试覆盖
- **路径**: `manyu_test/src/test/` 目录
- **证据**: 仅 `AlgorithmServiceImplTest.java` 存在。`TrackingServiceImpl`、`ExportServiceImpl`、`ReportController`、`ExportController` 均无测试。
- **建议**: 至少为 `TrackingServiceImpl`（数据库写入）和 `ExportServiceImpl`（Excel 生成）补充单元测试。

---

## Skipped Lanes and Reasons

| Lane | Reason |
|------|--------|
| Cause | 本次为全新功能开发，非 Bug 修复，无 root-cause 分析场景 |

---

## Suggested Next Actions

1. **P0**: 实现 `TrackingServiceImpl` 数据库写入（创建 Mapper + Entity）
2. **P0**: 实现 `ReportController` 数据库查询（替换 Mock 数据）
3. **P0**: 实现 `ExportServiceImpl` 数据库查询（替换 Mock 数据）
4. **P0**: 添加 CORS 配置
5. **P0**: 前端添加 `X-User-Id` Header 传递
6. **P1**: 修复 `durationMs` JSON 序列化字段名
7. **P1**: 实现 R10 90 天时间范围校验
8. **P1**: 补充 TrackingService / ExportService 单元测试
9. **P2**: 统一异常处理模式
10. **P2**: 清理未使用的请求字段

---

## Summary

| 统计项 | 数量 |
|--------|:----:|
| CRITICAL | 3 |
| HIGH | 4 |
| **Blocker 合计** | **7** |
| WARNING | 7 |
| INFO | 0 |

---

**VERDICT: `REJECT`**

核心原因：埋点、报表、导出三大模块均为 stub 实现（硬编码 Mock 数据），未与数据库集成，无法满足需求。同时存在 API 契约漂移、CORS 缺失、用户身份传递断裂等集成阻塞问题。