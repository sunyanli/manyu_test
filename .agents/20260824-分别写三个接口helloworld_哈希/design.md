# 设计文档：三接口 + 前端可视化 + 埋点报表

> 生成时间：2026-08-24  
> 阶段：需求澄清  
> 仓库：`manyu_test`（后端）、`manyu_test1`（前端）

---

## 1. 需求概述

| # | 需求项 | 说明 |
|---|--------|------|
| R1 | 三个后端接口 | HelloWorld、哈希算法、冒泡排序 |
| R2 | 前端页面 | 三个 Tab 分别展示三个接口的执行结果 |
| R3 | 导出功能 | 前端导出按钮 + 后端导出接口，支持导出各页面展示结果 |
| R4 | 埋点 | 后端记录每次接口调用的调用人和调用次数 |
| R5 | 可视化报表 | 前端当前页面展示调用情况，按维度（人员类型、人员层级、人员部门）展示折线图、饼图、柱状图 |

---

## 2. 跨仓依赖与现状摘要

```
┌─────────────────────────────────┐     ┌─────────────────────────────────┐
│         manyu_test (后端)        │     │        manyu_test1 (前端)        │
│  branch: cred-test-20260716022903│     │  branch: main                   │
│                                 │     │                                 │
│  现有资产:                       │     │  现有资产:                       │
│  ├── bubble_sort.py (145行)     │     │  └── README.md (空壳)            │
│  │   ├── bubble_sort()          │     │                                 │
│  │   ├── bubble_sort_optimized()│     │  待建设:                         │
│  │   └── bubble_sort_descending│     │  ├── React/Vue SPA              │
│  └── cred-helper-test.txt       │     │  ├── Tab 页面 (3个Tab)          │
│                                 │     │  ├── 导出按钮                    │
│  待建设:                         │     │  └── 图表可视化 (ECharts)        │
│  ├── Flask/FastAPI Web 服务     │     │                                 │
│  ├── /api/helloworld            │     │                                 │
│  ├── /api/hash                  │     │                                 │
│  ├── /api/bubblesort (复用现有)  │     │                                 │
│  ├── /api/export                │     │                                 │
│  ├── /api/tracking/stats        │     │                                 │
│  ├── 埋点中间件                   │     │                                 │
│  └── SQLite 追踪数据库           │     │                                 │
└─────────────────────────────────┘     └─────────────────────────────────┘
```

**关键发现**：
- `manyu_test` 已有成熟的 `bubble_sort.py`（含标准版、优化版、降序版 + 完整测试），可直接作为 `/api/bubblesort` 的核心算法层
- 两个仓库均为近乎空仓，需从零搭建 Web 框架和前端应用
- 无现有用户系统 → 埋点所需的"人员类型/层级/部门"需通过请求头或 JWT Token 传入

---

## 3. 技术选型决策

| 决策项 | 推荐方案 | 理由 |
|--------|----------|------|
| 后端框架 | **FastAPI (Python)** | 与现有 `bubble_sort.py` 同语言，自动生成 OpenAPI 文档，异步支持好 |
| 前端框架 | **React + TypeScript** | 组件化适合 Tab 切换，ECharts 集成成熟 |
| 图表库 | **ECharts** | 原生支持折线图、饼图、柱状图，中文文档完善 |
| 数据存储 | **SQLite** | 轻量级，零配置，适合埋点场景 |
| 导出格式 | **CSV** | 简单通用，前后端实现成本低 |
| 用户信息传递 | **请求头 `X-User-*`** | 简化版方案，无需完整认证系统 |

> **待确认**: 以上选型为推荐方案，如有偏好请指定。

---

## 4. 后端 API 设计 (`manyu_test`)

### 4.1 接口清单

