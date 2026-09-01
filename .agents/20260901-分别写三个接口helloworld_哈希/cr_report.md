# Code Review Report — 算法展示与监控子系统

> **Review Target**: manyu_test (Java 后端) + manyu_test1 (React 前端)
> **Requirement**: 三个接口 (helloworld/hash/bubble-sort) + 导出 + 埋点 + 可视化报表
> **Review Date**: 2026-09-01

---

## Project Profile

**State**: FOUND_AND_USED
**Source**: `REVIEW.md` (manyu_test root)
**Notes**: 已存在的 REVIEW.md 包含跨仓契约、数据完整性、异常处理、测试四个关卡，与当前项目上下文匹配，直接使用。

---

## Lane Verdict Table

| Lane | Verdict | Notes |
|---|---|---|
| Align | REJECT | 需求声明 vs 实际产出存在多处漂移（埋点未落库 / 报表为 mock 数据 / 导出为 mock 数据），前端未发送 X-User-Id 头，无 CORS 配置 |
| Design | REJECT | ReportController 和 ExportController 在 Controller 层自行捕获异常返回 ApiResponse，绕过 GlobalExceptionHandler，设计不一致 |
| Trim | APPROVE_WITH_COMMENTS | 代码整体精简，存在少量可优化项（冗余 try-catch-rethrow、axios 未使用） |
| Cause | NOT_RUN | 本次为全新开发，非缺陷修复，无 root-cause 分析场景 |
| Verify | REJECT | 埋点/导出/报表三大模块均为 mock 实现无真实数据通路；TrackingService 和 ExportService 缺少单元测试；ReportController 90 天限制未生效 |

---

## Blocking Findings

### [CRITICAL] [ALIGN] [DATA-INTEGRITY] TrackingServiceImpl.java:28-29 — 埋点仅写日志，未持久化到数据库

**Evidence**:
- `TrackingServiceImpl.recordCall()` 仅调用 `logger.info(...)` 记录日志，未注入任何 Mapper/Repository 执行数据库 INSERT
- `schema.sql` 已定义 `api_call_log` 表 (含 api_name, user_id, user_type, user_level, user_department 等字段)
- `pom.xml` 已引入 MyBatis-Plus 依赖，但未使用
- 注释标注 "同步记录到数据库"，与实现矛盾

**Recommendation**: 注入 MyBatis-Plus Mapper，在 `recordCall()` 中构造 `ApiCallLog` 实体 INSERT 到 `api_call_log` 表。同时需查询 `user_info` 表获取用户维度信息 (user_type/user_level/user_department)，或使用默认值 "unknown"。

---

### [CRITICAL] [ALIGN] [DATA-INTEGRITY] ReportController.java:51-56, 85-88 — 报表接口返回硬编码 mock 数据

**Evidence**:
- `callStats()` 方法 (line 51-56) 直接构造固定时序数据 `[("2026-09-01", 15), ("2026-09-02", 23), ("2026-09-03", 18)]`，未查询 `api_call_log` 表
- `dimensionStats()` 方法 (line 85-88) 直接构造固定维度数据 `["技术部", "产品部", "运营部"]`，未查询数据库
- 注释标注 "实际应查询数据库"，但交付时未实现
- 违反 REVIEW.md 关卡：Report queries must read from database, not mock data

**Recommendation**: 在 TrackingService 中新增 `queryCallStats()` 和 `queryDimensionStats()` 方法，根据时间范围和维度参数从 `api_call_log` 表 (JOIN `user_info` 表) 执行 GROUP BY 聚合查询。

---

### [CRITICAL] [ALIGN] [DATA-INTEGRITY] ExportServiceImpl.java:65-71 — 导出接口返回硬编码示例数据

**Evidence**:
- `exportData()` 方法 (line 65-71) 仅写入一行 `sample_user` 的固定数据，未查询数据库
- 注释标注 "实际场景应查询数据库"，但交付时未实现
- 违反 REVIEW.md 关卡：Export must read from database, not mock data

**Recommendation**: 根据 `exportType` 查询 `api_call_log` 表中对应 `api_name` 的记录，按时间范围筛选后写入 Excel。

---

### [HIGH] [DESIGN] [BOUNDARY-LEAK] ReportController.java:62-68, 96-102 / ExportController.java:57-64 — Controller 层自行捕获异常并构造响应，绕过 GlobalExceptionHandler

**Evidence**:
- `ReportController`: `catch (BusinessException e)` 直接返回 `ApiResponse.error(...)`；`catch (Exception e)` 返回硬编码 `"B0001"`
- `ExportController`: 同样在 Controller 层 catch 并 return `ResponseEntity.ok(ApiResponse.error(...))`
- `AlgorithmController` 则依赖 `GlobalExceptionHandler` 统一处理异常
- 同一项目内两种异常处理策略并存，维护者需同时理解两套路径

**Recommendation**: 统一使用 `GlobalExceptionHandler` 处理所有异常。Controller 层不应有 try-catch 包裹业务逻辑。如需特殊处理，应在 Service 层抛出 BusinessException，由 GlobalExceptionHandler 统一拦截。

---

### [HIGH] [ALIGN] [API-CONTRACT] AlgorithmDashboard.js — 前端未发送 X-User-Id 请求头

