# 需求澄清与设计方案

## 概述

基于需求"分别写三个接口helloworld、哈希算法以及冒泡排序；前端新增页面展示；导出功能；埋点统计与可视化报表"进行需求澄清与设计。

**确认技术栈**: Python Flask + 原生HTML/JS/CSS

---

## 一、需求澄清结果

### 已确认项

| 条目 | 确认结果 |
|------|---------|
| 技术栈 | Python Flask（后端）+ 原生HTML/JS/CSS（前端） |
| 后端接口数 | 3个：helloworld、哈希算法（SHA256 + MD5 双算法）、冒泡排序 |
| 仓库分配 | `manyu_test` = 后端仓库，`manyu_test1` = 前端仓库 |
| 前端页面 | 单页面，3个Tab分别展示各接口执行结果 |
| 导出功能 | 前端导出按钮 + 后端导出接口，支持导出两种内容：API调用结果 + 埋点统计数据，格式为 CSV |
| 埋点统计 | 记录调用次数和调用人，含人员类型/层级/部门维度 |
| 可视化报表 | 折线图、饼图、柱状图，维度：人员类型、人员层级、人员部门 |
| 调用人识别 | 页面输入用户名（简单方案，无登录认证） |
| 存储方式 | 内存存储（重启丢失可接受） |
| 前端可视化库 | ECharts 5.x CDN 引入 |
| 导出格式 | CSV |
| 哈希算法 | SHA256 + MD5 双算法 |
| 冒泡排序 | 复用已有 `bubble_sort.py`，包装为 Flask 接口 |

---

## 二、系统架构设计

### 整体架构

```
┌─────────────────────────────────────────────────┐
│                  浏览器 (前端)                    │
│  ┌──────────┐  ┌──────────┐  ┌───────────────┐  │
│  │ Tab 页面  │  │ 导出按钮  │  │ 报表可视化页面 │  │
│  │ (3个Tab)  │  │          │  │ (折线/饼/柱状图)│  │
│  └─────┬─────┘  └────┬─────┘  └───────┬───────┘  │
└────────┼──────────────┼────────────────┼──────────┘
         │              │                │
         ▼              ▼                ▼
┌─────────────────────────────────────────────────┐
│              Flask 后端 (manyu_test)              │
│  ┌──────────┐  ┌──────────┐  ┌───────────────┐  │
│  │ API 接口  │  │ 导出接口  │  │ 埋点统计接口   │  │
│  │ /api/*   │  │ /api/export│  │ /api/stats/*  │  │
│  └─────┬─────┘  └────┬─────┘  └───────┬───────┘  │
│        │             │                │          │
│        ▼             ▼                ▼          │
│  ┌──────────┐  ┌──────────┐  ┌───────────────┐  │
│  │ 算法模块  │  │ 导出模块  │  │ 埋点数据存储   │  │
│  │ (3个接口) │  │ (CSV)    │  │ (内存)        │  │
│  └──────────┘  └──────────┘  └───────────────┘  │
└─────────────────────────────────────────────────┘
```

### 模块划分

#### 后端模块（manyu_test）

| 模块 | 文件 | 职责 |
|------|------|------|
| API 路由 | `app.py` | Flask 应用入口，注册所有路由 |
| HelloWorld | `hello_world.py` | HelloWorld 接口实现 |
| 哈希算法 | `hash_algo.py` | 哈希算法接口实现（SHA256/MD5） |
| 冒泡排序 | `sort_api.py` | 包装已有 `bubble_sort.py` 为接口 |
| 埋点模块 | `tracking.py` | 调用次数、调用人记录的埋点逻辑 |
| 统计模块 | `tracking.py` | 统计数据聚合与查询（与埋点同文件） |
| 导出模块 | `export.py` | 导出接口实现（CSV） |

#### 前端模块（manyu_test1）

| 文件 | 职责 |
|------|------|
| `index.html` | 主页面，包含3个Tab + 导出按钮 + 报表区域 + 用户信息输入 |
| `style.css` | 样式文件 |
| `app.js` | 前端逻辑，API调用、Tab切换、图表渲染 |

---

## 三、接口设计

### 3.1 业务接口

#### GET /api/hello
- 描述: HelloWorld 接口
- 查询参数: `caller`, `user_type`, `user_level`, `department`（埋点用）
- 响应: `{ "message": "Hello World!", "timestamp": "..." }`

#### POST /api/hash
- 描述: 哈希算法接口
- 请求体: `{ "input": "待哈希字符串", "algorithm": "sha256", "caller": "...", "user_type": "...", "user_level": "...", "department": "..." }`
- 响应: `{ "input": "...", "algorithm": "sha256", "hash": "..." }`

#### POST /api/sort
- 描述: 冒泡排序接口
- 请求体: `{ "data": [5, 3, 8, 4, 2], "caller": "...", "user_type": "...", "user_level": "...", "department": "..." }`
- 响应: `{ "original": [...], "sorted": [...], "algorithm": "bubble_sort" }`

### 3.2 导出接口

#### 后端导出：GET /api/export?tab=hello
- 描述: 导出指定Tab页面的**埋点统计数据**（方案 C：后端仅导出埋点数据）
- 参数: `tab`: hello/hash/sort/all，不传则导出全部
- 响应: CSV 文件下载（Content-Type: text/csv）
- 示例: `GET /api/export?tab=hello` → 导出 HelloWorld 接口的埋点统计记录

