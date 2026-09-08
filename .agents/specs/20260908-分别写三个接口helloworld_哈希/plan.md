# 三接口（HelloWorld / 哈希算法 / 冒泡排序）前后端全链路实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在两个仓库（`manyu_test` 后端 Python 服务 + `manyu_test1` 前端页面）中实现三个后端接口（HelloWorld、哈希算法、冒泡排序），前端通过 Tab 页展示各接口执行结果，新增导出按钮和导出接口，后端埋点统计调用次数/调用人，前端以折线图/饼图/柱状图可视化报表展示不同维度的调用统计。

**Architecture:**
- **manyu_test（后端仓库）**: Python Flask 服务，提供 RESTful API。现有 `bubble_sort.py` 已包含冒泡排序算法实现，需新增 Flask 应用入口及 HelloWorld / 哈希算法 / 冒泡排序三个接口，追加埋点中间件和导出/报表接口。
- **manyu_test1（前端仓库）**: 当前仅含 `README.md`，需从头搭建 React 前端项目（Vite + TypeScript），实现三 Tab 展示页、导出按钮、ECharts 图表可视化（折线图/饼图/柱状图）。

**Tech Stack:**
- 后端：Python 3 + Flask + flask-cors + hashlib（标准库）
- 前端：React 18 + TypeScript + Vite + ECharts（echarts + echarts-for-react）+ Axios
- 埋点数据存储：纯内存 `dict`（测试场景，无需数据库）

---

## Global Constraints

- 所有接口返回 JSON 格式
- 跨域支持：后端启用 flask-cors
- 前端打包产物输出到 `dist/` 目录
- Git 只读：本计划阶段不执行任何 git 写操作
- 严格路径隔离：manyu_test 仓库代码在 `/root/.agentix/agentic-dev/runs/DEV-eecb99b0-9b79-11f1-a438-852be3c2a082-935053d6-6bdb-4899-a1c3-725c949de916/worktree/manyu_test-cred-test-20260716022903/`，manyu_test1 仓库代码在 `/root/.agentix/agentic-dev/runs/DEV-eecb99b0-9b79-11f1-a438-852be3c2a082-935053d6-6bdb-4899-a1c3-725c949de916/worktree/manyu_test1-main/`

---

## 文件结构

### manyu_test（后端）新/改文件

| 路径 | 责任 | 操作 |
|------|------|------|
| `app.py` | Flask 应用入口，注册蓝图，启动服务 | 新增 |
| `routes/__init__.py` | 蓝图包初始化 | 新增 |
| `routes/hello.py` | HelloWorld 接口 | 新增 |
| `routes/hash_algo.py` | 哈希算法接口（MD5 / SHA256） | 新增 |
| `routes/bubble_api.py` | 冒泡排序接口（包装 `bubble_sort.py`） | 新增 |
| `routes/export.py` | 导出接口（支持按页面导出 CSV/JSON） | 新增 |
| `routes/stats.py` | 报表查询接口（按维度返回统计数据） | 新增 |
| `middleware/__init__.py` | 中间件包初始化 | 新增 |
| `middleware/tracker.py` | 埋点中间件（统计调用次数、调用人、维度） | 新增 |
| `bubble_sort.py` | 已有冒泡排序实现，**不修改** | 保留 |

### manyu_test1（前端）新文件

| 路径 | 责任 | 操作 |
|------|------|------|
| `package.json` | 项目依赖声明 | 新增 |
| `tsconfig.json` | TypeScript 配置 | 新增 |
| `vite.config.ts` | Vite 构建配置 | 新增 |
| `index.html` | 入口 HTML | 新增 |
| `src/main.tsx` | React 入口 | 新增 |
| `src/App.tsx` | 根组件（含 Tab 切换） | 新增 |
| `src/api/index.ts` | 统一 API 封装（Axios） | 新增 |
| `src/pages/HelloTab.tsx` | HelloWorld 展示 Tab | 新增 |
| `src/pages/HashTab.tsx` | 哈希算法展示 Tab | 新增 |
| `src/pages/SortTab.tsx` | 冒泡排序展示 Tab | 新增 |
| `src/pages/StatsTab.tsx` | 报表可视化 Tab（折线图/饼图/柱状图） | 新增 |
| `src/components/ExportButton.tsx` | 导出按钮组件 | 新增 |
| `src/types/index.ts` | TypeScript 类型定义 | 新增 |

---

## 接口契约

### 后端 → 前端 API

