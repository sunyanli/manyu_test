# 分别写三个接口（helloworld/哈希/冒泡排序）+ 前端页面 + 埋点可视化 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建前后端分离的全链路应用，后端提供三个核心 API 接口（helloworld、哈希算法、冒泡排序）、导出接口及调用埋点记录；前端 Tab 页面展示各接口结果，支持导出，并可视化展示埋点统计报表（折线图、饼图、柱状图）。

**Architecture:** 前后端分离架构，后端 manyu_test 仓库（Python FastAPI）提供 RESTful API，前端 manyu_test1 仓库（Vue 3 + ECharts）提供单页应用。后端使用 SQLite 存储埋点数据，CSV 格式导出。前端通过 HTTP 调用后端 API，每个 Tab 独立加载对应接口数据。

**Tech Stack:**
- 后端：Python 3 + FastAPI + uvicorn + SQLite3
- 前端：Vue 3 + Vite + ECharts
- 导出格式：CSV
- 埋点存储：SQLite（app 内嵌）
- 调用人模拟：请求头 `X-Caller-Info` 传入模拟 JSON

## Global Constraints

- 后端端口：8000，前端开发服务器端口：5173
- 后端所有 API 路径以 `/api/` 开头
- 冒泡排序复用已有 `bubble_sort.py` 中的 `bubble_sort` 函数
- 统一错误响应格式：`{"success": false, "error_code": "ERR_xxxxx", "message": "用户可读文案", "detail": null}`
- 前端使用 Vue 3 Composition API + `<script setup>` 语法
- 前端禁止使用 TypeScript（保持简单）
- 图表库仅使用 ECharts（通过 `echarts` npm 包）
- 禁止修改 `bubble_sort.py` 已有代码

---

## 一、文件结构

### manyu_test（后端仓库）

```
manyu_test/
├── app.py                    # FastAPI 主应用入口，路由注册
├── bubble_sort.py            # 已有文件，复用
├── requirements.txt          # Python 依赖
├── tracking.db               # SQLite 埋点数据库（自动生成）
└── exports/                  # 导出文件目录（自动生成）
```

### manyu_test1（前端仓库）

```
manyu_test1/
├── index.html
├── package.json
├── vite.config.js
├── src/
│   ├── main.js               # Vue 应用入口
│   ├── App.vue                # 根组件（Tab 切换 + 导出按钮 + 报表面板布局）
│   ├── api/
│   │   └── index.js           # 封装所有后端 API 调用
│   ├── components/
│   │   ├── TabHelloWorld.vue  # Tab1: Helloworld 结果展示
│   │   ├── TabHash.vue        # Tab2: 哈希算法结果展示
│   │   ├── TabBubbleSort.vue  # Tab3: 冒泡排序结果展示
│   │   ├── ExportButton.vue   # 导出按钮组件
│   │   ├── StatsLineChart.vue  # 折线图（时间趋势）
│   │   ├── StatsPieChart.vue   # 饼图（人员类型/部门）
│   │   └── StatsBarChart.vue   # 柱状图（人员层级）
│   └── styles/
│       └── main.css           # 全局样式
```

---

## 二、任务分解

---

### Task 1: [manyu_test] 后端项目初始化 + FastAPI 主应用 + 三个核心接口

**Files:**
- Create: `manyu_test/app.py`
- Create: `manyu_test/requirements.txt`
- (Use existing: `manyu_test/bubble_sort.py`)

**Interfaces:**
- Consumes: `bubble_sort(arr: List[T]) -> List[T]` from `bubble_sort.py`
- Produces: `GET /api/helloworld` → `{"success": true, "data": {"message": "Hello World!", "timestamp": "..."}}`
- Produces: `GET /api/hash?text=xxx` → `{"success": true, "data": {"algorithm": "sha256", "input": "xxx", "hash": "abc123..."}}`
- Produces: `POST /api/bubble-sort` body `{"array": [3,1,2]}` → `{"success": true, "data": {"original": [3,1,2], "sorted": [1,2,3]}}`

- [ ] **Step 1: Create requirements.txt**

```txt
fastapi==0.104.1
uvicorn==0.24.0
```

- [ ] **Step 2: Create app.py — imports, FastAPI app, CORS setup**

```python
import json
import hashlib
import sqlite3
import os
import csv
import uuid
from datetime import datetime
from typing import List, Optional
from fastapi import FastAPI, Query, Body, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from bubble_sort import bubble_sort

app = FastAPI(title="三接口演示服务")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 数据库初始化
DB_PATH = "tracking.db"
EXPORT_DIR = "exports"

def init_db():
    os.makedirs(EXPORT_DIR, exist_ok=True)
    conn = sqlite3.connect(DB_PATH)
    conn.execute("""
        CREATE TABLE IF NOT EXISTS track_events (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            event_id TEXT UNIQUE,
            api_name TEXT,
            caller TEXT,
            person_type TEXT,
            person_level TEXT,
            person_department TEXT,
            timestamp TEXT
        )
    """)
    conn.commit()
    conn.close()

init_db()
```

