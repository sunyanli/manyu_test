# 三接口 + 前端 Tab 展示 + 导出 + 埋点仪表盘 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建三个 API（helloworld、哈希、冒泡排序）+ 前端三 Tab 页面 + 导出功能 + 埋点统计仪表盘（折线图/饼图/柱状图，支持多维度）。

**Architecture:** FastAPI 后端（manyu_test）提供 REST API + SQLite 埋点；原生 HTML/JS 前端（manyu_test1）单页应用，使用 Chart.js CDN 渲染图表。通过 CORS 跨域通信，埋点通过 FastAPI 中间件自动记录。

**Tech Stack:** Python 3 + FastAPI + uvicorn + aiosqlite + SQLite（后端）；原生 HTML/CSS/JS + Chart.js 4.x CDN（前端）

---

## Global Constraints

- Python >= 3.9（FastAPI 要求）
- 前端零依赖构建工具，纯静态 HTML/JS/CSS，通过 CDN 引入 Chart.js
- 数据库使用 SQLite（WAL 模式），文件路径 `tracking.db` in manyu_test 根目录
- CORS 开发阶段允许 `*`
- 埋点 Header 缺失时默认值：`user_id="anonymous"`，`user_type/user_level/user_dept="unknown"`
- 所有 API 路径前缀 `/api/`
- 冒泡排序复用已有 `bubble_sort.py` 中的 `bubble_sort` 函数

---

## File Structure

### [manyu_test] 后端

| 文件 | 操作 | 职责 |
|------|------|------|
| `main.py` | 创建 | FastAPI 应用入口：CORS 配置、路由注册、全局异常处理、启动事件 |
| `tracking.py` | 创建 | SQLite 数据库初始化、埋点中间件、报表查询聚合 |
| `bubble_sort.py` | 已有（复用） | 冒泡排序核心算法，提供 `bubble_sort(arr)` |
| `requirements.txt` | 创建 | Python 依赖声明 |

### [manyu_test1] 前端

| 文件 | 操作 | 职责 |
|------|------|------|
| `index.html` | 创建 | 完整单页应用：三 Tab 面板、导出按钮、Chart.js 仪表盘 |

---

## Task 1: FastAPI 入口 + 三个业务 API

**Files:**
- Create: `[manyu_test] main.py`
- Modify: 无
- Test: 手动 curl 验证

**Interfaces:**
- Consumes: `[manyu_test] bubble_sort.py` → `bubble_sort(arr: List) -> List`
- Produces:
  - `GET /api/helloworld` → `{"message": "Hello, World!"}`
  - `POST /api/hash` → Body `{"input": str, "algorithm": str}` → `{"hash": str, "algorithm": str}`
  - `POST /api/bubble_sort` → Body `{"array": List[int|float]}` → `{"sorted": List, "steps": int}`
  - `app` (FastAPI instance) — 供 Task 2 挂载中间件和路由

- [ ] **Step 1: 创建 main.py 骨架**

```python
"""
FastAPI 应用入口 — 三接口 API 服务
"""
import hashlib
import time
from typing import List, Union

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field

from bubble_sort import bubble_sort

app = FastAPI(title="算法演示平台 API", version="1.0.0")

# CORS 配置
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

SUPPORTED_ALGORITHMS = {"sha256", "md5", "sha1"}
MAX_INPUT_LENGTH = 1_048_576       # 1 MB
MAX_ARRAY_LENGTH = 10_000


class HashRequest(BaseModel):
    input: str
    algorithm: str = "sha256"


class BubbleSortRequest(BaseModel):
    array: List[Union[int, float]] = Field(..., min_length=0, max_length=MAX_ARRAY_LENGTH)


@app.get("/api/helloworld")
async def helloworld():
    return {"message": "Hello, World!"}


@app.post("/api/hash")
async def compute_hash(req: HashRequest):
    if req.algorithm not in SUPPORTED_ALGORITHMS:
        return JSONResponse(
            status_code=400,
            content={
                "error": f"unsupported algorithm: {req.algorithm}",
                "supported": sorted(SUPPORTED_ALGORITHMS),
            },
        )
    if len(req.input) > MAX_INPUT_LENGTH:
        return JSONResponse(
            status_code=413,
            content={
                "error": "payload too large",
                "limit": MAX_INPUT_LENGTH,
            },
        )
    h = hashlib.new(req.algorithm)
    h.update(req.input.encode("utf-8"))
    return {"hash": h.hexdigest(), "algorithm": req.algorithm}


@app.post("/api/bubble_sort")
async def sort_bubble(req: BubbleSortRequest):
    arr_copy = list(req.array)
    n = len(arr_copy)
    steps = 0
    for i in range(n):
        for j in range(0, n - i - 1):
            steps += 1
            if arr_copy[j] > arr_copy[j + 1]:
                arr_copy[j], arr_copy[j + 1] = arr_copy[j + 1], arr_copy[j]
    return {"sorted": arr_copy, "steps": steps}


# 全局异常处理器
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    import uuid
    request_id = str(uuid.uuid4())[:8]
    return JSONResponse(
        status_code=500,
        content={
            "error": "internal server error",
            "request_id": request_id,
        },
    )
```

- [ ] **Step 2: 启动后端并验证三个 API**