| 方法 | 路径 | 说明 | 请求体/参数 |
|------|------|------|------------|
| GET | `/api/helloworld` | 返回 HelloWorld 字符串 | `?name=World`（可选） |
| POST | `/api/hash` | 哈希算法 | `{"algorithm":"sha256","input":"hello"}` |
| POST | `/api/bubblesort` | 冒泡排序 | `{"array":[5,3,8,4,2],"order":"asc"}` |
| GET | `/api/export` | 导出结果 | `?type=helloworld\|hash\|bubblesort` |
| GET | `/api/tracking/stats` | 埋点统计 | `?dimension=person_type\|level\|department` |

### 4.2 接口详细设计

#### 4.2.1 GET /api/helloworld

```
请求:
  GET /api/helloworld?name=World

响应 200:
{
  "code": 0,
  "data": {
    "message": "Hello, World!",
    "timestamp": "2026-08-24T10:00:00Z"
  }
}
```

#### 4.2.2 POST /api/hash

```
请求:
  POST /api/hash
  Content-Type: application/json
  {
    "algorithm": "sha256",    // 支持: sha256, md5, sha1
    "input": "hello world"
  }

响应 200:
{
  "code": 0,
  "data": {
    "algorithm": "sha256",
    "input": "hello world",
    "hash": "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9"
  }
}
```

#### 4.2.3 POST /api/bubblesort

```
请求:
  POST /api/bubblesort
  Content-Type: application/json
  {
    "array": [5, 3, 8, 4, 2],
    "order": "asc",           // asc | desc
    "variant": "standard"     // standard | optimized | descending
  }

响应 200:
{
  "code": 0,
  "data": {
    "input": [5, 3, 8, 4, 2],
    "output": [2, 3, 4, 5, 8],
    "variant": "standard",
    "order": "asc",
    "comparisons": 10,
    "swaps": 6
  }
}
```

#### 4.2.4 GET /api/export

```
请求:
  GET /api/export?type=bubblesort&format=csv

响应 200:
  Content-Type: text/csv
  Content-Disposition: attachment; filename="bubblesort_export.csv"

  input,output,variant,order,timestamp
  "[5,3,8,4,2]","[2,3,4,5,8]",standard,asc,2026-08-24T10:00:00Z
```

#### 4.2.5 GET /api/tracking/stats

```
请求:
  GET /api/tracking/stats?dimension=person_type

响应 200:
{
  "code": 0,
  "data": {
    "dimension": "person_type",
    "items": [
      {"label": "正式员工", "count": 45},
      {"label": "外包", "count": 12},
      {"label": "实习生", "count": 8}
    ],
    "total": 65
  }
}
```

### 4.3 埋点中间件设计

```
请求流入 → 埋点中间件(TrackingMiddleware)
              │
              ├── 提取请求头: X-User-Id, X-User-Name, X-User-Type,
              │               X-User-Level, X-User-Department
              ├── 记录到 SQLite: tracking_records 表
              │     (api_path, user_id, user_name, user_type,
              │      user_level, user_department, timestamp)
              └── 透传请求到业务 handler
```

### 4.4 数据模型

```sql
-- SQLite 数据库: tracking.db

CREATE TABLE tracking_records (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    api_path TEXT NOT NULL,
    user_id TEXT NOT NULL DEFAULT 'anonymous',
    user_name TEXT NOT NULL DEFAULT 'anonymous',
    user_type TEXT NOT NULL DEFAULT 'unknown',       -- 人员类型: 正式员工/外包/实习生
    user_level TEXT NOT NULL DEFAULT 'unknown',      -- 人员层级: P6/P7/P8/M1/M2
    user_department TEXT NOT NULL DEFAULT 'unknown',  -- 人员部门: 技术部/产品部/运营部
    request_body TEXT,
    response_status INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_api_path ON tracking_records(api_path);
CREATE INDEX idx_user_type ON tracking_records(user_type);
CREATE INDEX idx_user_level ON tracking_records(user_level);
CREATE INDEX idx_user_department ON tracking_records(user_department);
CREATE INDEX idx_created_at ON tracking_records(created_at);
```

### 4.5 目录结构

