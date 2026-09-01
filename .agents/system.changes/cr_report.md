# Code Review Report — 三接口演示平台

**Review target**: 全量代码变更（manyu_test 后端 + manyu_test1 前端）
**Review date**: 2026-09-01
**Design doc**: `.agents/20260901-分别写三个接口helloworld_哈希/design.md`

---

## Project profile

**State**: FOUND_AND_USED
**Source**: `manyu_test-cred-test-20260716022903/REVIEW.md`
**Notes**: 项目特定的 REVIEW.md 已存在，包含后端 (Python/FastAPI/SQLite) 和前端 (HTML/JS/ECharts) 的门禁规则，以及跨仓对齐约束。所有 lane 均基于此 profile 执行。

---

## Lane verdict table

| Lane | Verdict | Notes |
|---|---|---|
| Align | APPROVE_WITH_COMMENTS | 实现与设计文档高度一致；API 契约、跨仓 Header、维度枚举、CSV 列均对齐。缺少数据库索引。 |
| Design | REJECT | `sys.path.insert` hack 违反项目门禁；BASE_URL 硬编码违反可配置性要求。 |
| Trim | APPROVE_WITH_COMMENTS | `HashResponse.input` 字段名遮蔽 Python 内置函数；`HTTPException` 导入未使用。 |
| Cause | NOT_RUN | 本次为全新功能开发，非 bug 修复，无 root-cause 分析上下文。 |
| Verify | REJECT | SQL f-string 插值违反参数化查询门禁；ECharts resize 事件监听器内存泄漏；bubble-sort 输入解析未捕获异常。 |

---

## Blocking findings

### [CRITICAL] [VERIFY] [IMPLEMENTATION-BUG] apis/analytics.py:24,41 — SQL 查询使用 f-string 插值

**Evidence**:
- `analytics.py:24`: `query = f"SELECT {dimension}, COUNT(*) as cnt FROM api_call_logs"`
- `analytics.py:41`: `query += f" GROUP BY {dimension} ORDER BY cnt DESC"`
- `REVIEW.md:10`: "SQL queries must use parameterized queries; f-string interpolation into SQL is forbidden even with whitelist validation."

**Recommendation**: 将 `dimension` 到列名的映射改为显式字典查找，避免 f-string 拼接 SQL：

```python
COLUMN_MAP = {"dept": "dept", "level": "level", "user_type": "user_type"}
col = COLUMN_MAP[dimension]  # dimension 已通过 VALID_DIMENSIONS 校验
query = f"SELECT {col}, COUNT(*) as cnt FROM api_call_logs"
```

> 注：虽然 `dimension` 已经过 `VALID_DIMENSIONS` 白名单校验，但项目门禁明确要求"即使有白名单校验也不允许 SQL f-string 插值"。该规则旨在防止后续维护中新增维度时遗忘校验而导致注入风险。

---

### [HIGH] [DESIGN] [BOUNDARY-LEAK] apis/bubble_sort.py:4 — sys.path.insert 导入 hack

**Evidence**:
- `apis/bubble_sort.py:4`: `sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))`
- `apis/bubble_sort.py:5`: `from bubble_sort import bubble_sort as bs`
- `REVIEW.md:12`: "`sys.path.insert` hacks for cross-module imports are not allowed; use proper package structure."

**Recommendation**: 将根目录的 `bubble_sort.py` 移入包内（如 `apis/` 或新建 `lib/`），使用标准相对导入：

```python
from .bubble_sort import bubble_sort as bs
```

---

### [HIGH] [VERIFY] [LIFECYCLE] js/charts.js:54-56 — ECharts resize 事件监听器内存泄漏

**Evidence**:
- `charts.js:54-56`: 每次 `renderChart()` 调用都添加新的 `window.addEventListener('resize', ...)` 而不移除旧监听器
- `app.js:189`: 初始化时调用 `loadAnalytics()`
- `app.js:105,118,136`: 每次 API 调用后 `loadAnalytics()`
- `app.js:173,183`: 切换维度/图表类型时 `loadAnalytics()`
- `REVIEW.md:17`: "Event listeners must be cleaned up to avoid memory leaks (especially ECharts resize)."

**Recommendation**: 使用具名函数并在注册前移除旧监听器：

```javascript
function onResize() {
    chartInstance && chartInstance.resize();
}
window.removeEventListener('resize', onResize);
window.addEventListener('resize', onResize);
```

---

### [HIGH] [DESIGN] [BOUNDARY-LEAK] js/app.js:2 — BASE_URL 硬编码为 localhost

**Evidence**:
- `app.js:2`: `const BASE_URL = 'http://localhost:8000';`
- `REVIEW.md:18`: "BASE_URL must be configurable, not hardcoded to localhost."

**Recommendation**: 从页面元素或 URL 参数读取后端地址，支持部署时动态配置：

```javascript
const BASE_URL = document.getElementById('backendUrl').textContent
    || window.location.origin.replace(/:\d+$/, ':8000');
```

---

### [HIGH] [VERIFY] [IMPLEMENTATION-BUG] js/app.js:128-132 — bubble-sort 输入解析未捕获异常

**Evidence**:
- `app.js:128-132`: `throw new Error(...)` 在 `.map()` 回调中执行，位于 `try/catch` 块之外，导致非数字输入时抛出未捕获异常，用户看不到错误提示。