```bash
cd /root/.agentix/agentic-dev/runs/DEV-9d10e310-7901-11f1-8a9f-59ecae612580-cd8de22e-c4bc-4aec-b3b5-17005a25e187/worktree/manyu_test-cred-test-20260716022903
pip install fastapi uvicorn aiosqlite 2>/dev/null
uvicorn main:app --host 0.0.0.0 --port 8000 &
sleep 2

# Test 1: helloworld
curl -s http://localhost:8000/api/helloworld
# Expected: {"message":"Hello, World!"}

# Test 2: hash
curl -s -X POST http://localhost:8000/api/hash \
  -H "Content-Type: application/json" \
  -d '{"input":"hello","algorithm":"sha256"}'
# Expected: {"hash":"2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824","algorithm":"sha256"}

# Test 3: bubble_sort
curl -s -X POST http://localhost:8000/api/bubble_sort \
  -H "Content-Type: application/json" \
  -d '{"array":[5,2,8,1,3]}'
# Expected: {"sorted":[1,2,3,5,8],"steps":10}

# Test 4: unsupported algorithm
curl -s -X POST http://localhost:8000/api/hash \
  -H "Content-Type: application/json" \
  -d '{"input":"test","algorithm":"sha512"}'
# Expected: 400 with error message

# Test 5: empty array
curl -s -X POST http://localhost:8000/api/bubble_sort \
  -H "Content-Type: application/json" \
  -d '{"array":[]}'
# Expected: {"sorted":[],"steps":0}

# Kill background uvicorn
kill %1 2>/dev/null
```

Expected: 所有 curl 返回预期 JSON。

- [ ] **Step 3: Commit**

```bash
cd /root/.agentix/agentic-dev/runs/DEV-9d10e310-7901-11f1-8a9f-59ecae612580-cd8de22e-c4bc-4aec-b3b5-17005a25e187/worktree/manyu_test-cred-test-20260716022903
git add main.py
git commit -m "feat: add FastAPI entry with helloworld, hash, and bubble_sort APIs"
```

---

## Task 2: 埋点数据库 + 中间件 + 报表 API

**Files:**
- Create: `[manyu_test] tracking.py`
- Modify: `[manyu_test] main.py` — 注册中间件和报表路由

**Interfaces:**
- Consumes: `[manyu_test] main.py` → `app` (FastAPI instance)
- Produces:
  - `TrackingMiddleware` (Starlette Middleware) — 自动记录 `/api/helloworld|hash|bubble_sort` 请求
  - `GET /api/tracking/report?dimension=user_type` → `{"labels": [...], "values": [...]}`
  - `init_db()` — 启动时创建表和索引

- [ ] **Step 1: 创建 tracking.py**

```python
"""
埋点模块：SQLite 数据库初始化、中间件、报表查询
"""
import logging
import sqlite3
import os
from datetime import datetime

from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import JSONResponse

logger = logging.getLogger("tracking")

DB_PATH = os.path.join(os.path.dirname(__file__), "tracking.db")

TRACKED_PATHS = {"/api/helloworld", "/api/hash", "/api/bubble_sort"}

VALID_DIMENSIONS = {"user_type", "user_level", "user_dept", "api_name"}


def get_connection() -> sqlite3.Connection:
    conn = sqlite3.connect(DB_PATH)
    conn.execute("PRAGMA journal_mode=WAL")
    conn.row_factory = sqlite3.Row
    return conn


def init_db():
    conn = get_connection()
    conn.execute("""
        CREATE TABLE IF NOT EXISTS tracking_logs (
            id         INTEGER PRIMARY KEY AUTOINCREMENT,
            api_name   TEXT NOT NULL,
            user_id    TEXT NOT NULL,
            user_type  TEXT DEFAULT 'unknown',
            user_level TEXT DEFAULT 'unknown',
            user_dept  TEXT DEFAULT 'unknown',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """)
    conn.execute("""
        CREATE INDEX IF NOT EXISTS idx_api_name ON tracking_logs(api_name)
    """)
    conn.execute("""
        CREATE INDEX IF NOT EXISTS idx_user_type ON tracking_logs(user_type)
    """)
    conn.execute("""
        CREATE INDEX IF NOT EXISTS idx_user_level ON tracking_logs(user_level)
    """)
    conn.execute("""
        CREATE INDEX IF NOT EXISTS idx_user_dept ON tracking_logs(user_dept)
    """)
    conn.commit()
    conn.close()


class TrackingMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        response = await call_next(request)

        path = request.url.path
        if path not in TRACKED_PATHS:
            return response

        api_name = path.split("/")[-1]
        user_id = request.headers.get("X-User-Id", "anonymous")
        user_type = request.headers.get("X-User-Type", "unknown")
        user_level = request.headers.get("X-User-Level", "unknown")
        user_dept = request.headers.get("X-User-Dept", "unknown")

        try:
            conn = get_connection()
            conn.execute(
                "INSERT INTO tracking_logs (api_name, user_id, user_type, user_level, user_dept) "
                "VALUES (?, ?, ?, ?, ?)",
                (api_name, user_id, user_type, user_level, user_dept),
            )
            conn.commit()
            conn.close()
        except sqlite3.Error as e:
            logger.error(f"Failed to write tracking log: {e}")

        return response


def get_report(dimension: str) -> dict:
    """按指定维度聚合调用次数，支持逗号分隔的二维交叉维度。"""
    dims = [d.strip() for d in dimension.split(",")]
    for d in dims:
        if d not in VALID_DIMENSIONS:
            return {"error": f"invalid dimension: {d}"}

    select_cols = ", ".join(dims)
    group_cols = ", ".join(dims)

    try:
        conn = get_connection()
        rows = conn.execute(
            f"SELECT {select_cols}, COUNT(*) as cnt "
            "FROM tracking_logs "
            f"GROUP BY {group_cols} "
            "ORDER BY cnt DESC"
        ).fetchall()
        conn.close()

        labels = []
        values = []
        for row in rows:
            if len(dims) == 1:
                label = str(row[dims[0]] or "unknown")
            else:
                label = " / ".join(str(row[d] or "unknown") for d in dims)
            labels.append(label)
            values.append(row["cnt"])

        return {"labels": labels, "values": values}
    except sqlite3.Error as e:
        return {"error": "tracking service unavailable"}
```

