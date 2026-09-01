# 三接口演示 + 埋点可视化 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个演示平台，后端提供三个计算接口（helloworld、SHA256哈希、冒泡排序）+ 埋点记录 + CSV导出 + 维度聚合分析；前端提供三 Tab 页面展示结果、导出下载和 ECharts 可视化报表。

**Architecture:** 后端 `manyu_test`（FastAPI + SQLite），前端 `manyu_test1`（原生 HTML/JS + ECharts CDN）。前端通过 HTTP 请求后端 API，用户身份通过自定义 Header 透传，埋点中间件异步写入 SQLite，导出和报表接口从 SQLite 聚合查询。

**Tech Stack:** Python 3, FastAPI, uvicorn, SQLite (sqlite3), pytest, httpx (TestClient)；HTML5, CSS3, vanilla JavaScript, ECharts 5.x (CDN)。

## Global Constraints

- 后端端口：`8000`
- CORS：允许所有来源 (`*`)
- 导出文件名：`{type}_export.csv`
- SQLite 文件路径：`manyu_test/tracking.db`
- 前端用户模拟区：支持预设用户快速切换
- 冒泡排序复用现有 `bubble_sort.py`，不修改原文件
- 所有 API 契约严格遵循设计文档 §3.2
- 埋点中间件仅记录 `/api/helloworld`、`/api/hash`、`/api/bubble-sort`，导出和 analytics 自身不计入

---

## Task 1: Backend Scaffold — FastAPI 入口与目录结构

**Files:**
- Create: `manyu_test/main.py`
- Create: `manyu_test/requirements.txt`
- Create: `manyu_test/apis/__init__.py`
- Create: `manyu_test/middleware/__init__.py`
- Create: `manyu_test/models/__init__.py`
- Create: `manyu_test/export/__init__.py`

**Interfaces:**
- Produces: FastAPI app object `app` in `main.py`，注册 CORS 中间件，端口 8000

- [ ] **Step 1: 创建 requirements.txt**

```txt
fastapi>=0.100.0
uvicorn>=0.23.0
```

- [ ] **Step 2: 创建空 `__init__.py` 文件**

执行：
```bash
mkdir -p manyu_test/apis manyu_test/middleware manyu_test/models manyu_test/export
touch manyu_test/apis/__init__.py manyu_test/middleware/__init__.py manyu_test/models/__init__.py manyu_test/export/__init__.py
```

- [ ] **Step 3: 编写 main.py**

```python
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

app = FastAPI(title="三接口演示平台", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
def health_check():
    return {"status": "ok"}


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)
```

- [ ] **Step 4: 验证服务可启动**

```bash
cd manyu_test && python main.py &
sleep 2
curl http://localhost:8000/health
# Expected: {"status":"ok"}
kill %1
```

- [ ] **Step 5: Commit**

```bash
git add requirements.txt main.py apis/__init__.py middleware/__init__.py models/__init__.py export/__init__.py
git commit -m "feat: add FastAPI scaffold with CORS and health check"
```

---

## Task 2: SQLite 数据模型 — 埋点日志表

**Files:**
- Create: `manyu_test/models/tracking.py`

**Interfaces:**
- Produces: `init_db(db_path: str)` — 创建 `api_call_logs` 表；`insert_log(db_path, api_name, caller_id, caller_name, dept, level, user_type)` — 插入一条日志；`get_db_path()` — 返回 `tracking.db` 路径

- [ ] **Step 1: 编写 models/tracking.py**

```python
import sqlite3
import os

DB_PATH = os.path.join(os.path.dirname(os.path.dirname(__file__)), "tracking.db")


def get_db_path() -> str:
    return DB_PATH


def init_db(db_path: str = DB_PATH) -> None:
    conn = sqlite3.connect(db_path)
    conn.execute(
        """
        CREATE TABLE IF NOT EXISTS api_call_logs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            api_name TEXT NOT NULL,
            caller_id TEXT,
            caller_name TEXT,
            dept TEXT,
            level TEXT,
            user_type TEXT,
            called_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
        """
    )
    conn.commit()
    conn.close()


def insert_log(
    db_path: str,
    api_name: str,
    caller_id: str | None,
    caller_name: str | None,
    dept: str | None,
    level: str | None,
    user_type: str | None,
) -> None:
    conn = sqlite3.connect(db_path)
    conn.execute(
        """
        INSERT INTO api_call_logs (api_name, caller_id, caller_name, dept, level, user_type)
        VALUES (?, ?, ?, ?, ?, ?)
        """,
        (api_name, caller_id, caller_name, dept, level, user_type),
    )
    conn.commit()
    conn.close()
```

- [ ] **Step 2: 验证数据库初始化**

```bash
cd manyu_test
python -c "
from models.tracking import init_db, get_db_path
init_db()
import sqlite3
conn = sqlite3.connect(get_db_path())
cols = [r[1] for r in conn.execute('PRAGMA table_info(api_call_logs)')]
print(cols)
conn.close()
"
# Expected: ['id', 'api_name', 'caller_id', 'caller_name', 'dept', 'level', 'user_type', 'called_at']
```

- [ ] **Step 3: Commit**

```bash
git add models/tracking.py
git commit -m "feat: add SQLite tracking model with api_call_logs table"
```

---

## Task 3: 埋点中间件 — TrackingMiddleware

**Files:**
- Create: `manyu_test/middleware/tracking.py`

