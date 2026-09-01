# API 接口 + 埋点追踪 + 可视化报表 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 manyu_test 构建 Flask 后端（3 业务接口 + 导出 + 埋点报表），为 manyu_test1 构建单页面前端（三 Tab 展示 + 导出 + 图表报表）。

**Architecture:** Flask 后端通过 before_request 中间件自动埋点，内存存储调用记录；前端通过 HTTP Header 传递身份信息，Chart.js CDN 渲染图表。

**Tech Stack:** Python 3 + Flask, 原生 HTML/CSS/JS + Chart.js 4.x CDN

**Repos:** [manyu_test] 后端 (base: cred-test-20260716022903), [manyu_test1] 前端 (base: main)

---

## Global Constraints

- 所有路径使用仓库物理绝对路径
- 冒泡排序复用已有 `bubble_sort.py`（不修改）
- 埋点数据存储在内存中（dict），重启丢失
- 前端通过 Header 传递身份：`X-User-Name`, `X-User-Type`, `X-User-Level`, `X-User-Dept`
- Flask 启用 CORS，允许前端跨域请求
- 零外部数据库依赖
- 测试使用 pytest

---

## Task 1: Backend Scaffolding

**Files:**
- Create: `[manyu_test]/app.py`
- Create: `[manyu_test]/requirements.txt`

**Interfaces:**
- Produces: Flask app instance `app` (importable by route modules), CORS enabled

- [ ] **Step 1: Write requirements.txt**

```
flask>=3.0
flask-cors>=4.0
```

- [ ] **Step 2: Install dependencies**

Run: `pip install -r requirements.txt`

- [ ] **Step 3: Write app.py**

```python
"""manyu_test Flask 后端主入口"""

from flask import Flask
from flask_cors import CORS


def create_app() -> Flask:
    app = Flask(__name__)
    CORS(app)

    # 注册路由
    from routes.helloworld import helloworld_bp
    from routes.hash import hash_bp
    from routes.bubblesort import bubblesort_bp
    from routes.export import export_bp
    from routes.tracking import tracking_bp

    app.register_blueprint(helloworld_bp)
    app.register_blueprint(hash_bp)
    app.register_blueprint(bubblesort_bp)
    app.register_blueprint(export_bp)
    app.register_blueprint(tracking_bp)

    return app


app = create_app()

if __name__ == "__main__":
    app.run(debug=True, port=5000)
```

- [ ] **Step 4: Create package `__init__.py` files**

Create empty `[manyu_test]/routes/__init__.py` and `[manyu_test]/middleware/__init__.py`

- [ ] **Step 5: Verify app starts**

Run: `python -c "from app import app; print('App created:', app.name)"`
Expected: `App created: __main__` (or similar, no errors)

---

## Task 2: Tracking Middleware

**Files:**
- Create: `[manyu_test]/middleware/tracking.py`
- Modify: `[manyu_test]/app.py`

**Interfaces:**
- Produces: `tracking_store` (list of dicts), `init_tracking(app)` (registers before_request hook)
- Record format: `{"name": str, "type": str, "level": str, "dept": str, "endpoint": str, "timestamp": str, "params": str}`

- [ ] **Step 1: Write middleware/tracking.py**

```python
"""埋点中间件：记录每次 API 调用的用户信息和接口详情"""

from datetime import datetime, timezone
from flask import Flask, request

# 全局内存存储
tracking_store: list[dict] = []


def init_tracking(app: Flask) -> None:
    """注册 before_request 钩子，自动记录 API 调用"""

    @app.before_request
    def record_tracking():
        # 只记录 /api/ 开头的请求，排除 /api/tracking 自身
        if not request.path.startswith("/api/"):
            return
        if request.path.startswith("/api/tracking"):
            return

        record = {
            "name": request.headers.get("X-User-Name", "anonymous"),
            "type": request.headers.get("X-User-Type", "unknown"),
            "level": request.headers.get("X-User-Level", "unknown"),
            "dept": request.headers.get("X-User-Dept", "unknown"),
            "endpoint": request.path,
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "params": str(request.get_json(silent=True) or request.args.to_dict()),
        }
        tracking_store.append(record)
```

- [ ] **Step 2: Wire middleware into app.py**

Modify `[manyu_test]/app.py` — add import and init call after `CORS(app)`:

```python
from middleware.tracking import init_tracking

# in create_app(), after CORS(app):
init_tracking(app)
```

- [ ] **Step 3: Verify import**

Run: `python -c "from middleware.tracking import tracking_store, init_tracking; print('Middleware loaded, store:', type(tracking_store))"`
Expected: `Middleware loaded, store: <class 'list'>`

---

## Task 3: /api/helloworld Route

**Files:**
- Create: `[manyu_test]/routes/helloworld.py`
- Create: `[manyu_test]/tests/test_helloworld.py`

**Interfaces:**
- Consumes: `app` from app.py (via blueprint), tracking middleware (auto)
- Produces: `helloworld_bp` Blueprint, `GET /api/helloworld` → `{"result": "Hello, World!"}`

- [ ] **Step 1: Write failing test**

`[manyu_test]/tests/test_helloworld.py`:

```python
import pytest
from app import create_app


@pytest.fixture
def client():
    app = create_app()
    app.config["TESTING"] = True
    return app.test_client()


def test_helloworld_returns_hello_world(client):
    resp = client.get("/api/helloworld")
    assert resp.status_code == 200
    data = resp.get_json()
    assert data["result"] == "Hello, World!"
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd [manyu_test] && python -m pytest tests/test_helloworld.py -v`
Expected: FAIL (ImportError or 404)

- [ ] **Step 3: Write routes/helloworld.py**

```python
"""HelloWorld 接口"""

from flask import Blueprint, jsonify

helloworld_bp = Blueprint("helloworld", __name__)


@helloworld_bp.route("/api/helloworld", methods=["GET"])
def helloworld():
    return jsonify({"result": "Hello, World!"})
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd [manyu_test] && python -m pytest tests/test_helloworld.py -v`
Expected: PASS

---

## Task 4: /api/hash Route

**Files:**
- Create: `[manyu_test]/routes/hash.py`
- Create: `[manyu_test]/tests/test_hash.py`

**Interfaces:**
- Consumes: `app` from app.py (via blueprint)
- Produces: `hash_bp` Blueprint, `POST /api/hash` with body `{"algorithm": "sha256|md5|sha1", "text": "..."}` → `{"algorithm": "...", "input": "...", "hash": "..."}`

- [ ] **Step 1: Write failing test**

`[manyu_test]/tests/test_hash.py`:

```python
import pytest
from app import create_app


@pytest.fixture
def client():
    app = create_app()
    app.config["TESTING"] = True
    return app.test_client()


def test_hash_sha256(client):
    resp = client.post("/api/hash", json={"algorithm": "sha256", "text": "hello"})
    assert resp.status_code == 200
    data = resp.get_json()
    assert data["algorithm"] == "sha256"
    assert data["input"] == "hello"
    assert len(data["hash"]) == 64  # SHA256 hex length


def test_hash_md5(client):
    resp = client.post("/api/hash", json={"algorithm": "md5", "text": "hello"})
    assert resp.status_code == 200
    data = resp.get_json()
    assert len(data["hash"]) == 32


def test_hash_sha1(client):
    resp = client.post("/api/hash", json={"algorithm": "sha1", "text": "hello"})
    assert resp.status_code == 200
    data = resp.get_json()
    assert len(data["hash"]) == 40


def test_hash_invalid_algorithm(client):
    resp = client.post("/api/hash", json={"algorithm": "crc32", "text": "hello"})
    assert resp.status_code == 400
    data = resp.get_json()
    assert "error" in data
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd [manyu_test] && python -m pytest tests/test_hash.py -v`
Expected: FAIL

- [ ] **Step 3: Write routes/hash.py**

```python
"""哈希算法接口"""

import hashlib
from flask import Blueprint, jsonify, request

hash_bp = Blueprint("hash", __name__)

SUPPORTED_ALGORITHMS = {"sha256", "md5", "sha1"}


def compute_hash(algorithm: str, text: str) -> str:
    h = hashlib.new(algorithm)
    h.update(text.encode("utf-8"))
    return h.hexdigest()


@hash_bp.route("/api/hash", methods=["POST"])
def hash_endpoint():
    data = request.get_json(silent=True) or {}
    algorithm = data.get("algorithm", "").lower()
    text = data.get("text", "")

    if algorithm not in SUPPORTED_ALGORITHMS:
        return jsonify({"error": f"Unsupported algorithm: {algorithm}. Supported: {', '.join(sorted(SUPPORTED_ALGORITHMS))}"}), 400

    return jsonify({
        "algorithm": algorithm,
        "input": text,
        "hash": compute_hash(algorithm, text),
    })
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd [manyu_test] && python -m pytest tests/test_hash.py -v`
Expected: PASS

