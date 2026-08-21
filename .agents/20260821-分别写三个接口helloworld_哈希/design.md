# 设计文档：三接口 + 前端 Tab 展示 + 导出 + 埋点仪表盘

> 阶段：需求澄清 → 概要设计
> 日期：2026-08-21
> 技术栈：Python (FastAPI) + 原生 HTML/JS + Chart.js + SQLite

---

## 1. 系统架构总览

```
┌─────────────────────────────────────────────────────┐
│  [manyu_test1] 前端 (静态 HTML/JS)                    │
│  ┌─────────────────────────────────────────────────┐ │
│  │  index.html                                      │ │
│  │  ┌───────┬───────┬───────┐                      │ │
│  │  │Tab1   │Tab2   │Tab3   │  ← 三 Tab 展示        │ │
│  │  │Hello  │Hash   │Bubble │                      │ │
│  │  ├───────┴───────┴───────┤                      │ │
│  │  │  [导出] 按钮           │  ← 导出当前 Tab       │ │
│  │  ├───────────────────────┤                      │ │
│  │  │ 埋点仪表盘             │                      │ │
│  │  │ 折线图 / 饼图 / 柱状图 │  ← Chart.js          │ │
│  │  │ 维度: 人员类型/层级/部门│                      │ │
│  └─────────────────────────────────────────────────┘ │
└───────────────┬─────────────────────────────────────┘
                │  HTTP (fetch / CORS)
                ▼
┌─────────────────────────────────────────────────────┐
│  [manyu_test] 后端 (FastAPI)                         │
│  ┌─────────────────────────────────────────────────┐ │
│  │  /api/helloworld      GET   → {message}         │ │
│  │  /api/hash            POST  → {hash}            │ │
│  │  /api/bubble_sort     POST  → {sorted}          │ │
│  │  /api/export/{type}   GET   → file download     │ │
│  │  /api/tracking/report GET   → 统计聚合数据       │ │
│  └─────────────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────────────┐ │
│  │  SQLite: tracking.db                             │ │
│  │  tracking_logs(id, api_name, user_id,           │ │
│  │    user_type, user_level, user_dept, ts)         │ │
│  └─────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
```

---

## 2. 仓库分配

| 仓库 | 角色 | 关键文件 |
|------|------|----------|
| `[manyu_test]` | 后端 | `main.py`（FastAPI 入口）、`bubble_sort.py`（已有，复用）、`models.py`、`tracking.py` |
| `[manyu_test1]` | 前端 | `index.html`（单页应用，含 Tab、导出、仪表盘） |

---

## 3. 后端 API 设计

### 3.1 接口清单

| 方法 | 路径 | 描述 | 请求体 | 响应 |
|------|------|------|--------|------|
| GET | `/api/helloworld` | Hello World | — | `{"message": "Hello, World!"}` |
| POST | `/api/hash` | 哈希计算 | `{"input": "...", "algorithm": "sha256"}` | `{"hash": "abc123..."}` |
| POST | `/api/bubble_sort` | 冒泡排序 | `{"array": [5,2,8,1,3]}` | `{"sorted": [1,2,3,5,8], "steps": 10}` |
| GET | `/api/export/helloworld` | 导出 HelloWorld 结果 | — | `application/json` 文件下载 |
| GET | `/api/export/hash` | 导出 Hash 结果 | `?input=...&algorithm=...` | `application/json` 文件下载 |
| GET | `/api/export/bubble_sort` | 导出 BubbleSort 结果 | `?array=5,2,8,1,3` | `application/json` 文件下载 |
| GET | `/api/tracking/report` | 埋点统计报表 | `?dimension=user_type` | `{"labels":[...], "values":[...]}` |

### 3.2 埋点数据模型