**Interfaces:**
- Consumes: `insert_log` from `models/tracking.py`, `get_db_path` from `models/tracking.py`
- Produces: `TrackingMiddleware` — ASGI 中间件，对 `/api/helloworld`、`/api/hash`、`/api/bubble-sort` 在响应后异步写入埋点日志

- [ ] **Step 1: 编写 middleware/tracking.py**

```python
import threading
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from models.tracking import insert_log, get_db_path

TRACKED_PATHS = {"/api/helloworld", "/api/hash", "/api/bubble-sort"}


class TrackingMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        response = await call_next(request)

        if request.url.path in TRACKED_PATHS:
            api_name = request.url.path.replace("/api/", "")
            caller_id = request.headers.get("X-User-Id")
            caller_name = request.headers.get("X-User-Name")
            dept = request.headers.get("X-User-Dept")
            level = request.headers.get("X-User-Level")
            user_type = request.headers.get("X-User-Type")

            threading.Thread(
                target=insert_log,
                args=(get_db_path(), api_name, caller_id, caller_name, dept, level, user_type),
                daemon=True,
            ).start()

        return response
```

- [ ] **Step 2: 在 main.py 中注册中间件**

在 `main.py` 的 `app = FastAPI(...)` 之后、`CORS` 之后的合适位置添加：

```python
from middleware.tracking import TrackingMiddleware

app.add_middleware(TrackingMiddleware)
```

- [ ] **Step 3: 验证中间件不阻塞空请求**

```bash
cd manyu_test
python -c "
from models.tracking import init_db; init_db()
"
python main.py &
sleep 2
curl -s http://localhost:8000/health
# Expected: {"status":"ok"}
kill %1
```

- [ ] **Step 4: Commit**

```bash
git add middleware/tracking.py main.py
git commit -m "feat: add tracking middleware for async call logging"
```

---

## Task 4: 三个计算接口 — helloworld、hash、bubble-sort

**Files:**
- Create: `manyu_test/apis/helloworld.py`
- Create: `manyu_test/apis/hash_api.py`
- Create: `manyu_test/apis/bubble_sort.py`
- Modify: `manyu_test/main.py`（注册路由）

**Interfaces:**
- Consumes: `bubble_sort` from `bubble_sort.py`（项目根目录现有文件）
- Produces:
  - `POST /api/helloworld` — 返回 `{"message": "Hello, World!", "timestamp": "..."}`
  - `POST /api/hash` — 请求 `{"text": "abc"}` → 返回 `{"algorithm": "SHA256", "input": "abc", "hash": "..."}`
  - `POST /api/bubble-sort` — 请求 `{"numbers": [5,3,8,4,2]}` → 返回 `{"original": [...], "sorted": [...], "algorithm": "bubble_sort"}`

- [ ] **Step 1: 编写 apis/helloworld.py**

```python
from fastapi import APIRouter
from datetime import datetime, timezone

router = APIRouter()


@router.post("/api/helloworld")
async def helloworld():
    return {
        "message": "Hello, World!",
        "timestamp": datetime.now(timezone.utc).isoformat(),
    }
```

- [ ] **Step 2: 编写 apis/hash_api.py**

```python
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field
import hashlib

router = APIRouter()


class HashRequest(BaseModel):
    text: str = Field(..., min_length=1, description="待哈希的文本")


class HashResponse(BaseModel):
    algorithm: str
    input: str
    hash: str


@router.post("/api/hash", response_model=HashResponse)
async def hash_text(req: HashRequest):
    return HashResponse(
        algorithm="SHA256",
        input=req.text,
        hash=hashlib.sha256(req.text.encode()).hexdigest(),
    )
```

- [ ] **Step 3: 编写 apis/bubble_sort.py**

```python
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))
from bubble_sort import bubble_sort as bs

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

router = APIRouter()


class SortRequest(BaseModel):
    numbers: list[int | float] = Field(..., min_length=1, description="待排序的数字数组")


class SortResponse(BaseModel):
    original: list[int | float]
    sorted: list[int | float]
    algorithm: str


@router.post("/api/bubble-sort", response_model=SortResponse)
async def bubble_sort_api(req: SortRequest):
    original = list(req.numbers)
    return SortResponse(
        original=original,
        sorted=bs(original.copy()),
        algorithm="bubble_sort",
    )
```

- [ ] **Step 4: 在 main.py 注册路由**

在 `main.py` 末尾（`if __name__` 之前）添加：

```python
from apis.helloworld import router as helloworld_router
from apis.hash_api import router as hash_router
from apis.bubble_sort import router as bubble_sort_router

app.include_router(helloworld_router)
app.include_router(hash_router)
app.include_router(bubble_sort_router)
```

- [ ] **Step 5: 验证三个接口**

```bash
cd manyu_test
python main.py &
sleep 2

echo "=== helloworld ==="
curl -s -X POST http://localhost:8000/api/helloworld | python -m json.tool

echo "=== hash ==="
curl -s -X POST http://localhost:8000/api/hash -H "Content-Type: application/json" -d '{"text":"abc"}' | python -m json.tool

echo "=== bubble-sort ==="
curl -s -X POST http://localhost:8000/api/bubble-sort -H "Content-Type: application/json" -d '{"numbers":[5,3,8,4,2]}' | python -m json.tool

kill %1
```

Expected outputs:
- helloworld: `{"message":"Hello, World!","timestamp":"2026-09-01T..."}`
- hash: `{"algorithm":"SHA256","input":"abc","hash":"ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"}`
- bubble-sort: `{"original":[5,3,8,4,2],"sorted":[2,3,4,5,8],"algorithm":"bubble_sort"}`