---

## Task 5: /api/bubblesort Route

**Files:**
- Create: `[manyu_test]/routes/bubblesort.py`
- Create: `[manyu_test]/tests/test_bubblesort.py`

**Interfaces:**
- Consumes: `bubble_sort` from `bubble_sort.py` (existing)
- Produces: `bubblesort_bp` Blueprint, `POST /api/bubblesort` with body `{"array": [5,3,8,4,2]}` → `{"input": [...], "sorted": [...], "steps": N}`

- [ ] **Step 1: Write failing test**

`[manyu_test]/tests/test_bubblesort.py`:

```python
import pytest
from app import create_app


@pytest.fixture
def client():
    app = create_app()
    app.config["TESTING"] = True
    return app.test_client()


def test_bubblesort_sorts_array(client):
    resp = client.post("/api/bubblesort", json={"array": [5, 3, 8, 4, 2]})
    assert resp.status_code == 200
    data = resp.get_json()
    assert data["input"] == [5, 3, 8, 4, 2]
    assert data["sorted"] == [2, 3, 4, 5, 8]
    assert data["steps"] > 0


def test_bubblesort_empty_array(client):
    resp = client.post("/api/bubblesort", json={"array": []})
    assert resp.status_code == 200
    data = resp.get_json()
    assert data["sorted"] == []
    assert data["steps"] == 0


def test_bubblesort_invalid_input(client):
    resp = client.post("/api/bubblesort", json={"array": "not_an_array"})
    assert resp.status_code == 400
    data = resp.get_json()
    assert "error" in data
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd [manyu_test] && python -m pytest tests/test_bubblesort.py -v`
Expected: FAIL

- [ ] **Step 3: Write routes/bubblesort.py**

```python
"""冒泡排序接口"""

from flask import Blueprint, jsonify, request
from bubble_sort import bubble_sort_optimized

bubblesort_bp = Blueprint("bubblesort", __name__)


@bubblesort_bp.route("/api/bubblesort", methods=["POST"])
def bubblesort_endpoint():
    data = request.get_json(silent=True) or {}
    arr = data.get("array")

    if not isinstance(arr, list):
        return jsonify({"error": "array must be a list of numbers"}), 400

    n = len(arr)
    sorted_arr = bubble_sort_optimized(arr.copy())

    return jsonify({
        "input": arr,
        "sorted": sorted_arr,
        "steps": n * (n - 1) // 2,  # 冒泡排序最大比较次数
    })
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd [manyu_test] && python -m pytest tests/test_bubblesort.py -v`
Expected: PASS

---

## Task 6: /api/export Route

**Files:**
- Create: `[manyu_test]/routes/export.py`
- Create: `[manyu_test]/tests/test_export.py`

**Interfaces:**
- Consumes: `tracking_store` from `middleware/tracking.py`
- Produces: `export_bp` Blueprint, `GET /api/export?type=helloworld|hash|bubblesort` → CSV file download

- [ ] **Step 1: Write failing test**

`[manyu_test]/tests/test_export.py`:

```python
import pytest
from app import create_app
from middleware.tracking import tracking_store


@pytest.fixture
def client():
    app = create_app()
    app.config["TESTING"] = True
    return app.test_client()


@pytest.fixture(autouse=True)
def clear_store():
    tracking_store.clear()


def test_export_returns_csv(client):
    # 先制造一条埋点记录
    client.get("/api/helloworld", headers={
        "X-User-Name": "testuser",
        "X-User-Type": "正式员工",
        "X-User-Level": "中级",
        "X-User-Dept": "技术部",
    })
    resp = client.get("/api/export?type=helloworld")
    assert resp.status_code == 200
    assert resp.content_type == "text/csv"
    assert "attachment" in resp.headers.get("Content-Disposition", "")
    body = resp.data.decode("utf-8")
    assert "testuser" in body
    assert "正式员工" in body


def test_export_empty_when_no_matching_type(client):
    resp = client.get("/api/export?type=helloworld")
    assert resp.status_code == 200
    body = resp.data.decode("utf-8")
    # 只有 header 行，没有数据行
    lines = body.strip().split("\n")
    assert len(lines) == 1  # header only
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd [manyu_test] && python -m pytest tests/test_export.py -v`
Expected: FAIL

