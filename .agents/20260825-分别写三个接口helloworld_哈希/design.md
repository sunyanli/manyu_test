# 系统分析设计文档 — 三个接口 + 前端 Tab 页面 + 导出

> 生成时间：2026-07-16  
> 任务节点：系分 (system-analysis-design)  
> 上游依赖：需求澄清 `dima.md` (8 项决策已确认) / 实施计划 `plan.md`  
> 涉及仓库：`manyu_test` (后端), `manyu_test1` (前端)

---

## 一、系统概述

### 1.1 业务目标

构建一个 Web 工具平台，提供三个独立功能：

| 功能 | 后端接口 | 前端展示 |
|------|---------|---------|
| HelloWorld | `GET /api/helloworld` | 展示欢迎消息 |
| 哈希算法 | `POST /api/hash` | 输入文本 + 算法选择 → 展示哈希摘要 |
| 冒泡排序 | `POST /api/bubble_sort` | 输入数组 + 排序方向 → 展示排序结果 |

全局导出按钮支持将当前 Tab 展示结果导出为 JSON 文件下载。

### 1.2 技术栈

| 层 | 技术 | 版本 | 说明 |
|----|------|------|------|
| 后端框架 | FastAPI | latest | 异步 Python Web 框架，自动生成 OpenAPI 文档 |
| ASGI 服务器 | uvicorn | latest | FastAPI 标准生产级服务器 |
| 数据校验 | Pydantic v2 | built-in | FastAPI 内置，请求/响应模型校验 |
| 哈希算法 | hashlib | stdlib | Python 标准库，SHA-256 / MD5 |
| 前端框架 | Vue 3 | latest | Composition API + `<script setup>` |
| 构建工具 | Vite | latest | 快速 HMR，ESM 原生支持 |
| HTTP 客户端 | axios | latest | 请求拦截、Blob 下载 |
| 前端路由 | vue-router | latest | SPA 路由 |
| 跨域 | fastapi.middleware.cors | built-in | 开发阶段全放通 |

---

## 二、架构设计

### 2.1 系统架构图

```
┌─────────────────────────────────────────────────┐
│                   浏览器 (Browser)                │
│  ┌───────────────────────────────────────────┐  │
│  │            Vue 3 SPA (manyu_test1)        │  │
│  │                                           │  │
│  │  ┌─────────────────────────────────────┐  │  │
│  │  │         ToolPage.vue                │  │  │
│  │  │  ┌──────────┬──────────┬──────────┐ │  │  │
│  │  │  │ Tab:     │ Tab:     │ Tab:     │ │  │  │
│  │  │  │ Hello    │ Hash     │ Sort     │ │  │  │
│  │  │  │ World    │          │          │ │  │  │
│  │  │  ├──────────┼──────────┼──────────┤ │  │  │
│  │  │  │ 结果展示  │ 输入+结果 │ 输入+结果 │ │  │  │
│  │  │  └──────────┴──────────┴──────────┘ │  │  │
│  │  │  ┌──────────────────────────────┐   │  │  │
│  │  │  │      [ 📥 导出当前结果 ]       │   │  │  │
│  │  │  └──────────────────────────────┘   │  │  │
│  │  └─────────────────────────────────────┘  │  │
│  │                    │                       │  │
│  │              axios (src/api/index.js)      │  │
│  └────────────────────┼──────────────────────┘  │
└───────────────────────┼─────────────────────────┘
                        │  HTTP/REST
                        │  CORS enabled
                        ▼
┌─────────────────────────────────────────────────┐
│           FastAPI Server (manyu_test)            │
│                                                 │
│  ┌───────────────────────────────────────────┐  │
│  │              main.py                       │  │
│  │  - FastAPI() app instance                 │  │
│  │  - CORSMiddleware                         │  │
│  │  - Router mounting: /api prefix           │  │
│  └───────────────────────────────────────────┘  │
│                      │                          │
│  ┌───────────┬───────┼───────┬───────────────┐  │
│  │           │       │       │               │  │
│  ▼           ▼       ▼       ▼               ▼  │
│  routers/   routers/ routers/ routers/          │
│  helloworld hash     bubble   export            │
│  .py        .py      _sort.py .py               │
│                      │                          │
│                      ▼                          │
│              bubble_sort.py                     │
│              (复用，不改动)                       │
└─────────────────────────────────────────────────┘
```