- [ ] **Step 6: Commit**

```bash
git add apis/helloworld.py apis/hash_api.py apis/bubble_sort.py main.py
git commit -m "feat: add three compute APIs (helloworld, hash, bubble-sort)"
```

---

## Task 5: CSV 导出接口

**Files:**
- Create: `manyu_test/export/csv_writer.py`
- Modify: `manyu_test/main.py`（注册路由）

**Interfaces:**
- Consumes: `get_db_path` from `models/tracking.py`
- Produces: `GET /api/export/{type}` — 返回 CSV 文件流，`type` ∈ `{helloworld, hash, bubble-sort}`

- [ ] **Step 1: 编写 export/csv_writer.py**

```python
import csv
import io
from fastapi import APIRouter, HTTPException
from fastapi.responses import StreamingResponse
from models.tracking import get_db_path
import sqlite3

VALID_EXPORT_TYPES = {"helloworld", "hash", "bubble-sort"}

router = APIRouter()


@router.get("/api/export/{type}")
async def export_csv(type: str):
    if type not in VALID_EXPORT_TYPES:
        raise HTTPException(status_code=400, detail=f"非法导出类型: {type}，可选值: {', '.join(sorted(VALID_EXPORT_TYPES))}")

    conn = sqlite3.connect(get_db_path())
    cursor = conn.execute(
        "SELECT caller_name, dept, level, user_type, api_name, called_at FROM api_call_logs WHERE api_name = ? ORDER BY called_at DESC",
        (type,),
    )
    rows = cursor.fetchall()
    conn.close()

    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow(["caller_name", "dept", "level", "user_type", "api_name", "timestamp"])
    for row in rows:
        writer.writerow(row)

    output.seek(0)
    return StreamingResponse(
        iter([output.getvalue()]),
        media_type="text/csv",
        headers={"Content-Disposition": f'attachment; filename="{type}_export.csv"'},
    )
```

- [ ] **Step 2: 在 main.py 注册导出路由**

在 `main.py` 的路由注册区域添加：

```python
from export.csv_writer import router as export_router

app.include_router(export_router)
```

- [ ] **Step 3: 验证导出接口**

```bash
cd manyu_test
python main.py &
sleep 2

# 先打几个埋点记录
curl -s -X POST http://localhost:8000/api/helloworld -H "X-User-Name: 张三" -H "X-User-Dept: 技术部"
curl -s -X POST http://localhost:8000/api/hash -H "Content-Type: application/json" -d '{"text":"test"}' -H "X-User-Name: 李四" -H "X-User-Dept: 产品部"

sleep 1  # 等待异步写入

echo "=== export helloworld ==="
curl -s http://localhost:8000/api/export/helloworld

echo "=== export invalid ==="
curl -s http://localhost:8000/api/export/invalid
# Expected: {"detail":"非法导出类型: invalid..."}

kill %1
```

- [ ] **Step 4: Commit**

```bash
git add export/csv_writer.py main.py
git commit -m "feat: add CSV export endpoint for call logs"
```

---

## Task 6: 埋点分析接口 — Analytics

**Files:**
- Create: `manyu_test/apis/analytics.py`
- Modify: `manyu_test/main.py`（注册路由）

**Interfaces:**
- Consumes: `get_db_path` from `models/tracking.py`
- Produces: `GET /api/analytics?dimension={dept|level|user_type}&api_name=` — 返回维度聚合数据

- [ ] **Step 1: 编写 apis/analytics.py**

```python
import sqlite3
from fastapi import APIRouter, HTTPException, Query
from models.tracking import get_db_path

VALID_DIMENSIONS = {"dept", "level", "user_type"}
VALID_API_NAMES = {"helloworld", "hash", "bubble-sort"}

router = APIRouter()


@router.get("/api/analytics")
async def analytics(
    dimension: str = Query(..., description="聚合维度: dept, level, user_type"),
    api_name: str | None = Query(None, description="可选，筛选特定接口"),
):
    if dimension not in VALID_DIMENSIONS:
        raise HTTPException(
            status_code=400,
            detail=f"非法维度: {dimension}，可选值: {', '.join(sorted(VALID_DIMENSIONS))}",
        )

    conn = sqlite3.connect(get_db_path())

    query = f"SELECT {dimension}, COUNT(*) as cnt FROM api_call_logs"
    params = []
    conditions = []

    if api_name:
        if api_name not in VALID_API_NAMES:
            conn.close()
            raise HTTPException(
                status_code=400,
                detail=f"非法接口名: {api_name}，可选值: {', '.join(sorted(VALID_API_NAMES))}",
            )
        conditions.append("api_name = ?")
        params.append(api_name)

    if conditions:
        query += " WHERE " + " AND ".join(conditions)

    query += f" GROUP BY {dimension} ORDER BY cnt DESC"

    cursor = conn.execute(query, params)
    rows = cursor.fetchall()
    conn.close()

    data = [
        {"label": row[0] if row[0] is not None else "(未设置)", "count": row[1]}
        for row in rows
    ]

    return {"dimension": dimension, "data": data}
```

- [ ] **Step 2: 在 main.py 注册 analytics 路由**

```python
from apis.analytics import router as analytics_router

app.include_router(analytics_router)
```

- [ ] **Step 3: 验证 analytics 接口**