- [ ] **Step 3: Write routes/export.py**

```python
"""导出接口：CSV 格式导出调用记录"""

import csv
import io
from flask import Blueprint, request, Response
from middleware.tracking import tracking_store

export_bp = Blueprint("export", __name__)


@export_bp.route("/api/export", methods=["GET"])
def export_csv():
    export_type = request.args.get("type", "")

    # 过滤对应接口的记录
    type_path_map = {
        "helloworld": "/api/helloworld",
        "hash": "/api/hash",
        "bubblesort": "/api/bubblesort",
    }
    target_path = type_path_map.get(export_type, "")
    records = [r for r in tracking_store if r["endpoint"] == target_path] if target_path else tracking_store

    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow(["姓名", "人员类型", "人员层级", "人员部门", "接口", "时间", "参数"])
    for r in records:
        writer.writerow([r["name"], r["type"], r["level"], r["dept"], r["endpoint"], r["timestamp"], r["params"]])

    csv_content = output.getvalue()
    output.close()

    return Response(
        csv_content,
        mimetype="text/csv",
        headers={"Content-Disposition": f"attachment; filename={export_type}_export.csv"}
    )
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd [manyu_test] && python -m pytest tests/test_export.py -v`
Expected: PASS

---

## Task 7: /api/tracking Route

**Files:**
- Create: `[manyu_test]/routes/tracking.py`
- Create: `[manyu_test]/tests/test_tracking.py`

**Interfaces:**
- Consumes: `tracking_store` from `middleware/tracking.py`
- Produces: `tracking_bp` Blueprint, `GET /api/tracking?dimension=type|level|dept|time` → aggregated JSON

- [ ] **Step 1: Write failing test**

`[manyu_test]/tests/test_tracking.py`:

```python
import pytest
from app import create_app
from middleware.tracking import tracking_store


@pytest.fixture
def client():
    app = create_app()
    app.config["TESTING"] = True
    return app.test_client()


@pytest.fixture(autouse=True)
def clear_store():
    tracking_store.clear()


def _seed_data(client):
    for i in range(3):
        client.get("/api/helloworld", headers={
            "X-User-Name": f"user{i}",
            "X-User-Type": "正式员工",
            "X-User-Level": "中级",
            "X-User-Dept": "技术部",
        })
    for i in range(2):
        client.get("/api/helloworld", headers={
            "X-User-Name": f"ext{i}",
            "X-User-Type": "外包",
            "X-User-Level": "初级",
            "X-User-Dept": "运营部",
        })


def test_tracking_by_type(client):
    _seed_data(client)
    resp = client.get("/api/tracking?dimension=type")
    assert resp.status_code == 200
    data = resp.get_json()
    assert data["dimension"] == "type"
    assert data["summary"]["total"] == 5
    types = {item["key"]: item["count"] for item in data["data"]}
    assert types["正式员工"] == 3
    assert types["外包"] == 2


def test_tracking_by_level(client):
    _seed_data(client)
    resp = client.get("/api/tracking?dimension=level")
    assert resp.status_code == 200
    data = resp.get_json()
    levels = {item["key"]: item["count"] for item in data["data"]}
    assert levels["中级"] == 3
    assert levels["初级"] == 2


def test_tracking_by_dept(client):
    _seed_data(client)
    resp = client.get("/api/tracking?dimension=dept")
    assert resp.status_code == 200
    data = resp.get_json()
    depts = {item["key"]: item["count"] for item in data["data"]}
    assert depts["技术部"] == 3
    assert depts["运营部"] == 2


def test_tracking_by_time(client):
    _seed_data(client)
    resp = client.get("/api/tracking?dimension=time")
    assert resp.status_code == 200
    data = resp.get_json()
    assert data["dimension"] == "time"
    assert data["summary"]["total"] == 5
    assert len(data["data"]) >= 1
    for item in data["data"]:
        assert "time" in item
        assert "count" in item


def test_tracking_default_dimension(client):
    _seed_data(client)
    resp = client.get("/api/tracking")
    assert resp.status_code == 200
    data = resp.get_json()
    assert data["dimension"] == "type"  # default
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd [manyu_test] && python -m pytest tests/test_tracking.py -v`
Expected: FAIL

