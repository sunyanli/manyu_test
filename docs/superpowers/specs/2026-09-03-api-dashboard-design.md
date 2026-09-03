# API 接口 + 可视化面板 设计文档

> 状态：已确认  
> 日期：2026-09-03  
> 涉及仓库：manyu_test（后端）、manyu_test1（前端）

---

## 1. 概述

在 manyu_test 仓库搭建 FastAPI 后端，提供三个业务接口（helloworld / 哈希算法 / 冒泡排序）、导出接口、埋点统计接口；在 manyu_test1 仓库搭建 React 前端，提供三 Tab 执行页面、CSV/Excel 导出、以及多维度埋点可视化报表。

---

## 2. 技术栈

| 层 | 技术 | 仓库 |
|----|------|------|
| 后端 | Python FastAPI | manyu_test |
| 前端 | React | manyu_test1 |
| 图表 | Chart.js + react-chartjs-2 | manyu_test1 |
| 导出 | csv 模块 + openpyxl | manyu_test |
| 哈希 | hashlib（标准库） | manyu_test |

---

## 3. 后端设计（manyu_test）

### 3.1 目录结构

```
manyu_test/
├── main.py                  # FastAPI 入口，挂载路由与 CORS 中间件
├── routers/
│   ├── business.py          # /api/helloworld, /api/hash, /api/bubblesort
│   ├── export.py            # /api/export/{tab}
│   └── tracking.py          # /api/tracking/stats
├── services/
│   ├── hash_service.py      # 哈希计算（SHA-256 / MD5 / SHA-1）
│   ├── sort_service.py      # 封装 bubble_sort.py
│   └── export_service.py    # 生成 CSV / Excel
├── middleware/
│   └── tracking.py          # 埋点中间件：解析请求头 → 记录调用
├── models/
│   └── tracking.py          # 埋点数据模型（内存存储）
└── bubble_sort.py           # 现有代码，不动
```

### 3.2 API 契约

#### 3.2.1 业务接口

| 方法 | 路径 | 请求体 | 响应 |
|------|------|--------|------|
| GET | `/api/helloworld` | — | `{"message": "Hello, World!"}` |
| POST | `/api/hash` | `{"text": "abc", "algorithm": "sha256"}` | `{"algorithm": "sha256", "input": "abc", "result": "ba7816bf..."}` |
| POST | `/api/bubblesort` | `{"array": [5, 3, 8, 4, 2]}` | `{"original": [5,3,8,4,2], "result": [2,3,4,5,8]}` |

- `/api/hash` 的 `algorithm` 字段可选值：`"sha256"` / `"md5"` / `"sha1"`，默认 `"sha256"`
- `/api/bubblesort` 复用现有 `bubble_sort.py` 的 `bubble_sort` 函数

#### 3.2.2 导出接口

| 方法 | 路径 | 参数 | 响应 |
|------|------|------|------|
| GET | `/api/export/{tab}` | `?format=csv` 或 `?format=xlsx` | 文件流（Content-Disposition: attachment） |

- `tab` 可选值：`"helloworld"` / `"hash"` / `"bubblesort"`
- 导出内容为对应 Tab 最近一次执行结果

#### 3.2.3 埋点统计接口

| 方法 | 路径 | 响应 |
|------|------|------|
| GET | `/api/tracking/stats` | 按维度聚合的调用统计数据 |

响应结构：
```json
{
  "total_calls": 150,
  "by_endpoint": {
    "helloworld": 50,
    "hash": 60,
    "bubblesort": 40
  },
  "by_user_type": { "正式员工": 100, "外包": 50 },
  "by_user_level": { "P5": 30, "P6": 60, "P7": 40, "P8": 20 },
  "by_user_dept": { "技术部": 80, "产品部": 40, "运营部": 30 },
  "timeline": [
    {"datetime": "2026-09-03T10:00:00", "count": 15},
    {"datetime": "2026-09-03T10:05:00", "count": 22}
  ]
}
```

### 3.3 埋点中间件

- 拦截路径：`/api/helloworld`、`/api/hash`、`/api/bubblesort`
- 从请求头提取维度：
  - `X-User-Type` → 人员类型
  - `X-User-Level` → 人员层级
  - `X-User-Dept` → 人员部门