```sql
CREATE TABLE tracking_logs (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    api_name   TEXT NOT NULL,          -- 'helloworld' | 'hash' | 'bubble_sort'
    user_id    TEXT NOT NULL,          -- 调用人标识
    user_type  TEXT,                   -- 人员类型: 正式/外包/实习生
    user_level TEXT,                   -- 人员层级: P6/P7/P8
    user_dept  TEXT,                   -- 人员部门: 技术/产品/运营
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 3.3 埋点中间件

所有 `/api/helloworld`、`/api/hash`、`/api/bubble_sort` 请求自动通过 FastAPI 中间件写入 `tracking_logs`，从请求 Header 提取用户信息：

| Header | 字段 |
|--------|------|
| `X-User-Id` | `user_id` |
| `X-User-Type` | `user_type` |
| `X-User-Level` | `user_level` |
| `X-User-Dept` | `user_dept` |

### 3.4 报表聚合维度

`/api/tracking/report?dimension=` 支持：

| 维度值 | 说明 |
|--------|------|
| `user_type` | 按人员类型聚合 |
| `user_level` | 按人员层级聚合 |
| `user_dept` | 按人员部门聚合 |
| `api_name` | 按接口聚合 |
| `user_type,api_name` | 二维交叉 |

---

## 4. 前端设计

### 4.1 页面结构 (`index.html`)

```
┌──────────────────────────────────────────────┐
│  🔧 算法演示平台                              │
├──────────────────────────────────────────────┤
│  [HelloWorld] [Hash] [BubbleSort]  ← Tab 栏  │
├──────────────────────────────────────────────┤
│                                              │
│  输入区域（对应 Tab）                          │
│  ┌──────────────────────────────────────┐    │
│  │  [输入框]  [执行]                      │    │
│  └──────────────────────────────────────┘    │
│                                              │
│  结果展示区                                   │
│  ┌──────────────────────────────────────┐    │
│  │  {json 结果}                          │    │
│  └──────────────────────────────────────┘    │
│                                              │
│  [📥 导出当前结果]                             │
├──────────────────────────────────────────────┤
│  📊 调用统计仪表盘                             │
│  ┌────┬────┬────┐                           │
│  │折线│饼图│柱状│  ← 图表类型切换              │
│  ├────┴────┴────┤                           │
│  │ 维度: [人员类型▼] [人员层级] [人员部门]     │
│  │                                             │
│  │  ┌─────────────────────────────┐           │
│  │  │       Chart.js 图表          │           │
│  │  └─────────────────────────────┘           │
│  └────────────────────────────────┘           │
└──────────────────────────────────────────────┘
```

### 4.2 技术细节

- **Tab 切换**：纯 CSS/JS，3 个 Tab 各自独立请求对应 API
- **导出**：点击按钮 → 构造对应 `/api/export/{type}` URL → 浏览器下载
- **图表**：Chart.js CDN，三种图表类型可切换，维度下拉联动
- **CORS**：FastAPI 配置 `CORSMiddleware` 允许跨域

---

## 5. 文件清单

### 5.1 `[manyu_test]` 后端

| 文件 | 说明 |
|------|------|
| `main.py` | FastAPI 应用入口，路由注册，中间件配置 |
| `bubble_sort.py` | 已有，复用冒泡排序逻辑 |
| `tracking.py` | 埋点数据库模型、中间件、报表查询 |
| `requirements.txt` | `fastapi`, `uvicorn`, `aiosqlite` |

### 5.2 `[manyu_test1]` 前端

| 文件 | 说明 |
|------|------|
| `index.html` | 完整单页应用 |

---

## 6. 仓间对齐点

| 对齐项 | `[manyu_test]` | `[manyu_test1]` |
|--------|---------------|-----------------|
| CORS 源 | 允许 `*`（开发阶段） | `fetch` 指向后端地址 |
| API 路径前缀 | `/api/` | 请求路径硬编码 `/api/...` |
| 导出格式 | JSON 文件流 | `download` 属性触发浏览器下载 |
| 埋点维度枚举 | `user_type/user_level/user_dept` | 下拉选项与后端一致 |
| 用户信息传递 | 读取 Header | 请求时附加 Header（开发阶段 Mock 固定值） |

---

## 7. 待确认项（后续迭代）

1. **用户认证**：当前 Mock Header，生产需接入统一认证（SSO/JWT）
2. **部署**：manyu_test 后端需运行 uvicorn；manyu_test1 前端需静态文件服务
3. **数据持久化**：SQLite 适合开发，生产考虑迁移到 PostgreSQL
4. **埋点数据量**：大流量场景需异步写入 + 批量聚合
5. **前端路由**：当前为单页，后续可拆分独立页面

---

## 8. 异常兜底方案

### 8.1 后端异常兜底

| 异常场景 | 触发条件 | 兜底策略 |
|----------|----------|----------|
| 参数校验失败 | 请求体缺失必填字段、类型错误（如 `array` 非数组） | FastAPI Pydantic 自动返回 `422 {"detail":[...]}`；前端展示友好提示「输入格式错误，请检查」 |
| 哈希算法不支持 | `algorithm` 非 `sha256/md5/sha1` 之一 | 返回 `400 {"error": "unsupported algorithm", "supported": ["sha256","md5","sha1"]}` |
| 数组为空 | `bubble_sort` 的 `array` 为 `[]` | 返回 `200 {"sorted": [], "steps": 0}`，不报错，前端展示「空数组无需排序」 |
| 输入超长 | `hash` 的 `input` 超过 1MB 或 `bubble_sort` 的 `array` 超过 10000 元素 | 返回 `413 {"error": "payload too large", "limit": ...}` |
| 内部异常 | 未预期的 Python 异常（如内存不足） | FastAPI 全局异常处理器捕获 → 返回 `500 {"error": "internal server error", "request_id": "..."}`；日志记录完整堆栈 |
| 数据库不可用 | SQLite 文件权限/磁盘满 | 埋点中间件静默失败（`try/except` 记录日志），不阻塞业务 API 返回；`/api/tracking/report` 返回 `503 {"error": "tracking service unavailable"}` |
| 导出数据为空 | 对应接口无调用记录 | 返回空 JSON 文件 `{"results": [], "exported_at": "..."}` 或仅含表头 CSV，不报错 |

### 8.2 埋点中间件兜底

```
请求进入 → 提取 Header → 业务处理 → 埋点写入
                              ↘ 写入失败 → log.error() → 不影响响应
