# 三接口（helloworld/哈希/冒泡排序）+ 前端三 Tab + 导出 + 埋点报表 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `[manyu_test]` 后端仓交付 helloworld / 哈希算法 / 冒泡排序三个 HTTP 接口及统一埋点、导出、报表聚合能力；在 `[manyu_test1]` 前端仓交付单页三 Tab 结果展示、逐 Tab 导出按钮与调用情况可视化报表（人员类型/人员层级/人员部门维度 × 折线/饼图/柱状图）。

**Architecture:** 后端采用 FastAPI + SQLite 单文件存储：三个业务接口经统一包装器 `execute_tracked` 落埋点表 `call_log`（时间/接口/调用人五要素/状态/耗时/请求响应快照）；调用人由 `X-User-Id` 请求头经 mock 用户表（`data/users.json`）解析出姓名/类型/层级/部门；导出接口按 tab 直接 dump `call_log` 快照为 CSV/JSON 附件；报表接口按 `user_type|user_level|department|day` 聚合返回统一 `{labels, series}` 结构。前端采用 React + Vite + AntD + ECharts：单页三 Tab 各自触发展示结果并带导出按钮，页面下方报表面板按维度与图表类型渲染。前端开发态经 Vite 代理 `/api → 127.0.0.1:8000` 调后端。

**Tech Stack:** Python 3.9+（实测 3.12.3）/ FastAPI（实测 0.141.1）/ uvicorn / pytest / httpx；Node.js 18+ / React 18 / Vite 5 / Ant Design 5 / ECharts 5（echarts-for-react）/ Vitest。

**仓库工作区（绝对路径）：**
- `[manyu_test]` 后端：`/root/.agentix/agentic-dev/runs/DEV-eecb99b0-9b79-11f1-a438-852be3c2a082-501fbda9-7573-4879-8fdd-ef19f9680994/worktree/manyu_test-cred-test-20260716022903`（分支 `AI/task-DEV-eecb99b0-9b79-11f1-a438-852be3c2a082-501fbda9-7573-4879-8fdd-ef19f9680994`，基线 `cred-test-20260716022903`）
- `[manyu_test1]` 前端：`/root/.agentix/agentic-dev/runs/DEV-eecb99b0-9b79-11f1-a438-852be3c2a082-501fbda9-7573-4879-8fdd-ef19f9680994/worktree/manyu_test1-main`（分支 `AI/task-DEV-eecb99b0-9b79-11f1-a438-852be3c2a082-501fbda9-7573-4879-8fdd-ef19f9680994`，基线 `main`）

上游依据：`[manyu_test] .agents/20260820-分别写三个接口helloworld_哈希/dima.md`（需求澄清与契约草案）。本计划对 dima 第 5 节待决项的裁定（自主决策，全流水线模式不再询问）：
- Q1 技术栈：后端 Python + FastAPI + SQLite；前端 React + Vite + AntD + ECharts（复用已有 Python 资产，成本最低）。
- Q2 哈希范围：md5 / sha1 / sha256 / sha512，默认 sha256（向后兼容的超集）。
- Q3 导出格式：CSV（默认，带 BOM 便于 Excel）与 JSON 两种，由 `format` 参数选择。
- Q4 调用人来源：无现成用户体系，采用 `X-User-Id` 请求头自报 + 仓内 mock 用户表补全“人员类型/层级/部门”三维度；未知/缺失按匿名兜底（演示级可信度，已在风险中声明）。

## Global Constraints

- **Git 只读（流水线硬性约束）**：禁止所有 Git 写操作（commit/push/merge/reset/rebase/checkout 等），仅允许 `git status/log/diff/show` 等只读命令；两仓均不做任何提交动作，版本化由流水线平台统一接管。各任务收尾步骤仅执行 `git status --short` 只读核对变更范围。
- **API 契约冻结表**（前后端唯一对齐基准，后续任务不得单方面变更路径与出入参形状）：

| 接口 | 方法/路径 | 入参 | 出参 |
|------|-----------|------|------|
| 健康检查 | `GET /api/health` | 无 | `{code:0,data:{status:"ok"}}` |
| HelloWorld | `GET /api/hello` | query `name`（可选，默认 `World`），header `X-User-Id`（可选） | `{code:0,data:{greeting}}` |
| 哈希算法 | `POST /api/hash` | body `{text:string, algorithm:"md5|sha1|sha256|sha512"}` | `{code:0,data:{algorithm,input,digest}}` |
| 冒泡排序 | `POST /api/bubble-sort` | body `{numbers:number[], order:"asc|desc"}` | `{code:0,data:{input,sorted,order,duration_ms}}` |
| 导出 | `GET /api/export` | query `tab=hello|hash|bubble`、`format=csv|json`（默认 csv） | 文件流，`Content-Disposition: attachment; filename="{tab}_{yyyyMMddHHmmss}.{ext}"` |
| 报表聚合 | `GET /api/metrics` | query `dimension=user_type|user_level|department|day`（默认 user_type）、`range=Nd`（默认 7d，上限 365d） | `{code:0,data:{dimension,range,labels,series:[{name:"调用次数",values}],total}}` |

- 统一响应包络：成功 `{code:0,data}`；业务失败 HTTP 状态码=包络 `code`，`{code,message}`；参数校验失败由框架返回 422（不计入埋点）。
- 埋点范围：仅 `/api/hello`、`/api/hash`、`/api/bubble-sort` 三个业务接口；成功与业务失败（400）都计数。
- 调用链标识：header 名固定 `X-User-Id`；mock 用户表 user_id 枚举 `u001..u004`；缺失记为 `anonymous`，未知记为 `未知用户{id}`，维度属性回落 `访客/无层级/未分组`。
- 导出文件命名：`{tab}_{yyyyMMddHHmmss}.{ext}`；CSV 列 = `call_log` 列顺序（ts, api, user_id, user_name, user_type, user_level, department, status, duration_ms, request_json, response_json）。
- 部署约定：后端 `127.0.0.1:8000`；前端开发端口 `5173`，Vite 代理 `/api → http://127.0.0.1:8000`；后端 CORS 放行 `http://localhost:5173` 与 `http://127.0.0.1:5173`。
- 依赖纪律：后端仅 fastapi/uvicorn/pytest/httpx；前端仅 antd/echarts/echarts-for-react/react/react-dom + vite/vitest 工具链。禁止引入数据库服务、Excel 库、额外状态管理库。
- 冒泡排序必须复用仓内既有 `[manyu_test] bubble_sort.py`（优化版升序 / 降序版），禁止重写算法。
- 与 dima 契约草案的差异声明：`/api/metrics` 取消 `chart` 入参（图表类型是纯前端渲染选择，后端对任意图表返回同一 `{labels,series}` 结构）；新增 `dimension=day` 取值以支撑折线图时间趋势。二者为兼容化裁定。
- 降级约定：同模块构建/测试连续 2 次失败、或单次 >120s 失败、或属环境级问题（如 npm 源不可达），按“降级协议”停止构建转静态审查，不得无限重试。

---

## File Structure

### [manyu_test]（后端）

| 文件 | 动作 | 职责 |
|------|------|------|
| `requirements.txt` | 新建 | 锁定 fastapi/uvicorn/pytest/httpx |
| `pytest.ini` | 新建 | 测试发现配置（testpaths=tests） |
| `app/__init__.py` | 新建 | 包标识（空文件） |
| `app/config.py` | 新建 | 路径常量、`db_path()`（env 可覆盖，测试隔离）、埋点 tab 映射 |
| `app/errors.py` | 新建 | `BusinessError(status, message)` |
| `app/db.py` | 新建 | SQLite 连接、`call_log` schema、列常量 |
| `app/users.py` | 新建 | mock 用户表加载与 `resolve_user` |
| `app/tracking.py` | 新建 | `record_call` + `execute_tracked`（埋点统一包装器） |
| `app/schemas.py` | 新建 | pydantic 请求体（HashRequest/BubbleSortRequest） |
| `app/services/__init__.py` | 新建 | 包标识（空文件） |
| `app/services/hello.py` | 新建 | `build_greeting(name)` |
| `app/services/hash_service.py` | 新建 | `compute(text, algorithm)` |
| `app/services/bubble.py` | 新建 | `sort_numbers(numbers, order)`，按路径加载仓根 `bubble_sort.py` |
| `app/routers/__init__.py` | 新建 | 包标识（空文件） |
| `app/routers/business.py` | 新建 | 三个业务端点（hello/hash/bubble-sort） |
| `app/routers/export.py` | 新建 | `GET /api/export` |
| `app/routers/metrics.py` | 新建 | `GET /api/metrics` |
| `app/main.py` | 新建后多次修改 | `create_app()`：CORS、异常处理、health、挂载路由 |
| `data/users.json` | 新建 | mock 用户表（u001..u004 × 三维度属性） |
| `tests/conftest.py` | 新建 | `client`/`read_logs` fixture（tmp 库隔离） |
| `tests/test_api.py` | 新建 | health 测试 |
| `tests/test_users.py` | 新建 | 用户解析测试 |
| `tests/test_hello.py` | 新建 | hello + 埋点断言 |
| `tests/test_hash.py` | 新建 | 哈希 + 失败埋点断言 |
| `tests/test_bubble.py` | 新建 | 排序（升/降/空/非法） |
| `tests/test_export.py` | 新建 | CSV/JSON 导出与非法 tab |
| `tests/test_metrics.py` | 新建 | 三维度 + day 聚合 |
| `README.md` | 新建 | 运行/测试/接口说明 |
| `bubble_sort.py` | 只读复用 | 既有算法实现，不修改 |

### [manyu_test1]（前端）