### 2.2 仓库职责划分

| 仓库 | 角色 | 分支 | 基分支 | 改动策略 |
|------|------|------|--------|----------|
| `manyu_test` | 后端服务 | `AI/task-DEV-...` | `cred-test-20260716022903` | 新增 7 文件，不改动存量 |
| `manyu_test1` | 前端 SPA | `AI/task-DEV-...` | `main` | 项目初始化 + 新增 ~9 文件 |

### 2.3 关键设计决策

| 决策 | 方案 | 理由 |
|------|------|------|
| 后端路由分文件 | 每个接口一个 `routers/*.py` | 单一职责，可独立测试与维护 |
| 冒泡排序复用 | `from bubble_sort import ...` 不修改原文件 | 存量代码零侵入 |
| 前端 Tab 状态 | 每个 Tab 组件独立管理自身状态 | 避免全局状态污染，数据和交互内聚 |
| 导出数据流 | 前端收集当前 Tab 数据 → `POST /api/export` → 后端返回 JSON 文件流 | 后端统一处理文件下载逻辑（Content-Disposition） |
| CORS 策略 | 开发阶段 `allow_origins=["*"]` | 快速联调，生产环境需收紧 |
| 前端 baseURL | axios 实例 `baseURL: "http://localhost:8000"` | 开发环境默认后端地址 |

---

## 三、后端详细设计 (manyu_test)

### 3.1 模块结构

```
manyu_test/
├── main.py                  # FastAPI 入口，挂载路由，启动 uvicorn
├── requirements.txt         # 依赖声明：fastapi, uvicorn
├── bubble_sort.py           # [存量] 冒泡排序算法（不改动）
└── routers/
    ├── __init__.py          # 包声明（空文件）
    ├── helloworld.py        # GET /api/helloworld
    ├── hash.py              # POST /api/hash
    ├── bubble_sort.py       # POST /api/bubble_sort
    └── export.py            # POST /api/export
```

### 3.2 数据模型 (Pydantic Schemas)

#### 3.2.1 HashRequest / HashResponse

```python
# routers/hash.py
from pydantic import BaseModel, Field
from typing import Literal

class HashRequest(BaseModel):
    text: str = Field(..., description="待哈希的文本")
    algorithm: Literal["sha256", "md5"] = Field(..., description="哈希算法")

class HashResponse(BaseModel):
    algorithm: str
    input: str
    hash: str
```

#### 3.2.2 BubbleSortRequest / BubbleSortResponse

```python
# routers/bubble_sort.py
from pydantic import BaseModel, Field
from typing import List, Literal

class BubbleSortRequest(BaseModel):
    array: List[int] = Field(..., description="待排序的整数数组")
    order: Literal["asc", "desc"] = Field(..., description="排序方向")

class BubbleSortResponse(BaseModel):
    original: List[int]
    sorted: List[int]
    order: str
```

#### 3.2.3 ExportRequest

```python
# routers/export.py
from pydantic import BaseModel, Field
from typing import Any, Literal

class ExportRequest(BaseModel):
    tab: Literal["helloworld", "hash", "bubble_sort"] = Field(..., description="来源 Tab")
    data: Any = Field(..., description="待导出的数据")
```

### 3.3 接口详细设计

#### 3.3.1 `GET /api/helloworld`

| 属性 | 值 |
|------|-----|
| 方法 | GET |
| 路径 | `/api/helloworld` |
| 请求体 | 无 |
| 成功响应 | `200 { "message": "Hello World!" }` |
| 错误响应 | 无（无参数，不会失败） |
| 实现文件 | `routers/helloworld.py` |

**处理流程**：

```
Client → GET /api/helloworld → 返回固定 {"message": "Hello World!"}
```

#### 3.3.2 `POST /api/hash`

| 属性 | 值 |
|------|-----|
| 方法 | POST |
| 路径 | `/api/hash` |
| Content-Type | `application/json` |
| 请求体 | `{"text": "<string>", "algorithm": "sha256" \| "md5"}` |
| 成功响应 | `200 {"algorithm": "sha256", "input": "<string>", "hash": "<hex>"}` |
| 校验失败 | `422` (Pydantic 自动校验) |
| 实现文件 | `routers/hash.py` |

**处理流程**：