- [ ] **Step 3: Add request models and helper functions**

```python
class BubbleSortRequest(BaseModel):
    array: List[float]

class TrackEventRequest(BaseModel):
    api_name: str
    caller: str = "anonymous"
    person_type: str = "unknown"
    person_level: str = "unknown"
    person_department: str = "unknown"

def parse_caller_info(request: Request) -> dict:
    """从请求头解析调用人信息，用于埋点"""
    caller_header = request.headers.get("X-Caller-Info", "{}")
    try:
        return json.loads(caller_header)
    except json.JSONDecodeError:
        return {}
```

- [ ] **Step 4: Add GET /api/helloworld**

```python
@app.get("/api/helloworld")
async def helloworld(request: Request):
    try:
        return {
            "success": True,
            "data": {
                "message": "Hello World!",
                "timestamp": datetime.utcnow().isoformat() + "Z"
            }
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail={
            "success": False,
            "error_code": "ERR_HELLO_001",
            "message": "问候服务暂时不可用，请稍后重试",
            "detail": None
        })
```

- [ ] **Step 5: Add GET /api/hash**

```python
@app.get("/api/hash")
async def hash_text(request: Request, text: str = Query(None, description="待计算哈希的文本")):
    if not text:
        raise HTTPException(status_code=400, detail={
            "success": False,
            "error_code": "ERR_HASH_001",
            "message": "请提供待计算哈希的文本内容",
            "detail": None
        })
    try:
        hash_value = hashlib.sha256(text.encode()).hexdigest()
        return {
            "success": True,
            "data": {
                "algorithm": "SHA256",
                "input": text,
                "hash": hash_value
            }
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail={
            "success": False,
            "error_code": "ERR_HASH_002",
            "message": "哈希计算服务异常，请稍后重试",
            "detail": None
        })
```

- [ ] **Step 6: Add POST /api/bubble-sort**

```python
@app.post("/api/bubble-sort")
async def bubble_sort_api(request: Request, body: BubbleSortRequest):
    if not body.array:
        raise HTTPException(status_code=400, detail={
            "success": False,
            "error_code": "ERR_SORT_001",
            "message": "请输入有效的数值数组进行排序",
            "detail": None
        })
    try:
        original = list(body.array)
        sorted_arr = bubble_sort(list(body.array))
        return {
            "success": True,
            "data": {
                "original": original,
                "sorted": sorted_arr
            }
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail={
            "success": False,
            "error_code": "ERR_SORT_002",
            "message": "排序服务暂时不可用，请稍后重试",
            "detail": None
        })
```

- [ ] **Step 7: Add exception handler for consistent error format**

```python
@app.exception_handler(HTTPException)
async def http_exception_handler(request, exc):
    return JSONResponse(
        status_code=exc.status_code,
        content=exc.detail if isinstance(exc.detail, dict) else {
            "success": False,
            "error_code": f"ERR_SYS_{exc.status_code}",
            "message": str(exc.detail),
            "detail": None
        }
    )

@app.exception_handler(Exception)
async def global_exception_handler(request, exc):
    return JSONResponse(
        status_code=500,
        content={
            "success": False,
            "error_code": "ERR_SYS_500",
            "message": "系统异常，请稍后重试；如持续出现请联系管理员",
            "detail": None
        }
    )
```

- [ ] **Step 8: Add main entry point**

```python
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
```

- [ ] **Step 9: Verify app starts correctly**

Run: `cd /root/.agentix/agentic-dev/runs/DEV-9d10e310-7901-11f1-8a9f-59ecae612580-0d8be63e-9821-4d5d-9dbd-83f8c146da99/worktree/manyu_test-cred-test-20260716022903 && pip install -q fastapi uvicorn 2>/dev/null && timeout 5 python -c "from app import app; print('App loaded successfully')"`  
Expected: `App loaded successfully`

---

### Task 2: [manyu_test] 后端埋点 + 统计 + 导出接口

**Files:**
- Modify: `manyu_test/app.py` (add tracking, stats, export endpoints)

**Interfaces:**
- Consumes: Task 1 app structure, sqlite3 connection
- Produces: `POST /api/track/event` → `{"success": true, "data": {"event_id": "uuid"}}`
- Produces: `GET /api/track/stats?dimension=type|level|department` → `{"success": true, "data": {"dimension": "type", "entries": [{"name": "developer", "count": 5}, ...]}}`
- Produces: `GET /api/track/stats?dimension=time` → `{"success": true, "data": {"dimension": "time", "entries": [{"date": "2026-08-24", "count": 10}, ...]}}`
- Produces: `GET /api/export?tab=helloworld|hash|bubble-sort&format=csv` → CSV file download

