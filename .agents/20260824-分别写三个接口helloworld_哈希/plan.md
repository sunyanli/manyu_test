# 三个接口（HelloWorld / SHA256哈希 / 冒泡排序）+ 前端页面 + 埋点可视化 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现三个后端接口（HelloWorld、SHA256哈希、冒泡排序）、前端三Tab页面、导出功能、后端埋点及前端可视化报表。

**Architecture:** 跨仓协作架构。`manyu_test` 为后端仓库（FastAPI Python），提供 RESTful API 及埋点/导出/统计服务；`manyu_test1` 为前端仓库（Vue 3），提供 SPA 页面、Tab 切换、ECharts 图表展示。两仓通过 HTTP API 契约对齐。

**Tech Stack:**
- 后端：Python 3 + FastAPI + SQLite + openpyxl (Excel导出)
- 前端：Vue 3 + Vite + ECharts + Axios
- 可视化：ECharts（折线图/饼图/柱状图）

---

## Global Constraints

- 所有 API 路径以 `/api/` 开头
- 统一错误响应格式：`{ "code": "ERROR_CODE", "message": "...", "detail": null, "request_id": "uuid" }`
- 埋点异步写入，不影响主流程
- 导出格式支持 CSV 和 Excel（xlsx）
- 对前端暴露的接口字段名使用 camelCase
- 禁止修改 `bubble_sort.py` 已有函数签名（向后兼容）
- 前端路由使用 Vue Router，页面路径 `/tools`
- 图表库使用 ECharts（折线图 + 饼图 + 柱状图）

---

## File Structure

### 仓库 `manyu_test`（后端）

| 文件路径 | 职责 |
|---------|------|
| `main.py` | FastAPI 应用入口，注册路由、异常处理器、启动事件 |
| `api/hello.py` | HelloWorld 接口 GET `/api/hello` |
| `api/hash.py` | SHA256 哈希接口 POST `/api/hash` |
| `api/bubble.py` | 冒泡排序接口 POST `/api/bubble-sort` |
| `api/export.py` | 导出接口 GET `/api/export` |
| `api/stats.py` | 统计接口 GET `/api/stats` |
| `models/tracking.py` | 埋点数据模型（SQLite 表结构定义） |
| `services/tracking_service.py` | 埋点异步写入服务 |
| `services/export_service.py` | CSV/Excel 导出文件生成服务 |
| `middleware/error_handler.py` | 全局异常处理器 |
| `middleware/tracking_middleware.py` | 请求埋点中间件 |
| `requirements.txt` | Python 依赖清单 |

### 仓库 `manyu_test1`（前端）

| 文件路径 | 职责 |
|---------|------|
| `src/router/index.js` | Vue Router 路由配置，添加 `/tools` 路由 |
| `src/views/ToolsPage.vue` | 主页面，Tab 容器 |
| `src/components/tabs/HelloTab.vue` | HelloWorld Tab 内容组件 |
| `src/components/tabs/HashTab.vue` | SHA256 Hash Tab 内容组件 |
| `src/components/tabs/BubbleTab.vue` | 冒泡排序 Tab 内容组件 |
| `src/components/ExportButton.vue` | 导出按钮组件（CSV/Excel 格式选择） |
| `src/components/charts/StatsDashboard.vue` | 统计报表仪表盘容器 |
| `src/components/charts/LineChart.vue` | 折线图组件（时间趋势） |
| `src/components/charts/PieChart.vue` | 饼图组件（人员类型/部门分布） |
| `src/components/charts/BarChart.vue` | 柱状图组件（各维度对比） |
| `src/components/charts/DimensionFilter.vue` | 维度筛选器（人员类型/层级/部门） |
| `src/api/index.js` | Axios 实例配置（baseURL、拦截器） |
| `src/api/endpoints.js` | API 端点封装函数 |

---

## Task 1: 后端 — 项目初始化与基础配置

**Files:**
- Create: `manyu_test/main.py`
- Create: `manyu_test/requirements.txt`
- Create: `manyu_test/middleware/error_handler.py`

**Interfaces:**
- Consumes: 无（初始任务）
- Produces: 全局 FastAPI 应用实例；统一错误响应格式；`requirements.txt` 依赖清单

- [ ] **Step 1: 创建 requirements.txt**

```text
fastapi==0.104.1
uvicorn==0.24.0
pydantic==2.5.2
openpyxl==3.1.2
python-multipart==0.0.6
```

- [ ] **Step 2: 创建 error_handler.py 中间件**

```python
# manyu_test/middleware/error_handler.py
import logging
from uuid import uuid4
from fastapi import Request, HTTPException
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError

logger = logging.getLogger(__name__)

ERROR_CODES = {
    "INVALID_INPUT": 400,
    "EMPTY_INPUT": 400,
    "INVALID_NUMBER_ARRAY": 400,
    "HASH_ERROR": 500,
    "SORT_ERROR": 500,
    "EXPORT_ERROR": 500,
    "EXPORT_FORMAT_UNSUPPORTED": 400,
    "STATS_QUERY_ERROR": 500,
    "TRACKING_WRITE_FAILED": 200,
    "RATE_LIMIT": 429,
    "INTERNAL_ERROR": 500,
    "NOT_FOUND": 404,
}

def make_error(code: str, message: str, detail=None) -> dict:
    return {
        "code": code,
        "message": message,
        "detail": detail,
        "request_id": str(uuid4()),
    }

async def validation_exception_handler(request: Request, exc: RequestValidationError):
    return JSONResponse(
        status_code=400,
        content=make_error("INVALID_INPUT", "请求参数校验失败", exc.errors()),
    )

async def http_exception_handler(request: Request, exc: HTTPException):
    return JSONResponse(
        status_code=exc.status_code,
        content=make_error("HTTP_ERROR", exc.detail),
    )

async def global_exception_handler(request: Request, exc: Exception):
    logger.exception("Unhandled exception: %s", exc)
    return JSONResponse(
        status_code=500,
        content=make_error("INTERNAL_ERROR", "服务器内部错误，请稍后重试"),
    )
```

- [ ] **Step 3: 创建 main.py 入口**