```
Client → POST /api/hash {text, algorithm}
  → Pydantic 校验 (algorithm 仅允许 sha256/md5)
  → hashlib.sha256(text.encode()).hexdigest() 或 hashlib.md5(...)
  → 返回 HashResponse
```

**边界条件**：
- 空字符串：合法输入，哈希算法正常处理
- 非法 algorithm：FastAPI 自动返回 422

#### 3.3.3 `POST /api/bubble_sort`

| 属性 | 值 |
|------|-----|
| 方法 | POST |
| 路径 | `/api/bubble_sort` |
| Content-Type | `application/json` |
| 请求体 | `{"array": [<int>, ...], "order": "asc" \| "desc"}` |
| 成功响应 | `200 {"original": [<int>, ...], "sorted": [<int>, ...], "order": "asc"}` |
| 校验失败 | `422` |
| 实现文件 | `routers/bubble_sort.py` |

**处理流程**：

```
Client → POST /api/bubble_sort {array, order}
  → Pydantic 校验 (array: list[int], order: Literal["asc","desc"])
  → 复制 original = array[:]
  → order=="asc"  → bubble_sort_optimized(array)
  → order=="desc" → bubble_sort_descending(array)
  → 返回 BubbleSortResponse
```

**模块依赖**：

```
routers/bubble_sort.py
  └── import from bubble_sort (根目录存量文件)
        ├── bubble_sort_optimized()
        └── bubble_sort_descending()
```

**边界条件**：
- 空数组 `[]`：合法，返回空数组
- 单元素 `[42]`：合法，返回相同数组
- 重复元素 `[2,2,2]`：合法，稳定排序
- 负数：`[-3, 0, 5]` 合法

#### 3.3.4 `POST /api/export`

| 属性 | 值 |
|------|-----|
| 方法 | POST |
| 路径 | `/api/export` |
| Content-Type | `application/json` |
| 请求体 | `{"tab": "helloworld" \| "hash" \| "bubble_sort", "data": <any>}` |
| 成功响应 | `200` + `Content-Disposition: attachment; filename="export_{tab}.json"` + JSON 文件流 |
| 校验失败 | `422` |
| 实现文件 | `routers/export.py` |

**处理流程**：

```
Client → POST /api/export {tab, data}
  → 构造 export_payload = {"tab": tab, "data": data, "exported_at": "<ISO timestamp>"}
  → StreamingResponse(iter([json_str]), media_type="application/json",
       headers={"Content-Disposition": f'attachment; filename="export_{tab}.json"'})
  → 浏览器触发下载
```

### 3.4 main.py 设计

```python
# main.py
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from routers import helloworld, hash, bubble_sort, export

app = FastAPI(title="Tool API", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(helloworld.router, prefix="/api", tags=["helloworld"])
app.include_router(hash.router, prefix="/api", tags=["hash"])
app.include_router(bubble_sort.router, prefix="/api", tags=["bubble_sort"])
app.include_router(export.router, prefix="/api", tags=["export"])

@app.get("/")
def root():
    return {"status": "ok", "service": "Tool API"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
```

### 3.5 路由注册表

| Router 模块 | 挂载前缀 | 路由路径 | 完整路径 | 方法 |
|-------------|---------|---------|---------|------|
| `helloworld.router` | `/api` | `/helloworld` | `/api/helloworld` | GET |
| `hash.router` | `/api` | `/hash` | `/api/hash` | POST |
| `bubble_sort.router` | `/api` | `/bubble_sort` | `/api/bubble_sort` | POST |
| `export.router` | `/api` | `/export` | `/api/export` | POST |

---

## 四、前端详细设计 (manyu_test1)

### 4.1 组件树

```
App.vue
 └── <router-view />
      └── ToolPage.vue                    ← 路由 /
           ├── Tab 导航栏 (3 个 Tab)
           │    ├── [HelloWorld] [Hash] [BubbleSort]
           │    └── 当前活跃 Tab 高亮
           ├── Tab 内容区
           │    ├── HelloWorldTab.vue      ← activeTab === 'helloworld'
           │    ├── HashTab.vue            ← activeTab === 'hash'
           │    └── BubbleSortTab.vue      ← activeTab === 'bubble_sort'
           └── 全局导出按钮
                └── [📥 导出当前结果]
```

### 4.2 组件详细设计

#### 4.2.1 ToolPage.vue（页面容器）

**职责**：Tab 切换、导出按钮、协调子组件数据

**状态**：