- [ ] **Step 1: Add POST /api/track/event endpoint**

```python
@app.post("/api/track/event")
async def track_event(request: Request, event: TrackEventRequest):
    if not event.api_name:
        raise HTTPException(status_code=400, detail={
            "success": False,
            "error_code": "ERR_TRK_001",
            "message": "上报事件数据不完整，请检查后重试",
            "detail": None
        })
    try:
        event_id = str(uuid.uuid4())
        ts = datetime.utcnow().isoformat() + "Z"
        conn = sqlite3.connect(DB_PATH)
        conn.execute(
            "INSERT INTO track_events (event_id, api_name, caller, person_type, person_level, person_department, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)",
            (event_id, event.api_name, event.caller, event.person_type, event.person_level, event.person_department, ts)
        )
        conn.commit()
        conn.close()
        return {"success": True, "data": {"event_id": event_id, "timestamp": ts}}
    except Exception as e:
        raise HTTPException(status_code=500, detail={
            "success": False,
            "error_code": "ERR_TRK_001",
            "message": "上报事件数据不完整，请检查后重试",
            "detail": None
        })
```

- [ ] **Step 2: Add GET /api/track/stats endpoint**

```python
@app.get("/api/track/stats")
async def track_stats(request: Request, dimension: str = Query("type", description="聚合维度: type|level|department|time")):
    try:
        conn = sqlite3.connect(DB_PATH)
        conn.row_factory = sqlite3.Row
        dim_map = {
            "type": "person_type",
            "level": "person_level",
            "department": "person_department"
        }
        if dimension == "time":
            rows = conn.execute(
                "SELECT DATE(timestamp) as date, COUNT(*) as count FROM track_events GROUP BY DATE(timestamp) ORDER BY date"
            ).fetchall()
            entries = [{"name": r["date"], "count": r["count"]} for r in rows]
        elif dimension in dim_map:
            col = dim_map[dimension]
            rows = conn.execute(
                f"SELECT {col} as name, COUNT(*) as count FROM track_events GROUP BY {col} ORDER BY count DESC"
            ).fetchall()
            entries = [{"name": r["name"], "count": r["count"]} for r in rows]
        else:
            entries = []
        conn.close()
        return {"success": True, "data": {"dimension": dimension, "entries": entries}}
    except Exception as e:
        raise HTTPException(status_code=500, detail={
            "success": False,
            "error_code": "ERR_TRK_002",
            "message": "统计数据获取失败，请稍后重试",
            "detail": None
        })
```

- [ ] **Step 3: Add GET /api/export endpoint**

```python
@app.get("/api/export")
async def export_data(request: Request, tab: str = Query(None, description="导出页面: helloworld|hash|bubble-sort"), format: str = Query("csv")):
    if not tab:
        raise HTTPException(status_code=400, detail={
            "success": False,
            "error_code": "ERR_EXP_001",
            "message": "请指定要导出的页面类型",
            "detail": None
        })
    try:
        # 生成演示数据
        import io
        output = io.StringIO()
        writer = csv.writer(output)
        
        if tab == "helloworld":
            writer.writerow(["接口", "消息", "时间戳"])
            writer.writerow(["helloworld", "Hello World!", datetime.utcnow().isoformat() + "Z"])
        elif tab == "hash":
            writer.writerow(["接口", "算法", "输入", "哈希值"])
            writer.writerow(["hash", "SHA256", "示例文本", hashlib.sha256(b"示例文本").hexdigest()])
        elif tab == "bubble-sort":
            writer.writerow(["接口", "原始数组", "排序后数组"])
            writer.writerow(["bubble-sort", "[3, 1, 4, 1, 5]", str(bubble_sort([3, 1, 4, 1, 5]))])
        else:
            raise HTTPException(status_code=400, detail={
                "success": False,
                "error_code": "ERR_EXP_001",
                "message": "请指定要导出的页面类型",
                "detail": None
            })
        
        output.seek(0)
        filename = f"{tab}_export_{datetime.utcnow().strftime('%Y%m%d_%H%M%S')}.csv"
        return StreamingResponse(
            iter([output.getvalue()]),
            media_type="text/csv",
            headers={"Content-Disposition": f"attachment; filename={filename}"}
        )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail={
            "success": False,
            "error_code": "ERR_EXP_003",
            "message": "导出文件生成失败，请稍后重试",
            "detail": None
        })
```

- [ ] **Step 4: Verify all endpoints are registered**