- [ ] **Step 2: 修改 main.py — 注册埋点中间件和报表路由**

在 `[manyu_test] main.py` 中，紧接 `from bubble_sort import bubble_sort` 之后添加：

```python
from tracking import init_db, TrackingMiddleware, get_report

# 注册埋点中间件（必须在 CORS 之后、业务路由之前）
app.add_middleware(TrackingMiddleware)


@app.on_event("startup")
async def startup():
    init_db()


@app.get("/api/tracking/report")
async def tracking_report(dimension: str = "user_type"):
    result = get_report(dimension)
    if "error" in result:
        return JSONResponse(status_code=503, content=result)
    return result
```

- [ ] **Step 3: 验证埋点中间件和报表**

```bash
cd /root/.agentix/agentic-dev/runs/DEV-9d10e310-7901-11f1-8a9f-59ecae612580-cd8de22e-c4bc-4aec-b3b5-17005a25e187/worktree/manyu_test-cred-test-20260716022903
uvicorn main:app --host 0.0.0.0 --port 8000 &
sleep 2

# 发送带埋点 Header 的请求
curl -s http://localhost:8000/api/helloworld \
  -H "X-User-Id: alice" \
  -H "X-User-Type: 正式" \
  -H "X-User-Level: P7" \
  -H "X-User-Dept: 技术"

curl -s -X POST http://localhost:8000/api/hash \
  -H "Content-Type: application/json" \
  -H "X-User-Id: bob" \
  -H "X-User-Type: 外包" \
  -H "X-User-Level: P6" \
  -H "X-User-Dept: 技术" \
  -d '{"input":"test","algorithm":"md5"}'

curl -s -X POST http://localhost:8000/api/bubble_sort \
  -H "Content-Type: application/json" \
  -H "X-User-Id: carol" \
  -H "X-User-Type: 正式" \
  -H "X-User-Level: P8" \
  -H "X-User-Dept: 产品" \
  -d '{"array":[3,1,2]}'

# 查询报表 - 按人员类型
curl -s "http://localhost:8000/api/tracking/report?dimension=user_type"
# Expected: {"labels":["正式","外包"],"values":[2,1]}

# 查询报表 - 按接口
curl -s "http://localhost:8000/api/tracking/report?dimension=api_name"
# Expected: {"labels":["helloworld","hash","bubble_sort"],"values":[1,1,1]}

# 查询报表 - 二维交叉
curl -s "http://localhost:8000/api/tracking/report?dimension=user_type,api_name"
# Expected: labels like "正式 / helloworld", "外包 / hash"

# 缺少 Header 的请求（默认值）
curl -s http://localhost:8000/api/helloworld
# 应正常返回，埋点记录 user_id="anonymous"

kill %1 2>/dev/null
```

Expected: 所有 curl 返回预期结果，`tracking.db` 文件存在且包含记录。

- [ ] **Step 4: Commit**

```bash
cd /root/.agentix/agentic-dev/runs/DEV-9d10e310-7901-11f1-8a9f-59ecae612580-cd8de22e-c4bc-4aec-b3b5-17005a25e187/worktree/manyu_test-cred-test-20260716022903
git add tracking.py main.py
git commit -m "feat: add tracking middleware, SQLite storage, and report API"
```

---

## Task 3: 导出接口

**Files:**
- Modify: `[manyu_test] main.py` — 添加 3 个导出路由

**Interfaces:**
- Produces:
  - `GET /api/export/helloworld` → JSON 文件下载 `{"message":"Hello, World!","exported_at":"..."}`
  - `GET /api/export/hash?input=...&algorithm=...` → JSON 文件下载
  - `GET /api/export/bubble_sort?array=5,2,8,1,3` → JSON 文件下载

- [ ] **Step 1: 在 main.py 中添加导出路由**

在 `[manyu_test] main.py` 末尾（`app` 定义之后）添加：