| 状态 | 类型 | 说明 |
|------|------|------|
| `activeTab` | `"helloworld" \| "hash" \| "bubble_sort"` | 当前活跃 Tab |
| `tabData` | `Record<string, any>` | 各 Tab 的最新结果数据 |

**关键方法**：

| 方法 | 说明 |
|------|------|
| `onTabChange(tab)` | 切换 activeTab |
| `onResultUpdate(tab, data)` | 子组件通知父组件更新结果数据 |
| `onExport()` | 收集 `tabData[activeTab]`，调用 `exportAPI` |

**交互流程**：

```
用户点击 Tab → onTabChange → activeTab 更新 → 对应子组件渲染
用户点击导出 → onExport → 取 tabData[activeTab] → POST /api/export → 触发浏览器下载
```

#### 4.2.2 HelloWorldTab.vue

**UI 布局**：

```
┌──────────────────────────────┐
│  Hello World                 │
│                              │
│  [ 执行 ]                    │
│                              │
│  结果：                      │
│  ┌────────────────────────┐  │
│  │ Hello World!           │  │
│  └────────────────────────┘  │
└──────────────────────────────┘
```

**状态**：

| 状态 | 类型 | 说明 |
|------|------|------|
| `result` | `{message: string} \| null` | 接口返回结果 |
| `loading` | `boolean` | 请求中标志 |

**事件**：

| 事件 | 载荷 | 说明 |
|------|------|------|
| `@update:result` | `{message: string}` | 通知父组件更新导出数据 |

**交互流程**：

```
点击 [执行]
  → loading = true
  → GET /api/helloworld
  → result = response.data
  → emit('update:result', result)
  → loading = false
```

#### 4.2.3 HashTab.vue

**UI 布局**：

```
┌──────────────────────────────┐
│  哈希计算                     │
│                              │
│  输入文本：                   │
│  ┌────────────────────────┐  │
│  │ hello world            │  │
│  └────────────────────────┘  │
│                              │
│  算法：[ sha256 ▼ ]          │
│                              │
│  [ 执行 ]                    │
│                              │
│  结果：                      │
│  ┌────────────────────────┐  │
│  │ 算法: sha256           │  │
│  │ 输入: hello world      │  │
│  │ 哈希: b94d27b9...      │  │
│  └────────────────────────┘  │
└──────────────────────────────┘
```

**状态**：

| 状态 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `text` | `string` | `""` | 输入文本 |
| `algorithm` | `"sha256" \| "md5"` | `"sha256"` | 算法选择 |
| `result` | `HashResponse \| null` | `null` | 接口返回 |
| `loading` | `boolean` | `false` | 请求中 |

**事件**：

| 事件 | 载荷 | 说明 |
|------|------|------|
| `@update:result` | `HashResponse` | 通知父组件 |

**校验**：

- 文本为空时不允许执行（按钮 disabled 或前端提示）

#### 4.2.4 BubbleSortTab.vue

**UI 布局**：

```
┌──────────────────────────────┐
│  冒泡排序                     │
│                              │
│  输入数组（逗号分隔）：        │
│  ┌────────────────────────┐  │
│  │ 5, 3, 8, 4, 2         │  │
│  └────────────────────────┘  │
│                              │
│  排序方向：[ 升序 ▼ ]         │
│                              │
│  [ 执行 ]                    │
│                              │
│  结果：                      │
│  ┌────────────────────────┐  │
│  │ 原始: [5, 3, 8, 4, 2] │  │
│  │ 排序: [2, 3, 4, 5, 8] │  │
│  │ 方向: asc              │  │
│  └────────────────────────┘  │
└──────────────────────────────┘
```

**状态**：

| 状态 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `arrayInput` | `string` | `""` | 逗号分隔的数字字符串 |
| `order` | `"asc" \| "desc"` | `"asc"` | 排序方向 |
| `result` | `BubbleSortResponse \| null` | `null` | 接口返回 |
| `loading` | `boolean` | `false` | 请求中 |

**前端解析**：

```javascript
// 将 "5, 3, 8" 解析为 [5, 3, 8]
const parseArray = (input) => input.split(',').map(s => parseInt(s.trim(), 10));
```

**校验**：

- 输入为空时不允许执行
- 包含非数字时提示错误（前端拦截，减轻后端负担）

### 4.3 API 层设计 (src/api/index.js)