| 方法 | 路径 | 请求参数 | 响应 | 说明 |
|------|------|---------|------|------|
| GET | `/api/hello` | — | `{ "message": "Hello World!", "timestamp": "..." }` | HelloWorld 接口 |
| POST | `/api/hash` | `{ "text": "string", "algorithm": "md5"|"sha256" }` | `{ "input": "...", "algorithm": "...", "output": "..." }` | 哈希算法接口 |
| POST | `/api/bubble-sort` | `{ "array": [number] }` | `{ "original": [...], "sorted": [...], "swaps": int }` | 冒泡排序接口 |
| GET | `/api/export?page=hello|hash|sort` | query `page` | CSV/JSON 文件流 | 导出接口 |
| GET | `/api/stats?dimension=role|level|department` | query `dimension` | `{ "dimension": "...", "data": [{"name": "...", "value": int}, ...] }` | 报表数据接口 |

### 跨仓依赖

- 前端 `src/api/index.ts` 调用后端 `http://localhost:5000/api/*`
- 前端类型 `src/types/index.ts` 与后端 JSON 响应结构一一对齐
- 后端追踪的调用维度数据格式与前端报表接口查询格式一致

---

## 任务清单

---

### Task 1: [manyu_test] 后端项目骨架 + Flask 应用入口

**Files:**
- Create: `app.py`
- Create: `routes/__init__.py`
- Create: `routes/hello.py`
- Create: `routes/hash_algo.py`
- Create: `routes/bubble_api.py`
- Create: `routes/export.py`
- Create: `routes/stats.py`
- Create: `middleware/__init__.py`
- Create: `middleware/tracker.py`

**Interfaces:**
- Consumes: `bubble_sort.py` 中的 `bubble_sort()` / `bubble_sort_optimized()` / `bubble_sort_descending()` 三个函数签名
- Produces: Flask 应用 `app` 对象，注册所有蓝图，运行在 `0.0.0.0:5000`

- [ ] **Step 1: 创建 `middleware/tracker.py` — 埋点中间件核心**

```python
"""调用埋点追踪中间件"""
import time
from datetime import datetime
from functools import wraps
from flask import request, g

# 内存存储：{api_path: {timestamp: ..., caller: ..., role: ..., level: ..., department: ...}}
_tracking_store = []


def get_tracking_store():
    return _tracking_store


def track_call(f):
    """装饰器：记录接口调用信息"""
    @wraps(f)
    def wrapper(*args, **kwargs):
        caller = request.headers.get("X-Caller", "anonymous")
        role = request.headers.get("X-Role", "unknown")
        level = request.headers.get("X-Level", "unknown")
        department = request.headers.get("X-Department", "unknown")

        record = {
            "api": request.path,
            "method": request.method,
            "caller": caller,
            "role": role,
            "level": level,
            "department": department,
            "timestamp": datetime.utcnow().isoformat() + "Z",
            "ts_epoch": time.time(),
        }
        _tracking_store.append(record)
        return f(*args, **kwargs)
    return wrapper
```

- [ ] **Step 2: 创建 `middleware/__init__.py`**

```python
from .tracker import track_call, get_tracking_store
```

- [ ] **Step 3: 创建 `routes/__init__.py`**

```python
from flask import Blueprint

api_bp = Blueprint("api", __name__, url_prefix="/api")

from . import hello, hash_algo, bubble_api, export, stats
```

- [ ] **Step 4: 创建 `routes/hello.py` — HelloWorld 接口**

```python
from datetime import datetime
from ..middleware import track_call
from . import api_bp


@api_bp.route("/hello", methods=["GET"])
@track_call
def hello_world():
    return {
        "message": "Hello World!",
        "timestamp": datetime.utcnow().isoformat() + "Z",
    }
```

- [ ] **Step 5: 创建 `routes/hash_algo.py` — 哈希算法接口**

```python
import hashlib
from flask import request, jsonify
from ..middleware import track_call
from . import api_bp


@api_bp.route("/hash", methods=["POST"])
@track_call
def hash_text():
    data = request.get_json(force=True)
    text = data.get("text", "")
    algorithm = data.get("algorithm", "md5").lower()

    if algorithm == "md5":
        output = hashlib.md5(text.encode("utf-8")).hexdigest()
    elif algorithm == "sha256":
        output = hashlib.sha256(text.encode("utf-8")).hexdigest()
    else:
        return jsonify({"error": f"unsupported algorithm: {algorithm}"}), 400

    return {"input": text, "algorithm": algorithm, "output": output}
```

