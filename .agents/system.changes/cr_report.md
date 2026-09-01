# Code Review Report — 三接口演示平台

## Review summary

Review target: 三接口演示平台跨仓实现（manyu_test 后端 + manyu_test1 前端），覆盖 helloworld / SHA256 哈希 / 冒泡排序三个计算接口、埋点中间件、CSV 导出、维度聚合分析、前端三 Tab 页面及 ECharts 可视化报表。

---

## Project profile

State: CREATED_AND_USED
Source: manyu_test/REVIEW.md (newly created)
Notes: 项目此前无 REVIEW.md，基于设计文档和代码结构生成了包含跨仓对齐规则的评审配置文件。

---

## Lane verdict table

| Lane | Verdict | Notes |
|---|---|---|
| align | APPROVE | 实现与设计文档 §4-§5 的 API 契约完全一致，Header 命名、维度枚举值、导出 CSV 列名、端口号等跨仓对齐点均匹配。 |
| design | APPROVE_WITH_COMMENTS | 存在 SQL 注入风险（f-string 拼接）、sys.path hacks 导入、模块导入时副作用初始化等设计问题。 |
| trim | APPROVE_WITH_COMMENTS | 存在未使用的 HTTPException 导入、StreamingResponse 非真流式等可精简项。 |
| cause | NOT_RUN | 本次为全新功能开发，非 bug 修复，无需 root-cause 审查。 |
| verify | REJECT | 发现前端未捕获异常（unhandled promise rejection）、测试用例时序依赖（async 写入未等待）、SQLite 并发写入风险、ECharts 事件监听器泄漏等实现缺陷。 |

---

## Blocking findings

### [HIGH] [VERIFY] [IMPLEMENTATION-BUG] manyu_test1/js/app.js:128-132
bubble-sort 的输入解析逻辑中 `throw new Error(...)` 位于 `.map()` 回调内，且该 `.map()` 调用在 try/catch 块之外，导致非数字输入时抛出未捕获的 Promise rejection，用户看不到错误提示。

Evidence:
```javascript
var numbers = raw.split(',').map(function(s) {   // line 128
    var n = parseFloat(s.trim());
    if (isNaN(n)) throw new Error('包含非数字: ' + s);  // line 130 — 在 try 外面
    return n;
});
try {                                               // line 133
    var data = await apiCall('POST', '/api/bubble-sort', { numbers: numbers });
```
Recommendation: 将 `.map()` 调用移入 try 块内，或将 map 中的 throw 改为返回标记值并在 try 块内统一校验。

---

### [HIGH] [VERIFY] [TEST-GAP] manyu_test/tests/test_apis.py:70-76
`TestExport.test_export_helloworld_csv` 仅校验了 HTTP 状态码和 Content-Type/Content-Disposition 头，未验证 CSV 内容是否包含埋点数据。由于埋点中间件使用 daemon 线程异步写入，测试可能在写入完成前执行查询，导致 CSV 仅含表头而无数据行，但测试仍通过。

Evidence:
```python
def test_export_helloworld_csv(self):
    client.post("/api/helloworld", headers={"X-User-Name": "ZhangSan"})
    resp = client.get("/api/export/helloworld")
    assert resp.status_code == 200
    assert "text/csv" in resp.headers["content-type"]
    # 未断言 CSV body 中包含 "ZhangSan"
```
Recommendation: 添加 `time.sleep(0.3)` 等待异步写入，并断言 CSV 响应体中包含预期的数据行。

---

### [HIGH] [VERIFY] [TEST-GAP] manyu_test/tests/test_apis.py:83-93
`TestAnalytics.test_analytics_by_dept` 在调用三个埋点 API 后立即查询 analytics，未等待异步写入线程完成，导致 `assert len(data["data"]) == 2` 可能在写入未完成时失败（实际上可能为 0 或 1）。

Evidence:
```python
def test_analytics_by_dept(self):
    client.post("/api/helloworld", headers={"X-User-Dept": "Tech"})
    client.post("/api/hash", json={"text": "x"}, headers={"X-User-Dept": "Tech"})
    client.post("/api/bubble-sort", json={"numbers": [1]}, headers={"X-User-Dept": "Product"})
    resp = client.get("/api/analytics?dimension=dept")  # 无 sleep
    assert len(data["data"]) == 2  # 竞态条件
```
Recommendation: 在 analytics 查询前添加 `time.sleep(0.3)` 等待异步写入完成。

---

### [HIGH] [DESIGN] [BOUNDARY-LEAK] manyu_test/apis/analytics.py:24
`dimension` 参数虽经 `VALID_DIMENSIONS` 白名单校验，但仍以 f-string 直接拼接进 SQL 查询。若未来有人新增维度枚举值但忘记在代码中做转义，或白名单校验被绕过，将导致 SQL 注入。