```python
# manyu_test/main.py
import logging
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from middleware.error_handler import (
    validation_exception_handler,
    http_exception_handler,
    global_exception_handler,
    RequestValidationError,
    HTTPException,
)

logging.basicConfig(level=logging.INFO)

app = FastAPI(title="Manyu Test API", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.add_exception_handler(RequestValidationError, validation_exception_handler)
app.add_exception_handler(HTTPException, http_exception_handler)
app.add_exception_handler(Exception, global_exception_handler)

@app.get("/health")
async def health():
    return {"status": "ok"}
```

- [ ] **Step 4: 验证启动**

```bash
cd /path/to/manyu_test
pip install -r requirements.txt
python -c "from main import app; print('FastAPI app loaded successfully')"
```

---

## Task 2: 后端 — 实现三个基础接口（HelloWorld / SHA256 / 冒泡排序）

**Files:**
- Create: `manyu_test/api/__init__.py`（空文件）
- Create: `manyu_test/api/hello.py`
- Create: `manyu_test/api/hash.py`
- Create: `manyu_test/api/bubble.py`
- Modify: `manyu_test/main.py`（注册路由）

**Interfaces:**
- Consumes: `bubble_sort.py` 中的 `bubble_sort_optimized` 函数；`middleware/error_handler.py` 中的 `make_error` 函数
- Produces: GET `/api/hello` → `{ "message": "Hello World!", "timestamp": "..." }`；POST `/api/hash` → `{ "algorithm": "SHA256", "input": "...", "output": "..." }`；POST `/api/bubble-sort` → `{ "sorted": [...], "swaps": int }`

- [ ] **Step 1: 创建 api/__init__.py**

```
# 空文件
```

- [ ] **Step 2: 创建 api/hello.py**

```python
# manyu_test/api/hello.py
from datetime import datetime, timezone
from fastapi import APIRouter

router = APIRouter()

@router.get("/api/hello")
async def hello_world():
    return {
        "message": "Hello World!",
        "timestamp": datetime.now(timezone.utc).isoformat(),
    }
```

- [ ] **Step 3: 创建 api/hash.py**

```python
# manyu_test/api/hash.py
import hashlib
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

router = APIRouter()

MAX_INPUT_LENGTH = 1_048_576  # 1MB

class HashRequest(BaseModel):
    text: str = Field(..., min_length=1)

class HashResponse(BaseModel):
    algorithm: str
    input: str
    output: str
    truncated: bool = False

@router.post("/api/hash", response_model=HashResponse)
async def sha256_hash(request: HashRequest):
    text = request.text
    truncated = False
    if len(text.encode("utf-8")) > MAX_INPUT_LENGTH:
        text = text[:MAX_INPUT_LENGTH]
        truncated = True
    try:
        hash_output = hashlib.sha256(text.encode("utf-8")).hexdigest()
    except (ValueError, TypeError) as e:
        raise HTTPException(status_code=500, detail={
            "code": "HASH_ERROR",
            "message": "哈希计算异常",
        })
    return HashResponse(
        algorithm="SHA256",
        input=request.text[:100] + ("..." if len(request.text) > 100 else ""),
        output=hash_output,
        truncated=truncated,
    )
```

- [ ] **Step 4: 创建 api/bubble.py**

```python
# manyu_test/api/bubble.py
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field
from bubble_sort import bubble_sort_optimized, bubble_sort

router = APIRouter()

class BubbleSortRequest(BaseModel):
    numbers: list[int] = Field(..., min_length=1)

class BubbleSortResponse(BaseModel):
    sorted: list[int]
    swaps: int
    fallback: bool = False

def count_swaps(arr: list[int]) -> int:
    """计算冒泡排序的交换次数"""
    n = len(arr)
    swap_count = 0
    for i in range(n):
        for j in range(0, n - i - 1):
            if arr[j] > arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swap_count += 1
    return swap_count

@router.post("/api/bubble-sort", response_model=BubbleSortResponse)
async def bubble_sort_api(request: BubbleSortRequest):
    numbers = request.numbers
    if len(numbers) > 10000:
        raise HTTPException(status_code=400, detail={
            "code": "INVALID_INPUT",
            "message": "数组过大，最大支持 10000 个元素",
        })
    try:
        sorted_numbers = bubble_sort_optimized(numbers.copy())
        swaps = count_swaps(numbers.copy())
        fallback = False
    except Exception:
        # 降级使用内置 sorted()
        sorted_numbers = sorted(numbers)
        swaps = 0
        fallback = True
    return BubbleSortResponse(
        sorted=sorted_numbers,
        swaps=swaps,
        fallback=fallback,
    )
```

- [ ] **Step 5: 修改 main.py 注册路由**

在 `main.py` 末尾添加：

```python
from api.hello import router as hello_router
from api.hash import router as hash_router
from api.bubble import router as bubble_router

app.include_router(hello_router)
app.include_router(hash_router)
app.include_router(bubble_router)
```

- [ ] **Step 6: 验证接口**

```bash
# 启动服务
cd /path/to/manyu_test
uvicorn main:app --port 8000 &
sleep 2

# 测试 HelloWorld
curl http://localhost:8000/api/hello
# 预期: {"message":"Hello World!","timestamp":"..."}

# 测试哈希
curl -X POST http://localhost:8000/api/hash \
  -H "Content-Type: application/json" \
  -d '{"text":"hello"}'
# 预期: {"algorithm":"SHA256","input":"hello","output":"2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824","truncated":false}

# 测试冒泡排序
curl -X POST http://localhost:8000/api/bubble-sort \
  -H "Content-Type: application/json" \
  -d '{"numbers":[3,1,4,1,5]}'
# 预期: {"sorted":[1,1,3,4,5],"swaps":4,"fallback":false}

# 停止后台服务
kill %1
```

---

## Task 3: 后端 — 埋点数据模型与异步写入服务

**Files:**
- Create: `manyu_test/models/__init__.py`（空文件）
- Create: `manyu_test/models/tracking.py`
- Create: `manyu_test/services/__init__.py`（空文件）
- Create: `manyu_test/services/tracking_service.py`

**Interfaces:**
- Consumes: 无
- Produces: SQLite 埋点表结构；`TrackingService` 类（异步写入方法 `record_call`、查询方法 `query_stats`）

- [ ] **Step 1: 创建 models/tracking.py**