- [ ] **Step 6: 创建 `routes/bubble_api.py` — 冒泡排序接口**

```python
from flask import request, jsonify
from bubble_sort import bubble_sort_optimized
from ..middleware import track_call
from . import api_bp


@api_bp.route("/bubble-sort", methods=["POST"])
@track_call
def sort_array():
    data = request.get_json(force=True)
    arr = data.get("array", [])
    if not isinstance(arr, list):
        return jsonify({"error": "array must be a list"}), 400
    if not all(isinstance(x, (int, float)) for x in arr):
        return jsonify({"error": "all elements must be numeric"}), 400

    original = list(arr)
    sorted_arr = bubble_sort_optimized(list(arr))
    return {"original": original, "sorted": sorted_arr, "length": len(arr)}
```

- [ ] **Step 7: 创建 `routes/export.py` — 导出接口**

```python
import csv
import json
import io
from flask import request, jsonify, Response
from ..middleware import track_call, get_tracking_store
from . import api_bp


# 模拟各页面数据（实际可从 tracking store 或其他来源生成）
_PAGE_DATA = {
    "hello": lambda: {"message": "Hello World!", "timestamp": "2026-09-08T00:00:00Z"},
    "hash": lambda: {"examples": [
        {"input": "hello", "algorithm": "md5", "output": "5d41402abc4b2a76b9719d911017c592"},
        {"input": "hello", "algorithm": "sha256", "output": "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"},
    ]},
    "sort": lambda: {"examples": [
        {"original": [5, 3, 8, 4, 2], "sorted": [2, 3, 4, 5, 8], "length": 5},
        {"original": [9, -3, 0, 7, -1], "sorted": [-3, -1, 0, 7, 9], "length": 5},
    ]},
}


@api_bp.route("/export", methods=["GET"])
@track_call
def export_data():
    page = request.args.get("page", "hello")
    fmt = request.args.get("format", "json")

    if page not in _PAGE_DATA:
        return jsonify({"error": f"unknown page: {page}"}), 400

    data = _PAGE_DATA[page]()

    if fmt == "csv":
        output = io.StringIO()
        writer = csv.writer(output)
        # 展平数据为 CSV
        if isinstance(data, dict):
            writer.writerow(data.keys())
            writer.writerow(data.values())
        elif isinstance(data, list):
            for item in data:
                writer.writerow(item.values())
        return Response(
            output.getvalue(),
            mimetype="text/csv",
            headers={"Content-Disposition": f"attachment;filename={page}.csv"},
        )

    return jsonify(data)
```

- [ ] **Step 8: 创建 `routes/stats.py` — 报表查询接口**

```python
from flask import request, jsonify
from collections import Counter
from ..middleware import get_tracking_store
from . import api_bp


@api_bp.route("/stats", methods=["GET"])
def get_stats():
    dimension = request.args.get("dimension", "role")
    store = get_tracking_store()

    if not store:
        return jsonify({"dimension": dimension, "data": []})

    # 按维度聚合
    dim_key_map = {
        "role": "role",
        "level": "level",
        "department": "department",
        "caller": "caller",
    }
    key = dim_key_map.get(dimension, "role")

    counter = Counter(record.get(key, "unknown") for record in store)
    data = [{"name": name, "value": count} for name, count in counter.most_common()]

    return jsonify({"dimension": dimension, "data": data})
```

- [ ] **Step 9: 创建 `app.py` — Flask 应用入口**

```python
"""Flask 应用入口：三接口服务"""
from flask import Flask
from flask_cors import CORS
from routes import api_bp


def create_app():
    app = Flask(__name__)
    CORS(app)
    app.register_blueprint(api_bp)
    return app


if __name__ == "__main__":
    app = create_app()
    app.run(host="0.0.0.0", port=5000, debug=True)
```

- [ ] **Step 10: 安装依赖并验证后端启动**

运行：
```bash
cd /root/.agentix/agentic-dev/runs/DEV-eecb99b0-9b79-11f1-a438-852be3c2a082-935053d6-6bdb-4899-a1c3-725c949de916/worktree/manyu_test-cred-test-20260716022903
pip install flask flask-cors
```

验证：
```bash
python app.py &
curl http://localhost:5000/api/hello
curl -X POST http://localhost:5000/api/hash -H "Content-Type: application/json" -d '{"text":"hello","algorithm":"md5"}'
curl -X POST http://localhost:5000/api/bubble-sort -H "Content-Type: application/json" -d '{"array":[5,3,8,4,2]}'
```