- [ ] **Step 3: Write routes/tracking.py**

```python
"""埋点报表接口"""

from collections import Counter
from datetime import datetime, timezone
from flask import Blueprint, jsonify, request
from middleware.tracking import tracking_store

tracking_bp = Blueprint("tracking", __name__)


def _aggregate_by_dimension(records: list[dict], dimension: str) -> list[dict]:
    counter = Counter()
    for r in records:
        key = r.get(dimension, "unknown")
        counter[key] += 1
    return [{"key": k, "count": v} for k, v in counter.most_common()]


def _aggregate_by_time(records: list[dict]) -> list[dict]:
    """按小时聚合时间序列"""
    counter = Counter()
    for r in records:
        ts = r.get("timestamp", "")
        try:
            dt = datetime.fromisoformat(ts)
            hour_key = dt.strftime("%Y-%m-%dT%H:00:00")
        except (ValueError, TypeError):
            hour_key = "unknown"
        counter[hour_key] += 1
    return [{"time": k, "count": v} for k, v in sorted(counter.items())]


@tracking_bp.route("/api/tracking", methods=["GET"])
def tracking_report():
    dimension = request.args.get("dimension", "type")

    if dimension == "time":
        data = _aggregate_by_time(tracking_store)
    elif dimension in ("type", "level", "dept"):
        data = _aggregate_by_dimension(tracking_store, dimension)
    else:
        data = _aggregate_by_dimension(tracking_store, "type")
        dimension = "type"

    return jsonify({
        "dimension": dimension,
        "data": data,
        "summary": {"total": len(tracking_store)},
    })
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd [manyu_test] && python -m pytest tests/test_tracking.py -v`
Expected: PASS

---

## Task 8: Frontend index.html

**Files:**
- Create: `[manyu_test1]/index.html`

**Interfaces:**
- Consumes: All 5 backend APIs at `http://localhost:5000`
- Produces: Complete single-page application with identity input, 3 tabs, export, chart dashboard