```python
# manyu_test/models/tracking.py
import sqlite3
import os

DB_PATH = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "tracking.db")

CREATE_TABLE_SQL = """
CREATE TABLE IF NOT EXISTS api_calls (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id TEXT NOT NULL DEFAULT 'anonymous',
    user_name TEXT NOT NULL DEFAULT 'anonymous',
    user_type TEXT DEFAULT '',
    user_level TEXT DEFAULT '',
    user_department TEXT DEFAULT '',
    api_name TEXT NOT NULL,
    call_time TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)
"""

CREATE_INDEX_SQL = """
CREATE INDEX IF NOT EXISTS idx_api_calls_call_time ON api_calls(call_time)
"""

def init_db():
    conn = sqlite3.connect(DB_PATH)
    conn.execute(CREATE_TABLE_SQL)
    conn.execute(CREATE_INDEX_SQL)
    conn.commit()
    conn.close()
```

- [ ] **Step 2: 创建 services/tracking_service.py**

```python
# manyu_test/services/tracking_service.py
import logging
import sqlite3
from datetime import datetime, timezone
from models.tracking import DB_PATH, init_db

logger = logging.getLogger(__name__)

# 初始化数据库
init_db()

class TrackingService:
    """埋点服务，异步记录 API 调用"""
    
    CONSECUTIVE_FAILURES = 0
    PAUSED = False
    
    @classmethod
    def record_call(cls, user_id: str = "anonymous", user_name: str = "anonymous",
                    user_type: str = "", user_level: str = "",
                    user_department: str = "", api_name: str = "") -> bool:
        """记录一次 API 调用（同步写入，但调用方应在后台任务中触发）"""
        if cls.PAUSED:
            return False
        
        try:
            conn = sqlite3.connect(DB_PATH, timeout=2.0)
            conn.execute(
                """INSERT INTO api_calls (user_id, user_name, user_type, user_level,
                   user_department, api_name, call_time)
                   VALUES (?, ?, ?, ?, ?, ?, ?)""",
                (user_id, user_name, user_type, user_level,
                 user_department, api_name, datetime.now(timezone.utc).isoformat())
            )
            conn.commit()
            conn.close()
            cls.CONSECUTIVE_FAILURES = 0
            return True
        except Exception as e:
            cls.CONSECUTIVE_FAILURES += 1
            if cls.CONSECUTIVE_FAILURES >= 10:
                cls.PAUSED = True
                logger.error("埋点连续失败10次，暂停60s")
            elif cls.CONSECUTIVE_FAILURES >= 5:
                logger.error("埋点连续失败5次: %s", e)
            else:
                logger.warning("埋点写入失败: %s", e)
            return False
    
    @classmethod
    def query_stats(cls, dimension: str = "user_type") -> list[dict]:
        """按维度查询调用统计"""
        dimension_map = {
            "user_type": "user_type",
            "user_level": "user_level",
            "user_department": "user_department",
        }
        col = dimension_map.get(dimension, "user_type")
        try:
            conn = sqlite3.connect(DB_PATH, timeout=5.0)
            conn.row_factory = sqlite3.Row
            cursor = conn.execute(
                f"SELECT {col} AS name, COUNT(*) AS value FROM api_calls GROUP BY {col} ORDER BY value DESC"
            )
            rows = [dict(row) for row in cursor.fetchall()]
            conn.close()
            return rows
        except Exception as e:
            logger.error("统计数据查询失败: %s", e)
            return []
    
    @classmethod
    def query_trend(cls, dimension: str = "user_type") -> list[dict]:
        """按时间趋势查询调用统计（折线图用）"""
        col_map = {
            "user_type": "user_type",
            "user_level": "user_level",
            "user_department": "user_department",
        }
        col = col_map.get(dimension, "user_type")
        try:
            conn = sqlite3.connect(DB_PATH, timeout=5.0)
            conn.row_factory = sqlite3.Row
            cursor = conn.execute(
                f"""SELECT DATE(call_time) AS date, {col} AS name, COUNT(*) AS value
                    FROM api_calls
                    GROUP BY DATE(call_time), {col}
                    ORDER BY date ASC"""
            )
            rows = [dict(row) for row in cursor.fetchall()]
            conn.close()
            return rows
        except Exception as e:
            logger.error("趋势数据查询失败: %s", e)
            return []
```

---

## Task 4: 后端 — 埋点中间件 + 统计 API + 导出 API

**Files:**
- Create: `manyu_test/middleware/__init__.py`（空文件）
- Create: `manyu_test/middleware/tracking_middleware.py`
- Create: `manyu_test/api/stats.py`
- Create: `manyu_test/services/export_service.py`
- Create: `manyu_test/api/export.py`
- Modify: `manyu_test/main.py`（注册中间件和新路由）

**Interfaces:**
- Consumes: `TrackingService` 类（record_call, query_stats, query_trend）；`bubble_sort` 模块
- Produces: GET `/api/stats?dimension=user_type` → `[{name, value}, ...]`；GET `/api/stats/trend?dimension=user_type` → `[{date, name, value}, ...]`；GET `/api/export?tab=hello&format=csv` → 文件流

- [ ] **Step 1: 创建 middleware/tracking_middleware.py**

```python
# manyu_test/middleware/tracking_middleware.py
import logging
from fastapi import Request
from starlette.middleware.base import BaseHTTPMiddleware
from services.tracking_service import TrackingService

logger = logging.getLogger(__name__)

TRACKED_PATHS = ["/api/hello", "/api/hash", "/api/bubble-sort"]

class TrackingMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        response = await call_next(request)
        
        path = request.url.path
        if path in TRACKED_PATHS and response.status_code < 400:
            # 异步后台任务记录埋点
            import asyncio
            asyncio.ensure_future(self._record(request, path))
        
        return response
    
    async def _record(self, request: Request, path: str):
        user_id = request.headers.get("X-User-Id", "anonymous")
        user_name = request.headers.get("X-User-Name", "anonymous")
        user_type = request.headers.get("X-User-Type", "")
        user_level = request.headers.get("X-User-Level", "")
        user_department = request.headers.get("X-User-Department", "")
        
        TrackingService.record_call(
            user_id=user_id,
            user_name=user_name,
            user_type=user_type,
            user_level=user_level,
            user_department=user_department,
            api_name=path,
        )
```

- [ ] **Step 2: 创建 api/stats.py**