```bash
cd manyu_test
python main.py &
sleep 2

# 打一些多维度埋点
curl -s -X POST http://localhost:8000/api/helloworld -H "X-User-Name: 张三" -H "X-User-Dept: 技术部" -H "X-User-Level: P6" -H "X-User-Type: 正式员工"
curl -s -X POST http://localhost:8000/api/hash -H "Content-Type: application/json" -d '{"text":"test"}' -H "X-User-Name: 李四" -H "X-User-Dept: 产品部" -H "X-User-Level: P7" -H "X-User-Type: 正式员工"
curl -s -X POST http://localhost:8000/api/bubble-sort -H "Content-Type: application/json" -d '{"numbers":[1,2,3]}' -H "X-User-Name: 王五" -H "X-User-Dept: 技术部" -H "X-User-Level: P5" -H "X-User-Type: 外包"

sleep 1

echo "=== analytics by dept ==="
curl -s "http://localhost:8000/api/analytics?dimension=dept" | python -m json.tool

echo "=== analytics by level ==="
curl -s "http://localhost:8000/api/analytics?dimension=level" | python -m json.tool

echo "=== analytics by user_type ==="
curl -s "http://localhost:8000/api/analytics?dimension=user_type" | python -m json.tool

kill %1
```

- [ ] **Step 4: Commit**

```bash
git add apis/analytics.py main.py
git commit -m "feat: add analytics endpoint with dimension aggregation"
```

---

## Task 7: 后端单元测试

**Files:**
- Create: `manyu_test/tests/__init__.py`
- Create: `manyu_test/tests/test_apis.py`

**Interfaces:**
- Consumes: 所有 API 路由和 `main.py` 的 `app` 对象
- Produces: pytest 测试套件，覆盖三个计算接口、导出、analytics、参数校验

- [ ] **Step 1: 编写 tests/test_apis.py**

```python
import pytest
from fastapi.testclient import TestClient
from main import app
from models.tracking import init_db, get_db_path
import os
import sqlite3

client = TestClient(app)


@pytest.fixture(autouse=True)
def setup_db():
    """每个测试前重置数据库"""
    db_path = get_db_path()
    if os.path.exists(db_path):
        os.remove(db_path)
    init_db(db_path)
    yield
    if os.path.exists(db_path):
        os.remove(db_path)


class TestHelloworld:
    def test_returns_greeting(self):
        resp = client.post("/api/helloworld")
        assert resp.status_code == 200
        data = resp.json()
        assert data["message"] == "Hello, World!"
        assert "timestamp" in data


class TestHash:
    def test_valid_hash(self):
        resp = client.post("/api/hash", json={"text": "abc"})
        assert resp.status_code == 200
        data = resp.json()
        assert data["algorithm"] == "SHA256"
        assert data["input"] == "abc"
        assert data["hash"] == "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"

    def test_empty_text_rejected(self):
        resp = client.post("/api/hash", json={"text": ""})
        assert resp.status_code == 422

    def test_missing_text_rejected(self):
        resp = client.post("/api/hash", json={})
        assert resp.status_code == 422


class TestBubbleSort:
    def test_sort_numbers(self):
        resp = client.post("/api/bubble-sort", json={"numbers": [5, 3, 8, 4, 2]})
        assert resp.status_code == 200
        data = resp.json()
        assert data["original"] == [5, 3, 8, 4, 2]
        assert data["sorted"] == [2, 3, 4, 5, 8]
        assert data["algorithm"] == "bubble_sort"

    def test_empty_array_rejected(self):
        resp = client.post("/api/bubble-sort", json={"numbers": []})
        assert resp.status_code == 422

    def test_missing_numbers_rejected(self):
        resp = client.post("/api/bubble-sort", json={})
        assert resp.status_code == 422


class TestExport:
    def test_export_helloworld_csv(self):
        client.post("/api/helloworld", headers={"X-User-Name": "张三"})
        resp = client.get("/api/export/helloworld")
        assert resp.status_code == 200
        assert "text/csv" in resp.headers["content-type"]
        assert "helloworld_export.csv" in resp.headers["content-disposition"]

    def test_export_invalid_type(self):
        resp = client.get("/api/export/invalid")
        assert resp.status_code == 400


class TestAnalytics:
    def test_analytics_by_dept(self):
        client.post("/api/helloworld", headers={"X-User-Dept": "技术部"})
        client.post("/api/hash", json={"text": "x"}, headers={"X-User-Dept": "技术部"})
        client.post("/api/bubble-sort", json={"numbers": [1]}, headers={"X-User-Dept": "产品部"})

        resp = client.get("/api/analytics?dimension=dept")
        assert resp.status_code == 200
        data = resp.json()
        assert data["dimension"] == "dept"
        assert len(data["data"]) == 2

    def test_analytics_invalid_dimension(self):
        resp = client.get("/api/analytics?dimension=invalid")
        assert resp.status_code == 400


class TestTracking:
    def test_tracking_inserts_log(self):
        import time

        client.post("/api/helloworld", headers={
            "X-User-Id": "u001",
            "X-User-Name": "张三",
            "X-User-Dept": "技术部",
            "X-User-Level": "P6",
            "X-User-Type": "正式员工",
        })
        time.sleep(0.3)  # 等待异步写入

        conn = sqlite3.connect(get_db_path())
        row = conn.execute("SELECT * FROM api_call_logs").fetchone()
        conn.close()
        assert row is not None
        assert row[1] == "helloworld"
        assert row[3] == "张三"

    def test_export_analytics_not_tracked(self):
        import time

        client.get("/api/export/helloworld")
        client.get("/api/analytics?dimension=dept")
        time.sleep(0.3)

        conn = sqlite3.connect(get_db_path())
        count = conn.execute("SELECT COUNT(*) FROM api_call_logs").fetchone()[0]
        conn.close()
        assert count == 0
```

