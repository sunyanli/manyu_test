# 三接口演示 + 埋点可视化 设计文档

> 版本: v1.0 | 日期: 2026-09-01 | 状态: 设计阶段

---

## 1. 概述

构建一个演示平台，包含三个后端计算接口（helloworld、哈希算法、冒泡排序），前端提供三 Tab 页面展示执行结果，支持导出和调用埋点可视化报表。

### 仓库分工

| 仓库 | 角色 | 技术栈 |
|------|------|--------|
| `manyu_test` | 后端 | Python 3 + FastAPI + SQLite |
| `manyu_test1` | 前端 | 原生 HTML/JS + ECharts (CDN) |

---

## 2. 架构总览

```
┌─────────────────────────────────────────────────────┐
│  manyu_test1 (前端 - 原生 HTML/JS)                    │
│  ┌───────────┐ ┌──────────┐ ┌───────────────────┐   │
│  │ 三 Tab 页  │ │ 导出按钮  │ │ 可视化报表 (ECharts)│   │
│  │ helloworld │ │          │ │ 折线/饼/柱状图      │   │
│  │ hash       │ │          │ │ 维度: 类型/层级/部门│   │
│  │ bubble-sort│ │          │ │                   │   │
│  └─────┬─────┘ └────┬─────┘ └────────┬──────────┘   │
└────────┼────────────┼───────────────┼───────────────┘
         │            │               │
    HTTP │ 请求       │               │
         ▼            ▼               ▼
┌─────────────────────────────────────────────────────┐
│  manyu_test (后端 - FastAPI)                          │
│  ┌──────────────────────────────────────────────┐    │
│  │  埋点中间件 (TrackingMiddleware)               │    │
│  │  Header: X-User-Id/Name/Dept/Level/Type      │    │
│  └────────────────────┬─────────────────────────┘    │
│  ┌────────────────────┼─────────────────────────┐    │
│  │  API 路由层                                    │    │
│  │  /api/helloworld  /api/hash  /api/bubble-sort │    │
│  │  /api/export/{type}  /api/analytics           │    │
│  └────────────────────┼─────────────────────────┘    │
│  ┌────────────────────┼─────────────────────────┐    │
│  │  SQLite (tracking.db)                         │    │
│  │  表: api_call_logs                             │    │
│  └──────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────┘
```

### 数据流

1. **计算调用**：前端 Tab 输入 → POST `/api/{name}` → 中间件提取用户 Header → 执行业务逻辑 → 写入埋点日志 → 返回结果
2. **导出**：前端点击导出 → GET `/api/export/{type}` → 查询该类型历史记录 → 返回 CSV 文件流
3. **报表查询**：前端请求报表 → GET `/api/analytics?dimension=X` → SQL 聚合查询 → 返回维度聚合数据 → ECharts 渲染

---

## 3. 后端设计 (`manyu_test`)

### 3.1 项目结构

```
manyu_test/
├── main.py                  # FastAPI 入口，注册路由和中间件
├── apis/
│   ├── __init__.py
│   ├── helloworld.py        # helloworld 接口
│   ├── hash_api.py          # 哈希算法接口
│   └── bubble_sort.py       # 复用现有 bubble_sort.py，封装为 API
├── middleware/
│   ├── __init__.py
│   └── tracking.py          # 埋点中间件
├── models/
│   ├── __init__.py
│   └── tracking.py          # SQLite 数据模型
├── export/
│   ├── __init__.py
│   └── csv_writer.py        # CSV 导出逻辑
├── bubble_sort.py           # 现有冒泡排序实现（保留不变）
└── requirements.txt
```

### 3.2 API 契约

#### 3.2.1 POST /api/helloworld

- **请求体**：`{}`（空或任意）
- **响应**：`{"message": "Hello, World!", "timestamp": "2026-09-01T12:00:00Z"}`
- **错误码**：无，始终返回 200

#### 3.2.2 POST /api/hash

- **请求体**：`{"text": "abc"}`
- **响应**：`{"algorithm": "SHA256", "input": "abc", "hash": "ba7816bf..."}`
- **校验**：`text` 必填，非空字符串；否则返回 422
- **错误**：422 `{"detail": "text 字段不能为空"}`

#### 3.2.3 POST /api/bubble-sort

- **请求体**：`{"numbers": [5, 3, 8, 4, 2]}`
- **响应**：`{"original": [5, 3, 8, 4, 2], "sorted": [2, 3, 4, 5, 8], "algorithm": "bubble_sort"}`
- **校验**：`numbers` 必填，非空数组，元素均为数字；否则返回 422
- **错误**：422 `{"detail": "numbers 必须是非空数字数组"}`

#### 3.2.4 GET /api/export/{type}

- **路径参数**：`type` ∈ `{helloworld, hash, bubble-sort}`
- **响应**：`Content-Type: text/csv`，`Content-Disposition: attachment; filename="helloworld_export.csv"`
- **CSV 列**：`caller_name, dept, level, user_type, api_name, timestamp`
- **错误**：非法 type → 400

#### 3.2.5 GET /api/analytics

- **查询参数**：
  - `dimension` ∈ `{dept, level, user_type}`（必填）
  - `api_name`（可选，筛选特定接口）
- **响应**：
  ```json
  {
    "dimension": "dept",
    "data": [
      {"label": "技术部", "count": 42},
      {"label": "产品部", "count": 18}
    ]
  }
  ```
- **错误**：非法 dimension → 400

### 3.3 埋点中间件

**用户身份来源**（Header 透传）：

| Header | 含义 | 示例 |
|--------|------|------|
| `X-User-Id` | 用户唯一标识 | `u001` |
| `X-User-Name` | 用户名 | `张三` |
| `X-User-Dept` | 部门 | `技术部` |
| `X-User-Level` | 层级 | `P6` |
| `X-User-Type` | 人员类型 | `正式员工` |