```javascript
var numbers = raw.split(',').map(function(s) {   // line 128
    var n = parseFloat(s.trim());
    if (isNaN(n)) throw new Error('包含非数字: ' + s);  // line 130 — 在 try 外面
    return n;
});
try {                                               // line 133
    var data = await apiCall('POST', '/api/bubble-sort', { numbers: numbers });
```

**Recommendation**: 将 `.map()` 调用移入 try 块内，或将 map 中的 throw 改为返回标记值并在 try 块内统一校验。

---

## Advisory findings

### [WARNING] [DESIGN] [OBSERVABILITY-GAP] middleware/tracking.py:21-25 — 埋点异步写入无错误日志

**Evidence**: `tracking.py:21-25` 中 daemon 线程的 `insert_log()` 调用无 try/except，写入失败时静默丢弃。虽然设计文档允许"异步线程静默失败"，但建议至少添加 `logging.warning()` 以便排查问题。

---

### [WARNING] [TRIM] [PUBLIC-SURFACE] apis/hash_api.py:14 — HashResponse.input 字段名遮蔽 Python 内置函数

**Evidence**: `hash_api.py:14`: `input: str` — 字段名 `input` 与 Python 内置函数 `input()` 同名。虽然 Pydantic 模型字段不会造成运行时冲突，但 IDE 和静态分析工具可能产生误报。建议改为 `input_text` 或 `source`。

---

### [WARNING] [TRIM] [DEAD-CODE] apis/bubble_sort.py:7, apis/hash_api.py:1 — HTTPException 导入未使用

**Evidence**: `bubble_sort.py:7` 和 `hash_api.py:1` 中的 `HTTPException` 导入未被使用。两个接口的错误处理均依赖 Pydantic 自动校验。

**Recommendation**: 移除未使用的 `HTTPException` 导入。

---

### [WARNING] [VERIFY] [TEST-GAP] tests/test_apis.py — 测试用例时序依赖与覆盖不足

**Evidence**:
- `test_export_helloworld_csv` (line 71-76): 未验证 CSV 内容是否包含埋点数据，仅校验了 HTTP 头和状态码。异步写入可能未完成。
- `test_analytics_by_dept` (line 84-93): 在调用埋点 API 后立即查询 analytics，未等待异步写入线程完成，存在竞态条件。
- 缺少: analytics 带 `api_name` 过滤参数、export 空数据 CSV、bubble-sort 非数字元素测试。

**Recommendation**: 在异步写入后添加 `time.sleep(0.3)` 等待，并断言 CSV 响应体内容；补充缺失的边界测试用例。

---

### [WARNING] [TRIM] [DATA-EXPOSURE] export/csv_writer.py:33-34 — StreamingResponse 非真流式

**Evidence**: `StreamingResponse(iter([output.getvalue()]), ...)` 将整个 CSV 内容一次性加载到内存后返回，并非真正的流式传输。

**Recommendation**: 使用生成器逐行 yield CSV 内容以实现真正的流式响应。

---

### [INFO] [ALIGN] [DOC-DRIFT] models/tracking.py — 缺少设计文档中声明的索引

**Evidence**: 设计文档 `design.md:516-521` 声明了 `api_name`、`dept`、`level`、`user_type` 四个索引，但 `tracking.py:13-26` 的 `init_db()` 仅创建表，未创建任何索引。

**Recommendation**: 在 `init_db()` 中添加 `CREATE INDEX IF NOT EXISTS` 语句。

---

### [INFO] [ALIGN] [API-CONTRACT] — 跨仓 Header 契约一致性验证通过

**Evidence**:
- 后端 `tracking.py:15-19`: 读取 `X-User-Id`, `X-User-Name`, `X-User-Dept`, `X-User-Level`, `X-User-Type`
- 前端 `app.js:19-25`: 发送 `X-User-Id`, `X-User-Name`, `X-User-Dept`, `X-User-Level`, `X-User-Type`
- API 路径: `/api/helloworld`, `/api/hash`, `/api/bubble-sort`, `/api/export/{type}`, `/api/analytics`
- 维度枚举: `dept`, `level`, `user_type` — 前后端一致

所有跨仓对齐点均已验证通过，无契约漂移。

---

## Skipped lanes and reasons

| Lane | Reason |
|---|---|
| Cause | 本次变更为全新功能开发，非 bug 修复或 root-cause 分析场景，缺少 Cause lane 所需的最小上下文。 |

---

## Suggested next actions

1. **P0**: 修复 `analytics.py` 的 SQL f-string 插值，改为列名映射字典（CRITICAL）
2. **P0**: 移除 `apis/bubble_sort.py` 的 `sys.path.insert` hack，改为正确的包导入（HIGH）
3. **P0**: 修复 `charts.js` 的 resize 事件监听器内存泄漏（HIGH）
4. **P0**: 使 `BASE_URL` 可配置，而非硬编码 localhost（HIGH）
5. **P0**: 修复 `app.js` bubble-sort 输入解析的未捕获异常（HIGH）
6. **P1**: 为 `init_db()` 添加设计文档中声明的索引
7. **P1**: 补充测试用例的异步等待和边界场景覆盖
8. **P2**: 为埋点异步写入线程添加错误日志
9. **P2**: 重命名 `HashResponse.input` 为 `input_text` 避免内置函数遮蔽
10. **P2**: 移除未使用的 `HTTPException` 导入

---

## VERDICT: **REJECT**

5 个 blocking findings（1 CRITICAL + 4 HIGH）需要修复后方可合入。