```javascript
// src/api/index.js
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8000',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
});

export const getHelloWorld = () => api.get('/api/helloworld');

export const postHash = (text, algorithm) =>
  api.post('/api/hash', { text, algorithm });

export const postBubbleSort = (array, order) =>
  api.post('/api/bubble_sort', { array, order });

export const postExport = (tab, data) =>
  api.post('/api/export', { tab, data }, { responseType: 'blob' });
```

### 4.4 路由设计

```javascript
// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router';
import ToolPage from '../views/ToolPage.vue';

const routes = [
  { path: '/', name: 'home', component: ToolPage },
];

export default createRouter({
  history: createWebHistory(),
  routes,
});
```

### 4.5 前端文件清单

```
manyu_test1/
├── index.html
├── package.json              # [初始化生成]
├── vite.config.js            # [初始化生成]
└── src/
    ├── main.js               # [初始化生成 + 修改：挂载 router]
    ├── App.vue               # [修改：<router-view />]
    ├── api/
    │   └── index.js          # [新建] axios 封装
    ├── router/
    │   └── index.js          # [新建] 路由配置
    ├── views/
    │   └── ToolPage.vue      # [新建] 页面容器
    └── components/
        ├── HelloWorldTab.vue # [新建] HelloWorld Tab
        ├── HashTab.vue       # [新建] 哈希 Tab
        └── BubbleSortTab.vue # [新建] 冒泡排序 Tab
```

---

## 五、跨仓接口契约

### 5.1 接口契约表

| # | 接口 | 方法 | 完整路径 | 请求体 | 响应体 | 前端调用 |
|---|------|------|---------|--------|--------|----------|
| 1 | helloworld | GET | `/api/helloworld` | 无 | `{"message": "string"}` | `api.get('/api/helloworld')` |
| 2 | hash | POST | `/api/hash` | `{"text":"...","algorithm":"sha256\|md5"}` | `{"algorithm":"...","input":"...","hash":"..."}` | `api.post('/api/hash', {...})` |
| 3 | bubble_sort | POST | `/api/bubble_sort` | `{"array":[...],"order":"asc\|desc"}` | `{"original":[...],"sorted":[...],"order":"..."}` | `api.post('/api/bubble_sort', {...})` |
| 4 | export | POST | `/api/export` | `{"tab":"...","data":...}` | JSON 文件流 (attachment) | `api.post('/api/export', {...}, {responseType:'blob'})` |

### 5.2 对齐检查点

| 检查项 | 预期 | 验证方式 |
|--------|------|----------|
| 字段命名 | 全小写 snake_case (`algorithm`, `order`, `array`, `text`, `tab`, `data`) | 前后端代码审查 |
| 数据类型 | `array` 为 `int[]`，`order` 为 `"asc"\|"desc"` 字符串 | Pydantic 校验 + 前端类型 |
| CORS 允许 | 前端 origin 可跨域 | `curl -H "Origin: http://localhost:5173"` 验证 |
| 导出响应头 | `Content-Disposition: attachment; filename="export_{tab}.json"` | `curl -v` 检查响应头 |
| 导出 Content-Type | `application/json` | `curl -v` 检查 |
| HTTP 状态码 | 200 成功 / 422 校验失败 | 自动化测试 |
| 冒泡排序复用 | 不改动 `bubble_sort.py` | `git diff bubble_sort.py` 为空 |

---

## 六、时序图

### 6.1 HelloWorld 交互时序

```
用户      浏览器(Vue)      后端(FastAPI)
 │           │                │
 │ 点击[执行] │                │
 │──────────>│                │
 │           │ GET /api/helloworld
 │           │───────────────>│
 │           │                │ 返回 {"message":"Hello World!"}
 │           │<───────────────│
 │           │ 更新 result    │
 │           │ emit('update:result')
 │           │ 展示结果        │
 │<──────────│                │
```

### 6.2 Hash 交互时序

```
用户         浏览器(Vue)        后端(FastAPI)
 │              │                   │
 │ 输入文本+算法 │                   │
 │ 点击[执行]   │                   │
 │─────────────>│                   │
 │              │ POST /api/hash    │
 │              │ {text, algorithm} │
 │              │──────────────────>│
 │              │                   │ Pydantic 校验
 │              │                   │ hashlib.sha256/md5
 │              │ 200 {algorithm,   │
 │              │      input, hash} │
 │              │<──────────────────│
 │              │ 更新 result       │
 │              │ emit('update:result')
 │ 展示结果     │                   │
 │<─────────────│                   │
```