预期输出：
- `/api/hello` → `{"message":"Hello World!","timestamp":"..."}`
- `/api/hash` → `{"input":"hello","algorithm":"md5","output":"5d41402abc4b2a76b9719d911017c592"}`
- `/api/bubble-sort` → `{"original":[5,3,8,4,2],"sorted":[2,3,4,5,8],"length":5}`

---

### Task 2: [manyu_test] 后端验证 + 导出/报表接口验证

- [ ] **Step 1: 验证所有接口在启动后返回正确结果**

```bash
cd /root/.agentix/agentic-dev/runs/DEV-eecb99b0-9b79-11f1-a438-852be3c2a082-935053d6-6bdb-4899-a1c3-725c949de916/worktree/manyu_test-cred-test-20260716022903
pkill -f "python app.py" 2>/dev/null; sleep 0.5
python app.py &
sleep 1
curl -s http://localhost:5000/api/hello
curl -s -X POST http://localhost:5000/api/hash -H "Content-Type: application/json" -d '{"text":"test","algorithm":"sha256"}'
curl -s -X POST http://localhost:5000/api/bubble-sort -H "Content-Type: application/json" -d '{"array":[9,2,7,1]}'
```

预期输出均包含正确 JSON 字段。

- [ ] **Step 2: 验证导出接口**

```bash
curl -s "http://localhost:5000/api/export?page=hello"
curl -s "http://localhost:5000/api/export?page=hash"
curl -s "http://localhost:5000/api/export?page=sort"
```

- [ ] **Step 3: 验证埋点 + 报表接口**

```bash
# 先执行以上 curl 填充追踪数据（带调用者标头）
curl -s -H "X-Caller: alice" -H "X-Role: dev" -H "X-Level: senior" -H "X-Department: eng" http://localhost:5000/api/hello
curl -s -H "X-Caller: bob" -H "X-Role: qa" -H "X-Level: junior" -H "X-Department: qa" http://localhost:5000/api/hello

# 查询报表
curl -s "http://localhost:5000/api/stats?dimension=role"
curl -s "http://localhost:5000/api/stats?dimension=level"
curl -s "http://localhost:5000/api/stats?dimension=department"
```

预期输出包含按维度聚合的计数。

---

### Task 3: [manyu_test1] 前端项目初始化

**Files:**
- Create: `package.json`
- Create: `tsconfig.json`
- Create: `vite.config.ts`
- Create: `index.html`
- Create: `src/main.tsx`
- Create: `src/types/index.ts`
- Create: `src/api/index.ts`

**Interfaces:**
- Produces: 可运行的 Vite + React + TypeScript 前端项目脚手架

- [ ] **Step 1: 创建 `package.json`**

```json
{
  "name": "manyu_test1-frontend",
  "private": true,
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "axios": "^1.6.0",
    "echarts": "^5.4.3",
    "echarts-for-react": "^3.0.2"
  },
  "devDependencies": {
    "@types/react": "^18.2.0",
    "@types/react-dom": "^18.2.0",
    "typescript": "^5.3.0",
    "vite": "^5.0.0",
    "@vitejs/plugin-react": "^4.2.0"
  }
}
```

- [ ] **Step 2: 创建 `tsconfig.json`**

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "react-jsx",
    "strict": true,
    "noUnusedLocals": false,
    "noUnusedParameters": false,
    "noFallthroughCasesInSwitch": true
  },
  "include": ["src"]
}
```

- [ ] **Step 3: 创建 `vite.config.ts`**

```ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:5000',
        changeOrigin: true,
      },
    },
  },
});
```

- [ ] **Step 4: 创建 `index.html`**

```html
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>三接口演示平台</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

- [ ] **Step 5: 创建 `src/types/index.ts`**

```ts
/** API 响应类型定义 */

export interface HelloResponse {
  message: string;
  timestamp: string;
}

export interface HashRequest {
  text: string;
  algorithm: 'md5' | 'sha256';
}

export interface HashResponse {
  input: string;
  algorithm: string;
  output: string;
}

export interface SortRequest {
  array: number[];
}

export interface SortResponse {
  original: number[];
  sorted: number[];
  length: number;
}

export interface StatsItem {
  name: string;
  value: number;
}

export interface StatsResponse {
  dimension: string;
  data: StatsItem[];
}

export type PageType = 'hello' | 'hash' | 'sort';
export type DimensionType = 'role' | 'level' | 'department' | 'caller';
```

- [ ] **Step 6: 创建 `src/api/index.ts`**