#### 前端导出：API 调用结果（浏览器端生成 CSV）
- 描述: 各 Tab 的 API 调用结果由前端 JavaScript 从页面展示数据直接生成 CSV 并下载
- 实现方式: 前端 `app.js` 中读取当前 Tab 的 result-area 展示内容，组装为 CSV 后通过 Blob 下载
- 优势: 后端无需额外存储 API 结果数据，减轻服务器负担

### 3.3 埋点统计接口

#### POST /api/track
- 描述: 手动记录调用埋点
- 请求体: `{ "api": "/api/hello", "caller": "username", "user_type": "developer", "user_level": "senior", "department": "engineering" }`
- 响应: `{ "status": "ok", "id": "uuid" }`

#### GET /api/stats/overview
- 描述: 获取统计数据概览
- 响应: `{ "total_calls": N, "by_api": {...}, "by_user": {...} }`

#### GET /api/stats/chart?dimension=user_type&chart_type=pie
- 描述: 按维度获取图表数据
- 参数: `dimension` (user_type/user_level/department), `chart_type` (pie/line/bar)
- 响应: `{ "labels": [...], "values": [...], "dimension": "user_type", "chart_type": "pie" }`

---

## 四、数据模型

### 埋点记录

```json
{
  "id": "uuid",
  "timestamp": "2026-09-01T12:00:00Z",
  "api": "/api/hello|/api/hash|/api/sort",
  "action": "call_api|export|view_report",
  "caller": "用户名",
  "user_type": "developer|manager|tester|admin",
  "user_level": "junior|mid|senior|principal",
  "department": "engineering|product|qa|operations"
}
```

### 统计维度

| 维度 | 字段 | 描述 |
|------|------|------|
| 人员类型 | `user_type` | developer / manager / tester / admin |
| 人员层级 | `user_level` | junior / mid / senior / principal |
| 人员部门 | `department` | engineering / product / qa / operations |

---

## 五、前端页面设计

### 页面布局

```
┌──────────────────────────────────────────────────────┐
│  调用人: [____] 类型: [▼] 层级: [▼] 部门: [▼]       │
├──────────────────────────────────────────────────────┤
│  [Tab: HelloWorld]  [Tab: 哈希算法]  [Tab: 冒泡排序]  │ [导出按钮]
├──────────────────────────────────────────────────────┤
│                                                       │
│               Tab 内容区域                             │
│         （输入参数 → 调用接口 → 展示结果）              │
│                                                       │
├──────────────────────────────────────────────────────┤
│  导出: [全部/HelloWorld/哈希/排序 ▼] [导出CSV]        │
├──────────────────────────────────────────────────────┤
│  📊 调用统计报表                                       │
│  维度切换: [人员类型 ▼] [刷新图表]                     │
│  ┌───────────┬───────────┬───────────┐               │
│  │ 折线图     │  饼图     │  柱状图    │               │
│  │ (趋势)    │ (分布)    │ (对比)    │               │
│  └───────────┴───────────┴───────────┘               │
└──────────────────────────────────────────────────────┘
```

### Tab 功能说明

| Tab | 输入 | 操作 | 展示 |
|-----|------|------|------|
| HelloWorld | 无 | 调用 GET /api/hello | 显示返回消息和时间戳 |
| 哈希算法 | 文本输入框 + 算法选择(SHA256/MD5) | 调用 POST /api/hash | 显示原文本和哈希值 |
| 冒泡排序 | 数字数组输入(JSON格式) | 调用 POST /api/sort | 显示排序前后的数组 |

---

## 六、实施计划概要

| 阶段 | 内容 | 预计工作量 |
|------|------|-----------|
| 1. 后端骨架 + HelloWorld | Flask 应用 + requirements.txt + hello_world.py | 小 |
| 2. 哈希算法接口 | hash_algo.py + 路由注册 | 小 |
| 3. 冒泡排序接口 | sort_api.py（包装已有 bubble_sort.py） | 小 |
| 4. 埋点模块 | tracking.py（数据模型 + 记录 + 统计聚合） | 中 |
| 5. 导出接口 | export.py（CSV 导出） | 小 |
| 6. 前端页面 | index.html + style.css（3Tab + 用户信息 + 导出按钮） | 中 |
| 7. 前端JS逻辑 | app.js（API调用 + ECharts图表渲染） | 中 |
| 8. 集成测试 | 全链路联调验证 | 小 |

---

## 七、自检清单

### 需求覆盖

| 需求 | 覆盖 |
|------|------|
| HelloWorld 接口 | ✅ |
| 哈希算法接口（SHA256 + MD5） | ✅ |
| 冒泡排序接口（复用已有代码） | ✅ |
| 前端页面（3个Tab展示） | ✅ |
| 导出按钮 + 后台导出接口 | ✅ |
| 埋点统计（调用次数+调用人） | ✅ |
| 可视化报表（折线图/饼图/柱状图） | ✅ |
| 多维度统计（人员类型/层级/部门） | ✅ |

### 一致性检查

- 所有接口签名一致 ✅
- 跨任务引用类型匹配 ✅
- 无 TBD/TODO 占位符 ✅