| 文件 | 动作 | 职责 |
|------|------|------|
| `package.json` | 新建 | 依赖与 scripts（dev/build/test） |
| `vite.config.js` | 新建 | react 插件、5173 端口、`/api` 代理、vitest node 环境 |
| `index.html` | 新建 | 入口 HTML |
| `src/main.jsx` | 新建 | React 挂载 |
| `src/App.jsx` | 新建后多次修改 | 布局 + 调用人选择 + Tabs + 报表面板 |
| `src/api/client.js` | 新建 | fetch 封装、mock 用户枚举、三接口 + metrics 调用、`exportTab` 下载 |
| `src/utils/parseNumbers.js` | 新建 | 数字列表解析（纯函数，可测） |
| `src/charts/buildChartOption.js` | 新建 | ECharts option 构建（line/pie/bar，纯函数，可测） |
| `src/components/HelloTab.jsx` | 新建后修改 | hello 调用/展示/导出按钮 |
| `src/components/HashTab.jsx` | 新建后修改 | 哈希调用/展示/导出按钮 |
| `src/components/BubbleTab.jsx` | 新建后修改 | 排序调用/展示/导出按钮 |
| `src/components/MetricsPanel.jsx` | 新建 | 维度/图表切换 + ECharts 渲染 + 总数展示 |
| `src/utils/parseNumbers.test.js` | 新建 | vitest 单测 |
| `src/charts/buildChartOption.test.js` | 新建 | vitest 单测 |
| `README.md` | 新建 | 运行/代理/构建说明 |

---

## Task 1: [manyu_test] 后端工程骨架与健康检查

**Files:**
- Create: `[manyu_test] requirements.txt`
- Create: `[manyu_test] pytest.ini`
- Create: `[manyu_test] app/__init__.py`
- Create: `[manyu_test] app/config.py`
- Create: `[manyu_test] app/errors.py`
- Create: `[manyu_test] app/db.py`
- Create: `[manyu_test] app/main.py`
- Create: `[manyu_test] tests/conftest.py`
- Test: `[manyu_test] tests/test_api.py`

**Interfaces:**
- Consumes: 无（首个任务）。当前开发机已预装 fastapi 0.141.1 / pytest 9.1.1（已验证），requirements.txt 用于环境固化。
- Produces: `create_app() -> FastAPI`（含 CORS、BusinessError 处理器、`GET /api/health`、`init_db()`）；`db.get_connection()`、`db.COLUMNS`、`config.db_path()`；`client`/`read_logs` 测试 fixture —— 后续所有任务的测试与端点都依赖它们。

- [ ] **Step 1: 创建依赖与测试配置**

`[manyu_test] requirements.txt`：
```
fastapi>=0.110,<1.0
uvicorn>=0.29
pytest>=8.0
httpx>=0.27
```

`[manyu_test] pytest.ini`：
```ini
[pytest]
testpaths = tests
addopts = -q
```

`[manyu_test] app/__init__.py`：空文件。

- [ ] **Step 2: 安装依赖**

Run（在 `[manyu_test]` 仓根目录）: `python3 -m pip install -r requirements.txt`
Expected: 全部 Already satisfied 或 Successfully installed；退出码 0。

- [ ] **Step 3: 写失败测试**

`[manyu_test] tests/conftest.py`：
```python
import pytest
from fastapi.testclient import TestClient


@pytest.fixture()
def client(tmp_path, monkeypatch):
    """每个用例使用独立 SQLite 文件，避免埋点数据互相污染。"""
    monkeypatch.setenv("APP_DB_PATH", str(tmp_path / "test.db"))
    from app.main import create_app

    return TestClient(create_app())


@pytest.fixture()
def read_logs():
    """读取当前隔离库中的全部埋点记录（按写入顺序）。"""

    def _read():
        from app.db import get_connection

        conn = get_connection()
        try:
            return [
                dict(row)
                for row in conn.execute(
                    "SELECT * FROM call_log ORDER BY id ASC"
                ).fetchall()
            ]
        finally:
            conn.close()

    return _read
```

`[manyu_test] tests/test_api.py`：
```python
def test_health(client):
    resp = client.get("/api/health")
    assert resp.status_code == 200
    assert resp.json() == {"code": 0, "data": {"status": "ok"}}
```

- [ ] **Step 4: 运行测试确认失败**

Run: `cd [manyu_test]仓根 && python3 -m pytest tests/test_api.py -v`
Expected: FAIL，`ModuleNotFoundError: No module named 'app'`。

- [ ] **Step 5: 实现骨架代码**

`[manyu_test] app/config.py`：
```python
"""全局配置：路径与埋点范围。APP_DB_PATH 环境变量可覆盖数据库位置（测试隔离用）。"""
import os
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent
DATA_DIR = BASE_DIR / "data"
USERS_JSON_PATH = DATA_DIR / "users.json"

# 需要埋点的三个业务接口（路径前缀 -> tab 名 / 埋点 api 名）
TRACKED_TABS = {
    "/api/hello": "hello",
    "/api/hash": "hash",
    "/api/bubble-sort": "bubble",
}


def db_path() -> Path:
    return Path(os.environ.get("APP_DB_PATH", str(DATA_DIR / "app.db")))
```

`[manyu_test] app/errors.py`：
```python
class BusinessError(Exception):
    """业务错误：status 即 HTTP 状态码，响应包络为 {code: status, message}。"""

    def __init__(self, status: int, message: str):
        super().__init__(message)
        self.status = status
        self.message = message
```

`[manyu_test] app/db.py`：
```python
"""SQLite 连接与 schema。单文件存储，无外部数据库依赖。"""
import sqlite3

from .config import db_path

SCHEMA = """
CREATE TABLE IF NOT EXISTS call_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ts TEXT NOT NULL,
    api TEXT NOT NULL,
    user_id TEXT NOT NULL,
    user_name TEXT NOT NULL,
    user_type TEXT NOT NULL,
    user_level TEXT NOT NULL,
    department TEXT NOT NULL,
    status INTEGER NOT NULL,
    duration_ms REAL NOT NULL,
    request_json TEXT NOT NULL DEFAULT '{}',
    response_json TEXT NOT NULL DEFAULT '{}'
);
CREATE INDEX IF NOT EXISTS idx_call_log_api ON call_log(api);
CREATE INDEX IF NOT EXISTS idx_call_log_ts ON call_log(ts);
"""

# 导出 CSV 列顺序与表结构一致
COLUMNS = (
    "ts", "api", "user_id", "user_name", "user_type",
    "user_level", "department", "status", "duration_ms",
    "request_json", "response_json",
)


def get_connection() -> sqlite3.Connection:
    path = db_path()
    path.parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(str(path))
    conn.row_factory = sqlite3.Row
    return conn


def init_db() -> None:
    conn = get_connection()
    try:
        conn.executescript(SCHEMA)
        conn.commit()
    finally:
        conn.close()
```

`[manyu_test] app/main.py`：
```python
"""应用入口：在仓根目录运行 `python3 -m uvicorn app.main:app --port 8000`。"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from .db import init_db
from .errors import BusinessError


def create_app() -> FastAPI:
    app = FastAPI(title="manyu_test demo API")
    app.add_middleware(
        CORSMiddleware,
        allow_origins=[
            "http://localhost:5173",
            "http://127.0.0.1:5173",
        ],
        allow_methods=["*"],
        allow_headers=["*"],
    )

    @app.exception_handler(BusinessError)
    async def on_business_error(request, exc: BusinessError):
        return JSONResponse(
            status_code=exc.status,
            content={"code": exc.status, "message": exc.message},
        )

    @app.get("/api/health")
    def health():
        return {"code": 0, "data": {"status": "ok"}}

    init_db()
    return app


app = create_app()
```

- [ ] **Step 6: 运行测试确认通过**

Run: `python3 -m pytest tests/test_api.py -v`
Expected: PASS，`1 passed`。

- [ ] **Step 7: 变更只读核对（流水线禁止 Git 写操作）**

```bash
cd [manyu_test]仓根
# 流水线禁止 git add/commit 等写操作，版本化由平台统一接管；此处仅只读核对变更范围
git status --short
git diff --stat
```

---

## Task 2: [manyu_test] Mock 用户表与调用人解析

**Files:**
- Create: `[manyu_test] data/users.json`
- Create: `[manyu_test] app/users.py`
- Test: `[manyu_test] tests/test_users.py`

**Interfaces:**
- Consumes: `config.USERS_JSON_PATH`（Task 1）。
- Produces: `resolve_user(user_id) -> dict`，键固定为 `user_id, user_name, user_type, user_level, department`；`load_users() -> dict[str, dict]`。Task 3 的埋点写入直接消费该返回结构。

- [ ] **Step 1: 写失败测试**

`[manyu_test] tests/test_users.py`：
```python
from app.users import load_users, resolve_user


def test_resolve_known_user():
    user = resolve_user("u001")
    assert user == {
        "user_id": "u001",
        "user_name": "张三",
        "user_type": "正式员工",
        "user_level": "P5",
        "department": "研发部",
    }


def test_resolve_missing_user_id_is_anonymous():
    user = resolve_user(None)
    assert user["user_id"] == "anonymous"
    assert user["user_type"] == "访客"
    assert user["user_level"] == "无层级"
    assert user["department"] == "未分组"


def test_resolve_unknown_user_falls_back():
    user = resolve_user("ghost99")
    assert user["user_name"] == "未知用户ghost99"
    assert user["user_type"] == "访客"


def test_load_users_has_four_mock_entries():
    users = load_users()
    assert set(users.keys()) == {"u001", "u002", "u003", "u004"}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `python3 -m pytest tests/test_users.py -v`
Expected: FAIL，`ModuleNotFoundError: No module named 'app.users'`。

- [ ] **Step 3: 创建 mock 用户数据**

`[manyu_test] data/users.json`：
```json
[
  {"user_id": "u001", "user_name": "张三", "user_type": "正式员工", "user_level": "P5", "department": "研发部"},
  {"user_id": "u002", "user_name": "李四", "user_type": "正式员工", "user_level": "P7", "department": "产品部"},
  {"user_id": "u003", "user_name": "王五", "user_type": "外包", "user_level": "P4", "department": "研发部"},
  {"user_id": "u004", "user_name": "赵六", "user_type": "实习生", "user_level": "P3", "department": "数据部"}
]
```

- [ ] **Step 4: 实现解析逻辑**

`[manyu_test] app/users.py`：
```python
"""Mock 用户表：按 X-User-Id 解析调用人属性（姓名/人员类型/人员层级/部门）。"""
import json

from .config import USERS_JSON_PATH

DEFAULT_ATTRS = {
    "user_name": "匿名用户",
    "user_type": "访客",
    "user_level": "无层级",
    "department": "未分组",
}