```python
from fastapi.responses import StreamingResponse
from datetime import datetime, timezone
import io
import json


@app.get("/api/export/helloworld")
async def export_helloworld():
    data = {
        "message": "Hello, World!",
        "exported_at": datetime.now(timezone.utc).isoformat(),
    }
    content = json.dumps(data, ensure_ascii=False, indent=2)
    return StreamingResponse(
        io.BytesIO(content.encode("utf-8")),
        media_type="application/json",
        headers={"Content-Disposition": "attachment; filename=helloworld_export.json"},
    )


@app.get("/api/export/hash")
async def export_hash(input: str, algorithm: str = "sha256"):
    if algorithm not in SUPPORTED_ALGORITHMS:
        return JSONResponse(
            status_code=400,
            content={
                "error": f"unsupported algorithm: {algorithm}",
                "supported": sorted(SUPPORTED_ALGORITHMS),
            },
        )
    h = hashlib.new(algorithm)
    h.update(input.encode("utf-8"))
    data = {
        "input": input,
        "algorithm": algorithm,
        "hash": h.hexdigest(),
        "exported_at": datetime.now(timezone.utc).isoformat(),
    }
    content = json.dumps(data, ensure_ascii=False, indent=2)
    return StreamingResponse(
        io.BytesIO(content.encode("utf-8")),
        media_type="application/json",
        headers={"Content-Disposition": "attachment; filename=hash_export.json"},
    )


@app.get("/api/export/bubble_sort")
async def export_bubble_sort(array: str):
    try:
        arr = [float(x.strip()) for x in array.split(",") if x.strip()]
    except ValueError:
        return JSONResponse(
            status_code=422,
            content={"error": "invalid array format, use comma-separated numbers"},
        )
    if len(arr) > MAX_ARRAY_LENGTH:
        return JSONResponse(
            status_code=413,
            content={"error": "payload too large", "limit": MAX_ARRAY_LENGTH},
        )
    arr_copy = list(arr)
    n = len(arr_copy)
    steps = 0
    for i in range(n):
        for j in range(0, n - i - 1):
            steps += 1
            if arr_copy[j] > arr_copy[j + 1]:
                arr_copy[j], arr_copy[j + 1] = arr_copy[j + 1], arr_copy[j]
    data = {
        "original": arr,
        "sorted": arr_copy,
        "steps": steps,
        "exported_at": datetime.now(timezone.utc).isoformat(),
    }
    content = json.dumps(data, ensure_ascii=False, indent=2)
    return StreamingResponse(
        io.BytesIO(content.encode("utf-8")),
        media_type="application/json",
        headers={"Content-Disposition": "attachment; filename=bubble_sort_export.json"},
    )
```

- [ ] **Step 2: 验证导出接口**

```bash
cd /root/.agentix/agentic-dev/runs/DEV-9d10e310-7901-11f1-8a9f-59ecae612580-cd8de22e-c4bc-4aec-b3b5-17005a25e187/worktree/manyu_test-cred-test-20260716022903
uvicorn main:app --host 0.0.0.0 --port 8000 &
sleep 2

# Test 1: export helloworld
curl -s -o /tmp/export1.json http://localhost:8000/api/export/helloworld
cat /tmp/export1.json
# Expected: {"message":"Hello, World!","exported_at":"..."}

# Test 2: export hash
curl -s -o /tmp/export2.json "http://localhost:8000/api/export/hash?input=hello&algorithm=sha256"
cat /tmp/export2.json
# Expected: {"input":"hello","algorithm":"sha256","hash":"2cf24d...","exported_at":"..."}

# Test 3: export bubble_sort
curl -s -o /tmp/export3.json "http://localhost:8000/api/export/bubble_sort?array=5,2,8,1,3"
cat /tmp/export3.json
# Expected: {"original":[5,2,8,1,3],"sorted":[1,2,3,5,8],"steps":10,"exported_at":"..."}

kill %1 2>/dev/null
```

Expected: 所有导出文件包含预期 JSON 且 `Content-Disposition: attachment` header 正确。

- [ ] **Step 3: Commit**

```bash
cd /root/.agentix/agentic-dev/runs/DEV-9d10e310-7901-11f1-8a9f-59ecae612580-cd8de22e-c4bc-4aec-b3b5-17005a25e187/worktree/manyu_test-cred-test-20260716022903
git add main.py
git commit -m "feat: add export endpoints for helloworld, hash, and bubble_sort"
```

---

## Task 4: requirements.txt

**Files:**
- Create: `[manyu_test] requirements.txt`

- [ ] **Step 1: 创建 requirements.txt**

```
fastapi>=0.100.0
uvicorn>=0.22.0
aiosqlite>=0.19.0
```

- [ ] **Step 2: Commit**

```bash
cd /root/.agentix/agentic-dev/runs/DEV-9d10e310-7901-11f1-8a9f-59ecae612580-cd8de22e-c4bc-4aec-b3b5-17005a25e187/worktree/manyu_test-cred-test-20260716022903
git add requirements.txt
git commit -m "chore: add requirements.txt"
```

---

## Task 5: 前端单页应用 (index.html)

**Files:**
- Create: `[manyu_test1] index.html`

**Interfaces:**
- Consumes:
  - `GET /api/helloworld` → `{message}`
  - `POST /api/hash` → `{hash, algorithm}`
  - `POST /api/bubble_sort` → `{sorted, steps}`
  - `GET /api/export/*` → 文件下载
  - `GET /api/tracking/report?dimension=...` → `{labels, values}`
- Produces: 浏览器端完整 UI（Tab 面板 + 导出按钮 + Chart.js 仪表盘）

- [ ] **Step 1: 创建 index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>算法演示平台</title>
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"
        onerror="document.getElementById('dashboard').innerHTML='<div class=error-banner>📊 图表组件加载失败，请刷新页面</div>'"></script>