- 写入内存存储（字典），按接口路径 + 维度 + 时间戳聚合计数
- 导出接口和统计接口本身不埋点

### 3.4 错误处理

- 哈希接口：`algorithm` 不合法返回 400 `{"error": "Unsupported algorithm: xxx"}`
- 冒泡排序：`array` 不是合法数组返回 400
- 导出：`tab` 不合法返回 400
- 全局异常处理返回 500

---

## 4. 前端设计（manyu_test1）

### 4.1 目录结构

```
manyu_test1/
├── public/
│   └── index.html
├── src/
│   ├── App.jsx                    # 路由入口（/dashboard, /tracking）
│   ├── pages/
│   │   ├── Dashboard.jsx          # 三 Tab 主页面
│   │   └── TrackingReport.jsx     # 埋点报表页
│   ├── components/
│   │   ├── HelloworldTab.jsx      # Tab 1：展示 helloworld 结果
│   │   ├── HashTab.jsx            # Tab 2：输入文本 + 选择算法 → 展示哈希
│   │   ├── BubblesortTab.jsx      # Tab 3：输入数组 → 展示排序结果
│   │   ├── ExportButton.jsx       # 下拉选择 CSV/Excel → 触发下载
│   │   └── charts/
│   │       ├── LineChart.jsx      # 折线图（时间趋势）
│   │       ├── PieChart.jsx       # 饼图（类型/层级/部门分布）
│   │       └── BarChart.jsx       # 柱状图（接口调用量对比）
│   ├── services/
│   │   └── api.js                 # axios 实例，统一 baseURL + 请求头注入
│   └── config.js                  # API_BASE_URL, mock 用户信息
├── package.json
└── ...
```

### 4.2 路由

| 路径 | 组件 | 说明 |
|------|------|------|
| `/dashboard` | Dashboard.jsx | 默认页，三 Tab 执行面板 |
| `/tracking` | TrackingReport.jsx | 埋点可视化报表 |

### 4.3 请求头注入

`api.js` 中 axios 拦截器统一注入：
```
X-User-Type: 正式员工
X-User-Level: P6
X-User-Dept: 技术部
```
默认 mock 值存入 `config.js`，后续可替换为真实 Token 解析。

### 4.4 导出流程

1. 用户在 `ExportButton` 下拉中选择 CSV 或 Excel
2. 调用 `GET /api/export/{currentTab}?format=csv|xlsx`
3. 后端返回文件流，前端触发浏览器下载

### 4.5 可视化报表

`TrackingReport.jsx`：顶部维度筛选器（人员类型 / 层级 / 部门 / 全部），下方三个图表卡片：

- **折线图**：时间维度调用趋势（每小时/每天聚合）
- **饼图**：按维度分布（支持切换类型/层级/部门）
- **柱状图**：三个接口调用量对比

---

## 5. 数据流

```
[前端] 用户操作 → axios (注入请求头) → [后端] FastAPI
                                           ├── 路由层 → 服务层 → 响应
                                           └── 埋点中间件 → 内存存储

[前端] 报表页 → GET /api/tracking/stats → 后端返回聚合数据 → Chart.js 渲染
[前端] 导出按钮 → GET /api/export/{tab} → 后端生成 CSV/Excel → 文件下载
```

---

## 6. 测试策略

| 层 | 测试内容 | 方式 |
|----|---------|------|
| 后端服务 | hash_service / sort_service 单元测试 | pytest |
| 后端路由 | 各接口请求/响应 + 错误码 | FastAPI TestClient |
| 后端埋点 | 中间件计数聚合正确性 | pytest |
| 前端组件 | 各 Tab 渲染、导出按钮交互 | 手动验证（当前阶段） |

---

## 7. 仓间对齐点

| 对齐项 | manyu_test | manyu_test1 |
|--------|-----------|-------------|
| API 路径 | 提供 `/api/*` 接口 | 通过 `config.js` 的 `API_BASE_URL` 对接 |
| 请求头 Key | 约定 `X-User-Type/Level/Dept` | 统一注入这三个 Header |
| 导出格式 | 支持 `?format=csv\|xlsx` | 仅发送这两种值 |
| Tab 标识 | `helloworld` / `hash` / `bubblesort` | 导出时传当前 Tab 标识 |
| CORS | 后端配置允许前端 origin | — |