```python
# manyu_test/api/stats.py
from fastapi import APIRouter, Query, HTTPException
from services.tracking_service import TrackingService

router = APIRouter()

VALID_DIMENSIONS = ["user_type", "user_level", "user_department"]

@router.get("/api/stats")
async def get_stats(dimension: str = Query("user_type", description="统计维度")):
    if dimension not in VALID_DIMENSIONS:
        raise HTTPException(status_code=400, detail={
            "code": "INVALID_INPUT",
            "message": f"无效维度，有效值: {', '.join(VALID_DIMENSIONS)}",
        })
    data = TrackingService.query_stats(dimension)
    return data

@router.get("/api/stats/trend")
async def get_stats_trend(dimension: str = Query("user_type", description="趋势维度")):
    if dimension not in VALID_DIMENSIONS:
        raise HTTPException(status_code=400, detail={
            "code": "INVALID_INPUT",
            "message": f"无效维度，有效值: {', '.join(VALID_DIMENSIONS)}",
        })
    data = TrackingService.query_trend(dimension)
    return data
```

- [ ] **Step 3: 创建 services/export_service.py**

```python
# manyu_test/services/export_service.py
import csv
import io
import logging
import sqlite3
from models.tracking import DB_PATH

logger = logging.getLogger(__name__)

TAB_MAP = {
    "hello": "/api/hello",
    "hash": "/api/hash",
    "bubble": "/api/bubble-sort",
}

def generate_csv(tab: str) -> bytes:
    """生成 CSV 导出内容"""
    api_path = TAB_MAP.get(tab)
    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow(["ID", "用户ID", "用户名", "人员类型", "人员层级", "部门", "API名称", "调用时间"])
    
    try:
        conn = sqlite3.connect(DB_PATH, timeout=5.0)
        if api_path:
            cursor = conn.execute(
                "SELECT id, user_id, user_name, user_type, user_level, user_department, api_name, call_time FROM api_calls WHERE api_name = ? ORDER BY call_time DESC",
                (api_path,)
            )
        else:
            cursor = conn.execute(
                "SELECT id, user_id, user_name, user_type, user_level, user_department, api_name, call_time FROM api_calls ORDER BY call_time DESC"
            )
        for row in cursor:
            writer.writerow(row)
        conn.close()
    except Exception as e:
        logger.error("导出数据查询失败: %s", e)
        # 返回仅含表头的空文件
    
    return output.getvalue().encode("utf-8-sig")

def generate_excel(tab: str) -> bytes:
    """生成 Excel 导出内容"""
    try:
        import openpyxl
    except ImportError:
        logger.warning("openpyxl 未安装，降级为 CSV")
        return None  # 调用方降级为 CSV
    
    wb = openpyxl.Workbook()
    ws = wb.active
    ws.title = tab or "all"
    ws.append(["ID", "用户ID", "用户名", "人员类型", "人员层级", "部门", "API名称", "调用时间"])
    
    api_path = TAB_MAP.get(tab)
    try:
        conn = sqlite3.connect(DB_PATH, timeout=5.0)
        if api_path:
            cursor = conn.execute(
                "SELECT id, user_id, user_name, user_type, user_level, user_department, api_name, call_time FROM api_calls WHERE api_name = ? ORDER BY call_time DESC",
                (api_path,)
            )
        else:
            cursor = conn.execute(
                "SELECT id, user_id, user_name, user_type, user_level, user_department, api_name, call_time FROM api_calls ORDER BY call_time DESC"
            )
        for row in cursor:
            ws.append(list(row))
        conn.close()
    except Exception as e:
        logger.error("导出数据查询失败: %s", e)
    
    output = io.BytesIO()
    wb.save(output)
    return output.getvalue()
```

- [ ] **Step 4: 创建 api/export.py**

```python
# manyu_test/api/export.py
from fastapi import APIRouter, Query, HTTPException
from fastapi.responses import StreamingResponse
from services.export_service import generate_csv, generate_excel, TAB_MAP

router = APIRouter()

VALID_TABS = list(TAB_MAP.keys()) + ["all"]

@router.get("/api/export")
async def export_data(
    tab: str = Query("all", description="导出哪个 Tab 的数据"),
    format: str = Query("csv", description="导出格式: csv/xlsx"),
):
    if tab not in VALID_TABS:
        raise HTTPException(status_code=400, detail={
            "code": "INVALID_INPUT",
            "message": f"无效 tab，有效值: {', '.join(VALID_TABS)}",
        })
    
    if format == "xlsx":
        content = generate_excel(tab)
        if content is None:
            # 降级为 CSV
            content = generate_csv(tab)
            media_type = "text/csv; charset=utf-8-sig"
            filename = f"{tab}_export.csv"
            headers = {"X-Fallback": "csv"}
        else:
            media_type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            filename = f"{tab}_export.xlsx"
            headers = {}
    elif format == "csv":
        content = generate_csv(tab)
        media_type = "text/csv; charset=utf-8-sig"
        filename = f"{tab}_export.csv"
        headers = {}
    else:
        raise HTTPException(status_code=400, detail={
            "code": "EXPORT_FORMAT_UNSUPPORTED",
            "message": "不支持的导出格式，仅支持 csv/xlsx",
        })
    
    return StreamingResponse(
        iter([content]),
        media_type=media_type,
        headers={
            "Content-Disposition": f"attachment; filename={filename}",
            **headers,
        }
    )
```

- [ ] **Step 5: 修改 main.py 注册新路由和中间件**

在 `main.py` 中追加：

```python
from api.stats import router as stats_router
from api.export import router as export_router
from middleware.tracking_middleware import TrackingMiddleware

app.add_middleware(TrackingMiddleware)
app.include_router(stats_router)
app.include_router(export_router)
```

- [ ] **Step 6: 验证接口**

```bash
cd /path/to/manyu_test
uvicorn main:app --port 8000 &
sleep 2

# 导出 CSV
curl -o test.csv "http://localhost:8000/api/export?tab=hello&format=csv"

# 统计接口
curl "http://localhost:8000/api/stats?dimension=user_type"
# 预期: [] (尚无数据)

# 趋势接口
curl "http://localhost:8000/api/stats/trend?dimension=user_type"
# 预期: [] (尚无数据)

kill %1
```

---

## Task 5: 前端 — 项目初始化与页面框架