Evidence:
```python
query = f"SELECT {dimension}, COUNT(*) as cnt FROM api_call_logs"  # line 24
```
Recommendation: 使用白名单映射将 dimension 转换为硬编码的列名，而非 f-string 拼接：
```python
column_map = {"dept": "dept", "level": "level", "user_type": "user_type"}
col = column_map[dimension]  # 已在白名单校验后，安全
query = f"SELECT {col}, COUNT(*) as cnt FROM api_call_logs"
```

---

### [HIGH] [VERIFY] [CONCURRENCY] manyu_test/middleware/tracking.py:21-25
在 FastAPI async 上下文中使用 `threading.Thread`（daemon）写入 SQLite。SQLite 默认串行化写入，多线程并发时可能出现 `database is locked` 错误。虽然每次 `insert_log` 创建新连接，但高并发下仍存在写入失败风险，且 daemon 线程的异常被静默吞没。

Evidence:
```python
threading.Thread(
    target=insert_log,
    args=(get_db_path(), api_name, caller_id, caller_name, dept, level, user_type),
    daemon=True,
).start()
```
Recommendation: 添加 `check_same_thread=False` 到 SQLite 连接参数，或使用 `threading.Lock` 保护写入操作，或使用 `asyncio.to_thread` 替代裸 `threading.Thread`。

---

## Advisory findings

### [WARNING] [TRIM] [DEAD-CODE] manyu_test/apis/bubble_sort.py:7
`HTTPException` 导入未被使用。该模块的所有错误处理均依赖 Pydantic 自动校验。

Recommendation: 移除 `from fastapi import APIRouter, HTTPException` 中未使用的 `HTTPException`。

---

### [WARNING] [TRIM] [DEAD-CODE] manyu_test/apis/hash_api.py:1
`HTTPException` 导入未被使用。hash 接口的错误处理完全依赖 Pydantic `Field(min_length=1)` 自动校验。

Recommendation: 移除 `from fastapi import APIRouter, HTTPException` 中未使用的 `HTTPException`。

---

### [WARNING] [VERIFY] [BOUNDARY-CASE] manyu_test1/js/charts.js:54-56
每次调用 `renderChart()` 都会通过 `window.addEventListener('resize', ...)` 注册新的 resize 监听器，但从未移除旧的监听器。多次切换维度/图表类型后会累积大量监听器，导致内存泄漏和性能下降。

Evidence:
```javascript
window.addEventListener('resize', function() {  // line 54 — 每次 renderChart 都新增
    chartInstance && chartInstance.resize();
});
```
Recommendation: 使用具名函数并在添加新监听器前移除旧的，或使用 `{ once: false }` 模式配合标记变量避免重复注册。

---

### [WARNING] [DESIGN] [WRONG-LAYER] manyu_test/apis/bubble_sort.py:4-5
使用 `sys.path.insert(0, ...)` 动态修改模块搜索路径来导入根目录的 `bubble_sort.py`，这是脆弱且非标准的做法。若项目结构调整或部署环境变化，导入将失败。

Evidence:
```python
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))
from bubble_sort import bubble_sort as bs
```
Recommendation: 将 `bubble_sort.py` 移至 `apis/` 或项目包内，使用标准相对/绝对导入；或将算法逻辑封装为独立模块并通过 `setup.py` / `pyproject.toml` 安装。

---

### [WARNING] [TRIM] [DATA-EXPOSURE] manyu_test/export/csv_writer.py:33-34
`StreamingResponse(iter([output.getvalue()]), ...)` 将整个 CSV 内容一次性加载到内存后返回，并非真正的流式传输。大数据量时可能导致内存压力。

Evidence:
```python
output.seek(0)
return StreamingResponse(
    iter([output.getvalue()]),  # 一次性读取全部内容
    ...
)
```
Recommendation: 使用生成器逐行 yield CSV 内容，或使用 `io.BytesIO` 配合分块读取以实现真正的流式响应。

---

## Skipped lanes and reasons

| Lane | Reason |
|---|---|
| cause | 本次为全新功能开发，非 bug 修复场景，无需进行 root-cause closure 审查。 |

---

## Suggested next actions

1. **修复 [HIGH] app.js:128-132**：将 bubble-sort 的输入解析移入 try/catch 块内。
2. **修复 [HIGH] test_apis.py:70-76,83-93**：在异步写入测试中添加 `time.sleep` 等待，并断言 CSV 内容。
3. **修复 [HIGH] analytics.py:24**：用列名映射替代 f-string SQL 拼接。
4. **修复 [HIGH] middleware/tracking.py:21-25**：为 SQLite 并发写入添加保护机制。
5. **清理 [WARNING]**：移除未使用的 `HTTPException` 导入，修复 charts.js 事件监听器泄漏。
6. **重构 [WARNING]**：消除 `sys.path.insert` hack，改用标准包导入。

---

VERDICT: **REJECT**