```

- **Header 缺失**：`user_id` 缺失时默认填 `"anonymous"`；`user_type/user_level/user_dept` 缺失时填 `"unknown"`
- **写入异常**：`try/except sqlite3.Error` 捕获，`logging.error` 记录，业务响应正常返回
- **并发写入**：SQLite WAL 模式 + 短事务，避免锁冲突

### 8.3 前端异常兜底

| 异常场景 | 触发条件 | 兜底策略 |
|----------|----------|----------|
| 网络不可达 | `fetch` 超时 / DNS 失败 / 后端未启动 | 显示红色 Toast「❌ 无法连接后端服务，请检查网络或稍后重试」；5s 超时自动 abort |
| HTTP 4xx/5xx | 后端返回错误状态码 | 解析响应体 `error` 字段并展示；非 JSON 响应展示「服务器返回异常 (HTTP {code})」 |
| 响应 JSON 解析失败 | 后端返回非 JSON | 兜底展示原始文本（截断 500 字符），标记「⚠️ 响应格式异常」 |
| Chart.js CDN 加载失败 | CDN 不可达 | `<script>` 的 `onerror` 回调 → 仪表盘区域显示「📊 图表组件加载失败，请刷新页面」 |
| 报表数据为空 | `/api/tracking/report` 返回 `{"labels":[],"values":[]}` | 图表区域显示「暂无调用数据，请先使用上方接口」 |
| 导出失败 | 导出请求超时或返回错误 | 弹窗提示「导出失败，请重试」；不自动关闭 |
| 浏览器兼容 | 老浏览器不支持 `fetch`/`AbortController` | 页面顶部 `noscript` + JS 检测 `typeof fetch === 'undefined'` → 显示「请使用现代浏览器（Chrome/Firefox/Edge）」 |

### 8.4 前端错误处理架构

```
┌─────────────────────────────────────────┐
│              fetchWrapper()              │
│  ┌─────────────────────────────────┐    │
│  │ timeout(5s) → AbortController   │    │
│  │ response.ok? → response.json()  │    │
│  │ !response.ok → throw ApiError   │    │
│  │ network error → throw NetError  │    │
│  └─────────────────────────────────┘    │
│         ↓ 统一错误分流                    │
│  ┌─────────────────────────────────┐    │
│  │ showToast(message, level)       │    │
│  │  - error (红色)                  │    │
│  │  - warning (橙色)                │    │
│  │  - info (蓝色)                   │    │
│  └─────────────────────────────────┘    │
└─────────────────────────────────────────┘
```

### 8.5 图表兜底

| 场景 | 处理 |
|------|------|
| 单维度数据全为 0 | 仍渲染图表，Y 轴从 0 开始，视觉上为空白柱/点 |
| 维度值过多（>20） | 饼图自动切换为柱状图（避免饼图切片过密）；柱状图启用横向滚动 |
| 数据加载中 | 图表区域显示 loading spinner |
| 切换维度时请求失败 | 保留上一次图表数据不变，Toast 提示刷新失败 |

---

## 9. 实施步骤

| 步骤 | 仓库 | 内容 |
|------|------|------|
| 1 | `[manyu_test]` | 创建 `main.py`：FastAPI 入口 + 三个业务 API |
| 2 | `[manyu_test]` | 创建 `tracking.py`：SQLite 模型 + 埋点中间件 + `/api/tracking/report` |
| 3 | `[manyu_test]` | 创建 `/api/export/*` 导出接口 |
| 4 | `[manyu_test]` | 创建 `requirements.txt` |
| 5 | `[manyu_test1]` | 创建 `index.html`：Tab 页 + 导出 + Chart.js 仪表盘 |
| 6 | 两端 | 联调验证：启动后端 → 前端访问 → 确认埋点数据写入 → 仪表盘展示 |

---

*本文档由 DTCoder 在 Brainstorming 阶段生成，待用户 Review 确认后进入实现阶段。*