- [ ] **Step 2: 运行测试**

```bash
cd manyu_test
pip install pytest httpx 2>/dev/null
python -m pytest tests/test_apis.py -v
# Expected: all tests PASS
```

- [ ] **Step 3: Commit**

```bash
git add tests/__init__.py tests/test_apis.py
git commit -m "test: add backend unit tests for all APIs and tracking"
```

---

## Task 8: 前端 — HTML 结构与样式

**Files:**
- Create: `manyu_test1/index.html`
- Create: `manyu_test1/css/style.css`

**Interfaces:**
- Produces: 三 Tab 页面骨架 + 用户模拟区 + 报表区 + 样式

- [ ] **Step 1: 编写 index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>三接口演示平台</title>
    <link rel="stylesheet" href="css/style.css">
    <script src="https://cdn.jsdelivr.net/npm/echarts@5.5.0/dist/echarts.min.js"></script>
</head>
<body>
    <header>
        <h1>三接口演示平台</h1>
    </header>

    <!-- 用户模拟区 -->
    <section class="user-simulator">
        <h3>用户模拟</h3>
        <div class="user-presets">
            <button class="preset-btn active" data-user='{"id":"u001","name":"张三","dept":"技术部","level":"P6","type":"正式员工"}'>张三 (技术部/P6)</button>
            <button class="preset-btn" data-user='{"id":"u002","name":"李四","dept":"产品部","level":"P7","type":"正式员工"}'>李四 (产品部/P7)</button>
            <button class="preset-btn" data-user='{"id":"u003","name":"王五","dept":"技术部","level":"P5","type":"外包"}'>王五 (技术部/P5)</button>
            <button class="preset-btn" data-user='{"id":"u004","name":"赵六","dept":"设计部","level":"P6","type":"实习生"}'>赵六 (设计部/P6)</button>
        </div>
        <div class="user-custom">
            <label>ID: <input id="userId" type="text" value="u001"></label>
            <label>姓名: <input id="userName" type="text" value="张三"></label>
            <label>部门: <input id="userDept" type="text" value="技术部"></label>
            <label>层级: <input id="userLevel" type="text" value="P6"></label>
            <label>类型: <input id="userType" type="text" value="正式员工"></label>
        </div>
    </section>

    <!-- Tab 区域 -->
    <section class="tab-area">
        <div class="tab-bar">
            <button class="tab-btn active" data-tab="helloworld">helloworld</button>
            <button class="tab-btn" data-tab="hash">哈希算法</button>
            <button class="tab-btn" data-tab="bubble-sort">冒泡排序</button>
            <button id="exportBtn" class="export-btn">导出 helloworld 记录</button>
        </div>

        <!-- helloworld Tab -->
        <div class="tab-content active" id="tab-helloworld">
            <div class="input-area">
                <button id="btn-helloworld" class="action-btn">执行</button>
            </div>
            <div class="result-area">
                <h4>结果</h4>
                <pre id="result-helloworld"></pre>
            </div>
        </div>

        <!-- 哈希算法 Tab -->
        <div class="tab-content" id="tab-hash">
            <div class="input-area">
                <input id="input-hash" type="text" placeholder="输入要哈希的文本">
                <button id="btn-hash" class="action-btn">计算哈希</button>
            </div>
            <div class="result-area">
                <h4>结果</h4>
                <pre id="result-hash"></pre>
            </div>
        </div>

        <!-- 冒泡排序 Tab -->
        <div class="tab-content" id="tab-bubble-sort">
            <div class="input-area">
                <input id="input-bubble" type="text" placeholder="输入数字，逗号分隔 (如: 5,3,8,4,2)">
                <button id="btn-bubble" class="action-btn">排序</button>
            </div>
            <div class="result-area">
                <h4>结果</h4>
                <pre id="result-bubble"></pre>
            </div>
        </div>
    </section>

    <!-- 可视化报表 -->
    <section class="report-area">
        <h3>调用统计报表</h3>
        <div class="report-controls">
            <div class="dimension-group">
                <span>维度：</span>
                <button class="dim-btn active" data-dim="dept">人员部门</button>
                <button class="dim-btn" data-dim="level">人员层级</button>
                <button class="dim-btn" data-dim="user_type">人员类型</button>
            </div>
            <div class="chart-type-group">
                <span>图表：</span>
                <button class="chart-btn active" data-chart="line">折线图</button>
                <button class="chart-btn" data-chart="pie">饼图</button>
                <button class="chart-btn" data-chart="bar">柱状图</button>
            </div>
        </div>
        <div id="chartContainer" class="chart-container"></div>
    </section>

    <footer>
        <p>后端地址: <span id="backendUrl">http://localhost:8000</span></p>
    </footer>

    <script src="js/app.js"></script>
    <script src="js/charts.js"></script>
</body>
</html>
```

- [ ] **Step 2: 编写 css/style.css**

```css
* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
}

body {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
    background: #f5f7fa;
    color: #333;
    max-width: 960px;
    margin: 0 auto;
    padding: 20px;
}

header {
    text-align: center;
    margin-bottom: 24px;
}

header h1 {
    font-size: 24px;
    color: #1a1a2e;
}

/* 用户模拟区 */
.user-simulator {
    background: #fff;
    border-radius: 8px;
    padding: 16px;
    margin-bottom: 16px;
    box-shadow: 0 1px 4px rgba(0,0,0,0.08);
}