```
manyu_test/
├── bubble_sort.py            # [现有] 冒泡排序算法
├── server.py                 # [新建] FastAPI 入口
├── requirements.txt          # [新建] Python 依赖
├── api/
│   ├── __init__.py
│   ├── helloworld.py         # HelloWorld 接口
│   ├── hash_api.py           # 哈希接口
│   ├── bubblesort_api.py     # 冒泡排序接口（包装现有 bubble_sort.py）
│   └── export.py             # 导出接口
├── middleware/
│   ├── __init__.py
│   └── tracking.py           # 埋点中间件
├── models/
│   ├── __init__.py
│   └── tracking.py           # 数据模型 & DB 操作
├── tracking.db               # [自动生成] SQLite 数据库
└── cred-helper-test.txt      # [现有]
```

---

## 5. 前端设计 (`manyu_test1`)

### 5.1 页面结构

```
┌──────────────────────────────────────────────────────────────┐
│  Header: "算法演示平台"                        [导出 CSV] 按钮 │
├──────────────────────────────────────────────────────────────┤
│  ┌─────────┬──────────┬──────────┐                           │
│  │ Tab 1   │  Tab 2   │  Tab 3   │  ← 三个 Tab              │
│  │HelloWorld│ 哈希算法  │ 冒泡排序  │                           │
│  └─────────┴──────────┴──────────┘                           │
│                                                              │
│  ┌──────────────────────────────────────────────────────────┐│
│  │                                                          ││
│  │  当前 Tab 内容区:                                         ││
│  │  - 输入表单 (参数输入)                                     ││
│  │  - 执行按钮                                               ││
│  │  - 结果展示区                                              ││
│  │                                                          ││
│  └──────────────────────────────────────────────────────────┘│
│                                                              │
│  ┌──────────────────────────────────────────────────────────┐│
│  │  调用统计报表 (可折叠)                                     ││
│  │  ┌──────────┬──────────┬──────────┐                      ││
│  │  │ 人员类型  │ 人员层级  │ 人员部门  │ ← 维度切换          ││
│  │  └──────────┴──────────┴──────────┘                      ││
│  │  ┌─────────────────────┐ ┌──────────┐ ┌──────────────┐  ││
│  │  │     折线图           │ │  饼图    │ │   柱状图      │  ││
│  │  │   (调用趋势)         │ │ (占比)   │ │  (对比)       │  ││
│  │  └─────────────────────┘ └──────────┘ └──────────────┘  ││
│  └──────────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────┘
```

### 5.2 组件树

```
App
├── Header
│   └── ExportButton          # 导出按钮（随当前 Tab 切换导出内容）
├── TabContainer
│   ├── Tab: HelloWorld
│   │   ├── NameInput         # 输入 name 参数
│   │   ├── ExecuteButton
│   │   └── ResultDisplay
│   ├── Tab: Hash
│   │   ├── AlgorithmSelect   # 选择算法: sha256/md5/sha1
│   │   ├── InputTextArea
│   │   ├── ExecuteButton
│   │   └── ResultDisplay
│   └── Tab: BubbleSort
│       ├── ArrayInput        # 输入数组
│       ├── OrderSelect       # asc/desc
│       ├── VariantSelect     # standard/optimized/descending
│       ├── ExecuteButton
│       └── ResultDisplay
└── TrackingDashboard         # 埋点可视化面板
    ├── DimensionTabs         # 人员类型/层级/部门
    ├── LineChart             # 折线图：调用趋势
    ├── PieChart              # 饼图：占比分布
    └── BarChart              # 柱状图：对比
```

### 5.3 前端目录结构

```
manyu_test1/
├── README.md
├── package.json
├── public/
│   └── index.html
├── src/
│   ├── App.tsx
│   ├── index.tsx
│   ├── api/
│   │   └── client.ts           # 封装 axios/fetch 调用后端
│   ├── components/
│   │   ├── Header.tsx
│   │   ├── ExportButton.tsx
│   │   ├── TabContainer.tsx
│   │   ├── tabs/
│   │   │   ├── HelloWorldTab.tsx
│   │   │   ├── HashTab.tsx
│   │   │   └── BubbleSortTab.tsx
│   │   └── charts/
│   │       ├── LineChart.tsx
│   │       ├── PieChart.tsx
│   │       └── BarChart.tsx
│   └── types/
│       └── index.ts
```