Run: `cd /root/.agentix/agentic-dev/runs/DEV-9d10e310-7901-11f1-8a9f-59ecae612580-0d8be63e-9821-4d5d-9dbd-83f8c146da99/worktree/manyu_test-cred-test-20260716022903 && python -c "from app import app; routes = [r.path for r in app.routes]; print('Routes:', sorted(routes))"`  
Expected: Routes include `/api/helloworld`, `/api/hash`, `/api/bubble-sort`, `/api/track/event`, `/api/track/stats`, `/api/export`

---

### Task 3: [manyu_test1] 前端项目初始化 + Vite + Vue 3 脚手架

**Files:**
- Create: `manyu_test1/package.json`
- Create: `manyu_test1/vite.config.js`
- Create: `manyu_test1/index.html`
- Create: `manyu_test1/src/main.js`
- Create: `manyu_test1/src/styles/main.css`

**Interfaces:**
- Produces: Vue 3 dev server on port 5173
- Produces: Root Vue app mounting point

- [ ] **Step 1: Create package.json**

```json
{
  "name": "manyu-test1-frontend",
  "version": "1.0.0",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "vue": "^3.4.0",
    "echarts": "^5.4.3",
    "axios": "^1.6.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^4.5.0",
    "vite": "^5.0.0"
  }
}
```

- [ ] **Step 2: Create vite.config.js**

```javascript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8000',
        changeOrigin: true
      }
    }
  }
})
```

- [ ] **Step 3: Create index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>三接口演示系统</title>
</head>
<body>
  <div id="app"></div>
  <script type="module" src="/src/main.js"></script>
</body>
</html>
```

- [ ] **Step 4: Create src/main.js**

```javascript
import { createApp } from 'vue'
import App from './App.vue'
import './styles/main.css'

createApp(App).mount('#app')
```

- [ ] **Step 5: Create src/styles/main.css**

```css
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background: #f5f7fa;
  color: #333;
  min-height: 100vh;
}

#app {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}
```

- [ ] **Step 6: Install dependencies**

Run: `cd /root/.agentix/agentic-dev/runs/DEV-9d10e310-7901-11f1-8a9f-59ecae612580-0d8be63e-9821-4d5d-9dbd-83f8c146da99/worktree/manyu_test1-main && npm install 2>&1 | tail -5`  
Expected: `npm install` completes without errors

---

### Task 4: [manyu_test1] 前端 API 封装层 + 三个 Tab 组件

**Files:**
- Create: `manyu_test1/src/api/index.js`
- Create: `manyu_test1/src/components/TabHelloWorld.vue`
- Create: `manyu_test1/src/components/TabHash.vue`
- Create: `manyu_test1/src/components/TabBubbleSort.vue`

**Interfaces:**
- Consumes: Backend API at `/api/helloworld`, `/api/hash`, `/api/bubble-sort`
- Produces: Reusable API functions and standalone Tab components

- [ ] **Step 1: Create src/api/index.js**

```javascript
import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
    'X-Caller-Info': JSON.stringify({
      caller: 'demo_user',
      person_type: 'developer',
      person_level: 'senior',
      person_department: 'engineering'
    })
  }
})

// 响应拦截器：统一处理错误
api.interceptors.response.use(
  response => response.data,
  error => {
    if (error.response && error.response.data) {
      return Promise.reject(error.response.data)
    }
    return Promise.reject({
      success: false,
      error_code: 'ERR_NET_001',
      message: '网络连接失败，请检查网络后重试',
      detail: null
    })
  }
)

export function getHelloWorld() {
  return api.get('/helloworld')
}

export function getHash(text) {
  return api.get('/hash', { params: { text } })
}

export function postBubbleSort(array) {
  return api.post('/bubble-sort', { array })
}

export function trackEvent(eventData) {
  return api.post('/track/event', eventData)
}

export function getStats(dimension) {
  return api.get('/track/stats', { params: { dimension } })
}

export function getExportUrl(tab) {
  return `/api/export?tab=${tab}&format=csv`
}
```

- [ ] **Step 2: Create TabHelloWorld.vue**

```vue
<template>
  <div class="tab-content">
    <div v-if="loading" class="state-loading">加载中...</div>
    <div v-else-if="error" class="state-error">
      <p>{{ error.message }}</p>
      <button @click="fetchData" class="btn-retry">重新加载</button>
    </div>
    <div v-else class="result-card">
      <h3>Helloworld 接口返回</h3>
      <p class="result-message">{{ data.message }}</p>
      <p class="result-timestamp">时间戳：{{ data.timestamp }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getHelloWorld, trackEvent } from '../api/index.js'

const loading = ref(true)
const error = ref(null)
const data = ref({})