- [ ] **Step 1: Write index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>API 调用与埋点追踪</title>
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
<style>
* { box-sizing: border-box; margin: 0; padding: 0; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #f0f2f5; color: #333; }
.container { max-width: 1100px; margin: 0 auto; padding: 20px; }
/* 身份输入栏 */
.identity-bar { background: #fff; border-radius: 8px; padding: 16px 20px; margin-bottom: 16px; display: flex; flex-wrap: wrap; gap: 12px; align-items: flex-end; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }
.identity-bar label { font-size: 13px; color: #666; display: block; margin-bottom: 4px; }
.identity-bar input, .identity-bar select { padding: 8px 12px; border: 1px solid #d9d9d9; border-radius: 6px; font-size: 14px; min-width: 120px; }
/* Tabs */
.tabs { display: flex; gap: 0; margin-bottom: 0; }
.tab-btn { flex: 1; padding: 12px; border: none; background: #e8e8e8; cursor: pointer; font-size: 14px; font-weight: 500; transition: background 0.2s; border-radius: 8px 8px 0 0; }
.tab-btn.active { background: #fff; color: #1677ff; }
.tab-content { background: #fff; padding: 24px; border-radius: 0 0 8px 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); margin-bottom: 16px; display: none; }
.tab-content.active { display: block; }
/* 操作区 */
.op-row { display: flex; gap: 12px; align-items: flex-end; margin-bottom: 16px; flex-wrap: wrap; }
.op-row input, .op-row select { padding: 8px 12px; border: 1px solid #d9d9d9; border-radius: 6px; font-size: 14px; }
.btn { padding: 8px 20px; border: none; border-radius: 6px; cursor: pointer; font-size: 14px; font-weight: 500; }
.btn-primary { background: #1677ff; color: #fff; }
.btn-primary:hover { background: #4096ff; }
.btn-export { background: #52c41a; color: #fff; }
.btn-export:hover { background: #73d13d; }
.result-box { background: #fafafa; border: 1px solid #f0f0f0; border-radius: 6px; padding: 16px; min-height: 60px; font-family: 'Courier New', monospace; font-size: 13px; white-space: pre-wrap; word-break: break-all; }
/* 报表区 */
.chart-section { background: #fff; border-radius: 8px; padding: 24px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }
.chart-controls { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.chart-controls select { padding: 8px 12px; border: 1px solid #d9d9d9; border-radius: 6px; font-size: 14px; }
.chart-container { max-width: 700px; margin: 0 auto; }
</style>
</head>
<body>
<div class="container">

  <!-- ① 身份输入栏 -->
  <div class="identity-bar">
    <div><label>姓名 *</label><input type="text" id="userName" placeholder="请输入姓名" value="张三"></div>
    <div><label>人员类型</label>
      <select id="userType"><option value="正式员工">正式员工</option><option value="外包">外包</option><option value="实习生">实习生</option></select></div>
    <div><label>人员层级</label>
      <select id="userLevel"><option value="初级">初级</option><option value="中级" selected>中级</option><option value="高级">高级</option><option value="专家">专家</option></select></div>
    <div><label>人员部门</label>
      <select id="userDept"><option value="技术部" selected>技术部</option><option value="产品部">产品部</option><option value="运营部">运营部</option><option value="市场部">市场部</option></select></div>
  </div>

  <!-- ② 三 Tab 结果区 -->
  <div class="tabs">
    <button class="tab-btn active" onclick="switchTab('helloworld')">HelloWorld</button>
    <button class="tab-btn" onclick="switchTab('hash')">Hash</button>
    <button class="tab-btn" onclick="switchTab('bubblesort')">冒泡排序</button>
  </div>

  <!-- Tab 1: HelloWorld -->
  <div id="tab-helloworld" class="tab-content active">
    <div class="op-row">
      <button class="btn btn-primary" onclick="callHelloWorld()">执行</button>
      <button class="btn btn-export" onclick="exportCSV('helloworld')">导出 CSV</button>
    </div>
    <div id="result-helloworld" class="result-box">点击"执行"查看结果</div>
  </div>

  <!-- Tab 2: Hash -->
  <div id="tab-hash" class="tab-content">
    <div class="op-row">
      <select id="hashAlgo"><option value="sha256">SHA256</option><option value="md5">MD5</option><option value="sha1">SHA1</option></select>
      <input type="text" id="hashText" placeholder="输入要哈希的文本" value="hello world" style="flex:1; min-width:200px;">
      <button class="btn btn-primary" onclick="callHash()">执行</button>
      <button class="btn btn-export" onclick="exportCSV('hash')">导出 CSV</button>
    </div>
    <div id="result-hash" class="result-box">点击"执行"查看结果</div>
  </div>

  <!-- Tab 3: 冒泡排序 -->
  <div id="tab-bubblesort" class="tab-content">
    <div class="op-row">
      <input type="text" id="sortArray" placeholder="输入数组，逗号分隔" value="5,3,8,4,2" style="flex:1; min-width:200px;">
      <button class="btn btn-primary" onclick="callBubbleSort()">执行</button>
      <button class="btn btn-export" onclick="exportCSV('bubblesort')">导出 CSV</button>
    </div>
    <div id="result-bubblesort" class="result-box">点击"执行"查看结果</div>
  </div>

  <!-- ④ 埋点报表区 -->
  <div class="chart-section">
    <h3 style="margin-bottom:12px;">调用情况报表</h3>
    <div class="chart-controls">
      <label>维度：<select id="chartDimension" onchange="refreshChart()">
        <option value="type">人员类型</option>
        <option value="level">人员层级</option>
        <option value="dept">人员部门</option>
        <option value="time">时间趋势</option>
      </select></label>
      <label>图表类型：<select id="chartType" onchange="refreshChart()">
        <option value="bar">柱状图</option>
        <option value="pie">饼图</option>
        <option value="line">折线图</option>
      </select></label>
      <button class="btn btn-primary" onclick="refreshChart()">刷新</button>
    </div>
    <div class="chart-container"><canvas id="trackingChart"></canvas></div>
  </div>

</div>

<script>
const BASE = 'http://localhost:5000';

function getHeaders() {
  return {
    'Content-Type': 'application/json',
    'X-User-Name': document.getElementById('userName').value || 'anonymous',
    'X-User-Type': document.getElementById('userType').value,
    'X-User-Level': document.getElementById('userLevel').value,
    'X-User-Dept': document.getElementById('userDept').value,
  };
}

function switchTab(tab) {
  document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
  document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
  document.querySelector(`.tab-btn[onclick*="${tab}"]`).classList.add('active');
  document.getElementById(`tab-${tab}`).classList.add('active');
}

async function callHelloWorld() {
  const res = await fetch(`${BASE}/api/helloworld`, { headers: getHeaders() });
  const data = await res.json();
  document.getElementById('result-helloworld').textContent = JSON.stringify(data, null, 2);
}

async function callHash() {
  const algo = document.getElementById('hashAlgo').value;
  const text = document.getElementById('hashText').value;
  const res = await fetch(`${BASE}/api/hash`, {
    method: 'POST', headers: getHeaders(),
    body: JSON.stringify({ algorithm: algo, text: text }),
  });
  const data = await res.json();
  document.getElementById('result-hash').textContent = JSON.stringify(data, null, 2);
}

async function callBubbleSort() {
  const raw = document.getElementById('sortArray').value;
  const arr = raw.split(',').map(s => Number(s.trim()));
  const res = await fetch(`${BASE}/api/bubblesort`, {
    method: 'POST', headers: getHeaders(),
    body: JSON.stringify({ array: arr }),
  });
  const data = await res.json();
  document.getElementById('result-bubblesort').textContent = JSON.stringify(data, null, 2);
}

function exportCSV(type) {
  const url = `${BASE}/api/export?type=${type}`;
  const a = document.createElement('a');
  a.href = url;
  a.download = `${type}_export.csv`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
}

let chartInstance = null;

async function refreshChart() {
  const dim = document.getElementById('chartDimension').value;
  const chartType = document.getElementById('chartType').value;
  const res = await fetch(`${BASE}/api/tracking?dimension=${dim}`);
  const json = await res.json();
  const labels = json.data.map(d => dim === 'time' ? d.time : d.key);
  const values = json.data.map(d => d.count);

  const ctx = document.getElementById('trackingChart').getContext('2d');
  if (chartInstance) chartInstance.destroy();

  chartInstance = new Chart(ctx, {
    type: chartType,
    data: {
      labels: labels,
      datasets: [{
        label: '调用次数',
        data: values,
        backgroundColor: ['#1677ff','#52c41a','#faad14','#f5222d','#722ed1','#13c2c2','#eb2f96'],
        borderColor: '#1677ff',
        borderWidth: 1,
      }],
    },
    options: {
      responsive: true,
      plugins: {
        title: { display: true, text: `按${dim === 'time' ? '时间' : dim === 'type' ? '人员类型' : dim === 'level' ? '人员层级' : '人员部门'}统计` }
      }
    }
  });
}

refreshChart();
</script>
</body>
</html>
```

- [ ] **Step 2: Verify frontend loads**

Open `index.html` in a browser, or verify file exists and is valid HTML.

Run: `python -c "with open('[manyu_test1]/index.html') as f: content = f.read(); assert '<html' in content; print('Valid HTML,', len(content), 'chars')"`
Expected: `Valid HTML, ... chars`

---

## Final Integration Test

- [ ] **Step 1: Start backend**

Run (in background): `cd [manyu_test] && python app.py`

- [ ] **Step 2: Run all tests**

Run: `cd [manyu_test] && python -m pytest tests/ -v`
Expected: All tests PASS

- [ ] **Step 3: Manual smoke test**

```bash
# Test helloworld
curl http://localhost:5000/api/helloworld

# Test hash
curl -X POST http://localhost:5000/api/hash \
  -H "Content-Type: application/json" \
  -H "X-User-Name: test" \
  -H "X-User-Type: 正式员工" \
  -H "X-User-Level: 中级" \
  -H "X-User-Dept: 技术部" \
  -d '{"algorithm":"sha256","text":"hello"}'

# Test bubblesort
curl -X POST http://localhost:5000/api/bubblesort \
  -H "Content-Type: application/json" \
  -H "X-User-Name: test" \
  -H "X-User-Type: 正式员工" \
  -H "X-User-Level: 中级" \
  -H "X-User-Dept: 技术部" \
  -d '{"array":[5,3,8,4,2]}'

# Test tracking
curl http://localhost:5000/api/tracking?dimension=type

# Test export
curl http://localhost:5000/api/export?type=helloworld
```