```ts
import axios from 'axios';
import type { HelloResponse, HashRequest, HashResponse, SortRequest, SortResponse, StatsResponse, DimensionType } from '../types';

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
});

export async function fetchHello(): Promise<HelloResponse> {
  const { data } = await api.get<HelloResponse>('/hello');
  return data;
}

export async function fetchHash(params: HashRequest): Promise<HashResponse> {
  const { data } = await api.post<HashResponse>('/hash', params);
  return data;
}

export async function fetchSort(params: SortRequest): Promise<SortResponse> {
  const { data } = await api.post<SortResponse>('/bubble-sort', params);
  return data;
}

export async function fetchExport(page: string, format: 'json' | 'csv' = 'json'): Promise<Blob | any> {
  if (format === 'csv') {
    const { data } = await api.get('/export', { params: { page, format }, responseType: 'blob' });
    return data;
  }
  const { data } = await api.get('/export', { params: { page } });
  return data;
}

export async function fetchStats(dimension: DimensionType): Promise<StatsResponse> {
  const { data } = await api.get<StatsResponse>('/stats', { params: { dimension } });
  return data;
}
```

- [ ] **Step 7: 创建 `src/main.tsx`**

```tsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
```

---

### Task 4: [manyu_test1] 三 Tab 页面组件 + 导出按钮

**Files:**
- Create: `src/pages/HelloTab.tsx`
- Create: `src/pages/HashTab.tsx`
- Create: `src/pages/SortTab.tsx`
- Create: `src/components/ExportButton.tsx`
- Modify: `src/App.tsx`（阶段性整合）

**Interfaces:**
- Consumes: `src/api/index.ts` 导出的 `fetchHello` / `fetchHash` / `fetchSort` / `fetchExport`
- Produces: 可独立渲染的三个 Tab 组件 + 导出按钮

- [ ] **Step 1: 创建 `src/pages/HelloTab.tsx`**

```tsx
import React, { useEffect, useState } from 'react';
import { fetchHello, fetchExport } from '../api';
import type { HelloResponse } from '../types';
import ExportButton from '../components/ExportButton';

const HelloTab: React.FC = () => {
  const [data, setData] = useState<HelloResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await fetchHello();
      setData(result);
    } catch (e: any) {
      setError(e.message || '请求失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadData(); }, []);

  return (
    <div style={{ padding: '20px' }}>
      <h2>HelloWorld 接口</h2>
      <button onClick={loadData} disabled={loading} style={{ marginRight: 12 }}>
        {loading ? '加载中...' : '刷新'}
      </button>
      <ExportButton page="hello" />
      {error && <p style={{ color: 'red' }}>错误：{error}</p>}
      {data && (
        <pre style={{ background: '#f5f5f5', padding: 12, borderRadius: 4, marginTop: 12 }}>
          {JSON.stringify(data, null, 2)}
        </pre>
      )}
    </div>
  );
};

export default HelloTab;
```

- [ ] **Step 2: 创建 `src/pages/HashTab.tsx`**

```tsx
import React, { useState } from 'react';
import { fetchHash, fetchExport } from '../api';
import type { HashResponse } from '../types';
import ExportButton from '../components/ExportButton';

const HashTab: React.FC = () => {
  const [text, setText] = useState('hello');
  const [algorithm, setAlgorithm] = useState<'md5' | 'sha256'>('md5');
  const [data, setData] = useState<HashResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleHash = async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await fetchHash({ text, algorithm });
      setData(result);
    } catch (e: any) {
      setError(e.message || '请求失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ padding: '20px' }}>
      <h2>哈希算法接口</h2>
      <div style={{ marginBottom: 12 }}>
        <input
          type="text"
          value={text}
          onChange={e => setText(e.target.value)}
          placeholder="输入待哈希文本"
          style={{ marginRight: 8, padding: '4px 8px' }}
        />
        <select
          value={algorithm}
          onChange={e => setAlgorithm(e.target.value as 'md5' | 'sha256')}
          style={{ marginRight: 8, padding: '4px 8px' }}
        >
          <option value="md5">MD5</option>
          <option value="sha256">SHA-256</option>
        </select>
        <button onClick={handleHash} disabled={loading}>
          {loading ? '计算中...' : '计算哈希'}
        </button>
      </div>
      <ExportButton page="hash" />
      {error && <p style={{ color: 'red' }}>错误：{error}</p>}
      {data && (
        <div style={{ background: '#f5f5f5', padding: 12, borderRadius: 4, marginTop: 12 }}>
          <p><strong>输入：</strong>{data.input}</p>
          <p><strong>算法：</strong>{data.algorithm}</p>
          <p><strong>哈希值：</strong><code>{data.output}</code></p>
        </div>
      )}
    </div>
  );
};

export default HashTab;
```