def load_users() -> dict:
    try:
        with open(USERS_JSON_PATH, "r", encoding="utf-8") as fh:
            items = json.load(fh)
        return {str(item["user_id"]): item for item in items}
    except (OSError, ValueError, KeyError):
        return {}


def resolve_user(user_id):
    """返回含 user_id, user_name, user_type, user_level, department 的 dict。"""
    if not user_id:
        return {"user_id": "anonymous", **DEFAULT_ATTRS}
    user = load_users().get(str(user_id))
    if user is None:
        return {
            "user_id": str(user_id),
            "user_name": f"未知用户{user_id}",
            "user_type": DEFAULT_ATTRS["user_type"],
            "user_level": DEFAULT_ATTRS["user_level"],
            "department": DEFAULT_ATTRS["department"],
        }
    return {
        "user_id": str(user_id),
        "user_name": user.get("user_name", f"用户{user_id}"),
        "user_type": user.get("user_type", DEFAULT_ATTRS["user_type"]),
        "user_level": user.get("user_level", DEFAULT_ATTRS["user_level"]),
        "department": user.get("department", DEFAULT_ATTRS["department"]),
    }
```

- [ ] **Step 5: 运行测试确认通过**

Run: `python3 -m pytest tests/test_users.py -v`
Expected: PASS，`4 passed`。

- [ ] **Step 6: 变更只读核对（流水线禁止 Git 写操作）**

```bash
cd [manyu_test]仓根
# 流水线禁止 git add/commit 等写操作，版本化由平台统一接管；此处仅只读核对变更范围
git status --short
git diff --stat
```

---

## Task 3: [manyu_test] 埋点核心与 HelloWorld 接口

**Files:**
- Create: `[manyu_test] app/tracking.py`
- Create: `[manyu_test] app/services/__init__.py`
- Create: `[manyu_test] app/services/hello.py`
- Create: `[manyu_test] app/routers/__init__.py`
- Create: `[manyu_test] app/routers/business.py`
- Modify: `[manyu_test] app/main.py`（挂载 business 路由）
- Test: `[manyu_test] tests/test_hello.py`

**Interfaces:**
- Consumes: `db.get_connection()`、`users.resolve_user(user_id)`、`errors.BusinessError`（Task 1/2 产出，签名见各自 Produces）。
- Produces:
  - `tracking.execute_tracked(api: str, user_id: str | None, request_payload: dict, handler: callable) -> JSONResponse`：handler 返回 dict 表示成功数据；抛 `BusinessError` 表示业务失败。成功响应 `{code:0,data}`，失败 `{code:status,message}`，两者都写埋点。**Task 4/5 的端点直接复用该函数。**
  - `tracking.record_call(api, user_id, status, duration_ms, request_payload, response_data) -> None`。
  - `services.hello.build_greeting(name) -> str`。
  - `GET /api/hello` 端点。

- [ ] **Step 1: 写失败测试**

`[manyu_test] tests/test_hello.py`：
```python
def test_hello_default(client):
    resp = client.get("/api/hello")
    assert resp.status_code == 200
    assert resp.json() == {"code": 0, "data": {"greeting": "Hello, World!"}}


def test_hello_with_name_tracked(client, read_logs):
    resp = client.get(
        "/api/hello", params={"name": "张三"}, headers={"X-User-Id": "u001"}
    )
    assert resp.json()["data"]["greeting"] == "Hello, 张三!"
    logs = read_logs()
    assert len(logs) == 1
    log = logs[0]
    assert log["api"] == "hello"
    assert log["user_id"] == "u001"
    assert log["user_name"] == "张三"
    assert log["user_type"] == "正式员工"
    assert log["user_level"] == "P5"
    assert log["department"] == "研发部"
    assert log["status"] == 200
    assert log["duration_ms"] >= 0
    assert '"name": "张三"' in log["request_json"]
    assert '"greeting"' in log["response_json"]


def test_hello_unknown_user_fallback(client, read_logs):
    client.get("/api/hello", headers={"X-User-Id": "ghost99"})
    log = read_logs()[0]
    assert log["user_name"] == "未知用户ghost99"
    assert log["user_type"] == "访客"


def test_hello_anonymous(client, read_logs):
    client.get("/api/hello")
    assert read_logs()[0]["user_id"] == "anonymous"
```

- [ ] **Step 2: 运行测试确认失败**

Run: `python3 -m pytest tests/test_hello.py -v`
Expected: FAIL，`404 Not Found`（路由不存在）。

- [ ] **Step 3: 实现埋点核心与 hello 服务**

`[manyu_test] app/tracking.py`：
```python
"""统一埋点：execute_tracked 包装业务处理函数，自动写 call_log。"""
import json
import time
from datetime import datetime, timezone

from .db import get_connection
from .errors import BusinessError
from .users import resolve_user