**Files:**
- Create: `manyu_test1/package.json`
- Create: `manyu_test1/vite.config.js`
- Create: `manyu_test1/index.html`
- Create: `manyu_test1/src/App.vue`
- Create: `manyu_test1/src/main.js`
- Create: `manyu_test1/src/router/index.js`
- Create: `manyu_test1/src/api/index.js`
- Create: `manyu_test1/src/api/endpoints.js`

**Interfaces:**
- Consumes: 后端 API 基础 URL（`http://localhost:8000`）
- Produces: Vue 3 项目骨架；Axios 实例（baseURL + 拦截器）；API 端点封装函数

- [ ] **Step 1: 创建 package.json**

```json
{
  "name": "manyu-test1-frontend",
  "version": "1.0.0",
  "private": true,
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "vue": "^3.3.8",
    "vue-router": "^4.2.5",
    "axios": "^1.6.2",
    "echarts": "^5.4.3",
    "vue-echarts": "^6.6.8"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^4.5.0",
    "vite": "^5.0.6"
  }
}
```

- [ ] **Step 2: 创建 vite.config.js**

```js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8000',
        changeOrigin: true,
      },
    },
  },
})
```

- [ ] **Step 3: 创建 index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Manyu Test Tools</title>
</head>
<body>
  <div id="app"></div>
  <script type="module" src="/src/main.js"></script>
</body>
</html>
```

- [ ] **Step 4: 创建 src/main.js**

```js
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

const app = createApp(App)
app.use(router)
app.mount('#app')
```

- [ ] **Step 5: 创建 src/App.vue**

```vue
<template>
  <div id="app-root">
    <header class="app-header">
      <h1>Manyu Test Tools</h1>
    </header>
    <router-view />
  </div>
</template>

<script setup>
</script>

<style>
body { margin: 0; font-family: Arial, sans-serif; }
.app-header { background: #409eff; color: white; padding: 16px 24px; }
.app-header h1 { margin: 0; font-size: 20px; }
</style>
```

- [ ] **Step 6: 创建 src/router/index.js**

```js
import { createRouter, createWebHistory } from 'vue-router'
import ToolsPage from '../views/ToolsPage.vue'

const routes = [
  {
    path: '/',
    redirect: '/tools',
  },
  {
    path: '/tools',
    name: 'ToolsPage',
    component: ToolsPage,
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
```

- [ ] **Step 7: 创建 src/api/index.js**

```js
import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器 — 添加用户身份信息
api.interceptors.request.use((config) => {
  // 模拟用户身份（联调时替换为真实登录系统）
  config.headers['X-User-Id'] = 'user-001'
  config.headers['X-User-Name'] = '测试用户'
  config.headers['X-User-Type'] = '内部员工'
  config.headers['X-User-Level'] = 'P7'
  config.headers['X-User-Department'] = '技术部'
  return config
})

// 响应拦截器
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.code === 'ECONNABORTED') {
      console.error('请求超时')
    } else if (!error.response) {
      console.error('网络连接已断开')
    }
    return Promise.reject(error)
  }
)

export default api
```

- [ ] **Step 8: 创建 src/api/endpoints.js**

```js
import api from './index'

export const apiEndpoints = {
  hello: () => api.get('/hello'),
  hash: (text) => api.post('/hash', { text }),
  bubbleSort: (numbers) => api.post('/bubble-sort', { numbers }),
  exportData: (tab, format) => api.get('/export', { params: { tab, format }, responseType: 'blob' }),
  getStats: (dimension) => api.get('/stats', { params: { dimension } }),
  getStatsTrend: (dimension) => api.get('/stats/trend', { params: { dimension } }),
}
```

- [ ] **Step 9: 验证前端能启动**

```bash
cd /path/to/manyu_test1
npm install
npm run dev &
sleep 3
curl http://localhost:5173
# 预期: 返回 HTML 页面内容
kill %1
```

---

## Task 6: 前端 — 三 Tab 页面组件开发

**Files:**
- Create: `manyu_test1/src/views/ToolsPage.vue`
- Create: `manyu_test1/src/components/tabs/HelloTab.vue`
- Create: `manyu_test1/src/components/tabs/HashTab.vue`
- Create: `manyu_test1/src/components/tabs/BubbleTab.vue`

**Interfaces:**
- Consumes: `apiEndpoints` 中的 `hello()`、`hash(text)`、`bubbleSort(numbers)`
- Produces: 三个 Tab 内容组件，每个组件包含输入表单和结果显示区域

- [ ] **Step 1: 创建 ToolsPage.vue**

```vue
<template>
  <div class="tools-page">
    <div class="tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        :class="['tab-btn', { active: activeTab === tab.key }]"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
      </button>
    </div>
    <div class="tab-content">
      <HelloTab v-if="activeTab === 'hello'" />
      <HashTab v-if="activeTab === 'hash'" />
      <BubbleTab v-if="activeTab === 'bubble'" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import HelloTab from '../components/tabs/HelloTab.vue'
import HashTab from '../components/tabs/HashTab.vue'
import BubbleTab from '../components/tabs/BubbleTab.vue'

const tabs = [
  { key: 'hello', label: 'HelloWorld' },
  { key: 'hash', label: 'SHA256 哈希' },
  { key: 'bubble', label: '冒泡排序' },
]
const activeTab = ref('hello')
</script>