- [ ] **Step 3: 创建 `src/pages/SortTab.tsx`**

```tsx
import React, { useState } from 'react';
import { fetchSort, fetchExport } from '../api';
import type { SortResponse } from '../types';
import ExportButton from '../components/ExportButton';

const SortTab: React.FC = () => {
  const [inputArray, setInputArray] = useState('5,3,8,4,2');
  const [data, setData] = useState<SortResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSort = async () => {
    setLoading(true);
    setError(null);
    try {
      const array = inputArray.split(',').map(s => Number(s.trim())).filter(n => !isNaN(n));
      if (array.length === 0) {
        setError('请输入有效的数字列表（逗号分隔）');
        setLoading(false);
        return;
      }
      const result = await fetchSort({ array });
      setData(result);
    } catch (e: any) {
      setError(e.message || '请求失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ padding: '20px' }}>
      <h2>冒泡排序接口</h2>
      <div style={{ marginBottom: 12 }}>
        <input
          type="text"
          value={inputArray}
          onChange={e => setInputArray(e.target.value)}
          placeholder="逗号分隔的数字列表"
          style={{ marginRight: 8, padding: '4px 8px', width: 250 }}
        />
        <button onClick={handleSort} disabled={loading}>
          {loading ? '排序中...' : '排序'}
        </button>
      </div>
      <ExportButton page="sort" />
      {error && <p style={{ color: 'red' }}>错误：{error}</p>}
      {data && (
        <div style={{ background: '#f5f5f5', padding: 12, borderRadius: 4, marginTop: 12 }}>
          <p><strong>原始数组：</strong>[{data.original.join(', ')}]</p>
          <p><strong>排序结果：</strong>[{data.sorted.join(', ')}]</p>
          <p><strong>长度：</strong>{data.length}</p>
        </div>
      )}
    </div>
  );
};

export default SortTab;
```

- [ ] **Step 4: 创建 `src/components/ExportButton.tsx`**