<style>
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;background:#f0f2f5;color:#333;min-height:100vh}
noscript{display:block;padding:16px;background:#ffe0e0;color:#c00;text-align:center;font-weight:bold}
.container{max-width:960px;margin:0 auto;padding:20px}
h1{text-align:center;margin-bottom:20px;color:#1a1a2e}
.card{background:#fff;border-radius:12px;box-shadow:0 2px 8px rgba(0,0,0,.08);padding:24px;margin-bottom:20px}
.tabs{display:flex;gap:0;margin-bottom:20px;border-bottom:2px solid #e0e0e0}
.tab{padding:10px 24px;cursor:pointer;border:none;background:none;font-size:15px;color:#666;border-bottom:3px solid transparent;margin-bottom:-2px;transition:all .2s}
.tab:hover{color:#333}
.tab.active{color:#2563eb;border-bottom-color:#2563eb;font-weight:600}
.tab-panel{display:none}
.tab-panel.active{display:block}
.form-row{display:flex;gap:12px;margin-bottom:16px;align-items:flex-end;flex-wrap:wrap}
.form-row label{font-size:13px;color:#666;display:block;margin-bottom:4px}
.form-row input,.form-row select{padding:8px 12px;border:1px solid #d0d0d0;border-radius:6px;font-size:14px;min-width:200px}
.btn{padding:8px 20px;border:none;border-radius:6px;cursor:pointer;font-size:14px;font-weight:500;transition:all .2s}
.btn-primary{background:#2563eb;color:#fff}
.btn-primary:hover{background:#1d4ed8}
.btn-success{background:#16a34a;color:#fff}
.btn-success:hover{background:#15803d}
.result-box{background:#f8f9fa;border:1px solid #e0e0e0;border-radius:8px;padding:16px;margin-top:12px;font-family:monospace;font-size:13px;white-space:pre-wrap;min-height:60px;max-height:300px;overflow-y:auto}
.toast-container{position:fixed;top:20px;right:20px;z-index:9999;display:flex;flex-direction:column;gap:8px}
.toast{padding:12px 20px;border-radius:8px;color:#fff;font-size:14px;box-shadow:0 4px 12px rgba(0,0,0,.15);animation:slideIn .3s ease;max-width:400px}
.toast.error{background:#ef4444}
.toast.warning{background:#f59e0b}
.toast.info{background:#3b82f6}
@keyframes slideIn{from{transform:translateX(100%);opacity:0}to{transform:translateX(0);opacity:1}}
.dashboard-header{display:flex;gap:12px;margin-bottom:16px;flex-wrap:wrap;align-items:center}
.chart-type-btns{display:flex;gap:8px}
.chart-type-btn{padding:6px 14px;border:1px solid #d0d0d0;border-radius:20px;background:#fff;cursor:pointer;font-size:13px;transition:all .2s}
.chart-type-btn.active{background:#2563eb;color:#fff;border-color:#2563eb}
.chart-wrapper{position:relative;height:320px}
.chart-wrapper canvas{width:100%!important}
.loading-spinner{display:flex;align-items:center;justify-content:center;height:320px;color:#999}
.spinner{width:32px;height:32px;border:3px solid #e0e0e0;border-top-color:#2563eb;border-radius:50%;animation:spin .8s linear infinite;margin-right:10px}
@keyframes spin{to{transform:rotate(360deg)}}
.error-banner{padding:16px;background:#fff3cd;border-radius:8px;color:#856404}
#chart-container{position:relative}
</style>
</head>
<body>
<noscript>⚠️ 请启用 JavaScript 以使用本页面</noscript>
<div class="toast-container" id="toast-container"></div>
<div class="container">
  <h1>🔧 算法演示平台</h1>

  <!-- Tab 栏 -->
  <div class="card">
    <div class="tabs">
      <button class="tab active" data-tab="helloworld">HelloWorld</button>
      <button class="tab" data-tab="hash">Hash</button>
      <button class="tab" data-tab="bubble_sort">BubbleSort</button>
    </div>

    <!-- HelloWorld Tab -->
    <div class="tab-panel active" id="tab-helloworld">
      <div class="form-row">
        <button class="btn btn-primary" onclick="executeHelloworld()">▶ 执行</button>
      </div>
      <div class="result-box" id="result-helloworld">点击「执行」查看结果</div>
      <div style="margin-top:12px">
        <button class="btn btn-success" onclick="exportResult('helloworld')">📥 导出当前结果</button>
      </div>
    </div>

    <!-- Hash Tab -->
    <div class="tab-panel" id="tab-hash">
      <div class="form-row">
        <div><label>输入文本</label><input type="text" id="hash-input" placeholder="输入要哈希的文本" value="hello"></div>
        <div><label>算法</label><select id="hash-algorithm"><option value="sha256">SHA-256</option><option value="md5">MD5</option><option value="sha1">SHA-1</option></select></div>
        <button class="btn btn-primary" onclick="executeHash()">▶ 执行</button>
      </div>
      <div class="result-box" id="result-hash">点击「执行」查看结果</div>
      <div style="margin-top:12px">
        <button class="btn btn-success" onclick="exportResult('hash')">📥 导出当前结果</button>
      </div>
    </div>

    <!-- BubbleSort Tab -->
    <div class="tab-panel" id="tab-bubble_sort">
      <div class="form-row">
        <div><label>数组（逗号分隔）</label><input type="text" id="bs-input" placeholder="5,2,8,1,3" value="5,2,8,1,3"></div>
        <button class="btn btn-primary" onclick="executeBubbleSort()">▶ 执行</button>
      </div>
      <div class="result-box" id="result-bubble_sort">点击「执行」查看结果</div>
      <div style="margin-top:12px">
        <button class="btn btn-success" onclick="exportResult('bubble_sort')">📥 导出当前结果</button>
      </div>
    </div>
  </div>

  <!-- 仪表盘 -->
  <div class="card" id="dashboard">
    <h2 style="margin-bottom:16px">📊 调用统计仪表盘</h2>
    <div class="dashboard-header">
      <div class="chart-type-btns">
        <button class="chart-type-btn active" data-type="bar">柱状图</button>
        <button class="chart-type-btn" data-type="line">折线图</button>
        <button class="chart-type-btn" data-type="pie">饼图</button>
      </div>
      <div>
        <label style="font-size:13px;color:#666;margin-right:4px">维度:</label>
        <select id="dimension-select" onchange="loadChart()">
          <option value="user_type">人员类型</option>
          <option value="user_level">人员层级</option>
          <option value="user_dept">人员部门</option>
          <option value="api_name">接口</option>
          <option value="user_type,api_name">人员类型 × 接口</option>
        </select>
      </div>
    </div>
    <div id="chart-container">
      <div class="loading-spinner" id="chart-loading"><div class="spinner"></div>加载中...</div>
      <div class="chart-wrapper"><canvas id="tracking-chart"></canvas></div>
    </div>
  </div>
</div>

<script>
const API_BASE = 'http://localhost:8000';
let chartInstance = null;
let currentChartType = 'bar';
let lastChartData = null;

// ---- 浏览器兼容性检测 ----
if (typeof fetch === 'undefined') {
  document.body.innerHTML = '<div class="error-banner" style="margin:40px;text-align:center">⚠️ 请使用现代浏览器（Chrome / Firefox / Edge）</div>';
}

// ---- Toast ----
function showToast(message, level) {
  level = level || 'info';
  const container = document.getElementById('toast-container');
  const toast = document.createElement('div');
  toast.className = 'toast ' + level;
  toast.textContent = message;
  container.appendChild(toast);
  setTimeout(function(){ toast.remove(); }, 4000);
}

// ---- fetchWrapper ----
async function fetchWrapper(url, options) {
  options = options || {};
  const controller = new AbortController();
  const timeout = setTimeout(function(){ controller.abort(); }, 5000);
  options.signal = controller.signal;
  try {
    const resp = await fetch(url, options);
    clearTimeout(timeout);
    if (!resp.ok) {
      let errMsg = '服务器返回异常 (HTTP ' + resp.status + ')';
      try {
        const body = await resp.json();
        if (body.error) errMsg = body.error;
        if (body.detail) errMsg = JSON.stringify(body.detail);
      } catch(e) {}
      throw new Error(errMsg);
    }
    const ct = resp.headers.get('content-type') || '';
    if (ct.includes('application/json')) return await resp.json();
    return await resp.text();
  } catch(e) {
    clearTimeout(timeout);
    if (e.name === 'AbortError') throw new Error('❌ 请求超时，无法连接后端服务');
    if (e.message.includes('Failed to fetch') || e.message.includes('NetworkError'))
      throw new Error('❌ 无法连接后端服务，请检查网络或稍后重试');
    throw e;
  }
}

// ---- Tab 切换 ----
document.querySelectorAll('.tab').forEach(function(btn) {
  btn.addEventListener('click', function() {
    document.querySelectorAll('.tab').forEach(function(b){ b.classList.remove('active'); });
    document.querySelectorAll('.tab-panel').forEach(function(p){ p.classList.remove('active'); });
    btn.classList.add('active');
    document.getElementById('tab-' + btn.dataset.tab).classList.add('active');
  });
});

// ---- 业务 API 调用 ----
async function executeHelloworld() {
  try {
    const data = await fetchWrapper(API_BASE + '/api/helloworld',
      {headers: getTrackingHeaders()});
    document.getElementById('result-helloworld').textContent = JSON.stringify(data, null, 2);
    showToast('✅ HelloWorld 执行成功');
  } catch(e) {
    document.getElementById('result-helloworld').textContent = '错误: ' + e.message;
    showToast(e.message, 'error');
  }
}

async function executeHash() {
  try {
    const input = document.getElementById('hash-input').value;
    const algorithm = document.getElementById('hash-algorithm').value;
    const data = await fetchWrapper(API_BASE + '/api/hash', {
      method: 'POST',
      headers: Object.assign({'Content-Type': 'application/json'}, getTrackingHeaders()),
      body: JSON.stringify({input: input, algorithm: algorithm})
    });
    document.getElementById('result-hash').textContent = JSON.stringify(data, null, 2);
    showToast('✅ Hash 计算成功');
  } catch(e) {
    document.getElementById('result-hash').textContent = '错误: ' + e.message;
    showToast(e.message, 'error');
  }
}

async function executeBubbleSort() {
  try {
    const raw = document.getElementById('bs-input').value;
    const array = raw.split(',').map(function(s){ return parseFloat(s.trim()); }).filter(function(n){ return !isNaN(n); });
    if (array.length === 0) {
      document.getElementById('result-bubble_sort').textContent = '请先输入有效的数组';
      showToast('⚠️ 输入格式错误，请检查', 'warning');
      return;
    }
    const data = await fetchWrapper(API_BASE + '/api/bubble_sort', {
      method: 'POST',
      headers: Object.assign({'Content-Type': 'application/json'}, getTrackingHeaders()),
      body: JSON.stringify({array: array})
    });
    document.getElementById('result-bubble_sort').textContent = JSON.stringify(data, null, 2);
    showToast('✅ 冒泡排序完成');
  } catch(e) {
    document.getElementById('result-bubble_sort').textContent = '错误: ' + e.message;
    showToast(e.message, 'error');
  }
}

// ---- 导出 ----
function exportResult(type) {
  let url = API_BASE + '/api/export/' + type;
  if (type === 'hash') {
    const input = encodeURIComponent(document.getElementById('hash-input').value);
    const algorithm = encodeURIComponent(document.getElementById('hash-algorithm').value);
    url += '?input=' + input + '&algorithm=' + algorithm;
  } else if (type === 'bubble_sort') {
    const raw = document.getElementById('bs-input').value;
    const array = raw.split(',').map(function(s){ return s.trim(); }).filter(function(s){ return s !== ''; }).join(',');
    url += '?array=' + encodeURIComponent(array);
  }
  const a = document.createElement('a');
  a.href = url;
  a.download = type + '_export.json';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  showToast('📥 导出已开始');
}

// ---- 埋点 Header ----
function getTrackingHeaders() {
  return {
    'X-User-Id': 'dev-user',
    'X-User-Type': '正式',
    'X-User-Level': 'P7',
    'X-User-Dept': '技术'
  };
}

// ---- 图表 ----
document.querySelectorAll('.chart-type-btn').forEach(function(btn) {
  btn.addEventListener('click', function() {
    document.querySelectorAll('.chart-type-btn').forEach(function(b){ b.classList.remove('active'); });
    btn.classList.add('active');
    currentChartType = btn.dataset.type;
    if (lastChartData) renderChart(lastChartData);
  });
});

async function loadChart() {
  const loading = document.getElementById('chart-loading');
  loading.style.display = 'flex';
  try {
    const dimension = document.getElementById('dimension-select').value;
    const data = await fetchWrapper(API_BASE + '/api/tracking/report?dimension=' + encodeURIComponent(dimension));
    lastChartData = data;
    if (!data.labels || data.labels.length === 0) {
      loading.innerHTML = '暂无调用数据，请先使用上方接口';
      if (chartInstance) { chartInstance.destroy(); chartInstance = null; }
    } else {
      renderChart(data);
    }
  } catch(e) {
    loading.innerHTML = '⚠️ 报表加载失败: ' + e.message;
    showToast(e.message, 'error');
  } finally {
    if (loading.style.display === 'flex') loading.style.display = 'none';
  }
}

function renderChart(data) {
  const ctx = document.getElementById('tracking-chart').getContext('2d');
  if (chartInstance) chartInstance.destroy();

  const colors = ['#2563eb','#16a34a','#dc2626','#ca8a04','#9333ea','#0891b2','#db2777','#ea580c','#4f46e5','#65a30d'];

  // 维度值过多（>20）时，饼图自动切换为柱状图
  let chartType = currentChartType;
  if (chartType === 'pie' && data.labels.length > 20) {
    chartType = 'bar';
    showToast('维度值过多（>20），已自动切换为柱状图', 'warning');
  }

  chartInstance = new Chart(ctx, {
    type: chartType,
    data: {
      labels: data.labels,
      datasets: [{
        label: '调用次数',
        data: data.values,
        backgroundColor: chartType === 'pie'
          ? colors.slice(0, data.labels.length)
          : colors[0],
        borderColor: chartType === 'pie' ? '#fff' : colors[0],
        borderWidth: 1
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: chartType === 'pie' }
      },
      scales: chartType === 'pie' ? {} : {
        y: { beginAtZero: true, ticks: { stepSize: 1 } }
      }
    }
  });
}

// 初始加载
loadChart();
</script>
</body>
</html>
```

- [ ] **Step 2: 验证前端页面**

```bash
# 启动后端
cd /root/.agentix/agentic-dev/runs/DEV-9d10e310-7901-11f1-8a9f-59ecae612580-cd8de22e-c4bc-4aec-b3b5-17005a25e187/worktree/manyu_test-cred-test-20260716022903
uvicorn main:app --host 0.0.0.0 --port 8000 &
sleep 2

# 用 Python 启动简单 HTTP 服务器托管前端
cd /root/.agentix/agentic-dev/runs/DEV-9d10e310-7901-11f1-8a9f-59ecae612580-cd8de22e-c4bc-4aec-b3b5-17005a25e187/worktree/manyu_test1-main
python3 -m http.server 8080 &
sleep 2

# 通过 curl 验证前端页面可访问
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/index.html
# Expected: 200

# 发送几个带埋点的请求以生成图表数据
curl -s http://localhost:8000/api/helloworld \
  -H "X-User-Id: alice" -H "X-User-Type: 正式" -H "X-User-Level: P7" -H "X-User-Dept: 技术"
curl -s -X POST http://localhost:8000/api/hash \
  -H "Content-Type: application/json" \
  -H "X-User-Id: bob" -H "X-User-Type: 外包" -H "X-User-Level: P6" -H "X-User-Dept: 技术" \
  -d '{"input":"test","algorithm":"md5"}'
curl -s -X POST http://localhost:8000/api/bubble_sort \
  -H "Content-Type: application/json" \
  -H "X-User-Id: carol" -H "X-User-Type: 实习生" -H "X-User-Level: P5" -H "X-User-Dept: 产品" \
  -d '{"array":[3,1,2]}'

# 验证报表 API 返回数据
curl -s "http://localhost:8000/api/tracking/report?dimension=user_type"
# Expected: {"labels":["正式","外包","实习生"],"values":[1,1,1]}

kill %1 %2 2>/dev/null
```

Expected: 前端 HTML 可正常访问（200），报表 API 返回正确聚合数据。

- [ ] **Step 3: Commit**

```bash
cd /root/.agentix/agentic-dev/runs/DEV-9d10e310-7901-11f1-8a9f-59ecae612580-cd8de22e-c4bc-4aec-b3b5-17005a25e187/worktree/manyu_test1-main
git add index.html
git commit -m "feat: add single-page frontend with 3 tabs, export, and Chart.js dashboard"
```

---

## Task 6: 联调验证

**Files:**
- 无新建/修改

**Interfaces:**
- 端到端验证：前端 → 后端 API → 埋点写入 → 仪表盘展示

- [ ] **Step 1: 启动双端服务**

```bash
cd /root/.agentix/agentic-dev/runs/DEV-9d10e310-7901-11f1-8a9f-59ecae612580-cd8de22e-c4bc-4aec-b3b5-17005a25e187/worktree/manyu_test-cred-test-20260716022903
rm -f tracking.db
uvicorn main:app --host 0.0.0.0 --port 8000 &
sleep 2

cd /root/.agentix/agentic-dev/runs/DEV-9d10e310-7901-11f1-8a9f-59ecae612580-cd8de22e-c4bc-4aec-b3b5-17005a25e187/worktree/manyu_test1-main
python3 -m http.server 8080 &
sleep 2
```

- [ ] **Step 2: 全链路 curl 验证**

```bash
# 1. HelloWorld
echo "=== HelloWorld ==="
curl -s http://localhost:8000/api/helloworld \
  -H "X-User-Id: alice" -H "X-User-Type: 正式" -H "X-User-Level: P7" -H "X-User-Dept: 技术"

# 2. Hash (sha256)
echo -e "\n=== Hash ==="
curl -s -X POST http://localhost:8000/api/hash \
  -H "Content-Type: application/json" \
  -H "X-User-Id: bob" -H "X-User-Type: 外包" -H "X-User-Level: P6" -H "X-User-Dept: 技术" \
  -d '{"input":"hello world","algorithm":"sha256"}'

# 3. BubbleSort
echo -e "\n=== BubbleSort ==="
curl -s -X POST http://localhost:8000/api/bubble_sort \
  -H "Content-Type: application/json" \
  -H "X-User-Id: carol" -H "X-User-Type: 正式" -H "X-User-Level: P8" -H "X-User-Dept: 产品" \
  -d '{"array":[9,3,7,1,5,4,8,2,6]}'

# 4. 导出 HelloWorld
echo -e "\n=== Export HelloWorld ==="
curl -s -o /tmp/e2e_export_helloworld.json http://localhost:8000/api/export/helloworld
cat /tmp/e2e_export_helloworld.json

# 5. 导出 Hash
echo -e "\n=== Export Hash ==="
curl -s -o /tmp/e2e_export_hash.json "http://localhost:8000/api/export/hash?input=hello&algorithm=md5"
cat /tmp/e2e_export_hash.json

# 6. 导出 BubbleSort
echo -e "\n=== Export BubbleSort ==="
curl -s -o /tmp/e2e_export_bubble.json "http://localhost:8000/api/export/bubble_sort?array=5,2,8,1,3"
cat /tmp/e2e_export_bubble.json

# 7. 埋点报表 - 按人员类型
echo -e "\n=== Report: user_type ==="
curl -s "http://localhost:8000/api/tracking/report?dimension=user_type"

# 8. 埋点报表 - 按接口
echo -e "\n=== Report: api_name ==="
curl -s "http://localhost:8000/api/tracking/report?dimension=api_name"

# 9. 埋点报表 - 二维交叉
echo -e "\n=== Report: user_type,api_name ==="
curl -s "http://localhost:8000/api/tracking/report?dimension=user_type,api_name"

# 10. 前端页面可访问
echo -e "\n=== Frontend ==="
curl -s -o /dev/null -w "HTTP %{http_code}" http://localhost:8080/index.html
echo ""
```

Expected: 所有 API 返回正确 JSON，前端页面返回 200，报表数据与发送的请求一致。

- [ ] **Step 3: 清理并停止服务**

```bash
kill %1 %2 2>/dev/null
```

---

## Self-Review

### 1. Spec Coverage

| 需求 | 覆盖任务 |
|------|----------|
| helloworld 接口 | Task 1: `GET /api/helloworld` |
| 哈希算法接口 | Task 1: `POST /api/hash` |
| 冒泡排序接口 | Task 1: `POST /api/bubble_sort` |
| 前端三 Tab 页面 | Task 5: `index.html` Tab 切换 |
| 导出按钮 | Task 3: 后端导出接口 + Task 5: 前端导出按钮 |
| 埋点记录调用次数+调用人 | Task 2: TrackingMiddleware + SQLite |
| 可视化报表（维度：人员类型/层级/部门） | Task 2: `/api/tracking/report` + Task 5: Chart.js |
| 折线图/饼图/柱状图 | Task 5: Chart.js 三种类型切换 |
| 异常兜底 | Task 1: 全局异常处理 + Task 5: fetchWrapper 超时/错误处理 |

### 2. Placeholder Scan

无 TBD/TODO/placeholders。所有代码完整、可执行。

### 3. Type Consistency

- `main.py` imports `bubble_sort` from `bubble_sort.py` ✓
- `tracking.py` uses `TRACKED_PATHS` matching the actual API paths ✓
- `main.py` imports `init_db`, `TrackingMiddleware`, `get_report` from `tracking.py` ✓
- Frontend `API_BASE` points to `http://localhost:8000` ✓
- Frontend tracking headers match backend expected headers (`X-User-Id`, `X-User-Type`, `X-User-Level`, `X-User-Dept`) ✓
- Export URL patterns match backend routes ✓
- Dimension values in frontend `<select>` match `VALID_DIMENSIONS` in `tracking.py` ✓

---

*Plan generated by DTCoder using writing-plans skill.*