**Evidence**:
- `AlgorithmController.getUserId()` 从 `HttpServletRequest.getHeader("X-User-Id")` 读取用户 ID
- 前端 `AlgorithmDashboard.js` 中所有 fetch 请求仅设置 `Content-Type: application/json`，未设置 `X-User-Id` 头
- REVIEW.md 要求：User identity header (X-User-Id) contract must be honored by both sides
- 结果：所有埋点记录的 `user_id` 将始终为 `"anonymous"`

**Recommendation**: 前端在所有 fetch 请求中添加 `X-User-Id` header（从 SSO 或登录态获取），或在请求拦截器中统一注入。

---

### [HIGH] [ALIGN] [CONFIG-CONTRACT] application.yml — 无 CORS 跨域配置

**Evidence**:
- 前端部署在 `localhost:3000` (React)，后端在 `localhost:8080` (Spring Boot) — 不同源
- `application.yml` 中无任何 CORS 配置
- REVIEW.md 要求：CORS must be configured for cross-origin frontend requests

**Recommendation**: 添加 CORS 配置（WebMvcConfigurer.addCorsMappings 或 `@CrossOrigin` 注解），允许前端域名的跨域请求。

---

### [HIGH] [VERIFY] [TEST-GAP] TrackingServiceImpl.java — 缺少单元测试

**Evidence**:
- REVIEW.md 关卡：Unit tests required for all Service implementations
- `AlgorithmServiceImpl` 有 12 个测试用例，但 `TrackingServiceImpl` 和 `ExportServiceImpl` 均无测试文件
- 无法验证埋点记录逻辑的正确性

**Recommendation**: 新增 `TrackingServiceImplTest`，覆盖：正常记录、tracking.enabled=false 跳过、异常时不影响主流程。

---

### [HIGH] [VERIFY] [TEST-GAP] ExportServiceImpl.java — 缺少单元测试

**Evidence**:
- 同上，`ExportServiceImpl` 无任何测试覆盖

**Recommendation**: 新增 `ExportServiceImplTest`，覆盖：合法导出类型、非法导出类型抛异常、导出功能禁用、max-records 限制。

---

### [HIGH] [VERIFY] [BOUNDARY-CASE] ReportController.java:108-117 — validateTimeRange 未校验 90 天上限

**Evidence**:
- `validateTimeRange()` 仅校验 null 和 startTime > endTime
- 错误码 `TRK_001` 描述为 "时间范围不能超过90天"，但代码中未执行该比较
- 若传入 365 天范围，方法不会拒绝

**Recommendation**: 在 `validateTimeRange()` 中解析日期字符串并计算差值，超过 90 天时抛出 `TRK_001`。

---

## Advisory Findings

### [WARNING] [TRIM] [LOGIC-SIMPLIFICATION] AlgorithmController.java:43-50, 58-65, 73-80 — 冗余 try-catch-rethrow

**Evidence**:
- 三个接口方法中 `catch (Exception e) { logger.error(...); throw e; }` 仅记录日志后重新抛出
- GlobalExceptionHandler 已全局捕获所有异常并记录日志
- 该模式增加噪音，不提供额外价值

**Recommendation**: 移除 try-catch 块，让异常自然传播到 GlobalExceptionHandler。

---

### [WARNING] [TRIM] [UNUSED-ABSTRACTION] package.json — axios 依赖已声明但未使用

**Evidence**:
- `package.json` 声明 `"axios": "^1.6.0"`
- 前端代码全部使用原生 `fetch` API，未 import axios

**Recommendation**: 移除 axios 依赖，或统一使用 axios 替代原生 fetch 以获得更好的错误处理。

---

### [WARNING] [VERIFY] [TEST-GAP] AlgorithmServiceImplTest — 缺少 request == null 的测试用例

**Evidence**:
- `computeHash()` 方法第一行检查 `request == null`，但测试中仅测试 `request.input == null` 场景
- 缺少传入 `null` 作为 request 参数的测试

**Recommendation**: 新增 `shouldThrowException_whenRequestNull` 测试。

---

## Skipped Lanes and Reasons

| Lane | Reason |
|------|--------|
| Cause | 本次为全新功能开发，非缺陷修复，无 root-cause 分析场景 |

---

## Suggested Next Actions

1. **P0**: 实现 TrackingServiceImpl 的数据库持久化（注入 Mapper，INSERT api_call_log）
2. **P0**: 实现 ReportController 的数据库查询（聚合 api_call_log + user_info）
3. **P0**: 实现 ExportServiceImpl 的数据库查询
4. **P1**: 统一异常处理策略 — 移除 Controller 层 try-catch，统一走 GlobalExceptionHandler
5. **P1**: 前端添加 X-User-Id 请求头
6. **P1**: 添加 CORS 跨域配置
7. **P1**: 新增 TrackingServiceImplTest 和 ExportServiceImplTest
8. **P2**: 修复 ReportController 90 天时间范围校验
9. **P2**: 移除 AlgorithmController 冗余 try-catch-rethrow
10. **P2**: 移除 package.json 中未使用的 axios

---

## VERDICT: REJECT

**Summary**: 3 个 CRITICAL + 6 个 HIGH 阻塞项。核心问题：埋点/报表/导出三大模块均为 mock 实现，未建立真实数据通路，违反项目数据完整性关卡。前端缺少 X-User-Id 和 CORS 配置，跨仓契约未对齐。