.user-simulator h3 {
    margin-bottom: 10px;
    font-size: 14px;
    color: #666;
}

.user-presets {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
    margin-bottom: 10px;
}

.preset-btn {
    padding: 6px 12px;
    border: 1px solid #d0d5dd;
    border-radius: 6px;
    background: #fff;
    cursor: pointer;
    font-size: 12px;
    transition: all 0.2s;
}

.preset-btn.active {
    background: #1a56db;
    color: #fff;
    border-color: #1a56db;
}

.user-custom {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
}

.user-custom label {
    font-size: 12px;
    color: #888;
}

.user-custom input {
    width: 100px;
    padding: 4px 8px;
    border: 1px solid #d0d5dd;
    border-radius: 4px;
    font-size: 12px;
}

/* Tab 区域 */
.tab-area {
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 1px 4px rgba(0,0,0,0.08);
    margin-bottom: 16px;
    overflow: hidden;
}

.tab-bar {
    display: flex;
    align-items: center;
    border-bottom: 1px solid #e5e7eb;
    padding: 0 16px;
    background: #fafbfc;
}

.tab-btn {
    padding: 12px 20px;
    border: none;
    background: none;
    cursor: pointer;
    font-size: 14px;
    color: #6b7280;
    border-bottom: 2px solid transparent;
    transition: all 0.2s;
}

.tab-btn.active {
    color: #1a56db;
    border-bottom-color: #1a56db;
    font-weight: 600;
}

.export-btn {
    margin-left: auto;
    padding: 8px 16px;
    background: #1a56db;
    color: #fff;
    border: none;
    border-radius: 6px;
    cursor: pointer;
    font-size: 13px;
    transition: background 0.2s;
}

.export-btn:hover {
    background: #1648c0;
}

.tab-content {
    display: none;
    padding: 20px;
}

.tab-content.active {
    display: block;
}

.input-area {
    display: flex;
    gap: 10px;
    margin-bottom: 16px;
}

.input-area input {
    flex: 1;
    padding: 8px 12px;
    border: 1px solid #d0d5dd;
    border-radius: 6px;
    font-size: 14px;
}

.action-btn {
    padding: 8px 20px;
    background: #1a56db;
    color: #fff;
    border: none;
    border-radius: 6px;
    cursor: pointer;
    font-size: 14px;
    transition: background 0.2s;
}

.action-btn:hover {
    background: #1648c0;
}

.result-area h4 {
    font-size: 13px;
    color: #888;
    margin-bottom: 8px;
}

.result-area pre {
    background: #f9fafb;
    border: 1px solid #e5e7eb;
    border-radius: 6px;
    padding: 12px;
    min-height: 40px;
    font-size: 13px;
    white-space: pre-wrap;
    word-break: break-all;
    color: #1f2937;
}

.result-area pre.error {
    color: #dc2626;
    background: #fef2f2;
}

/* 报表区 */
.report-area {
    background: #fff;
    border-radius: 8px;
    padding: 20px;
    box-shadow: 0 1px 4px rgba(0,0,0,0.08);
    margin-bottom: 16px;
}

.report-area h3 {
    margin-bottom: 12px;
    font-size: 16px;
}

.report-controls {
    display: flex;
    gap: 20px;
    margin-bottom: 16px;
    flex-wrap: wrap;
}

.dimension-group, .chart-type-group {
    display: flex;
    align-items: center;
    gap: 8px;
}

.dimension-group span, .chart-type-group span {
    font-size: 13px;
    color: #888;
}

.dim-btn, .chart-btn {
    padding: 5px 14px;
    border: 1px solid #d0d5dd;
    border-radius: 6px;
    background: #fff;
    cursor: pointer;
    font-size: 12px;
    transition: all 0.2s;
}

.dim-btn.active, .chart-btn.active {
    background: #1a56db;
    color: #fff;
    border-color: #1a56db;
}

.chart-container {
    width: 100%;
    height: 400px;
}