---

## 6. 仓间接口契约（对齐点）

| 契约项 | 后端 (manyu_test) | 前端 (manyu_test1) | 对齐状态 |
|--------|-------------------|---------------------|----------|
| API 基地址 | `http://localhost:8000` | 通过环境变量 `REACT_APP_API_BASE` 配置 | ⚠️ 需对齐 |
| 请求头 - 用户信息 | `X-User-Id`, `X-User-Name`, `X-User-Type`, `X-User-Level`, `X-User-Department` | 前端在请求中携带（从登录态或模拟数据获取） | ⚠️ 需对齐 |
| 响应格式 | `{"code":0,"data":{...}}` 统一封装 | 前端按统一格式解析 | ✅ 已约定 |
| 导出格式 | CSV, `Content-Disposition: attachment` | 前端触发下载 | ✅ 已约定 |
| 埋点统计维度 | `dimension=person_type\|level\|department` | 前端按维度切换请求 | ✅ 已约定 |
| CORS | 后端启用 CORS 允许前端跨域 | 前端无需额外配置 | ⚠️ 需后端实现 |

---

## 7. 待决策问题

以下问题需要在进入开发阶段前明确：

| # | 问题 | 影响范围 | 建议默认值 |
|---|------|----------|-----------|
| Q1 | 前端框架选择：React 还是 Vue？ | manyu_test1 全部代码 | React |
| Q2 | 用户信息从何而来？是否有现有认证系统？ | 埋点中间件、请求头 | 模拟：前端硬编码用户信息到请求头 |
| Q3 | 哈希算法支持范围？是否只要 SHA-256？ | /api/hash | SHA-256, MD5, SHA-1 |
| Q4 | 导出格式：CSV 还是 Excel (xlsx)？ | /api/export | CSV |
| Q5 | 冒泡排序是否需要返回中间步骤（用于可视化排序过程）？ | /api/bubblesort | 仅返回最终结果 + 比较/交换次数 |
| Q6 | 图表展示的是全量历史数据还是近期数据？是否需要时间范围筛选？ | 前端图表 + /api/tracking/stats | 全量 + 按维度聚合 |
| Q7 | 后端部署端口和前端开发端口？ | 两个仓库的配置 | 后端:8000, 前端:3000 |

---

## 8. 实施计划概览

| 步骤 | 仓库 | 内容 | 预估产物 |
|------|------|------|----------|
| S1 | manyu_test | 搭建 FastAPI 骨架 + requirements.txt | `server.py`, `requirements.txt` |
| S2 | manyu_test | 实现 /api/helloworld | `api/helloworld.py` |
| S3 | manyu_test | 实现 /api/hash | `api/hash_api.py` |
| S4 | manyu_test | 实现 /api/bubblesort（复用 bubble_sort.py） | `api/bubblesort_api.py` |
| S5 | manyu_test | 实现埋点中间件 + SQLite 模型 | `middleware/tracking.py`, `models/tracking.py` |
| S6 | manyu_test | 实现 /api/tracking/stats | 在 `api/` 中新增 |
| S7 | manyu_test | 实现 /api/export | `api/export.py` |
| S8 | manyu_test | 集成测试 + 启动验证 | 测试脚本 |
| S9 | manyu_test1 | 搭建 React 项目骨架 | `package.json`, `src/` |
| S10 | manyu_test1 | 实现 Tab 页面（三个 Tab） | `components/tabs/*` |
| S11 | manyu_test1 | 实现导出按钮 | `components/ExportButton.tsx` |
| S12 | manyu_test1 | 实现图表可视化（折线图/饼图/柱状图） | `components/charts/*` |
| S13 | manyu_test1 | 联调验证 | 联调测试 |