**记录逻辑**：对 `/api/helloworld`、`/api/hash`、`/api/bubble-sort` 三个接口，在请求结束后异步写入 SQLite：

```sql
CREATE TABLE api_call_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    api_name TEXT NOT NULL,
    caller_id TEXT,
    caller_name TEXT,
    dept TEXT,
    level TEXT,
    user_type TEXT,
    called_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**设计要点**：
- 异步写入，不阻塞 API 响应
- Header 缺失时字段为 NULL，不拒绝请求
- 导出和 analytics 接口自身不计入埋点

### 3.4 依赖

```
fastapi>=0.100.0
uvicorn>=0.23.0
```

SQLite 使用 Python 标准库 `sqlite3`，无需额外依赖。

---

## 4. 前端设计 (`manyu_test1`)

### 4.1 项目结构

```
manyu_test1/
├── index.html           # 主页面（三 Tab + 导出 + 报表）
├── css/
│   └── style.css        # 样式
├── js/
│   ├── app.js           # 主逻辑（Tab 切换、API 调用、导出）
│   └── charts.js        # ECharts 图表渲染
└── README.md
```

### 4.2 页面布局

```
┌──────────────────────────────────────────────────┐
│  三接口演示平台                                     │
├──────────────────────────────────────────────────┤
│  [helloworld] [哈希算法] [冒泡排序]    [导出 CSV]   │
├──────────────────────────────────────────────────┤
│  Tab 内容区（输入框 + 结果展示）                     │
│  ┌────────────────────────────────────────────┐   │
│  │ 输入: [___________]  [执行]                  │   │
│  │ 结果:                                       │   │
│  │ ┌──────────────────────────────────────┐    │   │
│  │ │  API 返回结果...                       │    │   │
│  │ └──────────────────────────────────────┘    │   │
│  └────────────────────────────────────────────┘   │
├──────────────────────────────────────────────────┤
│  调用统计报表                                       │
│  维度: [人员类型 ▾] [人员层级] [人员部门]             │
│  图表: [折线图] [饼图] [柱状图]                      │
│  ┌────────────────────────────────────────────┐   │
│  │         ECharts 图表区域                     │   │
│  └────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────┘
```

### 4.3 Tab 交互

| Tab | 输入项 | 按钮 | 结果展示 |
|-----|--------|------|----------|
| helloworld | 无（直接执行） | "执行" | 显示返回的 greeting 消息 |
| 哈希算法 | 文本输入框 | "计算哈希" | 显示 SHA256 哈希值 |
| 冒泡排序 | 数字输入框（逗号分隔） | "排序" | 显示原始数组和排序结果 |

### 4.4 导出

- 导出按钮根据当前激活的 Tab，调用 `/api/export/{type}`
- 后端返回 CSV 文件流，前端触发浏览器下载
- 导出按钮文字随 Tab 切换变化："导出 helloworld 记录" / "导出哈希记录" / "导出排序记录"

### 4.5 可视化报表

**维度切换**：单选按钮组，切换时重新请求 `/api/analytics?dimension=X`

**图表类型切换**：折线图、饼图、柱状图，切换时用同一份数据以不同图表形式渲染

**ECharts 配置要点**：
- 折线图：`type: 'line'`，X 轴为维度标签，Y 轴为调用次数
- 饼图：`type: 'pie'`，以 label 为扇区名，count 为值
- 柱状图：`type: 'bar'`，X 轴为维度标签，Y 轴为调用次数

### 4.6 用户模拟

页面顶部提供用户信息模拟区，允许手动设置 Header 值（Name、Dept、Level、Type），以便演示不同维度下的埋点效果。

---

## 5. 仓间对齐点

| 对齐项 | manyu_test (后端) | manyu_test1 (前端) |
|--------|-------------------|---------------------|
| 用户身份 | 从 Header 读取 | 前端模拟区写入 Header |
| API 契约 | 严格按 3.2 节定义 | 按契约调用，处理错误 |
| 导出格式 | CSV，Content-Disposition 触发下载 | 通过 `<a>` 下载或 Blob |
| 维度枚举 | `dept`, `level`, `user_type` | 前端用相同枚举值 |
| CORS | 后端配置允许跨域 | 前端 fetch 跨域请求 |

---

## 6. 错误处理策略

| 场景 | 后端行为 | 前端行为 |
|------|----------|----------|
| 参数校验失败 | 返回 422 + detail | 显示红色错误提示 |
| 接口不存在 | 返回 404 | 显示"接口未找到" |
| 服务内部错误 | 返回 500 | 显示"服务异常，请稍后重试" |
| 网络不通 | - | 显示"网络连接失败" |
| Header 缺失 | 埋点字段为 NULL | 不影响 API 调用 |

---

## 7. 测试策略

### 后端

- **单元测试**：三个计算接口独立测试（pytest）
- **集成测试**：FastAPI TestClient 模拟 Header 请求，验证埋点写入
- **冒烟测试**：启动服务，curl 调用三个接口，检查 SQLite 记录

### 前端

- **功能测试**：手动验证 Tab 切换、API 调用、导出下载、图表渲染
- **边界测试**：空输入、非法输入、大数组排序

---

## 8. 已确认项 ✅

| # | 项目 | 确认值 |
|---|------|--------|
| 1 | 后端服务端口 | `8000` |
| 2 | CORS 策略 | 允许所有来源 (`*`) |
| 3 | 导出文件名 | `{type}_export.csv` |
| 4 | SQLite 文件路径 | `manyu_test/tracking.db` |
| 5 | 前端用户模拟区 | 需要，支持预设用户快速切换 |