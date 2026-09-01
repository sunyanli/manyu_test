# API 接口 + 埋点追踪 + 可视化报表 设计文档

> 日期: 2026-09-01
> 状态: 设计完成，待用户审阅

---

## 1. 概述

为 [manyu_test] 后端新增三个业务接口（HelloWorld、哈希算法、冒泡排序），同时实现埋点追踪和导出功能；为 [manyu_test1] 前端新增单页面，包含三 Tab 结果展示、导出下载、以及调用情况可视化报表。

---

## 2. 仓库分工

| 仓库 | 角色 | 技术栈 |
|------|------|--------|
| [manyu_test] | 后端 | Python 3 + Flask |
| [manyu_test1] | 前端 | 原生 HTML + JS + Chart.js CDN |

---

## 3. 架构图

```
┌─────────────────────────────────────────────────────┐
│  [manyu_test1] 前端 (index.html)                     │
│  ┌───────────┐ ┌──────────┐ ┌───────────────────┐  │
│  │ 身份输入区 │ │ 3-Tab 页  │ │ 埋点报表 (图表)    │  │
│  │(姓名/类型/ │ │HelloWorld │ │ 折线/饼图/柱状图   │  │
│  │ 层级/部门) │ │Hash/排序  │ │ 按维度切换        │  │
│  └───────────┘ └──────────┘ └───────────────────┘  │
│                     │  HTTP JSON                    │
└─────────────────────┼───────────────────────────────┘
                      │
┌─────────────────────┼───────────────────────────────┐
│  [manyu_test] Flask 后端                             │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐            │
│  │/helloworld│ │/hash     │ │/bubblesort│           │
│  └──────────┘ └──────────┘ └──────────┘            │
│  ┌──────────────────────────────────────┐           │
│  │ 埋点中间件 (before_request)            │           │
│  │ 记录: 调用人/时间/接口/参数            │           │
│  │ 存储: 内存 dict                       │           │
│  └──────────────────────────────────────┘           │
│  ┌──────────┐ ┌──────────┐                         │
│  │/export   │ │/tracking │                         │
│  └──────────┘ └──────────┘                         │
└─────────────────────────────────────────────────────┘
```

---

## 4. 后端 API 设计 [manyu_test]

### 4.1 接口列表

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/helloworld` | GET | 返回 "Hello, World!" |
| `/api/hash` | POST | 哈希计算（支持 sha256/md5/sha1） |
| `/api/bubblesort` | POST | 冒泡排序 |
| `/api/export` | GET | 导出 CSV |
| `/api/tracking` | GET | 埋点聚合数据 |

### 4.2 接口详情

#### `/api/helloworld`
- **方法**: GET
- **请求**: 无参数
- **响应**: `{"result": "Hello, World!"}`

#### `/api/hash`
- **方法**: POST
- **请求体**: `{"algorithm": "sha256|md5|sha1", "text": "要哈希的文本"}`
- **响应**: `{"algorithm": "sha256", "input": "要哈希的文本", "hash": "abc123..."}`
- **错误**: 400 不支持的算法

#### `/api/bubblesort`
- **方法**: POST
- **请求体**: `{"array": [5, 3, 8, 4, 2]}`
- **响应**: `{"input": [5, 3, 8, 4, 2], "sorted": [2, 3, 4, 5, 8], "steps": 10}`
- **错误**: 400 数组格式无效

#### `/api/export`
- **方法**: GET
- **参数**: `?type=helloworld|hash|bubblesort`
- **响应**: CSV 文件流（`Content-Disposition: attachment`）
- **CSV 列**: 调用人(name/type/level/dept)、时间戳、接口、参数、结果

#### `/api/tracking`
- **方法**: GET
- **参数**: `?dimension=type|level|dept` 或 `?dimension=time`（时间序列）
- **维度聚合响应**: `{"dimension": "type", "data": [{"key": "正式员工", "count": 15}, ...], "summary": {"total": 42}}`
- **时间序列响应**: `{"dimension": "time", "data": [{"time": "2026-09-01T10:00:00", "count": 5}, ...], "summary": {"total": 42}}`（按小时聚合，用于折线图）

### 4.3 埋点中间件

- 使用 Flask `before_request` 钩子
- 从请求体/Header 提取调用人信息：`name`, `type`, `level`, `dept`
- 记录字段：`name`, `type`, `level`, `dept`, `endpoint`, `timestamp`, `params_summary`
- 存储：全局 `list` 字典（内存存储，重启丢失）
- 仅对 `/api/` 前缀的请求埋点，排除 `/api/tracking` 自身

### 4.4 文件结构

```
[manyu_test]
├── app.py                  # Flask 主入口，路由注册
├── bubble_sort.py          # 已有，冒泡排序实现
├── middleware/
│   └── tracking.py         # 埋点中间件 + 数据存储
├── routes/
│   ├── helloworld.py       # /api/helloworld
│   ├── hash.py             # /api/hash
│   ├── bubblesort.py       # /api/bubblesort
│   ├── export.py           # /api/export
│   └── tracking.py         # /api/tracking
└── requirements.txt        # flask
```

---

## 5. 前端设计 [manyu_test1]

### 5.1 文件结构

```
[manyu_test1]
└── index.html              # 单文件，包含所有 HTML/CSS/JS
```

### 5.2 页面布局

#### 区域① — 身份输入栏（顶部固定）
- 姓名：文本输入框（必填）
- 人员类型：下拉选择（正式员工/外包/实习生）
- 人员层级：下拉选择（初级/中级/高级/专家）
- 人员部门：下拉选择（技术部/产品部/运营部/市场部）

#### 区域② — 三 Tab 结果区
- **Tab 1 - HelloWorld**: 点击"执行"按钮 → 调用 `/api/helloworld` → 显示返回结果
- **Tab 2 - Hash**: 算法下拉(sha256/md5/sha1) + 文本输入框 + "执行"按钮 → 调用 `/api/hash` → 显示哈希结果
- **Tab 3 - 冒泡排序**: 数组输入框（逗号分隔）+ "执行"按钮 → 调用 `/api/bubblesort` → 显示排序结果和步数

#### 区域③ — 导出按钮
- 每个 Tab 内独立"导出"按钮
- 点击后调用 `/api/export?type=xxx`，触发浏览器下载 CSV

#### 区域④ — 埋点报表区
- 维度切换：人员类型 / 人员层级 / 人员部门
- 图表切换：折线图 / 饼图 / 柱状图
- 调用 `/api/tracking?dimension=xxx` 获取数据
- 使用 Chart.js CDN 渲染

### 5.3 外部依赖

```html
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
```

---

## 6. 数据流

```
用户填写身份 → 前端存储身份信息
     │
     ├─ 执行接口调用 → POST/GET /api/xxx (Header 携带 name/type/level/dept)
     │       │
     │       ├─ Flask before_request 中间件自动记录埋点
     │       └─ 路由处理 → 返回 JSON → 前端展示结果
     │
     ├─ 导出 → GET /api/export?type=xxx → CSV 流 → 浏览器下载
     │
     └─ 查看报表 → GET /api/tracking?dimension=xxx → 聚合数据 → Chart.js 渲染
```

---

## 7. 边界与约束

- 埋点数据存储在内存中，服务重启后丢失
- 前端所有请求通过 Header 传递身份信息（`X-User-Name`, `X-User-Type`, `X-User-Level`, `X-User-Dept`）
- 无 CORS 问题的开发环境假设（前后端同域，或 Flask 启用 CORS）
- 不使用数据库，零外部服务依赖

---

## 8. 未决事项

无。所有设计决策已在本轮澄清中确认。