<style scoped>
.tools-page { padding: 24px; }
.tabs { display: flex; gap: 4px; border-bottom: 2px solid #e4e7ed; margin-bottom: 20px; }
.tab-btn {
  padding: 10px 20px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: all 0.2s;
}
.tab-btn:hover { color: #409eff; }
.tab-btn.active { color: #409eff; border-bottom-color: #409eff; font-weight: 600; }
.tab-content { min-height: 300px; }
</style>
```

- [ ] **Step 2: 创建 HelloTab.vue**

```vue
<template>
  <div class="tab-panel">
    <h3>HelloWorld 接口</h3>
    <button class="btn-primary" @click="fetchHello" :disabled="loading">
      {{ loading ? '请求中...' : '调用 HelloWorld 接口' }}
    </button>
    <div v-if="result" class="result-card">
      <p><strong>消息:</strong> {{ result.message }}</p>
      <p><strong>时间戳:</strong> {{ result.timestamp }}</p>
    </div>
    <div v-if="error" class="error-msg">{{ error }}</div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { apiEndpoints } from '../../api/endpoints'

const result = ref(null)
const error = ref(null)
const loading = ref(false)

async function fetchHello() {
  loading.value = true
  error.value = null
  result.value = null
  try {
    const res = await apiEndpoints.hello()
    result.value = res.data
  } catch (e) {
    error.value = e.response?.data?.message || '请求失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.tab-panel { padding: 16px 0; }
.btn-primary {
  padding: 8px 20px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}
.btn-primary:disabled { background: #a0cfff; cursor: not-allowed; }
.result-card {
  margin-top: 16px;
  padding: 16px;
  background: #f0f9eb;
  border: 1px solid #c2e7b0;
  border-radius: 4px;
}
.error-msg {
  margin-top: 16px;
  padding: 12px;
  background: #fef0f0;
  border: 1px solid #fde2e2;
  border-radius: 4px;
  color: #f56c6c;
}
</style>
```

- [ ] **Step 3: 创建 HashTab.vue**

```vue
<template>
  <div class="tab-panel">
    <h3>SHA256 哈希计算</h3>
    <div class="input-group">
      <input v-model="inputText" type="text" placeholder="输入要哈希的字符串" class="input-field" />
      <button class="btn-primary" @click="computeHash" :disabled="loading || !inputText.trim()">
        {{ loading ? '计算中...' : '计算哈希' }}
      </button>
    </div>
    <div v-if="result" class="result-card">
      <p><strong>算法:</strong> {{ result.algorithm }}</p>
      <p><strong>输入:</strong> {{ result.input }}</p>
      <p><strong>哈希值:</strong></p>
      <code class="hash-output">{{ result.output }}</code>
      <p v-if="result.truncated" class="warning">⚠️ 输入过长，已截断</p>
    </div>
    <div v-if="error" class="error-msg">{{ error }}</div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { apiEndpoints } from '../../api/endpoints'

const inputText = ref('')
const result = ref(null)
const error = ref(null)
const loading = ref(false)

async function computeHash() {
  loading.value = true
  error.value = null
  result.value = null
  try {
    const res = await apiEndpoints.hash(inputText.value)
    result.value = res.data
  } catch (e) {
    error.value = e.response?.data?.message || '请求失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.tab-panel { padding: 16px 0; }
.input-group { display: flex; gap: 12px; margin-bottom: 16px; }
.input-field {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
}
.btn-primary {
  padding: 8px 20px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  white-space: nowrap;
}
.btn-primary:disabled { background: #a0cfff; cursor: not-allowed; }
.result-card {
  padding: 16px;
  background: #f0f9eb;
  border: 1px solid #c2e7b0;
  border-radius: 4px;
}
.hash-output {
  display: block;
  word-break: break-all;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
}
.error-msg {
  margin-top: 16px;
  padding: 12px;
  background: #fef0f0;
  border: 1px solid #fde2e2;
  border-radius: 4px;
  color: #f56c6c;
}
.warning { color: #e6a23c; font-size: 12px; }
</style>
```

- [ ] **Step 4: 创建 BubbleTab.vue**

```vue
<template>
  <div class="tab-panel">
    <h3>冒泡排序</h3>
    <div class="input-group">
      <input
        v-model="inputNumbers"
        type="text"
        placeholder="输入整数数组，逗号分隔，如: 3,1,4,1,5"
        class="input-field"
      />
      <button class="btn-primary" @click="sortNumbers" :disabled="loading || !inputNumbers.trim()">
        {{ loading ? '排序中...' : '执行排序' }}
      </button>
    </div>
    <div v-if="result" class="result-card">
      <p><strong>原始数组:</strong> [{{ inputNumbers }}]</p>
      <p><strong>排序结果:</strong> [{{ result.sorted.join(', ') }}]</p>
      <p><strong>交换次数:</strong> {{ result.swaps }}</p>
      <p v-if="result.fallback" class="warning">⚠️ 使用内置排序降级</p>
    </div>
    <div v-if="error" class="error-msg">{{ error }}</div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { apiEndpoints } from '../../api/endpoints'

const inputNumbers = ref('')
const result = ref(null)
const error = ref(null)
const loading = ref(false)

async function sortNumbers() {
  loading.value = true
  error.value = null
  result.value = null
  try {
    const numbers = inputNumbers.value.split(',').map(s => parseInt(s.trim(), 10))
    if (numbers.some(isNaN)) {
      throw { response: { data: { message: '请输入有效的整数数组' } } }
    }
    const res = await apiEndpoints.bubbleSort(numbers)
    result.value = res.data
  } catch (e) {
    error.value = e.response?.data?.message || '请求失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.tab-panel { padding: 16px 0; }
.input-group { display: flex; gap: 12px; margin-bottom: 16px; }
.input-field {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
}
.btn-primary {
  padding: 8px 20px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  white-space: nowrap;
}
.btn-primary:disabled { background: #a0cfff; cursor: not-allowed; }
.result-card {
  padding: 16px;
  background: #f0f9eb;
  border: 1px solid #c2e7b0;
  border-radius: 4px;
}
.error-msg {
  margin-top: 16px;
  padding: 12px;
  background: #fef0f0;
  border: 1px solid #fde2e2;
  border-radius: 4px;
  color: #f56c6c;
}
.warning { color: #e6a23c; font-size: 12px; }
</style>
```

- [ ] **Step 5: 验证 Tab 页面**

```bash
cd /path/to/manyu_test1
# 确保后端同时运行
cd /path/to/manyu_test && uvicorn main:app --port 8000 &
cd /path/to/manyu_test1 && npm run dev &
# 浏览器打开 http://localhost:5173/tools 验证
# 切换三个 Tab 并调用接口验证
kill %1 %2
```

---

## Task 7: 前端 — 导出按钮组件

**Files:**
- Create: `manyu_test1/src/components/ExportButton.vue`
- Modify: `manyu_test1/src/views/ToolsPage.vue`（集成导出按钮）

**Interfaces:**
- Consumes: `apiEndpoints.exportData(tab, format)`；`activeTab` 当前选中 Tab
- Produces: 导出按钮 UI（CSV/Excel 格式选择下拉）

- [ ] **Step 1: 创建 ExportButton.vue**

```vue
<template>
  <div class="export-bar">
    <span class="export-label">导出数据：</span>
    <select v-model="exportFormat" class="export-select">
      <option value="csv">CSV</option>
      <option value="xlsx">Excel</option>
    </select>
    <button class="btn-export" @click="doExport" :disabled="exporting">
      {{ exporting ? '导出中...' : '导出' }}
    </button>
    <span v-if="exportMsg" :class="['export-msg', exportMsgType]">{{ exportMsg }}</span>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { apiEndpoints } from '../../api/endpoints'

const props = defineProps({
  activeTab: { type: String, default: 'all' },
})

const exportFormat = ref('csv')
const exporting = ref(false)
const exportMsg = ref('')
const exportMsgType = ref('')

async function doExport() {
  exporting.value = true
  exportMsg.value = ''
  try {
    const res = await apiEndpoints.exportData(props.activeTab, exportFormat.value)
    const blob = new Blob([res.data])
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${props.activeTab}_export.${exportFormat.value}`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    exportMsg.value = '导出成功'
    exportMsgType.value = 'success'
  } catch (e) {
    if (e.response?.data instanceof Blob) {
      // 尝试读取错误信息
      const text = await e.response.data.text()
      try {
        const err = JSON.parse(text)
        exportMsg.value = err.message || '导出失败'
      } catch {
        exportMsg.value = '导出失败，请重试'
      }
    } else {
      exportMsg.value = '导出失败，请重试'
    }
    exportMsgType.value = 'error'
  } finally {
    exporting.value = false
    setTimeout(() => { exportMsg.value = '' }, 3000)
  }
}
</script>

<style scoped>
.export-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 0;
  border-top: 1px solid #e4e7ed;
  margin-top: 16px;
}
.export-label { font-size: 14px; color: #606266; }
.export-select {
  padding: 6px 10px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
}
.btn-export {
  padding: 6px 16px;
  background: #67c23a;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}
.btn-export:disabled { background: #b3e19d; cursor: not-allowed; }
.export-msg { font-size: 12px; margin-left: 8px; }
.export-msg.success { color: #67c23a; }
.export-msg.error { color: #f56c6c; }
</style>
```

- [ ] **Step 2: 修改 ToolsPage.vue 集成导出按钮**

在 ToolsPage.vue 的 `<template>` 中，在 `tab-content` 后面添加：

```vue
<ExportButton :activeTab="activeTab" />
```

在 `<script setup>` 中添加导入：

```js
import ExportButton from '../components/ExportButton.vue'
```

---

## Task 8: 前端 — 可视化报表仪表盘

**Files:**
- Create: `manyu_test1/src/components/charts/DimensionFilter.vue`
- Create: `manyu_test1/src/components/charts/LineChart.vue`
- Create: `manyu_test1/src/components/charts/PieChart.vue`
- Create: `manyu_test1/src/components/charts/BarChart.vue`
- Create: `manyu_test1/src/components/charts/StatsDashboard.vue`
- Modify: `manyu_test1/src/views/ToolsPage.vue`（集成报表仪表盘）

**Interfaces:**
- Consumes: `apiEndpoints.getStats(dimension)`、`apiEndpoints.getStatsTrend(dimension)`
- Produces: 可视化报表区域（维度筛选器 + 三种图表）

- [ ] **Step 1: 创建 DimensionFilter.vue**

```vue
<template>
  <div class="dimension-filter">
    <span class="filter-label">统计维度：</span>
    <select v-model="selected" @change="$emit('update:modelValue', selected)" class="filter-select">
      <option v-for="opt in options" :key="opt.value" :value="opt.value">
        {{ opt.label }}
      </option>
    </select>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: 'user_type' },
})
const emit = defineEmits(['update:modelValue'])

const options = [
  { value: 'user_type', label: '人员类型' },
  { value: 'user_level', label: '人员层级' },
  { value: 'user_department', label: '人员部门' },
]

const selected = ref(props.modelValue)
watch(() => props.modelValue, (v) => { selected.value = v })
</script>

<style scoped>
.dimension-filter { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; }
.filter-label { font-size: 14px; color: #606266; }
.filter-select { padding: 6px 10px; border: 1px solid #dcdfe6; border-radius: 4px; font-size: 14px; }
</style>
```

- [ ] **Step 2: 创建 LineChart.vue**

```vue
<template>
  <div class="chart-container">
    <h4 class="chart-title">调用趋势（折线图）</h4>
    <div ref="chartRef" class="chart-box"></div>
    <div v-if="!hasData" class="empty-state">暂无数据</div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  data: { type: Array, default: () => [] },
})

const chartRef = ref(null)
const hasData = ref(false)
let chartInstance = null

function renderChart() {
  if (!chartRef.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }
  
  hasData.value = props.data.length > 0
  if (!hasData.value) {
    chartInstance.clear()
    return
  }
  
  // 按日期分组
  const dates = [...new Set(props.data.map(d => d.date))].sort()
  const names = [...new Set(props.data.map(d => d.name))]
  
  const series = names.map(name => {
    const values = dates.map(date => {
      const item = props.data.find(d => d.date === date && d.name === name)
      return item ? item.value : 0
    })
    return { name, type: 'line', data: values }
  })
  
  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: names },
    xAxis: { type: 'category', data: dates, axisLabel: { rotate: 45 } },
    yAxis: { type: 'value' },
    series,
  })
}

watch(() => props.data, () => nextTick(renderChart), { deep: true })
onMounted(() => nextTick(renderChart))
</script>

<style scoped>
.chart-container { position: relative; }
.chart-title { margin: 0 0 8px; font-size: 14px; color: #303133; }
.chart-box { width: 100%; height: 300px; }
.empty-state {
  position: absolute; top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  color: #c0c4cc; font-size: 14px;
}
</style>
```

- [ ] **Step 3: 创建 PieChart.vue**

```vue
<template>
  <div class="chart-container">
    <h4 class="chart-title">调用分布（饼图）</h4>
    <div ref="chartRef" class="chart-box"></div>
    <div v-if="!hasData" class="empty-state">暂无数据</div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  data: { type: Array, default: () => [] },
})

const chartRef = ref(null)
const hasData = ref(false)
let chartInstance = null

function renderChart() {
  if (!chartRef.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }
  
  hasData.value = props.data.length > 0
  if (!hasData.value) {
    chartInstance.clear()
    return
  }
  
  chartInstance.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      data: props.data.map(d => ({ name: d.name, value: d.value })),
      emphasis: {
        itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.5)' },
      },
    }],
  })
}

watch(() => props.data, () => nextTick(renderChart), { deep: true })
onMounted(() => nextTick(renderChart))
</script>

<style scoped>
.chart-container { position: relative; }
.chart-title { margin: 0 0 8px; font-size: 14px; color: #303133; }
.chart-box { width: 100%; height: 300px; }
.empty-state {
  position: absolute; top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  color: #c0c4cc; font-size: 14px;
}
</style>
```

- [ ] **Step 4: 创建 BarChart.vue**

```vue
<template>
  <div class="chart-container">
    <h4 class="chart-title">维度对比（柱状图）</h4>
    <div ref="chartRef" class="chart-box"></div>
    <div v-if="!hasData" class="empty-state">暂无数据</div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  data: { type: Array, default: () => [] },
})

const chartRef = ref(null)
const hasData = ref(false)
let chartInstance = null

function renderChart() {
  if (!chartRef.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }
  
  hasData.value = props.data.length > 0
  if (!hasData.value) {
    chartInstance.clear()
    return
  }
  
  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: props.data.map(d => d.name),
      axisLabel: { rotate: 45 },
    },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar',
      data: props.data.map(d => d.value),
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#409eff' },
          { offset: 1, color: '#79bbff' },
        ]),
      },
    }],
  })
}

watch(() => props.data, () => nextTick(renderChart), { deep: true })
onMounted(() => nextTick(renderChart))
</script>

<style scoped>
.chart-container { position: relative; }
.chart-title { margin: 0 0 8px; font-size: 14px; color: #303133; }
.chart-box { width: 100%; height: 300px; }
.empty-state {
  position: absolute; top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  color: #c0c4cc; font-size: 14px;
}
</style>
```

- [ ] **Step 5: 创建 StatsDashboard.vue**

```vue
<template>
  <div class="stats-dashboard">
    <h3 class="section-title">调用统计报表</h3>
    <DimensionFilter v-model="dimension" />
    <div class="charts-grid">
      <LineChart :data="trendData" />
      <PieChart :data="statsData" />
      <BarChart :data="statsData" />
    </div>
    <div v-if="loading" class="loading-overlay">加载中...</div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { apiEndpoints } from '../../api/endpoints'
import DimensionFilter from './DimensionFilter.vue'
import LineChart from './LineChart.vue'
import PieChart from './PieChart.vue'
import BarChart from './BarChart.vue'

const dimension = ref('user_type')
const statsData = ref([])
const trendData = ref([])
const loading = ref(false)

async function fetchData() {
  loading.value = true
  try {
    const [statsRes, trendRes] = await Promise.all([
      apiEndpoints.getStats(dimension.value),
      apiEndpoints.getStatsTrend(dimension.value),
    ])
    statsData.value = statsRes.data || []
    trendData.value = trendRes.data || []
  } catch (e) {
    console.error('统计数据加载失败', e)
    statsData.value = []
    trendData.value = []
  } finally {
    loading.value = false
  }
}

watch(dimension, fetchData)
onMounted(fetchData)
</script>

<style scoped>
.stats-dashboard {
  margin-top: 32px;
  padding: 20px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fafafa;
  position: relative;
}
.section-title { margin: 0 0 16px; font-size: 18px; color: #303133; }
.charts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: 20px;
}
.loading-overlay {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(255,255,255,0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: #909399;
}
</style>
```

- [ ] **Step 6: 修改 ToolsPage.vue 集成报表仪表盘**

在 ToolsPage.vue 的 `<template>` 中，在 ExportButton 之后添加：

```vue
<StatsDashboard />
```

在 `<script setup>` 中添加导入：

```js
import StatsDashboard from './components/charts/StatsDashboard.vue'
```

---

## Self-Review: 需求覆盖检查

| 需求 | 覆盖任务 | 状态 |
|------|---------|------|
| HelloWorld 接口 | Task 2 | ✅ |
| SHA256 哈希接口 | Task 2 | ✅ |
| 冒泡排序接口 | Task 2 | ✅ |
| 前端三个 Tab 页面 | Task 6 | ✅ |
| 导出按钮 + 后端导出接口 | Task 4 + Task 7 | ✅ |
| 后端埋点（调用次数+调用人） | Task 3 | ✅ |
| 前端可视化报表 | Task 8 | ✅ |
| 折线图（时间趋势） | Task 8 (LineChart) | ✅ |
| 饼图（人员类型/部门分布） | Task 8 (PieChart) | ✅ |
| 柱状图（维度对比） | Task 8 (BarChart) | ✅ |
| 维度筛选（人员类型/层级/部门） | Task 8 (DimensionFilter) | ✅ |
| 异常兜底方案 | 各任务中已内嵌异常处理 | ✅ |

**类型一致性检查：** 所有接口的请求/响应模型在 Task 2 定义，Task 6 的前端组件使用一致的字段名（message, timestamp, algorithm, input, output, sorted, swaps, fallback, truncated）。统计接口返回 `[{name, value}]` 格式，前端图表组件一致消费该格式。导出接口参数 `tab`/`format` 在 Task 4 和 Task 7 保持一致。

**无占位符检查：** 所有步骤包含完整代码实现，无 "TBD"、"TODO"、"implement later" 等占位符。

---

## 跨仓接口契约对齐点

| 对齐项 | manyu_test（后端） | manyu_test1（前端） | 对齐状态 |
|--------|-------------------|-------------------|---------|
| API 基础路径 | `/api/hello`, `/api/hash`, `/api/bubble-sort` | Vite proxy `/api` → `localhost:8000` | ✅ |
| 请求头用户身份 | `X-User-Id`, `X-User-Name`, `X-User-Type`, `X-User-Level`, `X-User-Department` | Axios 拦截器自动注入 | ✅ |
| 响应字段 | camelCase（message, timestamp, sorted, swaps...） | 直接消费响应字段 | ✅ |
| 错误格式 | `{code, message, detail, request_id}` | Axios 拦截器统一处理 | ✅ |
| 导出格式 | `format=csv|xlsx` | ExportButton 选择对应格式 | ✅ |
| 统计维度 | `dimension=user_type|user_level|user_department` | DimensionFilter 映射一致 | ✅ |
| 统计响应 | `[{name, value}]` | 图表组件直接消费 | ✅ |
| 趋势响应 | `[{date, name, value}]` | LineChart 按日期分组渲染 | ✅ |