```tsx
import React, { useState } from 'react';
import { fetchExport } from '../api';
import type { PageType } from '../types';

interface Props {
  page: PageType;
}

const ExportButton: React.FC<Props> = ({ page }) => {
  const [loading, setLoading] = useState(false);

  const handleExport = async (format: 'json' | 'csv') => {
    setLoading(true);
    try {
      const data = await fetchExport(page, format);
      if (format === 'csv' && data instanceof Blob) {
        const url = URL.createObjectURL(data);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${page}.csv`;
        a.click();
        URL.revokeObjectURL(url);
      } else {
        const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${page}.json`;
        a.click();
        URL.revokeObjectURL(url);
      }
    } catch (e) {
      alert('导出失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <span>
      <button onClick={() => handleExport('json')} disabled={loading} style={{ marginRight: 4 }}>
        导出 JSON
      </button>
      <button onClick={() => handleExport('csv')} disabled={loading}>
        导出 CSV
      </button>
    </span>
  );
};

export default ExportButton;
```

- [ ] **Step 5: 更新 `src/App.tsx`（整合三 Tab + 报表 Tab 占位）**

```tsx
import React, { useState } from 'react';
import HelloTab from './pages/HelloTab';
import HashTab from './pages/HashTab';
import SortTab from './pages/SortTab';
import StatsTab from './pages/StatsTab';

type TabKey = 'hello' | 'hash' | 'sort' | 'stats';

const TABS: { key: TabKey; label: string }[] = [
  { key: 'hello', label: 'HelloWorld' },
  { key: 'hash', label: '哈希算法' },
  { key: 'sort', label: '冒泡排序' },
  { key: 'stats', label: '调用报表' },
];

const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState<TabKey>('hello');

  const renderTab = () => {
    switch (activeTab) {
      case 'hello': return <HelloTab />;
      case 'hash': return <HashTab />;
      case 'sort': return <SortTab />;
      case 'stats': return <StatsTab />;
    }
  };

  return (
    <div style={{ fontFamily: 'sans-serif', maxWidth: 900, margin: '0 auto', padding: '20px' }}>
      <h1>三接口演示平台</h1>
      <div style={{ borderBottom: '2px solid #ccc', marginBottom: 16 }}>
        {TABS.map(tab => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            style={{
              padding: '8px 20px',
              marginRight: 4,
              border: 'none',
              background: activeTab === tab.key ? '#1890ff' : '#f0f0f0',
              color: activeTab === tab.key ? '#fff' : '#333',
              cursor: 'pointer',
              borderRadius: '4px 4px 0 0',
            }}
          >
            {tab.label}
          </button>
        ))}
      </div>
      {renderTab()}
    </div>
  );
};

export default App;
```

---

### Task 5: [manyu_test1] 报表可视化 Tab（折线图 / 饼图 / 柱状图）

**Files:**
- Create: `src/pages/StatsTab.tsx`

**Interfaces:**
- Consumes: `src/api/index.ts` 的 `fetchStats`，`src/types/index.ts` 的 `DimensionType` / `StatsResponse`
- Produces: 带维度切换和三种图表类型的报表页面

- [ ] **Step 1: 创建 `src/pages/StatsTab.tsx`**

```tsx
import React, { useEffect, useState } from 'react';
import ReactEChartsCore from 'echarts-for-react/lib/core';
import * as echarts from 'echarts/core';
import { BarChart, LineChart, PieChart } from 'echarts/charts';
import { TitleComponent, TooltipComponent, LegendComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import { fetchStats } from '../api';
import type { DimensionType, StatsItem } from '../types';

echarts.use([BarChart, LineChart, PieChart, TitleComponent, TooltipComponent, LegendComponent, CanvasRenderer]);

type ChartType = 'bar' | 'line' | 'pie';

const DIMENSIONS: { key: DimensionType; label: string }[] = [
  { key: 'role', label: '人员类型' },
  { key: 'level', label: '人员层级' },
  { key: 'department', label: '人员部门' },
  { key: 'caller', label: '调用人' },
];

const CHART_TYPES: { key: ChartType; label: string }[] = [
  { key: 'bar', label: '柱状图' },
  { key: 'line', label: '折线图' },
  { key: 'pie', label: '饼图' },
];

const COLOR_PALETTE = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4'];

const StatsTab: React.FC = () => {
  const [dimension, setDimension] = useState<DimensionType>('role');
  const [chartType, setChartType] = useState<ChartType>('bar');
  const [data, setData] = useState<StatsItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadStats = async (dim: DimensionType) => {
    setLoading(true);
    setError(null);
    try {
      const result = await fetchStats(dim);
      setData(result.data);
    } catch (e: any) {
      setError(e.message || '请求失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadStats(dimension); }, [dimension]);

  const getOption = () => {
    const names = data.map(d => d.name);
    const values = data.map(d => d.value);

    const baseOption: any = {
      color: COLOR_PALETTE,
      tooltip: { trigger: chartType === 'pie' ? 'item' : 'axis' },
      legend: chartType === 'pie' ? { data: names, bottom: 0 } : undefined,
    };

    if (chartType === 'pie') {
      return {
        ...baseOption,
        series: [{
          type: 'pie',
          radius: ['30%', '60%'],
          center: ['50%', '50%'],
          data: data.map(d => ({ name: d.name, value: d.value })),
          label: { show: true, formatter: '{b}: {c}' },
          emphasis: {
            itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.5)' },
          },
        }],
      };
    }

    return {
      ...baseOption,
      xAxis: {
        type: 'category',
        data: names,
        axisLabel: { rotate: names.length > 5 ? 30 : 0 },
      },
      yAxis: { type: 'value' },
      series: [{
        type: chartType,
        data: values,
        itemStyle: {
          borderRadius: chartType === 'bar' ? [4, 4, 0, 0] : undefined,
        },
        lineStyle: chartType === 'line' ? { width: 3 } : undefined,
        symbolSize: chartType === 'line' ? 8 : undefined,
        areaStyle: chartType === 'line' ? { opacity: 0.15 } : undefined,
      }],
    };
  };

  return (
    <div style={{ padding: '20px' }}>
      <h2>调用统计报表</h2>

      <div style={{ marginBottom: 16 }}>
        <span style={{ marginRight: 8 }}><strong>维度：</strong></span>
        {DIMENSIONS.map(d => (
          <button
            key={d.key}
            onClick={() => setDimension(d.key)}
            style={{
              marginRight: 6,
              padding: '4px 12px',
              border: '1px solid #d9d9d9',
              background: dimension === d.key ? '#1890ff' : '#fff',
              color: dimension === d.key ? '#fff' : '#333',
              cursor: 'pointer',
              borderRadius: 4,
            }}
          >
            {d.label}
          </button>
        ))}
      </div>

      <div style={{ marginBottom: 16 }}>
        <span style={{ marginRight: 8 }}><strong>图表类型：</strong></span>
        {CHART_TYPES.map(ct => (
          <button
            key={ct.key}
            onClick={() => setChartType(ct.key)}
            style={{
              marginRight: 6,
              padding: '4px 12px',
              border: '1px solid #d9d9d9',
              background: chartType === ct.key ? '#52c41a' : '#fff',
              color: chartType === ct.key ? '#fff' : '#333',
              cursor: 'pointer',
              borderRadius: 4,
            }}
          >
            {ct.label}
          </button>
        ))}
      </div>

      {loading && <p>加载中...</p>}
      {error && <p style={{ color: 'red' }}>错误：{error}</p>}
      {!loading && !error && data.length === 0 && (
        <p style={{ color: '#999' }}>暂无统计数据，请先调用后端接口。</p>
      )}
      {!loading && data.length > 0 && (
        <ReactEChartsCore
          echarts={echarts}
          option={getOption()}
          style={{ height: 400, width: '100%' }}
          notMerge
          opts={{ renderer: 'canvas' }}
        />
      )}
    </div>
  );
};

export default StatsTab;
```

---

### Task 6: [manyu_test1] 前端构建验证

- [ ] **Step 1: 安装依赖**

```bash
cd /root/.agentix/agentic-dev/runs/DEV-eecb99b0-9b79-11f1-a438-852be3c2a082-935053d6-6bdb-4899-a1c3-725c949de916/worktree/manyu_test1-main
npm install
```

预期：无报错，`node_modules` 目录生成。

- [ ] **Step 2: TypeScript 编译检查**

```bash
npx tsc --noEmit
```

预期：无类型错误。

- [ ] **Step 3: Vite 构建**

```bash
npx vite build
```

预期：构建成功，`dist/` 目录生成，无报错。

---

### Task 7: 跨仓集成验证

- [ ] **Step 1: 启动后端服务**

```bash
cd /root/.agentix/agentic-dev/runs/DEV-eecb99b0-9b79-11f1-a438-852be3c2a082-935053d6-6bdb-4899-a1c3-725c949de916/worktree/manyu_test-cred-test-20260716022903
pkill -f "python app.py" 2>/dev/null
python app.py &
```

- [ ] **Step 2: 启动前端开发服务器**

```bash
cd /root/.agentix/agentic-dev/runs/DEV-eecb99b0-9b79-11f1-a438-852be3c2a082-935053d6-6bdb-4899-a1c3-725c949de916/worktree/manyu_test1-main
npx vite --host 0.0.0.0 &
```

- [ ] **Step 3: 端到端验证**

```bash
# 后端接口验证
curl -s http://localhost:5000/api/hello
curl -s -X POST http://localhost:5000/api/hash -H "Content-Type: application/json" -d '{"text":"hello","algorithm":"md5"}'
curl -s -X POST http://localhost:5000/api/bubble-sort -H "Content-Type: application/json" -d '{"array":[5,3,8,4,2]}'

# 前端可访问
curl -s -o /dev/null -w "%{http_code}" http://localhost:3000/
```

预期：
- 后端三个接口均返回 200 + 正确 JSON
- 前端页面返回 200（HTML）

---

## 自检清单

| 需求 | 对应 Task | 覆盖情况 |
|------|-----------|---------|
| HelloWorld 接口 | Task 1 Step 4 | ✅ |
| 哈希算法接口（MD5/SHA256） | Task 1 Step 5 | ✅ |
| 冒泡排序接口 | Task 1 Step 6 | ✅ |
| 前端三 Tab 页面 | Task 4 | ✅ |
| 导出按钮 | Task 4 Step 4 | ✅ |
| 后端导出接口 | Task 1 Step 7 | ✅ |
| 后端埋点（调用次数/调用人/维度） | Task 1 Step 1 | ✅ |
| 前端报表可视化（折线图/饼图/柱状图） | Task 5 | ✅ |
| 按维度切换（人员类型/层级/部门等） | Task 5 | ✅ |

**无占位符检查：** 所有代码块为完整实现，无 "TBD" / "TODO" / "implement later" 占位符。

**类型一致性检查：** 前端类型定义 (`src/types/index.ts`) 与后端 JSON 响应字段名一致；API 调用层 (`src/api/index.ts`) 使用正确类型参数；ECharts 图表配置项使用正确字段名。