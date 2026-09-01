# 代码评审报告

> 评审日期：2026-09-01
> 任务编号：DEV-9d10e310-7901-11f1-8a9f-59ecae612580-1bde062f-e608-4ffe-aedf-004ab8c93b57
> 评审范围：manyu_test（28 文件）+ manyu_test1（4 文件）
> 需求：算法展示与监控子系统（helloworld + 哈希算法 + 冒泡排序 + 前端展示 + 导出 + 埋点报表）

---

## 项目评审画像

**State**: FOUND_AND_USED
**Source**: `REVIEW.md` (manyu_test)
**Notes**: 已有项目评审画像，覆盖跨仓契约、数据完整性、错误处理、测试四个门禁。实际评审中额外关注了 SQL 注入、数据流完整性等通用安全与正确性检查。

---

## Lane 评审结论表

| Lane | Verdict | 说明 |
|------|---------|------|
| Align | REJECT | 埋点维度筛选声明与实现不一致（dimension/dimensionValue 已声明但未生效）；埋点记录维度字段总是写死 "unknown" |
| Design | REJECT | 埋点记录将所有用户维度字段硬编码为 "unknown"，导致报表功能完全不可用；SQL 注入风险 |
| Trim | APPROVE_WITH_COMMENTS | 代码结构清晰，模块划分合理，少量死代码 |
| Cause | NOT_RUN | 本次为全新功能开发，非缺陷修复，无 root-cause closure 场景 |
| Verify | REJECT | SQL 注入漏洞；埋点维度数据流断裂；缺少 TrackingService/ExportService 单元测试 |

---

## 阻塞性发现 (Blocking)

### [CRITICAL] [VERIFY] [IMPLEMENTATION-BUG] src/main/java/com/example/demo/tracking/dao/mapper/ApiCallLogMapper.java:48-53 — SQL 注入漏洞

**Evidence**: `dimensionStats` 方法使用 MyBatis `${dimension}`（字符串替换）而非 `#{dimension}`（参数化）：

```java
@Select("SELECT ${dimension} AS label, COUNT(*) AS count "
        + "FROM api_call_log ...")
```

`dimension` 参数来自 HTTP 请求体 `DimensionStatsRequest.dimension`。虽然在 `ReportController` 中有白名单校验（`VALID_DIMENSIONS`），但 Mapper 层本身是开放的——任何绕过 Controller 的调用路径（如直接调用 Service、其他内部调用方）都会导致 SQL 注入。

**Recommendation**: 将 `${dimension}` 改为 `#{dimension}`，并在 Mapper 内使用 CASE WHEN 或枚举映射，不在 SQL 字符串中拼接用户输入。

---

### [HIGH] [DESIGN] [BOUNDARY-LEAK] src/main/java/com/example/demo/tracking/service/impl/TrackingServiceImpl.java:49-51 — 埋点维度字段始终写死为 "unknown"

**Evidence**:
```java
log.setUserType("unknown");
log.setUserLevel("unknown");
log.setUserDepartment("unknown");
```

`recordCall()` 方法将所有用户维度字段硬编码为 `"unknown"`。这意味着：
- W05 调用统计按维度筛选时，所有维度值均为 `"unknown"`
- W06 维度统计（饼图/柱状图）的 `GROUP BY user_type/user_level/user_department` 只会返回一条 `"unknown"` 分组
- 前端报表功能的全部维度筛选、饼图、柱状图均无法展示有意义的分类数据，功能完全失效

**Recommendation**: 从请求上下文或 `user_info` 表查询当前用户的真实维度信息（user_type/user_level/user_department），或通过 `X-User-Id` header 关联 `user_info` 表获取。

---

### [HIGH] [ALIGN] [CLAIM-DRIFT] src/main/java/com/example/demo/tracking/controller/ReportController.java:52-62 + src/main/java/com/example/demo/tracking/service/impl/TrackingServiceImpl.java:60-77 — CallStatsRequest 的 dimension 筛选未生效

**Evidence**: `ReportController.callStats()` 接收并校验了 `CallStatsRequest.dimension` 和 `dimensionValue`，但调用 `trackingService.queryCallStats(request)` 时，`TrackingServiceImpl.queryCallStats()` 完全忽略了这两个字段，始终以 `null` 作为 `apiName` 传给 `callStatsByDay`：

```java
// TrackingServiceImpl:64
List<Map<String, Object>> rows = apiCallLogMapper.callStatsByDay(
        null, startTime, endTime);
```

设计文档中 `CallStatsRequest` 明确声明了 `dimension`（筛选维度）和 `dimensionValue`（维度值），但 SQL 查询 `callStatsByDay` 不支持按维度筛选，只支持按 `apiName` 筛选。这导致前端传了维度筛选也得不到过滤结果。

**Recommendation**: 在 `callStatsByDay` SQL 中增加按维度筛选的条件，或根据 `dimension`/`dimensionValue` 动态拼接 WHERE 条件（使用参数化查询）。

---

### [HIGH] [VERIFY] [TEST-GAP] — 缺少 TrackingService 和 ExportService 的单元测试