### 6.3 导出交互时序

```
用户         浏览器(Vue)             后端(FastAPI)
 │              │                        │
 │ 点击[导出]   │                        │
 │─────────────>│                        │
 │              │ 取 tabData[activeTab]  │
 │              │ POST /api/export       │
 │              │ {tab, data}            │
 │              │───────────────────────>│
 │              │                        │ 构造 JSON payload
 │              │                        │ 添加 exported_at 时间戳
 │              │ 200 Content-Disposition│
 │              │    attachment;         │
 │              │    filename="export_   │
 │              │    helloworld.json"    │
 │              │    (JSON 文件流)       │
 │              │<───────────────────────│
 │              │ 触发浏览器下载          │
 │ 文件下载     │                        │
 │<─────────────│                        │
```

---

## 七、错误处理设计

### 7.1 后端错误处理

| 场景 | HTTP 状态码 | 响应体 | 处理方式 |
|------|-----------|--------|----------|
| 请求体格式错误 | 422 | `{"detail": [{"loc": [...], "msg": "..."}]}` | FastAPI 自动处理 |
| algorithm 非法值 | 422 | `{"detail": [{"loc": ["body","algorithm"], "msg": "..."}]}` | Pydantic Literal 校验 |
| order 非法值 | 422 | 同上 | Pydantic Literal 校验 |
| array 非整数数组 | 422 | 同上 | Pydantic `list[int]` 校验 |
| 未知路由 | 404 | `{"detail": "Not Found"}` | FastAPI 默认 |
| 服务器内部错误 | 500 | `{"detail": "Internal Server Error"}` | FastAPI 默认 |

### 7.2 前端错误处理

| 场景 | 处理方式 |
|------|----------|
| 网络错误（后端未启动） | axios catch → 展示 "无法连接到服务器，请确认后端已启动" |
| 422 校验错误 | 展示后端返回的具体校验错误信息 |
| 请求超时 | axios timeout → 展示 "请求超时，请重试" |
| 导出失败 | 提示 "导出失败，请重试" |
| 前端输入校验失败 | 按钮 disabled + 输入框下方红色提示 |

---

## 八、部署视图

### 8.1 开发环境

```
┌─────────────────────────────────────────┐
│  开发机 (localhost)                       │
│                                         │
│  Terminal 1:                            │
│  $ cd manyu_test                        │
│  $ pip install -r requirements.txt      │
│  $ python main.py                        │
│  → FastAPI @ http://localhost:8000       │
│  → Swagger UI @ http://localhost:8000/docs│
│                                         │
│  Terminal 2:                            │
│  $ cd manyu_test1                        │
│  $ npm install                           │
│  $ npm run dev                           │
│  → Vite @ http://localhost:5173          │
└─────────────────────────────────────────┘
```

### 8.2 启动命令

| 仓库 | 命令 | 端口 |
|------|------|------|
| manyu_test | `pip install -r requirements.txt && python main.py` | 8000 |
| manyu_test1 | `npm install && npm run dev` | 5173 |

---

## 九、设计评审检查清单

| # | 检查项 | 状态 |
|---|--------|------|
| 1 | 接口契约完整且前后端一致 | ✅ |
| 2 | 字段命名统一（snake_case） | ✅ |
| 3 | 数据模型覆盖所有请求/响应 | ✅ |
| 4 | 错误处理覆盖主要异常场景 | ✅ |
| 5 | 冒泡排序复用，不改动存量代码 | ✅ |
| 6 | 前端组件职责单一，状态管理清晰 | ✅ |
| 7 | 导出数据流完整（前端收集 → 后端流式返回） | ✅ |
| 8 | CORS 配置满足开发联调需求 | ✅ |
| 9 | 启动命令和端口明确 | ✅ |
| 10 | 无额外依赖引入（仅 fastapi/uvicorn/axios/vue-router） | ✅ |

---

## 十、下一步

1. ✅ 系统分析设计完成 — 架构、模块、接口、数据流、错误处理全部覆盖
2. ➡️ 进入 **编码执行** 阶段：按 `plan.md` §六 执行顺序实施
3. 编码完成后按 `plan.md` §八 验收标准 7 项逐项验证