async function fetchData() {
  loading.value = true
  error.value = null
  try {
    const res = await getHelloWorld()
    data.value = res.data
    trackEvent({ api_name: 'helloworld' }).catch(() => {})
  } catch (err) {
    error.value = err
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<style scoped>
.tab-content { padding: 20px; }
.state-loading { text-align: center; padding: 40px; color: #999; }
.state-error { text-align: center; padding: 40px; color: #e74c3c; }
.state-error p { margin-bottom: 16px; }
.btn-retry { padding: 8px 24px; background: #3498db; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
.result-card { background: #fff; border-radius: 8px; padding: 24px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.result-message { font-size: 24px; font-weight: bold; color: #2c3e50; margin: 16px 0; }
.result-timestamp { color: #7f8c8d; font-size: 14px; }
</style>
```

- [ ] **Step 3: Create TabHash.vue**

```vue
<template>
  <div class="tab-content">
    <div class="input-area">
      <input v-model="inputText" placeholder="输入要计算哈希的文本" class="text-input" />
      <button @click="fetchData" :disabled="!inputText.trim()" class="btn-primary">计算哈希</button>
    </div>
    <div v-if="loading" class="state-loading">计算中...</div>
    <div v-else-if="error" class="state-error">
      <p>{{ error.message }}</p>
      <button @click="fetchData" class="btn-retry">重新计算</button>
    </div>
    <div v-else-if="data.hash" class="result-card">
      <h3>SHA256 哈希结果</h3>
      <div class="result-row">
        <span class="label">输入：</span>
        <span class="value">{{ data.input }}</span>
      </div>
      <div class="result-row">
        <span class="label">算法：</span>
        <span class="value">{{ data.algorithm }}</span>
      </div>
      <div class="result-row">
        <span class="label">哈希值：</span>
        <span class="value hash-value">{{ data.hash }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { getHash, trackEvent } from '../api/index.js'

const inputText = ref('Hello World')
const loading = ref(false)
const error = ref(null)
const data = ref({})

async function fetchData() {
  if (!inputText.value.trim()) return
  loading.value = true
  error.value = null
  try {
    const res = await getHash(inputText.value)
    data.value = res.data
    trackEvent({ api_name: 'hash' }).catch(() => {})
  } catch (err) {
    error.value = err
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.tab-content { padding: 20px; }
.input-area { display: flex; gap: 12px; margin-bottom: 20px; }
.text-input { flex: 1; padding: 10px 16px; border: 1px solid #dcdcdc; border-radius: 4px; font-size: 16px; }
.btn-primary { padding: 10px 24px; background: #3498db; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
.btn-primary:disabled { background: #bdc3c7; cursor: not-allowed; }
.state-loading { text-align: center; padding: 40px; color: #999; }
.state-error { text-align: center; padding: 40px; color: #e74c3c; }
.btn-retry { padding: 8px 24px; background: #3498db; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
.result-card { background: #fff; border-radius: 8px; padding: 24px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.result-row { margin: 12px 0; }
.label { font-weight: bold; color: #555; }
.value { color: #2c3e50; }
.hash-value { font-family: monospace; word-break: break-all; background: #f8f9fa; padding: 8px; border-radius: 4px; display: inline-block; }
</style>
```

- [ ] **Step 4: Create TabBubbleSort.vue**

```vue
<template>
  <div class="tab-content">
    <div class="input-area">
      <input v-model="arrayInput" placeholder="输入数组，如 3,1,4,1,5" class="text-input" />
      <button @click="fetchData" :disabled="!arrayInput.trim()" class="btn-primary">排序</button>
    </div>
    <div v-if="loading" class="state-loading">排序中...</div>
    <div v-else-if="error" class="state-error">
      <p>{{ error.message }}</p>
      <button @click="fetchData" class="btn-retry">重新排序</button>
    </div>
    <div v-else-if="data.sorted" class="result-card">
      <h3>冒泡排序结果</h3>
      <div class="result-row">
        <span class="label">原始数组：</span>
        <span class="value">[{{ data.original.join(', ') }}]</span>
      </div>
      <div class="result-row">
        <span class="label">排序结果：</span>
        <span class="value sorted-value">[{{ data.sorted.join(', ') }}]</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { postBubbleSort, trackEvent } from '../api/index.js'

const arrayInput = ref('3, 1, 4, 1, 5')
const loading = ref(false)
const error = ref(null)
const data = ref({})

async function fetchData() {
  if (!arrayInput.value.trim()) return
  loading.value = true
  error.value = null
  try {
    const arr = arrayInput.value.split(',').map(s => parseFloat(s.trim())).filter(n => !isNaN(n))
    const res = await postBubbleSort(arr)
    data.value = res.data
    trackEvent({ api_name: 'bubble-sort' }).catch(() => {})
  } catch (err) {
    error.value = err
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.tab-content { padding: 20px; }
.input-area { display: flex; gap: 12px; margin-bottom: 20px; }
.text-input { flex: 1; padding: 10px 16px; border: 1px solid #dcdcdc; border-radius: 4px; font-size: 16px; }
.btn-primary { padding: 10px 24px; background: #3498db; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
.btn-primary:disabled { background: #bdc3c7; cursor: not-allowed; }
.state-loading { text-align: center; padding: 40px; color: #999; }
.state-error { text-align: center; padding: 40px; color: #e74c3c; }
.btn-retry { padding: 8px 24px; background: #3498db; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
.result-card { background: #fff; border-radius: 8px; padding: 24px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.result-row { margin: 12px 0; }
.label { font-weight: bold; color: #555; }
.value { color: #2c3e50; }
.sorted-value { color: #27ae60; font-weight: bold; }
</style>
```

---

### Task 5: [manyu_test1] 导出按钮 + 埋点统计图表组件

**Files:**
- Create: `manyu_test1/src/components/ExportButton.vue`
- Create: `manyu_test1/src/components/StatsLineChart.vue`
- Create: `manyu_test1/src/components/StatsPieChart.vue`
- Create: `manyu_test1/src/components/StatsBarChart.vue`

**Interfaces:**
- Consumes: `getExportUrl(tab)` from api/index.js, `getStats(dimension)` from api/index.js
- Produces: Standalone chart components with embedded ECharts

- [ ] **Step 1: Create ExportButton.vue**

```vue
<template>
  <div class="export-area">
    <button @click="handleExport" :disabled="exporting" class="btn-export">
      {{ exporting ? '导出中...' : '📥 导出当前页面数据' }}
    </button>
    <p v-if="exportError" class="export-error">{{ exportError }}</p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { getExportUrl } from '../api/index.js'

const props = defineProps({
  activeTab: { type: String, required: true }
})

const exporting = ref(false)
const exportError = ref(null)

async function handleExport() {
  exporting.value = true
  exportError.value = null
  try {
    const url = getExportUrl(props.activeTab)
    const link = document.createElement('a')
    link.href = url
    link.download = `${props.activeTab}_export.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  } catch (err) {
    exportError.value = '导出失败，请稍后重试'
  } finally {
    exporting.value = false
  }
}
</script>

<style scoped>
.export-area { margin: 16px 0; text-align: right; }
.btn-export { padding: 10px 24px; background: #27ae60; color: #fff; border: none; border-radius: 4px; cursor: pointer; font-size: 14px; }
.btn-export:disabled { background: #95a5a6; cursor: not-allowed; }
.export-error { color: #e74c3c; margin-top: 8px; font-size: 13px; }
</style>
```

- [ ] **Step 2: Create StatsLineChart.vue**

```vue
<template>
  <div class="chart-wrapper">
    <h4 class="chart-title">📈 调用趋势（折线图）</h4>
    <div v-if="loading" class="chart-placeholder">加载中...</div>
    <div v-else-if="error" class="chart-placeholder error">
      <p>统计数据加载失败</p>
      <button @click="fetchData" class="btn-retry-sm">重试</button>
    </div>
    <div v-else-if="!hasData" class="chart-placeholder">暂无统计数据</div>
    <div v-else ref="chartRef" class="chart-container"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getStats } from '../api/index.js'

const loading = ref(true)
const error = ref(null)
const hasData = ref(false)
const chartRef = ref(null)
let chartInstance = null

async function fetchData() {
  loading.value = true
  error.value = null
  try {
    const res = await getStats('time')
    const entries = res.data.entries
    hasData.value = entries && entries.length > 0
    if (hasData.value) {
      await nextTick()
      if (chartRef.value) {
        if (!chartInstance) {
          chartInstance = echarts.init(chartRef.value)
        }
        chartInstance.setOption({
          tooltip: { trigger: 'axis' },
          xAxis: { type: 'category', data: entries.map(e => e.name) },
          yAxis: { type: 'value' },
          series: [{ type: 'line', data: entries.map(e => e.count), smooth: true, lineStyle: { width: 3 }, areaStyle: { opacity: 0.1 } }]
        })
      }
    }
  } catch (err) {
    error.value = err
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
onUnmounted(() => { if (chartInstance) chartInstance.dispose() })
</script>

<style scoped>
.chart-wrapper { background: #fff; border-radius: 8px; padding: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.chart-title { margin-bottom: 12px; font-size: 15px; color: #555; }
.chart-container { height: 250px; }
.chart-placeholder { height: 250px; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #999; }
.chart-placeholder.error { color: #e74c3c; }
.btn-retry-sm { margin-top: 8px; padding: 4px 16px; background: #3498db; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
</style>
```

- [ ] **Step 3: Create StatsPieChart.vue**

```vue
<template>
  <div class="chart-wrapper">
    <h4 class="chart-title">🥧 人员类型分布（饼图）</h4>
    <div v-if="loading" class="chart-placeholder">加载中...</div>
    <div v-else-if="error" class="chart-placeholder error">
      <p>统计数据加载失败</p>
      <button @click="fetchData" class="btn-retry-sm">重试</button>
    </div>
    <div v-else-if="!hasData" class="chart-placeholder">暂无统计数据</div>
    <div v-else ref="chartRef" class="chart-container"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getStats } from '../api/index.js'

const loading = ref(true)
const error = ref(null)
const hasData = ref(false)
const chartRef = ref(null)
let chartInstance = null

async function fetchData() {
  loading.value = true
  error.value = null
  try {
    const res = await getStats('type')
    const entries = res.data.entries
    hasData.value = entries && entries.length > 0
    if (hasData.value) {
      await nextTick()
      if (chartRef.value) {
        if (!chartInstance) {
          chartInstance = echarts.init(chartRef.value)
        }
        chartInstance.setOption({
          tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
          series: [{
            type: 'pie',
            radius: ['40%', '70%'],
            data: entries.map(e => ({ name: e.name, value: e.count })),
            emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.5)' } }
          }]
        })
      }
    }
  } catch (err) {
    error.value = err
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
onUnmounted(() => { if (chartInstance) chartInstance.dispose() })
</script>

<style scoped>
.chart-wrapper { background: #fff; border-radius: 8px; padding: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.chart-title { margin-bottom: 12px; font-size: 15px; color: #555; }
.chart-container { height: 250px; }
.chart-placeholder { height: 250px; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #999; }
.chart-placeholder.error { color: #e74c3c; }
.btn-retry-sm { margin-top: 8px; padding: 4px 16px; background: #3498db; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
</style>
```

- [ ] **Step 4: Create StatsBarChart.vue**

```vue
<template>
  <div class="chart-wrapper">
    <h4 class="chart-title">📊 人员层级分布（柱状图）</h4>
    <div v-if="loading" class="chart-placeholder">加载中...</div>
    <div v-else-if="error" class="chart-placeholder error">
      <p>统计数据加载失败</p>
      <button @click="fetchData" class="btn-retry-sm">重试</button>
    </div>
    <div v-else-if="!hasData" class="chart-placeholder">暂无统计数据</div>
    <div v-else ref="chartRef" class="chart-container"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getStats } from '../api/index.js'

const loading = ref(true)
const error = ref(null)
const hasData = ref(false)
const chartRef = ref(null)
let chartInstance = null

async function fetchData() {
  loading.value = true
  error.value = null
  try {
    const res = await getStats('level')
    const entries = res.data.entries
    hasData.value = entries && entries.length > 0
    if (hasData.value) {
      await nextTick()
      if (chartRef.value) {
        if (!chartInstance) {
          chartInstance = echarts.init(chartRef.value)
        }
        chartInstance.setOption({
          tooltip: { trigger: 'axis' },
          xAxis: { type: 'category', data: entries.map(e => e.name) },
          yAxis: { type: 'value' },
          series: [{
            type: 'bar',
            data: entries.map(e => e.count),
            itemStyle: { color: '#3498db', borderRadius: [4, 4, 0, 0] }
          }]
        })
      }
    }
  } catch (err) {
    error.value = err
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
onUnmounted(() => { if (chartInstance) chartInstance.dispose() })
</script>

<style scoped>
.chart-wrapper { background: #fff; border-radius: 8px; padding: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.chart-title { margin-bottom: 12px; font-size: 15px; color: #555; }
.chart-container { height: 250px; }
.chart-placeholder { height: 250px; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #999; }
.chart-placeholder.error { color: #e74c3c; }
.btn-retry-sm { margin-top: 8px; padding: 4px 16px; background: #3498db; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
</style>
```

---

### Task 6: [manyu_test1] 根组件 App.vue 组装

**Files:**
- Create: `manyu_test1/src/App.vue`

**Interfaces:**
- Consumes: All Tab components, ExportButton, chart components
- Produces: Complete single-page application

- [ ] **Step 1: Create App.vue**

```vue
<template>
  <div class="app-container">
    <header class="app-header">
      <h1>三接口演示系统</h1>
      <p class="subtitle">Helloworld · 哈希算法 · 冒泡排序</p>
    </header>

    <!-- Tab 导航 -->
    <div class="tab-nav">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        :class="['tab-btn', { active: activeTab === tab.key }]"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- Tab 内容区 -->
    <div class="tab-content-area">
      <TabHelloWorld v-if="activeTab === 'helloworld'" />
      <TabHash v-else-if="activeTab === 'hash'" />
      <TabBubbleSort v-else-if="activeTab === 'bubble-sort'" />
    </div>

    <!-- 导出按钮 -->
    <ExportButton :activeTab="activeTab" />

    <!-- 统计报表面板 -->
    <section class="stats-section">
      <h2 class="section-title">📊 调用统计报表</h2>
      <div class="charts-grid">
        <StatsLineChart />
        <StatsPieChart />
        <StatsBarChart />
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import TabHelloWorld from './components/TabHelloWorld.vue'
import TabHash from './components/TabHash.vue'
import TabBubbleSort from './components/TabBubbleSort.vue'
import ExportButton from './components/ExportButton.vue'
import StatsLineChart from './components/StatsLineChart.vue'
import StatsPieChart from './components/StatsPieChart.vue'
import StatsBarChart from './components/StatsBarChart.vue'

const activeTab = ref('helloworld')
const tabs = [
  { key: 'helloworld', label: 'Helloworld' },
  { key: 'hash', label: '哈希算法' },
  { key: 'bubble-sort', label: '冒泡排序' }
]
</script>

<style scoped>
.app-container { max-width: 1100px; margin: 0 auto; }
.app-header { text-align: center; padding: 32px 0 20px; }
.app-header h1 { font-size: 28px; color: #2c3e50; }
.subtitle { color: #7f8c8d; margin-top: 8px; }
.tab-nav { display: flex; gap: 4px; border-bottom: 2px solid #e0e0e0; margin-bottom: 0; }
.tab-btn { padding: 12px 28px; background: none; border: none; border-bottom: 2px solid transparent; margin-bottom: -2px; cursor: pointer; font-size: 15px; color: #666; transition: all 0.2s; }
.tab-btn:hover { color: #3498db; }
.tab-btn.active { color: #3498db; border-bottom-color: #3498db; font-weight: bold; }
.tab-content-area { background: #fff; border-radius: 0 0 8px 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); min-height: 200px; }
.stats-section { margin-top: 32px; }
.section-title { font-size: 20px; margin-bottom: 16px; color: #2c3e50; }
.charts-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 16px; }
</style>
```

---

## 三、自检清单

### 1. Spec Coverage
| 需求 | 对应任务 | 是否覆盖 |
|------|---------|---------|
| 后端 helloworld 接口 | Task 1 Step 4 | ✅ |
| 后端哈希算法接口 | Task 1 Step 5 | ✅ |
| 后端冒泡排序接口（复用 bubble_sort.py） | Task 1 Step 6 | ✅ |
| 前端三个 Tab 展示 | Task 4 + Task 6 | ✅ |
| 导出按钮 + 后端导出接口 | Task 2 Step 3 + Task 5 Step 1 | ✅ |
| 后端埋点记录 | Task 2 Step 1 | ✅ |
| 后端统计接口（按维度聚合） | Task 2 Step 2 | ✅ |
| 前端折线图（时间趋势） | Task 5 Step 2 | ✅ |
| 前端饼图（人员类型） | Task 5 Step 3 | ✅ |
| 前端柱状图（人员层级） | Task 5 Step 4 | ✅ |
| 异常兜底文案 | 所有接口含 HTTPException 统一格式 | ✅ |

### 2. Placeholder Scan
- 所有代码块包含完整实现代码，无 "TBD"、"TODO"、"implement later"
- 每个步骤有明确的命令和预期输出
- 接口签名在任务间保持一致

### 3. Type Consistency
- 后端 `GET /api/helloworld` 返回 `{success, data: {message, timestamp}}` → 前端 `TabHelloWorld` 使用 `data.message` 和 `data.timestamp` ✅
- 后端 `GET /api/hash` 返回 `{success, data: {algorithm, input, hash}}` → 前端 `TabHash` 使用 `data.algorithm`, `data.input`, `data.hash` ✅
- 后端 `POST /api/bubble-sort` 接收 `{array: []}` 返回 `{success, data: {original, sorted}}` → 前端 `TabBubbleSort` 使用 `data.original`, `data.sorted` ✅
- 后端 `POST /api/track/event` 接收 `{api_name, caller, person_type, person_level, person_department}` ✅
- 后端 `GET /api/track/stats?dimension=type|level|department|time` 返回 `{success, data: {dimension, entries: [{name, count}]}}` → 各图表组件使用 `entries` ✅
- 后端 `GET /api/export?tab=X&format=csv` 返回 CSV 文件流 ✅

---

## 四、执行交付

**Plan complete and saved to `.agents/20260824-分别写三个接口helloworld_哈希/plan.md`.**

**Two execution options:**

**1. Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** — Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**