# 需求澄清与设计方案

## 一、项目概览

### 技术栈

| 层 | 技术 | 说明 |
|----|------|------|
| 后端 | FastAPI (Python) | 异步 Web 框架，提供 RESTful API |
| 前端 | Vue 3 | 前端 SPA 框架 |
| 可视化 | ECharts / Chart.js | 折线图、饼图、柱状图 |

### 仓库分工

| 仓库 | 角色 | 主要变更 |
|------|------|---------|
| `manyu_test` | 后端 | 新增 API 接口、埋点、导出逻辑 |
| `manyu_test1` | 前端 | 新增页面、Tab 切换、图表展示 |

---

## 二、需求拆解

### 2.1 后端接口（三个）

| 接口 | 路径 | 方法 | 说明 |
|------|------|------|------|
| HelloWorld | `/api/hello` | GET | 返回固定问候语及请求时间 |
| SHA256 哈希 | `/api/hash` | POST | 接收字符串，返回 SHA256 哈希值 |
| 冒泡排序 | `/api/bubble-sort` | POST | 接收整数数组，返回排序结果 |

现有 `bubble_sort.py` 作为工具模块，接口层复用其逻辑。

### 2.2 前端页面

- 新增页面 `/tools`
- 三个 Tab：HelloWorld、SHA256 哈希、冒泡排序
- 每个 Tab 展示对应接口的调用输入和输出结果

### 2.3 导出功能

- 前端「导出」按钮，支持选择格式（CSV / Excel）
- 后端导出 API：`/api/export`
- 参数：`tab`（哪个 Tab 的结果）、`format`（csv / xlsx）
- 导出内容：当前 Tab 下的调用记录

### 2.4 埋点（调用统计）

- 后端埋点字段：`user_id`（登录用户）、`user_name`、`user_type`（人员类型）、`user_level`（人员层级）、`user_department`（部门）、`api_name`、`call_time`
- 埋点存储：SQLite（初期）或数据库表
- 通过登录系统自动识别用户身份

### 2.5 可视化报表

- 调用统计 API：`/api/stats`，支持按维度查询
- 维度：人员类型、人员层级、人员部门
- 图表形式：
  - 折线图：按时间维度展示调用趋势
  - 饼图：按人员类型/部门展示调用分布
  - 柱状图：对比不同维度的调用量

---

## 三、接口契约

### 3.1 后端 API 列表

| 方法 | 路径 | 请求参数 | 响应 |
|------|------|---------|------|
| GET | `/api/hello` | 无 | `{ "message": "Hello World!", "timestamp": "..." }` |
| POST | `/api/hash` | `{ "text": "string" }` | `{ "algorithm": "SHA256", "input": "...", "output": "..." }` |
| POST | `/api/bubble-sort` | `{ "numbers": [int] }` | `{ "sorted": [...], "swaps": int }` |
| GET | `/api/export` | `?tab=hello&format=csv` | 文件流（CSV/Excel） |
| GET | `/api/stats` | `?dimension=user_type` | 统计数据 JSON |

### 3.2 仓间对齐点

- `manyu_test` → `manyu_test1`: API 响应格式、字段命名
- 埋点数据模型：前端需展示 user_type / user_level / user_department 字段

---

## 四、数据流

```
[用户] → [Vue 3 前端] → [FastAPI 后端] → [bubble_sort.py 模块]
                             ↓
                        [埋点记录] → [统计 API] → [前端图表]
                        [导出 API] → [文件下载]
```

---

## 五、待实施步骤（后续节点）

| 步骤 | 内容 |
|------|------|
| 1 | 后端：搭建 FastAPI 项目，实现三个基础接口 |
| 2 | 后端：实现埋点中间件和数据存储 |
| 3 | 后端：实现导出 API（CSV + Excel） |
| 4 | 后端：实现统计 API |
| 5 | 前端：初始化 Vue 3 项目，搭建页面框架 |
| 6 | 前端：三个 Tab 组件开发 |
| 7 | 前端：导出按钮对接 |
| 8 | 前端：图表可视化（折线图/饼图/柱状图） |
| 9 | 联调与测试 |