footer {
    text-align: center;
    font-size: 12px;
    color: #aaa;
    padding: 10px;
}
```

- [ ] **Step 3: Commit**

```bash
cd manyu_test1
git add index.html css/style.css
git commit -m "feat: add frontend HTML structure and styles"
```

---

## Task 9: 前端 — JavaScript 交互逻辑

**Files:**
- Create: `manyu_test1/js/app.js`

**Interfaces:**
- Consumes: HTML DOM 元素（id 选择器），`/api/*` 后端接口
- Produces: Tab 切换、API 调用及结果展示、导出下载、用户模拟切换、报表维度/图表切换（调用 `charts.js`）

- [ ] **Step 1: 编写 js/app.js**

```javascript
// ============ 配置 ============
const BASE_URL = 'http://localhost:8000';

// 当前用户信息
let currentUser = {
    id: 'u001',
    name: '张三',
    dept: '技术部',
    level: 'P6',
    type: '正式员工'
};

let currentTab = 'helloworld';
let currentDimension = 'dept';
let currentChartType = 'line';

// ============ 用户模拟 ============
function getUserHeaders() {
    return {
        'X-User-Id': currentUser.id,
        'X-User-Name': currentUser.name,
        'X-User-Dept': currentUser.dept,
        'X-User-Level': currentUser.level,
        'X-User-Type': currentUser.type
    };
}

function updateUserInputs() {
    document.getElementById('userId').value = currentUser.id;
    document.getElementById('userName').value = currentUser.name;
    document.getElementById('userDept').value = currentUser.dept;
    document.getElementById('userLevel').value = currentUser.level;
    document.getElementById('userType').value = currentUser.type;
}

function syncUserFromInputs() {
    currentUser.id = document.getElementById('userId').value;
    currentUser.name = document.getElementById('userName').value;
    currentUser.dept = document.getElementById('userDept').value;
    currentUser.level = document.getElementById('userLevel').value;
    currentUser.type = document.getElementById('userType').value;
}

document.querySelectorAll('.preset-btn').forEach(function(btn) {
    btn.addEventListener('click', function() {
        document.querySelectorAll('.preset-btn').forEach(function(b) { b.classList.remove('active'); });
        btn.classList.add('active');
        currentUser = JSON.parse(btn.getAttribute('data-user'));
        updateUserInputs();
    });
});

document.querySelectorAll('#userId, #userName, #userDept, #userLevel, #userType').forEach(function(input) {
    input.addEventListener('change', syncUserFromInputs);
});

// ============ Tab 切换 ============
document.querySelectorAll('.tab-btn').forEach(function(btn) {
    btn.addEventListener('click', function() {
        currentTab = btn.getAttribute('data-tab');

        document.querySelectorAll('.tab-btn').forEach(function(b) { b.classList.remove('active'); });
        btn.classList.add('active');

        document.querySelectorAll('.tab-content').forEach(function(c) { c.classList.remove('active'); });
        document.getElementById('tab-' + currentTab).classList.add('active');

        updateExportButton();
    });
});

function updateExportButton() {
    var labels = { helloworld: 'helloworld', hash: '哈希', 'bubble-sort': '排序' };
    var btn = document.getElementById('exportBtn');
    btn.textContent = '导出 ' + labels[currentTab] + ' 记录';
}

// ============ API 调用 ============
async function apiCall(method, path, body) {
    var opts = {
        method: method,
        headers: Object.assign({ 'Content-Type': 'application/json' }, getUserHeaders())
    };
    if (body) opts.body = JSON.stringify(body);

    var resp = await fetch(BASE_URL + path, opts);
    if (!resp.ok) {
        var err = await resp.json();
        throw new Error(err.detail || '请求失败 (' + resp.status + ')');
    }
    return resp.json();
}

function showResult(tab, data, isError) {
    var pre = document.getElementById('result-' + tab);
    pre.textContent = isError ? data : JSON.stringify(data, null, 2);
    pre.className = isError ? 'error' : '';
}

// helloworld
document.getElementById('btn-helloworld').addEventListener('click', async function() {
    try {
        var data = await apiCall('POST', '/api/helloworld');
        showResult('helloworld', data);
        loadAnalytics();
    } catch (e) {
        showResult('helloworld', e.message, true);
    }
});

// hash
document.getElementById('btn-hash').addEventListener('click', async function() {
    var text = document.getElementById('input-hash').value.trim();
    if (!text) { showResult('hash', '请输入要哈希的文本', true); return; }
    try {
        var data = await apiCall('POST', '/api/hash', { text: text });
        showResult('hash', data);
        loadAnalytics();
    } catch (e) {
        showResult('hash', e.message, true);
    }
});

// bubble-sort
document.getElementById('btn-bubble').addEventListener('click', async function() {
    var raw = document.getElementById('input-bubble').value.trim();
    if (!raw) { showResult('bubble-sort', '请输入数字，逗号分隔', true); return; }
    var numbers = raw.split(',').map(function(s) {
        var n = parseFloat(s.trim());
        if (isNaN(n)) throw new Error('包含非数字: ' + s);
        return n;
    });
    try {
        var data = await apiCall('POST', '/api/bubble-sort', { numbers: numbers });
        showResult('bubble-sort', data);
        loadAnalytics();
    } catch (e) {
        showResult('bubble-sort', e.message, true);
    }
});

// ============ 导出 ============
document.getElementById('exportBtn').addEventListener('click', function() {
    var url = BASE_URL + '/api/export/' + currentTab;
    var a = document.createElement('a');
    a.href = url;
    a.download = currentTab + '_export.csv';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
});

// ============ 报表 ============
async function loadAnalytics() {
    try {
        var resp = await fetch(
            BASE_URL + '/api/analytics?dimension=' + currentDimension,
            { headers: Object.assign({}, getUserHeaders()) }
        );
        if (!resp.ok) throw new Error('获取报表失败');
        var data = await resp.json();
        renderChart(data.data, currentDimension, currentChartType);
    } catch (e) {
        console.error('Analytics error:', e);
    }
}

document.querySelectorAll('.dim-btn').forEach(function(btn) {
    btn.addEventListener('click', function() {
        currentDimension = btn.getAttribute('data-dim');
        document.querySelectorAll('.dim-btn').forEach(function(b) { b.classList.remove('active'); });
        btn.classList.add('active');
        loadAnalytics();
    });
});

document.querySelectorAll('.chart-btn').forEach(function(btn) {
    btn.addEventListener('click', function() {
        currentChartType = btn.getAttribute('data-chart');
        document.querySelectorAll('.chart-btn').forEach(function(b) { b.classList.remove('active'); });
        btn.classList.add('active');
        loadAnalytics();
    });
});

// 初始化
updateUserInputs();
updateExportButton();
loadAnalytics();
```

- [ ] **Step 2: Commit**

```bash
cd manyu_test1
git add js/app.js
git commit -m "feat: add frontend interaction logic (tabs, API calls, export, analytics)"
```

---

## Task 10: 前端 — ECharts 图表渲染

**Files:**
- Create: `manyu_test1/js/charts.js`

**Interfaces:**
- Consumes: ECharts (CDN 全局 `echarts`)，DOM 元素 `#chartContainer`
- Produces: `renderChart(data, dimension, chartType)` — 根据数据和图表类型渲染 ECharts

- [ ] **Step 1: 编写 js/charts.js**

```javascript
var chartInstance = null;

function renderChart(data, dimension, chartType) {
    var container = document.getElementById('chartContainer');
    if (!chartInstance) {
        chartInstance = echarts.init(container);
    }

    var labels = data.map(function(d) { return d.label; });
    var values = data.map(function(d) { return d.count; });

    var dimLabels = { dept: '人员部门', level: '人员层级', user_type: '人员类型' };

    var option = {
        title: {
            text: '调用次数统计（按' + (dimLabels[dimension] || dimension) + '）',
            left: 'center',
            textStyle: { fontSize: 14 }
        },
        tooltip: { trigger: 'axis' },
        legend: { show: chartType === 'pie' },
        xAxis: chartType === 'pie' ? null : {
            type: 'category',
            data: labels,
            axisLabel: { rotate: labels.length > 5 ? 30 : 0 }
        },
        yAxis: chartType === 'pie' ? null : {
            type: 'value',
            name: '调用次数',
            minInterval: 1
        },
        series: [{
            name: '调用次数',
            type: chartType,
            data: labels.map(function(label, i) {
                return chartType === 'pie'
                    ? { name: label, value: values[i] }
                    : values[i];
            }),
            radius: chartType === 'pie' ? ['30%', '65%'] : undefined,
            center: chartType === 'pie' ? ['50%', '55%'] : undefined,
            label: {
                show: chartType === 'pie',
                formatter: '{b}: {c} ({d}%)'
            },
            itemStyle: {
                borderRadius: chartType === 'bar' ? [4, 4, 0, 0] : undefined
            }
        }]
    };

    chartInstance.setOption(option, true);

    window.addEventListener('resize', function() {
        chartInstance && chartInstance.resize();
    });
}
```

- [ ] **Step 2: Commit**

```bash
cd manyu_test1
git add js/charts.js
git commit -m "feat: add ECharts rendering (line, pie, bar) for analytics"
```

---

## Task 11: 集成验证 — 端到端冒烟测试

**Files:**
- 无新文件（验证已有产物）

**Interfaces:**
- Consumes: 后端 `main.py`（端口 8000），前端 `index.html`（浏览器打开）

- [ ] **Step 1: 启动后端并验证全链路**

```bash
cd manyu_test
pip install fastapi uvicorn 2>/dev/null
python main.py &
sleep 2

# 1. 三个接口均可调用
curl -s -X POST http://localhost:8000/api/helloworld > /dev/null && echo "helloworld OK"
curl -s -X POST http://localhost:8000/api/hash -H "Content-Type: application/json" -d '{"text":"test"}' > /dev/null && echo "hash OK"
curl -s -X POST http://localhost:8000/api/bubble-sort -H "Content-Type: application/json" -d '{"numbers":[1,2,3]}' > /dev/null && echo "bubble-sort OK"

# 2. 带 Header 调用，验证埋点
curl -s -X POST http://localhost:8000/api/helloworld \
  -H "X-User-Name: 张三" -H "X-User-Dept: 技术部" -H "X-User-Level: P6" -H "X-User-Type: 正式员工" > /dev/null
sleep 1

# 3. 导出 CSV
curl -s http://localhost:8000/api/export/helloworld
echo ""

# 4. Analytics
curl -s "http://localhost:8000/api/analytics?dimension=dept" | python -m json.tool

kill %1
```

- [ ] **Step 2: 运行 pytest 完整测试套件**

```bash
cd manyu_test
python -m pytest tests/ -v
# Expected: all tests PASS
```

- [ ] **Step 3: 验证前端文件完整性**

```bash
cd manyu_test1
ls -la index.html css/style.css js/app.js js/charts.js
# Expected: all 4 files exist
```

- [ ] **Step 4: Commit**

```bash
# 无新增文件，验证通过即完成
```

---

## 仓间对齐清单

| 对齐项 | manyu_test (后端) | manyu_test1 (前端) | 状态 |
|--------|-------------------|---------------------|------|
| API 契约 | Task 4 严格按设计 §3.2 | Task 9 按契约调用 | ✅ |
| 用户身份 | Task 3 中间件读取 Header | Task 9 前端模拟区写入 Header | ✅ |
| 导出格式 | Task 5 CSV + Content-Disposition | Task 9 通过 `<a>` 下载 | ✅ |
| 维度枚举 | Task 6 `dept, level, user_type` | Task 9 前端使用相同枚举 | ✅ |
| CORS | Task 1 允许所有来源 | Task 9 fetch 跨域请求 | ✅ |
| 端口 | Task 1 端口 8000 | Task 9 BASE_URL 指向 8000 | ✅ |
| 图表类型 | Task 6 返回聚合数据 | Task 10 折线/饼/柱状图 | ✅ |

---

## 自检清单

1. **Spec 覆盖**：三接口 ✅、埋点 ✅、导出 ✅、报表（维度+图表）✅、用户模拟 ✅
2. **无 Placeholder**：所有步骤包含完整代码，无 TBD/TODO
3. **类型一致性**：`dimension` 枚举值 `dept/level/user_type` 在 Task 6 和 Task 9 中一致；`api_name` 值 `helloworld/hash/bubble-sort` 全局一致
4. **文件路径**：所有路径均为相对仓库根目录的精确路径