**Evidence**: 项目 `REVIEW.md` 要求 "Unit tests required for all Service implementations"。当前仅有 `AlgorithmServiceImplTest.java`（12 个测试），缺少：
- `TrackingServiceImplTest` — 覆盖 `recordCall`、`queryCallStats`、`queryDimensionStats`
- `ExportServiceImplTest` — 覆盖 `exportData` 的正常/异常/边界路径

**Recommendation**: 为 `TrackingServiceImpl` 和 `ExportServiceImpl` 补充单元测试，至少覆盖正常路径、异常路径（开关关闭、参数非法）和边界条件（空数据导出）。

---

## 建议性发现 (Advisory)

### [WARNING] [DESIGN] [OBSERVABILITY-GAP] src/main/java/com/example/demo/tracking/service/impl/TrackingServiceImpl.java:39-57 — 埋点记录为同步阻塞，不符合设计约定

**Evidence**: 设计文档 R08 规定 "埋点失败不影响主流程，异步记录日志告警"。当前实现为同步 `insert`（虽然 try-catch 防止了异常传播），但会阻塞主请求线程直到数据库写入完成。

**Recommendation**: 使用 `@Async` + `@EnableAsync` 或线程池异步执行埋点写入，避免数据库延迟影响业务接口响应时间。

---

### [WARNING] [TRIM] [DEAD-CODE] src/main/java/com/example/demo/tracking/model/request/CallStatsRequest.java:24-27 — dimensionValue 字段从未被使用

**Evidence**: `CallStatsRequest.dimensionValue` 字段在 Controller 中校验了 `dimension` 合法性，但 `dimensionValue` 完全未被任何代码消费。SQL 查询 `callStatsByDay` 不支持按维度值过滤。

**Recommendation**: 要么实现 dimensionValue 过滤逻辑，要么从 `CallStatsRequest` 中移除该字段直到真正需要时再添加。

---

### [WARNING] [VERIFY] [ERROR-PATH] src/main/java/com/example/demo/export/controller/ExportController.java:40-51 + src/main/java/com/example/demo/export/service/impl/ExportServiceImpl.java:51-103 — 导出异常时返回 JSON 而非二进制流

**Evidence**: 当 `ExportServiceImpl` 抛出 `BusinessException`（如 EXP_001 导出类型非法），`GlobalExceptionHandler` 会返回 `ApiResponse` JSON 而非二进制 Excel 流。前端 `handleExport` 检查 `res.ok` 并尝试将响应解析为 blob，但在 HTTP 200 + JSON 响应体的情况下，用户会下载到一个包含 JSON 错误信息的 .xlsx 文件，而非得到有意义的错误提示。

**Recommendation**: 在 `ExportController` 中捕获 `BusinessException` 并返回适当的 HTTP 错误状态码（如 400），或在前端先检查 `Content-Type` 再决定处理方式。

---

### [INFO] [TRIM] [PUBLIC-SURFACE] — ApiCallLog 实体包含 userName 字段但从未被赋值

**Evidence**: `ApiCallLog` 实体定义了 `userName` 字段，`recordCall()` 方法中从未设置 `userName`。`selectForExport` 查询了 `user_name` 列，但写入时总是为空。

**Recommendation**: 在 `recordCall()` 中从用户上下文获取并设置 `userName`，或如果短期内不需要则移除该字段以减少维护负担。

---

## 跳过的 Lane

| Lane | 原因 |
|------|------|
| Cause | 本次为全新功能开发，非缺陷修复。无 root-cause closure 场景可评审。 |

---

## 建议后续行动

1. **立即修复** SQL 注入漏洞（ApiCallLogMapper.dimensionStats 的 `${dimension}`）
2. **修复** 埋点维度字段从硬编码 "unknown" 改为从用户上下文获取真实值
3. **实现** CallStatsRequest 的 dimension/dimensionValue 筛选逻辑
4. **补充** TrackingServiceImpl 和 ExportServiceImpl 的单元测试
5. **考虑** 将埋点写入改为异步执行
6. **考虑** 修复导出异常时的错误响应格式问题

---

## 跨仓对齐点检查

| 对齐项 | 状态 | 说明 |
|--------|------|------|
| API 路径 (frontend ↔ backend) | ✅ | 前端 `API_BASE + /api/algorithm/*` 与后端 `@RequestMapping` 一致 |
| X-User-Id Header | ✅ | 前端 `apiFetch` 发送 `X-User-Id`，后端 `getUserId()` 读取 |
| CORS 配置 | ✅ | `CorsConfig` 已配置 `/api/**` 允许跨域 |
| 导出格式 | ✅ | 后端返回 `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`，前端以 blob 下载 |
| 报表数据格式 | ✅ | `CallStatsVO.series[{time, count}]` 与前端折线图 `s.time/s.count` 对齐；`DimensionStatsVO.items[{label, count, percentage}]` 与前端饼图/柱状图对齐 |

---

**VERDICT: REJECT**

> 评审结论：存在 1 个 CRITICAL（SQL 注入）和 3 个 HIGH（埋点维度数据断裂、维度筛选未生效、缺少单元测试），必须修复后方可合入。