def now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def record_call(api, user_id, status, duration_ms, request_payload, response_data) -> None:
    user = resolve_user(user_id)
    conn = get_connection()
    try:
        conn.execute(
            """INSERT INTO call_log
               (ts, api, user_id, user_name, user_type, user_level, department,
                status, duration_ms, request_json, response_json)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            (
                now_iso(), api, user["user_id"], user["user_name"],
                user["user_type"], user["user_level"], user["department"],
                status, round(duration_ms, 3),
                json.dumps(request_payload, ensure_ascii=False),
                json.dumps(response_data, ensure_ascii=False),
            ),
        )
        conn.commit()
    finally:
        conn.close()


def execute_tracked(api, user_id, request_payload, handler):
    """执行 handler() 并统一记录埋点。

    handler 成功返回 dict（业务数据）；抛 BusinessError 表示业务失败。
    返回 JSONResponse：成功 {code:0,data:...}；失败 {code:status,message:...}。
    """
    from fastapi.responses import JSONResponse

    start = time.perf_counter()
    try:
        data = handler()
        status, body = 200, {"code": 0, "data": data}
    except BusinessError as exc:
        status, body = exc.status, {"code": exc.status, "message": exc.message}
    duration_ms = (time.perf_counter() - start) * 1000.0
    record_call(api, user_id, status, duration_ms, request_payload, body.get("data", {}))
    return JSONResponse(status_code=status, content=body)
```

`[manyu_test] app/services/__init__.py`：空文件。

`[manyu_test] app/services/hello.py`：
```python
def build_greeting(name) -> str:
    name = (name or "").strip() or "World"
    return f"Hello, {name}!"
```

- [ ] **Step 4: 实现 hello 端点并挂载路由**

`[manyu_test] app/routers/__init__.py`：空文件。

`[manyu_test] app/routers/business.py`：
```python
"""三个业务接口：hello / hash / bubble-sort，均经 execute_tracked 统一埋点。"""
from fastapi import APIRouter, Query, Request

from ..services.hello import build_greeting
from ..tracking import execute_tracked

router = APIRouter()


@router.get("/api/hello")
def hello(request: Request, name: str = Query("World")):
    return execute_tracked(
        "hello",
        request.headers.get("X-User-Id"),
        {"name": name},
        lambda: {"greeting": build_greeting(name)},
    )
```

修改 `[manyu_test] app/main.py`：在 `from .errors import BusinessError` 后新增一行导入：
```python
from .routers import business
```
并在 `create_app()` 内 `init_db()` 之后、`return app` 之前新增：
```python
    app.include_router(business.router)
```

- [ ] **Step 5: 运行测试确认通过**

Run: `python3 -m pytest tests/test_hello.py tests/test_api.py -v`
Expected: PASS，`5 passed`。

- [ ] **Step 6: 变更只读核对（流水线禁止 Git 写操作）**

```bash
cd [manyu_test]仓根
# 流水线禁止 git add/commit 等写操作，版本化由平台统一接管；此处仅只读核对变更范围
git status --short
git diff --stat
```

---

## Task 4: [manyu_test] 哈希算法接口

**Files:**
- Create: `[manyu_test] app/services/hash_service.py`
- Modify: `[manyu_test] app/routers/business.py`（新增 hash 端点）
- Create: `[manyu_test] app/schemas.py`
- Test: `[manyu_test] tests/test_hash.py`

**Interfaces:**
- Consumes: `tracking.execute_tracked(api, user_id, request_payload, handler)`（Task 3 产出）。
- Produces: `hash_service.compute(text: str, algorithm: str) -> dict`（键 `algorithm, input, digest`；非法算法抛 `BusinessError(400, ...)`）；`schemas.HashRequest`（pydantic：`text: str = ""`，`algorithm: str = "sha256"`）；`POST /api/hash` 端点。

- [ ] **Step 1: 写失败测试**

`[manyu_test] tests/test_hash.py`：
```python
import hashlib


def test_hash_default_sha256(client):
    resp = client.post("/api/hash", json={"text": "hello"}, headers={"X-User-Id": "u002"})
    body = resp.json()
    assert body["code"] == 0
    assert body["data"]["algorithm"] == "sha256"
    assert body["data"]["input"] == "hello"
    assert body["data"]["digest"] == hashlib.sha256(b"hello").hexdigest()


def test_hash_md5_case_insensitive(client):
    resp = client.post("/api/hash", json={"text": "hello", "algorithm": "MD5"})
    assert resp.json()["data"]["digest"] == hashlib.md5(b"hello").hexdigest()


def test_hash_sha1_and_sha512(client):
    for algo in ("sha1", "sha512"):
        resp = client.post("/api/hash", json={"text": "x", "algorithm": algo})
        expected = hashlib.new(algo, b"x").hexdigest()
        assert resp.json()["data"]["digest"] == expected


def test_hash_unsupported_algorithm_tracked(client, read_logs):
    resp = client.post("/api/hash", json={"text": "x", "algorithm": "crc32"})
    assert resp.status_code == 400
    body = resp.json()
    assert body["code"] == 400
    assert "不支持的哈希算法" in body["message"]
    logs = read_logs()
    assert len(logs) == 1
    assert logs[0]["status"] == 400  # 失败调用也计入埋点
```

- [ ] **Step 2: 运行测试确认失败**

Run: `python3 -m pytest tests/test_hash.py -v`
Expected: FAIL，`404 Not Found`。

- [ ] **Step 3: 实现哈希服务与请求体**

`[manyu_test] app/services/hash_service.py`：
```python
import hashlib

from ..errors import BusinessError

SUPPORTED_ALGORITHMS = ("md5", "sha1", "sha256", "sha512")


def compute(text, algorithm):
    algo = (algorithm or "sha256").strip().lower()
    if algo not in SUPPORTED_ALGORITHMS:
        raise BusinessError(
            400,
            f"不支持的哈希算法: {algorithm}，可选值: {', '.join(SUPPORTED_ALGORITHMS)}",
        )
    digest = hashlib.new(algo, (text or "").encode("utf-8")).hexdigest()
    return {"algorithm": algo, "input": text or "", "digest": digest}
```

`[manyu_test] app/schemas.py`：
```python
from typing import List, Literal, Union

from pydantic import BaseModel, Field

Number = Union[int, float]


class HashRequest(BaseModel):
    text: str = ""
    algorithm: str = "sha256"


class BubbleSortRequest(BaseModel):
    numbers: List[Number] = Field(default_factory=list)
    order: Literal["asc", "desc"] = "asc"
```

- [ ] **Step 4: 新增 hash 端点**

修改 `[manyu_test] app/routers/business.py`，在文件头部导入区新增：
```python
from ..schemas import HashRequest
from ..services import hash_service
```
并在文件末尾（hello 端点之后）追加：
```python
@router.post("/api/hash")
def hash_digest(request: Request, payload: HashRequest):
    return execute_tracked(
        "hash",
        request.headers.get("X-User-Id"),
        {"text": payload.text, "algorithm": payload.algorithm},
        lambda: hash_service.compute(payload.text, payload.algorithm),
    )
```

- [ ] **Step 5: 运行测试确认通过**

Run: `python3 -m pytest tests/test_hash.py tests/test_hello.py -v`
Expected: PASS，`8 passed`（无回归）。

- [ ] **Step 6: 变更只读核对（流水线禁止 Git 写操作）**

```bash
cd [manyu_test]仓根
# 流水线禁止 git add/commit 等写操作，版本化由平台统一接管；此处仅只读核对变更范围
git status --short
git diff --stat
```

---

## Task 5: [manyu_test] 冒泡排序接口（复用既有算法）

**Files:**
- Create: `[manyu_test] app/services/bubble.py`
- Modify: `[manyu_test] app/routers/business.py`（新增 bubble-sort 端点）
- Test: `[manyu_test] tests/test_bubble.py`

**Interfaces:**
- Consumes: `tracking.execute_tracked`（Task 3）、`schemas.BubbleSortRequest`（Task 4）、仓根既有 `[manyu_test] bubble_sort.py` 中的 `bubble_sort_optimized(arr) -> list` 与 `bubble_sort_descending(arr) -> list`（原地排序并返回列表）。
- Produces: `bubble.sort_numbers(numbers: list, order: str) -> dict`（键 `input, sorted, order, duration_ms`）；`POST /api/bubble-sort` 端点。

- [ ] **Step 1: 写失败测试**

`[manyu_test] tests/test_bubble.py`：
```python
def test_bubble_asc(client):
    resp = client.post("/api/bubble-sort", json={"numbers": [5, 3, 8, 4, 2]})
    data = resp.json()["data"]
    assert data["sorted"] == [2, 3, 4, 5, 8]
    assert data["input"] == [5, 3, 8, 4, 2]
    assert data["order"] == "asc"
    assert data["duration_ms"] >= 0


def test_bubble_desc(client):
    resp = client.post(
        "/api/bubble-sort", json={"numbers": [3, 1, 4, 1, 5], "order": "desc"}
    )
    assert resp.json()["data"]["sorted"] == [5, 4, 3, 1, 1]


def test_bubble_empty_and_single(client):
    assert client.post("/api/bubble-sort", json={"numbers": []}).json()["data"]["sorted"] == []
    assert client.post("/api/bubble-sort", json={"numbers": [42]}).json()["data"]["sorted"] == [42]


def test_bubble_negative_and_floats(client):
    resp = client.post("/api/bubble-sort", json={"numbers": [9, -3, 0, 7.5, -1]})
    assert resp.json()["data"]["sorted"] == [-3, -1, 0, 7.5, 9]


def test_bubble_tracked(client, read_logs):
    client.post("/api/bubble-sort", json={"numbers": [2, 1]}, headers={"X-User-Id": "u003"})
    log = read_logs()[0]
    assert log["api"] == "bubble"
    assert log["department"] == "研发部"
    assert '"sorted": [1, 2]' in log["response_json"]


def test_bubble_invalid_payload_rejected(client):
    resp = client.post("/api/bubble-sort", json={"numbers": ["abc"]})
    assert resp.status_code == 422  # pydantic 校验拒绝，不进入埋点
```

- [ ] **Step 2: 运行测试确认失败**

Run: `python3 -m pytest tests/test_bubble.py -v`
Expected: FAIL，`404 Not Found`。

- [ ] **Step 3: 实现排序服务（按路径加载仓根 bubble_sort.py）**

`[manyu_test] app/services/bubble.py`：
```python
"""冒泡排序服务：复用仓根 bubble_sort.py（按绝对路径加载，避免依赖运行 cwd）。"""
import importlib.util
import time
from pathlib import Path

from ..errors import BusinessError

_MODULE_PATH = Path(__file__).resolve().parents[2] / "bubble_sort.py"


def _load_module():
    spec = importlib.util.spec_from_file_location("bubble_sort_impl", _MODULE_PATH)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


_bubble = _load_module()


def sort_numbers(numbers, order="asc"):
    if order not in ("asc", "desc"):
        raise BusinessError(400, "order 仅支持 asc|desc")
    cleaned = []
    for item in numbers or []:
        if isinstance(item, bool) or not isinstance(item, (int, float)):
            raise BusinessError(400, "numbers 必须是数字数组")
        cleaned.append(item)
    start = time.perf_counter()
    if order == "desc":
        sorted_arr = _bubble.bubble_sort_descending(list(cleaned))
    else:
        sorted_arr = _bubble.bubble_sort_optimized(list(cleaned))
    duration_ms = (time.perf_counter() - start) * 1000.0
    return {
        "input": list(cleaned),
        "sorted": sorted_arr,
        "order": order,
        "duration_ms": round(duration_ms, 3),
    }
```

- [ ] **Step 4: 新增 bubble-sort 端点**

修改 `[manyu_test] app/routers/business.py`，在导入区新增：
```python
from ..schemas import BubbleSortRequest
from ..services.bubble import sort_numbers
```
并在文件末尾追加：
```python
@router.post("/api/bubble-sort")
def bubble_sort(request: Request, payload: BubbleSortRequest):
    return execute_tracked(
        "bubble",
        request.headers.get("X-User-Id"),
        {"numbers": payload.numbers, "order": payload.order},
        lambda: sort_numbers(payload.numbers, payload.order),
    )
```

- [ ] **Step 5: 运行测试确认通过**

Run: `python3 -m pytest tests/test_bubble.py -v`
Expected: PASS，`6 passed`。

- [ ] **Step 6: 变更只读核对（流水线禁止 Git 写操作）**

```bash
cd [manyu_test]仓根
# 流水线禁止 git add/commit 等写操作，版本化由平台统一接管；此处仅只读核对变更范围
git status --short
git diff --stat
```

---

## Task 6: [manyu_test] 导出接口（CSV/JSON 附件）

**Files:**
- Create: `[manyu_test] app/routers/export.py`
- Modify: `[manyu_test] app/main.py`（挂载 export 路由）
- Test: `[manyu_test] tests/test_export.py`

**Interfaces:**
- Consumes: `db.get_connection()`、`db.COLUMNS`、`errors.BusinessError`（Task 1）；埋点数据由 Task 3–5 的端点写入。
- Produces: `GET /api/export?tab=hello|hash|bubble&format=csv|json`。CSV 首行为 `COLUMNS` 表头，UTF-8 带 BOM；JSON 为 `call_log` 记录数组；`Content-Disposition: attachment; filename="{tab}_{yyyyMMddHHmmss}.{format}"`；非法 tab/format 返回 400 包络。前端 Task 10 的 `exportTab` 按此契约消费。

- [ ] **Step 1: 写失败测试**

`[manyu_test] tests/test_export.py`：
```python
def _seed(client):
    client.get("/api/hello", headers={"X-User-Id": "u001"})
    client.post("/api/hash", json={"text": "a"}, headers={"X-User-Id": "u002"})
    client.post("/api/bubble-sort", json={"numbers": [2, 1]}, headers={"X-User-Id": "u001"})


def test_export_csv_only_selected_tab(client):
    _seed(client)
    resp = client.get("/api/export", params={"tab": "hello", "format": "csv"})
    assert resp.status_code == 200
    assert resp.headers["content-type"].startswith("text/csv")
    assert "attachment" in resp.headers["content-disposition"]
    assert "hello_" in resp.headers["content-disposition"]
    text = resp.text.lstrip("\ufeff")
    lines = text.strip().splitlines()
    assert lines[0].startswith("ts,api,user_id")
    assert len(lines) == 2  # 表头 + 1 条 hello 记录（不含 hash/bubble）


def test_export_json_contains_response_snapshot(client):
    _seed(client)
    resp = client.get("/api/export", params={"tab": "bubble", "format": "json"})
    records = resp.json()
    assert len(records) == 1
    assert records[0]["api"] == "bubble"
    assert '"sorted": [1, 2]' in records[0]["response_json"]


def test_export_default_format_is_csv(client):
    _seed(client)
    resp = client.get("/api/export", params={"tab": "hash"})
    assert resp.headers["content-type"].startswith("text/csv")


def test_export_invalid_tab(client):
    resp = client.get("/api/export", params={"tab": "nope"})
    assert resp.status_code == 400
    assert resp.json()["code"] == 400


def test_export_invalid_format(client):
    resp = client.get("/api/export", params={"tab": "hello", "format": "xlsx"})
    assert resp.status_code == 400
```

- [ ] **Step 2: 运行测试确认失败**

Run: `python3 -m pytest tests/test_export.py -v`
Expected: FAIL，`404 Not Found`。

- [ ] **Step 3: 实现导出路由**

`[manyu_test] app/routers/export.py`：
```python
"""导出接口：按 tab 将 call_log 记录导出为 CSV/JSON 附件。"""
import csv
import io
import json
from datetime import datetime

from fastapi import APIRouter, Query
from fastapi.responses import Response

from ..db import COLUMNS, get_connection
from ..errors import BusinessError

router = APIRouter()

VALID_TABS = ("hello", "hash", "bubble")
VALID_FORMATS = ("csv", "json")
CSV_COLUMNS = list(COLUMNS)


@router.get("/api/export")
def export(tab: str = Query(...), format: str = Query("csv")):
    if tab not in VALID_TABS:
        raise BusinessError(400, f"tab 仅支持 {'|'.join(VALID_TABS)}")
    if format not in VALID_FORMATS:
        raise BusinessError(400, f"format 仅支持 {'|'.join(VALID_FORMATS)}")

    conn = get_connection()
    try:
        rows = conn.execute(
            f"SELECT {', '.join(CSV_COLUMNS)} FROM call_log WHERE api = ? ORDER BY id ASC",
            (tab,),
        ).fetchall()
    finally:
        conn.close()

    records = [dict(row) for row in rows]
    filename = f"{tab}_{datetime.now().strftime('%Y%m%d%H%M%S')}.{format}"

    if format == "json":
        content = json.dumps(records, ensure_ascii=False, indent=2)
        media_type = "application/json; charset=utf-8"
    else:
        buf = io.StringIO()
        writer = csv.writer(buf)
        writer.writerow(CSV_COLUMNS)
        for record in records:
            writer.writerow([record[col] for col in CSV_COLUMNS])
        content = "\ufeff" + buf.getvalue()  # BOM：Excel 直接打开不乱码
        media_type = "text/csv; charset=utf-8"

    return Response(
        content=content,
        media_type=media_type,
        headers={"Content-Disposition": f'attachment; filename="{filename}"'},
    )
```

- [ ] **Step 4: 挂载路由**

修改 `[manyu_test] app/main.py`：将 `from .routers import business` 改为：
```python
from .routers import business, export
```
并在 `app.include_router(business.router)` 之后新增：
```python
    app.include_router(export.router)
```

- [ ] **Step 5: 运行测试确认通过**

Run: `python3 -m pytest tests/test_export.py -v`
Expected: PASS，`5 passed`。

- [ ] **Step 6: 变更只读核对（流水线禁止 Git 写操作）**

```bash
cd [manyu_test]仓根
# 流水线禁止 git add/commit 等写操作，版本化由平台统一接管；此处仅只读核对变更范围
git status --short
git diff --stat
```

---

## Task 7: [manyu_test] 报表聚合接口（三人员维度 + 按天）

**Files:**
- Create: `[manyu_test] app/routers/metrics.py`
- Modify: `[manyu_test] app/main.py`（挂载 metrics 路由）
- Test: `[manyu_test] tests/test_metrics.py`

**Interfaces:**
- Consumes: `db.get_connection()`、`errors.BusinessError`；`call_log` 表中的 `user_type/user_level/department/ts` 列（Task 1 schema、Task 3 写入）。
- Produces: `GET /api/metrics?dimension=...&range=Nd`，返回 `{code:0,data:{dimension,range,labels,series:[{name:"调用次数",values}],total}}`。`dimension=day` 时 labels 为 `YYYY-MM-DD` 升序日期（供折线图）；其余维度按计数降序、标签升序（供饼图/柱状图）。前端 Task 11 的 `api.metrics` 按此契约消费。

- [ ] **Step 1: 写失败测试**

`[manyu_test] tests/test_metrics.py`：
```python
def _seed(client):
    # u001 张三（正式员工/P5/研发部）调用 2 次；u003 王五（外包/P4/研发部）调用 1 次
    client.get("/api/hello", headers={"X-User-Id": "u001"})
    client.post("/api/hash", json={"text": "a"}, headers={"X-User-Id": "u001"})
    client.post("/api/bubble-sort", json={"numbers": [1]}, headers={"X-User-Id": "u003"})


def test_metrics_by_user_type(client):
    _seed(client)
    data = client.get("/api/metrics", params={"dimension": "user_type"}).json()["data"]
    assert data["total"] == 3
    assert data["labels"] == ["正式员工", "外包"]
    assert data["series"][0]["name"] == "调用次数"
    assert data["series"][0]["values"] == [2, 1]


def test_metrics_by_user_level(client):
    _seed(client)
    data = client.get("/api/metrics", params={"dimension": "user_level"}).json()["data"]
    assert data["labels"] == ["P5", "P4"]
    assert data["series"][0]["values"] == [2, 1]


def test_metrics_by_department(client):
    _seed(client)
    data = client.get("/api/metrics", params={"dimension": "department"}).json()["data"]
    assert data["labels"] == ["研发部"]
    assert data["series"][0]["values"] == [3]


def test_metrics_by_day_for_line_chart(client):
    _seed(client)
    data = client.get(
        "/api/metrics", params={"dimension": "day", "range": "7d"}
    ).json()["data"]
    assert len(data["labels"]) == 1  # 种子数据都在同一天
    assert data["labels"][0].count("-") == 2  # YYYY-MM-DD
    assert data["series"][0]["values"] == [3]


def test_metrics_invalid_dimension(client):
    resp = client.get("/api/metrics", params={"dimension": "unknown"})
    assert resp.status_code == 400
    assert resp.json()["code"] == 400


def test_metrics_empty_db(client):
    data = client.get("/api/metrics").json()["data"]
    assert data["labels"] == []
    assert data["series"][0]["values"] == []
    assert data["total"] == 0
```

- [ ] **Step 2: 运行测试确认失败**

Run: `python3 -m pytest tests/test_metrics.py -v`
Expected: FAIL，`404 Not Found`。

- [ ] **Step 3: 实现聚合路由**

`[manyu_test] app/routers/metrics.py`：
```python
"""报表聚合接口：按人员维度或按天聚合 call_log。"""
from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Query

from ..db import get_connection
from ..errors import BusinessError

router = APIRouter()

DIMENSIONS = ("user_type", "user_level", "department", "day")
DEFAULT_DAYS = 7
MAX_DAYS = 365


def parse_days(range_str):
    text = (range_str or f"{DEFAULT_DAYS}d").strip().lower()
    if text.endswith("d") and text[:-1].isdigit():
        return min(max(int(text[:-1]), 1), MAX_DAYS)
    return DEFAULT_DAYS


@router.get("/api/metrics")
def metrics(dimension: str = Query("user_type"), range: str = Query(f"{DEFAULT_DAYS}d")):
    if dimension not in DIMENSIONS:
        raise BusinessError(400, f"dimension 仅支持 {'|'.join(DIMENSIONS)}")
    days = parse_days(range)
    cutoff = (datetime.now(timezone.utc) - timedelta(days=days)).isoformat()

    conn = get_connection()
    try:
        if dimension == "day":
            rows = conn.execute(
                "SELECT substr(ts, 1, 10) AS label, COUNT(*) AS cnt "
                "FROM call_log WHERE ts >= ? GROUP BY label ORDER BY label ASC",
                (cutoff,),
            ).fetchall()
        else:
            # dimension 已通过白名单校验，拼接安全
            rows = conn.execute(
                f"SELECT {dimension} AS label, COUNT(*) AS cnt "
                "FROM call_log WHERE ts >= ? GROUP BY label ORDER BY cnt DESC, label ASC",
                (cutoff,),
            ).fetchall()
    finally:
        conn.close()

    labels = [row["label"] for row in rows]
    values = [row["cnt"] for row in rows]
    return {
        "code": 0,
        "data": {
            "dimension": dimension,
            "range": range,
            "labels": labels,
            "series": [{"name": "调用次数", "values": values}],
            "total": sum(values),
        },
    }
```

- [ ] **Step 4: 挂载路由**

修改 `[manyu_test] app/main.py`：将 `from .routers import business, export` 改为：
```python
from .routers import business, export, metrics
```
并在 `app.include_router(export.router)` 之后新增：
```python
    app.include_router(metrics.router)
```

- [ ] **Step 5: 运行测试确认通过**

Run: `python3 -m pytest tests/test_metrics.py -v`
Expected: PASS，`6 passed`。

- [ ] **Step 6: 全量回归**

Run: `python3 -m pytest`
Expected: PASS，全部通过（约 `26 passed`：health 1 + users 4 + hello 4 + hash 4 + bubble 6 + export 5 + metrics 6，± 以实际为准），退出码 0。

- [ ] **Step 7: 变更只读核对（流水线禁止 Git 写操作）**

```bash
cd [manyu_test]仓根
# 流水线禁止 git add/commit 等写操作，版本化由平台统一接管；此处仅只读核对变更范围
git status --short
git diff --stat
```

---

## Task 8: [manyu_test1] 前端工程骨架（Vite + React + AntD）

**Files:**
- Create: `[manyu_test1] package.json`
- Create: `[manyu_test1] vite.config.js`
- Create: `[manyu_test1] index.html`
- Create: `[manyu_test1] src/main.jsx`
- Create: `[manyu_test1] src/App.jsx`

**Interfaces:**
- Consumes: 无（前端首个任务）。
- Produces: 可构建的前端工程；`npm run dev` 提供 5173 端口且 `/api` 代理到 `http://127.0.0.1:8000`（后端 Task 1–7 的服务地址）；`App` 默认导出组件（Task 9/11 在其上叠加 Tabs 与报表面板）。

**前置环境（重要）：** 当前执行环境已验证**未安装 node/npm**（`node: not found`）。执行本任务前先安装 Node.js ≥ 18（例如 `nvm install 18` 或发行版包管理器）。若因网络/权限无法安装，按 Global Constraints 降级协议转静态审查，不得阻塞后续任务代码落盘。

- [ ] **Step 1: 创建工程文件**

`[manyu_test1] package.json`：
```json
{
  "name": "manyu-test1-frontend",
  "private": true,
  "version": "0.1.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview",
    "test": "vitest run"
  },
  "dependencies": {
    "antd": "^5.16.0",
    "echarts": "^5.5.0",
    "echarts-for-react": "^3.0.2",
    "react": "^18.2.0",
    "react-dom": "^18.2.0"
  },
  "devDependencies": {
    "@vitejs/plugin-react": "^4.2.1",
    "vite": "^5.2.0",
    "vitest": "^1.5.0"
  }
}
```

`[manyu_test1] vite.config.js`：
```js
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://127.0.0.1:8000', changeOrigin: true },
    },
  },
  test: {
    environment: 'node',
  },
})
```

`[manyu_test1] index.html`：
```html
<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>三接口演示平台</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.jsx"></script>
  </body>
</html>
```

`[manyu_test1] src/main.jsx`：
```jsx
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
```

`[manyu_test1] src/App.jsx`（骨架版，Task 9/11 会整体替换）：
```jsx
import { Layout, Typography } from 'antd'

export default function App() {
  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Layout.Header>
        <Typography.Title level={4} style={{ color: '#fff', margin: 0 }}>
          三接口演示平台
        </Typography.Title>
      </Layout.Header>
      <Layout.Content style={{ padding: 24 }}>
        <Typography.Text>页面骨架就绪（Tabs 与报表将在后续任务接入）</Typography.Text>
      </Layout.Content>
    </Layout>
  )
}
```

- [ ] **Step 2: 安装依赖**

Run（在 `[manyu_test1]` 仓根目录）: `npm install`
Expected: 退出码 0，生成 `node_modules/` 与 `package-lock.json`。（依赖安装失败重试仍失败 → 降级协议。）

- [ ] **Step 3: 构建验证**

Run: `npm run build`
Expected: 退出码 0，生成 `dist/index.html` 与 `dist/assets/*.js`。

- [ ] **Step 4: 变更只读核对（流水线禁止 Git 写操作）**

```bash
cd [manyu_test1]仓根
# 流水线禁止 git add/commit 等写操作，版本化由平台统一接管；此处仅只读核对变更范围
git status --short
git diff --stat
```

---

## Task 9: [manyu_test1] API 客户端与三 Tab 页面

**Files:**
- Create: `[manyu_test1] src/api/client.js`
- Create: `[manyu_test1] src/utils/parseNumbers.js`
- Test: `[manyu_test1] src/utils/parseNumbers.test.js`
- Create: `[manyu_test1] src/components/HelloTab.jsx`
- Create: `[manyu_test1] src/components/HashTab.jsx`
- Create: `[manyu_test1] src/components/BubbleTab.jsx`
- Modify: `[manyu_test1] src/App.jsx`（整体替换为 Tabs 版）

**Interfaces:**
- Consumes: 后端契约冻结表中的 `/api/hello`、`/api/hash`、`/api/bubble-sort`（Task 3–5）；mock 用户枚举与 `[manyu_test] data/users.json` 的 `u001..u004` 保持一致。
- Produces: `api.hello(name)`、`api.hash(text, algorithm)`、`api.bubbleSort(numbers, order)`、`api.metrics(dimension, range)`（返回包络中的 `data`，失败抛 `Error(message)`）；`setCurrentUser(userId)`；`MOCK_USERS`；`parseNumbers(text) -> number[]`（非法 token 抛错）。Task 10 在同文件追加 `exportTab`，Task 11 消费 `api.metrics`。

- [ ] **Step 1: 写失败测试**

`[manyu_test1] src/utils/parseNumbers.test.js`：
```js
import { describe, expect, it } from 'vitest'
import { parseNumbers } from './parseNumbers.js'

describe('parseNumbers', () => {
  it('支持逗号/中文逗号/分号/空白混合分隔', () => {
    expect(parseNumbers('3, 1，4;1 5')).toEqual([3, 1, 4, 1, 5])
  })

  it('空白输入返回空数组', () => {
    expect(parseNumbers('   ')).toEqual([])
  })

  it('支持负数与小数', () => {
    expect(parseNumbers('-3, 0, 7.5')).toEqual([-3, 0, 7.5])
  })

  it('非法 token 抛出错误', () => {
    expect(() => parseNumbers('1,abc')).toThrow('无法解析数字')
  })
})
```

- [ ] **Step 2: 运行测试确认失败**

Run（在 `[manyu_test1]` 仓根目录）: `npm test`
Expected: FAIL（vitest 报 `Failed to resolve import ./parseNumbers.js`）。

- [ ] **Step 3: 实现纯函数与 API 客户端**

`[manyu_test1] src/utils/parseNumbers.js`：
```js
export function parseNumbers(text) {
  const tokens = String(text ?? '')
    .split(/[,，;\s]+/)
    .filter((token) => token !== '')
  const numbers = []
  for (const token of tokens) {
    const value = Number(token)
    if (Number.isNaN(value)) {
      throw new Error(`无法解析数字: "${token}"`)
    }
    numbers.push(value)
  }
  return numbers
}
```

`[manyu_test1] src/api/client.js`：
```js
export const MOCK_USERS = [
  { userId: 'anonymous', label: '匿名用户' },
  { userId: 'u001', label: '张三 (u001 · 正式员工/P5/研发部)' },
  { userId: 'u002', label: '李四 (u002 · 正式员工/P7/产品部)' },
  { userId: 'u003', label: '王五 (u003 · 外包/P4/研发部)' },
  { userId: 'u004', label: '赵六 (u004 · 实习生/P3/数据部)' },
]

let currentUser = 'u001'

export function setCurrentUser(userId) {
  currentUser = userId
}

export function getCurrentUser() {
  return currentUser
}

async function request(path, options = {}) {
  const resp = await fetch(path, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      'X-User-Id': currentUser,
      ...(options.headers || {}),
    },
  })
  const body = await resp.json().catch(() => ({}))
  if (!resp.ok || body.code !== 0) {
    throw new Error(body.message || `请求失败: HTTP ${resp.status}`)
  }
  return body.data
}

export const api = {
  hello: (name) => request(`/api/hello?name=${encodeURIComponent(name || '')}`),
  hash: (text, algorithm) =>
    request('/api/hash', {
      method: 'POST',
      body: JSON.stringify({ text, algorithm }),
    }),
  bubbleSort: (numbers, order) =>
    request('/api/bubble-sort', {
      method: 'POST',
      body: JSON.stringify({ numbers, order }),
    }),
  metrics: (dimension, range = '7d') =>
    request(
      `/api/metrics?dimension=${encodeURIComponent(dimension)}&range=${encodeURIComponent(range)}`,
    ),
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `npm test`
Expected: PASS，`4 passed`（parseNumbers 4 个用例）。

- [ ] **Step 5: 实现三个 Tab 组件**

`[manyu_test1] src/components/HelloTab.jsx`：
```jsx
import { Alert, Button, Card, Input, Space, Typography } from 'antd'
import { useState } from 'react'
import { api } from '../api/client.js'

export default function HelloTab() {
  const [name, setName] = useState('')
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const run = async () => {
    setLoading(true)
    setError('')
    try {
      setResult(await api.hello(name))
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <Card>
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        <Space>
          <Input
            placeholder="输入姓名（留空默认 World）"
            value={name}
            onChange={(e) => setName(e.target.value)}
            style={{ width: 280 }}
          />
          <Button type="primary" loading={loading} onClick={run}>
            调用 /api/hello
          </Button>
        </Space>
        {error ? <Alert type="error" message={error} /> : null}
        {result ? (
          <Typography.Paragraph code style={{ whiteSpace: 'pre-wrap' }}>
            {JSON.stringify(result, null, 2)}
          </Typography.Paragraph>
        ) : null}
      </Space>
    </Card>
  )
}
```

`[manyu_test1] src/components/HashTab.jsx`：
```jsx
import { Alert, Button, Card, Input, Select, Space, Typography } from 'antd'
import { useState } from 'react'
import { api } from '../api/client.js'

const ALGORITHMS = ['md5', 'sha1', 'sha256', 'sha512']

export default function HashTab() {
  const [text, setText] = useState('')
  const [algorithm, setAlgorithm] = useState('sha256')
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const run = async () => {
    setLoading(true)
    setError('')
    try {
      setResult(await api.hash(text, algorithm))
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <Card>
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        <Input.TextArea
          placeholder="输入要哈希的文本"
          value={text}
          onChange={(e) => setText(e.target.value)}
          rows={3}
        />
        <Space>
          <Select
            value={algorithm}
            onChange={(value) => setAlgorithm(value)}
            options={ALGORITHMS.map((a) => ({ value: a, label: a }))}
            style={{ width: 140 }}
          />
          <Button type="primary" loading={loading} onClick={run}>
            调用 /api/hash
          </Button>
        </Space>
        {error ? <Alert type="error" message={error} /> : null}
        {result ? (
          <Typography.Paragraph code style={{ whiteSpace: 'pre-wrap' }}>
            {JSON.stringify(result, null, 2)}
          </Typography.Paragraph>
        ) : null}
      </Space>
    </Card>
  )
}
```

`[manyu_test1] src/components/BubbleTab.jsx`：
```jsx
import { Alert, Button, Card, Input, Radio, Space, Typography } from 'antd'
import { useState } from 'react'
import { api } from '../api/client.js'
import { parseNumbers } from '../utils/parseNumbers.js'

export default function BubbleTab() {
  const [raw, setRaw] = useState('5, 3, 8, 4, 2')
  const [order, setOrder] = useState('asc')
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const run = async () => {
    setError('')
    let numbers
    try {
      numbers = parseNumbers(raw)
    } catch (err) {
      setError(err.message)
      return
    }
    setLoading(true)
    try {
      setResult(await api.bubbleSort(numbers, order))
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <Card>
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        <Input
          placeholder="输入数字，如 5, 3, 8, 4, 2"
          value={raw}
          onChange={(e) => setRaw(e.target.value)}
        />
        <Space>
          <Radio.Group value={order} onChange={(e) => setOrder(e.target.value)}>
            <Radio.Button value="asc">升序</Radio.Button>
            <Radio.Button value="desc">降序</Radio.Button>
          </Radio.Group>
          <Button type="primary" loading={loading} onClick={run}>
            调用 /api/bubble-sort
          </Button>
        </Space>
        {error ? <Alert type="error" message={error} /> : null}
        {result ? (
          <Typography.Paragraph code style={{ whiteSpace: 'pre-wrap' }}>
            {JSON.stringify(result, null, 2)}
          </Typography.Paragraph>
        ) : null}
      </Space>
    </Card>
  )
}
```

- [ ] **Step 6: 整体替换 App.jsx（三 Tab + 调用人选择）**

`[manyu_test1] src/App.jsx`：
```jsx
import { Layout, Select, Space, Tabs, Typography } from 'antd'
import { useState } from 'react'
import { MOCK_USERS, setCurrentUser } from './api/client.js'
import BubbleTab from './components/BubbleTab.jsx'
import HashTab from './components/HashTab.jsx'
import HelloTab from './components/HelloTab.jsx'

const { Header, Content } = Layout

export default function App() {
  const [userId, setUserId] = useState('u001')
  setCurrentUser(userId)

  const items = [
    { key: 'hello', label: 'HelloWorld', children: <HelloTab /> },
    { key: 'hash', label: '哈希算法', children: <HashTab /> },
    { key: 'bubble', label: '冒泡排序', children: <BubbleTab /> },
  ]

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
        <Typography.Title level={4} style={{ color: '#fff', margin: 0 }}>
          三接口演示平台
        </Typography.Title>
        <Space>
          <span style={{ color: '#fff' }}>当前调用人：</span>
          <Select
            value={userId}
            onChange={(value) => setUserId(value)}
            options={MOCK_USERS.map((u) => ({ value: u.userId, label: u.label }))}
            style={{ width: 320 }}
          />
        </Space>
      </Header>
      <Content style={{ padding: 24, maxWidth: 1080, margin: '0 auto', width: '100%' }}>
        <Tabs items={items} />
      </Content>
    </Layout>
  )
}
```

- [ ] **Step 7: 构建验证**

Run: `npm run build`
Expected: 退出码 0，`dist/` 更新。

- [ ] **Step 8: 变更只读核对（流水线禁止 Git 写操作）**

```bash
cd [manyu_test1]仓根
# 流水线禁止 git add/commit 等写操作，版本化由平台统一接管；此处仅只读核对变更范围
git status --short
git diff --stat
```

---

## Task 10: [manyu_test1] 导出按钮（逐 Tab 下载展示结果）

**Files:**
- Modify: `[manyu_test1] src/api/client.js`（追加 `exportTab`）
- Modify: `[manyu_test1] src/components/HelloTab.jsx`、`HashTab.jsx`、`BubbleTab.jsx`（各加导出按钮）

**Interfaces:**
- Consumes: `GET /api/export?tab=&format=`（Task 6，契约：blob 文件流 + `Content-Disposition` 中 `filename="{tab}_{yyyyMMddHHmmss}.{ext}"`）；tab 枚举 `hello|hash|bubble` 与 Tabs key 完全一致。
- Produces: `exportTab(tab, format='csv') -> Promise<void>`：下载失败抛 `Error`；文件名优先取后端响应头，缺省回落 `{tab}_export.{format}`。

- [ ] **Step 1: 在 client.js 末尾追加导出函数**

在 `[manyu_test1] src/api/client.js` 文件末尾追加：
```js
export async function exportTab(tab, format = 'csv') {
  const resp = await fetch(
    `/api/export?tab=${encodeURIComponent(tab)}&format=${encodeURIComponent(format)}`,
    { headers: { 'X-User-Id': currentUser } },
  )
  if (!resp.ok) {
    throw new Error(`导出失败: HTTP ${resp.status}`)
  }
  const blob = await resp.blob()
  const disposition = resp.headers.get('Content-Disposition') || ''
  const match = disposition.match(/filename="?([^";]+)"?/)
  const filename = match ? match[1] : `${tab}_export.${format}`
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  anchor.click()
  URL.revokeObjectURL(url)
}
```

- [ ] **Step 2: 三个 Tab 组件各加导出按钮**

对 `[manyu_test1] src/components/HelloTab.jsx`、`HashTab.jsx`、`BubbleTab.jsx` 分别做三处同型修改（以 HelloTab 为例，HashTab/BubbleTab 将 `hello` 替换为 `hash`/`bubble`，其余完全相同）：

1. 导入行改为：
```jsx
import { api, exportTab } from '../api/client.js'
```
2. 在 `run` 函数之后新增：
```jsx
  const doExport = async () => {
    setError('')
    try {
      await exportTab('hello', 'csv')
    } catch (err) {
      setError(err.message)
    }
  }
```
3. 在“调用 /api/hello”按钮之后、`</Space>` 之前新增：
```jsx
          <Button onClick={doExport}>导出本 Tab 结果 (CSV)</Button>
```

- [ ] **Step 3: 构建验证**

Run: `npm run build`
Expected: 退出码 0。

- [ ] **Step 4: 变更只读核对（流水线禁止 Git 写操作）**

```bash
cd [manyu_test1]仓根
# 流水线禁止 git add/commit 等写操作，版本化由平台统一接管；此处仅只读核对变更范围
git status --short
git diff --stat
```

---

## Task 11: [manyu_test1] 埋点报表面板（维度切换 × 三种图表）

**Files:**
- Create: `[manyu_test1] src/charts/buildChartOption.js`
- Test: `[manyu_test1] src/charts/buildChartOption.test.js`
- Create: `[manyu_test1] src/components/MetricsPanel.jsx`
- Modify: `[manyu_test1] src/App.jsx`（Tabs 下方挂载报表面板）

**Interfaces:**
- Consumes: `api.metrics(dimension, range)`（Task 9 客户端 + Task 7 后端）；数据形状 `{labels: string[], series: [{name, values: number[]}], total: number}`。
- Produces: `buildChartOption(chartType, {labels, values, title}) -> ECharts option`（`pie` 用 name/value 对；`line`/`bar` 用 category xAxis + values；未知类型回落 bar）；`MetricsPanel` 组件（维度：人员类型/人员层级/人员部门/时间趋势；图表：折线图/饼图/柱状图；展示总调用次数）。

- [ ] **Step 1: 写失败测试**

`[manyu_test1] src/charts/buildChartOption.test.js`：
```js
import { describe, expect, it } from 'vitest'
import { buildChartOption } from './buildChartOption.js'

describe('buildChartOption', () => {
  const payload = { labels: ['研发部', '产品部'], values: [3, 1], title: 't' }

  it('饼图输出 name/value 数据对', () => {
    const option = buildChartOption('pie', payload)
    expect(option.series[0].type).toBe('pie')
    expect(option.series[0].data).toEqual([
      { name: '研发部', value: 3 },
      { name: '产品部', value: 1 },
    ])
  })

  it('折线图输出 category 轴与数值序列', () => {
    const option = buildChartOption('line', payload)
    expect(option.series[0].type).toBe('line')
    expect(option.xAxis.data).toEqual(['研发部', '产品部'])
    expect(option.series[0].data).toEqual([3, 1])
  })

  it('柱状图为默认回落类型', () => {
    const option = buildChartOption('scatter-unknown', payload)
    expect(option.series[0].type).toBe('bar')
  })
})
```

- [ ] **Step 2: 运行测试确认失败**

Run: `npm test`
Expected: FAIL（`Failed to resolve import ./buildChartOption.js`）。

- [ ] **Step 3: 实现图表 option 构建器**

`[manyu_test1] src/charts/buildChartOption.js`：
```js
export function buildChartOption(chartType, { labels = [], values = [], title = '' } = {}) {
  if (chartType === 'pie') {
    return {
      title: { text: title, left: 'center' },
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [
        {
          type: 'pie',
          radius: ['35%', '65%'],
          data: labels.map((label, index) => ({ name: label, value: values[index] })),
        },
      ],
    }
  }
  if (chartType === 'line') {
    return {
      title: { text: title, left: 'center' },
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: labels },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{ type: 'line', data: values, smooth: true }],
    }
  }
  return {
    title: { text: title, left: 'center' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: labels },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ type: 'bar', data: values }],
  }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `npm test`
Expected: PASS，`7 passed`（buildChartOption 3 + parseNumbers 4）。

- [ ] **Step 5: 实现报表面板并挂载**

`[manyu_test1] src/components/MetricsPanel.jsx`：
```jsx
import { Alert, Button, Card, Radio, Space, Spin } from 'antd'
import ReactECharts from 'echarts-for-react'
import { useCallback, useEffect, useState } from 'react'
import { api } from '../api/client.js'
import { buildChartOption } from '../charts/buildChartOption.js'

const DIMENSIONS = [
  { value: 'user_type', label: '人员类型' },
  { value: 'user_level', label: '人员层级' },
  { value: 'department', label: '人员部门' },
  { value: 'day', label: '时间趋势' },
]

const CHARTS = [
  { value: 'line', label: '折线图' },
  { value: 'pie', label: '饼图' },
  { value: 'bar', label: '柱状图' },
]

export default function MetricsPanel() {
  const [dimension, setDimension] = useState('user_type')
  const [chartType, setChartType] = useState('bar')
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const range = '7d'

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setData(await api.metrics(dimension, range))
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [dimension, range])

  useEffect(() => {
    load()
  }, [load])

  const dimensionLabel = DIMENSIONS.find((d) => d.value === dimension)?.label ?? dimension
  const option = data
    ? buildChartOption(chartType, {
        labels: data.labels,
        values: data.series?.[0]?.values ?? [],
        title: `调用次数统计（${dimensionLabel} · 近 ${range}）`,
      })
    : null

  return (
    <Card title="调用情况报表（埋点）" style={{ marginTop: 24 }}>
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        <Space wrap>
          <span>统计维度：</span>
          <Radio.Group value={dimension} onChange={(e) => setDimension(e.target.value)}>
            {DIMENSIONS.map((d) => (
              <Radio.Button key={d.value} value={d.value}>
                {d.label}
              </Radio.Button>
            ))}
          </Radio.Group>
          <span>图表类型：</span>
          <Radio.Group value={chartType} onChange={(e) => setChartType(e.target.value)}>
            {CHARTS.map((c) => (
              <Radio.Button key={c.value} value={c.value}>
                {c.label}
              </Radio.Button>
            ))}
          </Radio.Group>
          <Button onClick={load}>刷新</Button>
          {data ? <span>共 {data.total} 次调用</span> : null}
        </Space>
        {error ? <Alert type="error" message={error} /> : null}
        {loading ? <Spin /> : null}
        {!loading && option ? (
          <ReactECharts option={option} style={{ height: 360 }} notMerge />
        ) : null}
      </Space>
    </Card>
  )
}
```

修改 `[manyu_test1] src/App.jsx`：在导入区新增：
```jsx
import MetricsPanel from './components/MetricsPanel.jsx'
```
并在 `<Tabs items={items} />` 之后新增：
```jsx
        <MetricsPanel />
```

- [ ] **Step 6: 构建验证**

Run: `npm run build`
Expected: 退出码 0。

- [ ] **Step 7: 变更只读核对（流水线禁止 Git 写操作）**

```bash
cd [manyu_test1]仓根
# 流水线禁止 git add/commit 等写操作，版本化由平台统一接管；此处仅只读核对变更范围
git status --short
git diff --stat
```

---

## Task 12: 跨仓联调验证与文档

**Files:**
- Create: `[manyu_test] README.md`
- Create: `[manyu_test1] README.md`

**Interfaces:**
- Consumes: 全部前序任务产物（两仓服务）。
- Produces: 联调通过证据 + 两仓 README。

- [ ] **Step 1: 后端全量测试**

Run（在 `[manyu_test]` 仓根目录）: `python3 -m pytest`
Expected: 全部 PASS，退出码 0。

- [ ] **Step 2: 启动后端并逐接口验证**

Run（在 `[manyu_test]` 仓根目录）:
```bash
python3 -m uvicorn app.main:app --host 127.0.0.1 --port 8000 > /tmp/backend.log 2>&1 &
echo $! > /tmp/backend.pid
sleep 2
curl -s http://127.0.0.1:8000/api/health
curl -s "http://127.0.0.1:8000/api/hello?name=DTCoder" -H "X-User-Id: u001"
curl -s -X POST http://127.0.0.1:8000/api/hash -H "Content-Type: application/json" -H "X-User-Id: u002" -d '{"text":"hello","algorithm":"sha256"}'
curl -s -X POST http://127.0.0.1:8000/api/bubble-sort -H "Content-Type: application/json" -H "X-User-Id: u003" -d '{"numbers":[5,3,8,4,2],"order":"asc"}'
curl -s -D - -o /tmp/export_hello.csv "http://127.0.0.1:8000/api/export?tab=hello&format=csv"
curl -s "http://127.0.0.1:8000/api/metrics?dimension=user_type"
```
Expected:
- health → `{"code":0,"data":{"status":"ok"}}`
- hello → `data.greeting == "Hello, DTCoder!"`
- hash → `data.digest` 为 64 位十六进制
- bubble-sort → `data.sorted == [2,3,4,5,8]`
- export 响应头含 `Content-Disposition: attachment; filename="hello_`，`/tmp/export_hello.csv` 首行为 `ts,api,user_id,...` 表头
- metrics → `data.labels` 含 `正式员工` 且 `total >= 3`

- [ ] **Step 3: 前端联调验证（经代理）**

Run（在 `[manyu_test1]` 仓根目录）:
```bash
npm run dev > /tmp/frontend.log 2>&1 &
echo $! > /tmp/frontend.pid
sleep 5
curl -s http://127.0.0.1:5173/ | head -15
curl -s http://127.0.0.1:5173/api/health
```
Expected: 首页 HTML 含 `<div id="root">`；代理 health 返回 `{"code":0,"data":{"status":"ok"}}`。

- [ ] **Step 4: 停止联调进程**

Run:
```bash
kill "$(cat /tmp/backend.pid)" "$(cat /tmp/frontend.pid)" 2>/dev/null || true
```
Expected: 无报错；端口释放。

- [ ] **Step 5: 写两仓 README**

`[manyu_test] README.md`：
```markdown
# manyu_test 后端演示服务

三个业务接口（helloworld / 哈希算法 / 冒泡排序）+ 统一埋点（调用次数、调用人）+ 导出 + 报表聚合。

## 运行
python3 -m pip install -r requirements.txt
python3 -m uvicorn app.main:app --host 127.0.0.1 --port 8000

## 测试
python3 -m pytest

## 接口一览
| 方法/路径 | 说明 |
|------|------|
| GET /api/health | 健康检查 |
| GET /api/hello?name= | HelloWorld（埋点） |
| POST /api/hash `{text, algorithm}` | 哈希：md5/sha1/sha256/sha512（埋点） |
| POST /api/bubble-sort `{numbers, order}` | 冒泡排序 asc/desc，复用 bubble_sort.py（埋点） |
| GET /api/export?tab=hello|hash|bubble&format=csv|json | 导出对应 Tab 的调用结果 |
| GET /api/metrics?dimension=user_type|user_level|department|day&range=Nd | 报表聚合 |

调用人识别：请求头 `X-User-Id`，mock 用户表见 `data/users.json`（u001..u004）。
数据存储：SQLite `data/app.db`（可用环境变量 `APP_DB_PATH` 覆盖，测试即借此隔离）。
```

`[manyu_test1] README.md`：
```markdown
# manyu_test1 前端演示页面

单页三 Tab（HelloWorld / 哈希算法 / 冒泡排序）+ 逐 Tab 导出按钮 + 埋点调用情况报表（人员类型/人员层级/人员部门/时间趋势 × 折线图/饼图/柱状图）。

## 运行
npm install
npm run dev   # http://localhost:5173，/api 代理到 http://127.0.0.1:8000

## 测试与构建
npm test      # vitest：parseNumbers / buildChartOption 纯函数
npm run build # 产物 dist/

## 依赖
需先启动后端 manyu_test 服务（端口 8000）。页面右上角可切换调用人（u001..u004 / 匿名），用于演示埋点报表维度。
```

- [ ] **Step 6: 变更只读核对（流水线禁止 Git 写操作）**

```bash
cd [manyu_test]仓根
# 流水线禁止 git add/commit 等写操作，版本化由平台统一接管；此处仅只读核对两仓变更范围
git status --short

cd [manyu_test1]仓根
git status --short
```

---

## 跨仓对齐点自检表（静态审查基线）

| # | 对齐点 | 后端位置 | 前端位置 | 结论 |
|---|--------|----------|----------|------|
| 1 | `GET /api/hello` 出入参 | Task 3 `routers/business.py`：`{code,data:{greeting:string}}` | Task 9 `api.hello` 取 `body.data` 渲染 | 匹配 |
| 2 | `POST /api/hash` 请求体/响应 | Task 4：`{text,algorithm}` → `{algorithm,input,digest}` | Task 9 `api.hash(text, algorithm)` | 匹配 |
| 3 | `POST /api/bubble-sort` 类型 | Task 5：`numbers:number[]`，pydantic 拒绝非数字（422） | Task 9 `parseNumbers` 产出 number[]；非法输入前端先拦截 | 匹配 |
| 4 | `GET /api/export` 契约 | Task 6：blob + `Content-Disposition` 文件名 | Task 10 `exportTab` 解析 filename 并触发下载；tab 枚举与 Tabs key 一致（hello/hash/bubble） | 匹配 |
| 5 | `GET /api/metrics` 形状 | Task 7：`{labels, series:[{name,values}], total}` | Task 11 `buildChartOption` 消费 labels/values；chart 类型仅前端渲染选择 | 匹配 |
| 6 | 调用人标识 | 读 header `X-User-Id`，users.json `u001..u004` | 注入 `X-User-Id: currentUser`，`MOCK_USERS` 同为 `u001..u004` | 匹配 |
| 7 | 响应包络与错误 | 成功 `{code:0,data}`；失败 `{code,message}`+HTTP 状态码 | `body.code !== 0` 抛错展示 `message` | 匹配 |
| 8 | 部署/代理/CORS | 8000 端口，CORS 放行 5173 | 5173 端口，`/api → 127.0.0.1:8000` 代理 | 匹配 |

## 验收演示脚本（评审用）

1. `[manyu_test]` 仓根：`python3 -m pytest` 全绿 → 启动 uvicorn（8000）。
2. `[manyu_test1]` 仓根：`npm run dev`，打开 http://localhost:5173。
3. 右上角依次切换调用人（张三/李四/王五/赵六），在三个 Tab 分别点击调用按钮，确认各 Tab 展示 `greeting` / `digest` / `sorted` 结果。
4. 每个 Tab 点击“导出本 Tab 结果 (CSV)”，浏览器下载 `hello_*.csv` / `hash_*.csv` / `bubble_*.csv`，内容与页面调用记录一致。
5. 页面底部报表：切换“人员类型 / 人员层级 / 人员部门 / 时间趋势”与“折线图 / 饼图 / 柱状图”，图形随维度变化，总次数等于三 Tab 累计调用次数。

## Self-Review 记录（writing-plans 自查）

1. **需求覆盖**：R1 三接口 → Task 3/4/5；R2 三 Tab 页面 → Task 8/9；R3 导出按钮+导出接口 → Task 6/10；R4 埋点+报表可视化 → Task 2/3/7/11；跨仓联调与文档 → Task 12。无遗漏。
2. **占位符扫描**：无 TBD/TODO/“类似 Task N”；所有代码步骤均给出完整代码或精确定位的修改片段。
3. **类型一致性**：`execute_tracked(api, user_id, request_payload, handler)` 在 Task 3 定义、Task 4/5 原样复用；`data.labels/series/total` 在 Task 7 与 Task 11 一致；`tab` 枚举（hello/hash/bubble）在 Task 6/10 一致；`X-User-Id` 大小写在两端一致。
4. **修正**：已移除草稿中 Task 5 测试的一行冗余占位断言，`test_bubble_tracked` 仅保留真实断言。
5. **合规修正（loop-1 实施计划阶段）**：按流水线硬性约束将 Global Constraints 中的“执行者仅做 git add + git commit”改为 **Git 只读**；Task 1–12 原 12 个 Commit 收尾步骤全部替换为 `git status --short` / `git diff --stat` 只读核对，版本化由流水线平台统一接管。其余任务内容、接口契约与文件结构保持不变。
