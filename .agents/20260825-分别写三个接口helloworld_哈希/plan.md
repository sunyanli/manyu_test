# 实施计划 — 三个接口 + 前端 Tab 页面 + 导出

> 生成时间：2026-07-16  
> 任务节点：实施计划 (loop-1)  
> 上游依赖：需求澄清文档 `dima.md`（8 项决策已确认，接口契约已定稿）  
> 涉及仓库：`manyu_test` (后端), `manyu_test1` (前端)

---

## 一、现状盘点

### 1.1 manyu_test (后端仓库)

| 维度 | 详情 |
|------|------|
| 路径 | `/root/.../manyu_test-cred-test-20260716022903` |
| 分支 | `AI/task-DEV-...` (base: `cred-test-20260716022903`) |
| 语言 | Python 3 |
| 现有资产 | `bubble_sort.py` — 三个冒泡排序函数（标准/优化/降序），含 doctest + 单元测试 |
| 现有框架 | **无**（无 Web 框架、无 API 入口、无 `requirements.txt`） |
| 存量改动风险 | 低 — `bubble_sort.py` 仅需导入复用，不修改原文件 |

### 1.2 manyu_test1 (前端仓库)

| 维度 | 详情 |
|------|------|
| 路径 | `/root/.../manyu_test1-main` |
| 分支 | `AI/task-DEV-...` (base: `main`) |
| 语言 | 空仓（仅 `README.md`） |
| 现有资产 | 无 |
| 现有框架 | 无 |

---

## 二、跨仓改动总览

```
┌──────────────────────────────────────┐
│ manyu_test (后端 FastAPI)             │
│                                      │
│ 新增文件:                             │
│  ├── requirements.txt                │
│  ├── main.py          (FastAPI 入口)  │
│  └── routers/                        │
│      ├── __init__.py                 │
│      ├── helloworld.py  (GET)        │
│      ├── hash.py         (POST)      │
│      ├── bubble_sort.py  (POST)      │
│      └── export.py       (POST)      │
│                                      │
│ 不改动: bubble_sort.py (仅导入复用)    │
└──────────────┬───────────────────────┘
               │  HTTP/REST
               │  CORS enabled
               ▼
┌──────────────────────────────────────┐
│ manyu_test1 (前端 Vue 3)              │
│                                      │
│ 初始化: npm create vue@latest        │
│ 新增文件:                             │
│  ├── src/views/ToolPage.vue          │
│  ├── src/components/                 │
│  │   ├── HelloWorldTab.vue           │
│  │   ├── HashTab.vue                 │
│  │   └── BubbleSortTab.vue           │
│  ├── src/api/index.js   (axios 封装) │
│  └── src/router/index.js (路由)      │
│                                      │
│ 修改: App.vue (挂载路由)              │
└──────────────────────────────────────┘
```

---

## 三、后端 (manyu_test) 详细实施计划

### 3.1 技术选型

| 组件 | 选型 | 版本 | 说明 |
|------|------|------|------|
| Web 框架 | FastAPI | latest | 用户已确认 |
| ASGI 服务器 | uvicorn | latest | FastAPI 标配 |
| 哈希 | hashlib (标准库) | — | SHA-256 / MD5 |
| CORS | fastapi.middleware.cors | built-in | 允许前端跨域 |
| 导出 | fastapi.responses.StreamingResponse | built-in | JSON 文件下载 |

### 3.2 文件清单 & 改动顺序

#### Step 1: `requirements.txt` — 新建

```
fastapi
uvicorn
```

#### Step 2: `routers/__init__.py` — 新建

空文件，使 `routers/` 成为 Python 包。

#### Step 3: `routers/helloworld.py` — 新建

**接口契约**（来自 dima.md §4.1）：
- `GET /api/helloworld`
- Response 200: `{"message": "Hello World!"}`

**实现要点**：
- 创建 `APIRouter`，prefix 留空（由 main.py 统一挂载 `/api`）
- 单一路由 `@router.get("/helloworld")`
- 无参数，直接返回固定 JSON

#### Step 4: `routers/hash.py` — 新建

**接口契约**（来自 dima.md §4.2）：
- `POST /api/hash`
- Body: `{"text": "<string>", "algorithm": "sha256" | "md5"}`
- Response 200: `{"algorithm": "sha256", "input": "<string>", "hash": "<hex>"}`

**实现要点**：
- 使用 Pydantic `BaseModel` 定义请求体
- `algorithm` 字段校验：仅允许 `"sha256"` 或 `"md5"`
- 使用 `hashlib.sha256(text.encode()).hexdigest()` / `hashlib.md5(...)`
- 错误处理：非法 algorithm → 422

#### Step 5: `routers/bubble_sort.py` — 新建

**接口契约**（来自 dima.md §4.3）：
- `POST /api/bubble_sort`
- Body: `{"array": [<int>, ...], "order": "asc" | "desc"}`
- Response 200: `{"original": [<int>, ...], "sorted": [<int>, ...], "order": "asc"}`

**实现要点**：
- 导入仓库根目录的 `bubble_sort` 模块（`from bubble_sort import bubble_sort, bubble_sort_descending`）
- Pydantic 模型校验 `array` 为 `list[int]`，`order` 为 `Literal["asc", "desc"]`
- 根据 `order` 调用对应函数，保留 `original` 副本

#### Step 6: `routers/export.py` — 新建

**接口契约**（来自 dima.md §4.4）：
- `POST /api/export`
- Body: `{"tab": "helloworld" | "hash" | "bubble_sort", "data": <any>}`
- Response 200: `Content-Disposition: attachment; filename="export.json"`

**实现要点**：
- 接收 `tab` 和 `data`，将整个 payload 序列化为 JSON
- 使用 `StreamingResponse` 返回，设置 `Content-Disposition` 头
- 文件名格式：`export_{tab}.json`

#### Step 7: `main.py` — 新建

**实现要点**：
- 创建 `FastAPI()` 实例
- 配置 CORS 中间件（`allow_origins=["*"]`，开发阶段全放通）
- 挂载四个 router 到 `/api` 前缀
- 根路径 `/` 返回健康检查
- `if __name__ == "__main__"` 启动 uvicorn

---

## 四、前端 (manyu_test1) 详细实施计划

### 4.1 技术选型

| 组件 | 选型 | 说明 |
|------|------|------|
| 框架 | Vue 3 (Composition API) | 用户已确认 |
| 构建工具 | Vite | Vue 3 官方推荐 |
| HTTP 客户端 | axios | 调用后端 4 接口 |
| UI 组件 | 原生 CSS / 可选 Element Plus | 轻量优先，原生 CSS |
| 路由 | vue-router | 单页路由 |
| 导出 | Blob + URL.createObjectURL | 前端下载文件 |

### 4.2 初始化步骤

```bash
cd manyu_test1-main
npm create vue@latest . -- --force  # 或手动搭建 Vite + Vue 3
npm install axios vue-router
```

### 4.3 文件清单 & 改动顺序

#### Step 1: 项目初始化（Vite + Vue 3）

使用 `npm create vite@latest . -- --template vue` 初始化项目骨架，安装依赖。

#### Step 2: `src/api/index.js` — 新建 (axios 封装)

**实现要点**：
- 创建 axios 实例，`baseURL` 指向后端地址（开发环境 `http://localhost:8000`）
- 封装四个 API 方法：
  - `getHelloWorld()` → `GET /api/helloworld`
  - `postHash(text, algorithm)` → `POST /api/hash`
  - `postBubbleSort(array, order)` → `POST /api/bubble_sort`
  - `postExport(tab, data)` → `POST /api/export` (responseType: blob)

#### Step 3: `src/components/HelloWorldTab.vue` — 新建

**交互模式**（来自 dima.md §3 决策）：
- 无输入区（helloworld 无参数）
- 「执行」按钮 → 调用 `GET /api/helloworld`
- 结果展示区：显示 `message` 字段

#### Step 4: `src/components/HashTab.vue` — 新建

**交互模式**：
- 输入区：文本框（text）+ 下拉选择（algorithm: sha256/md5）
- 「执行」按钮 → 调用 `POST /api/hash`
- 结果展示区：显示 `algorithm`、`input`、`hash`

#### Step 5: `src/components/BubbleSortTab.vue` — 新建

**交互模式**：
- 输入区：文本框（逗号分隔的数字数组）+ 下拉选择（order: asc/desc）
- 「执行」按钮 → 调用 `POST /api/bubble_sort`
- 结果展示区：显示 `original`、`sorted`、`order`

#### Step 6: `src/views/ToolPage.vue` — 新建

**实现要点**：
- 三 Tab 布局（使用原生 CSS tab 切换或简易状态驱动）
- 每个 Tab 内嵌对应组件（HelloWorldTab / HashTab / BubbleSortTab）
- 全局「导出」按钮：获取当前 Tab 的活动数据，调用 `POST /api/export`

#### Step 7: `src/router/index.js` — 新建

**路由配置**：
- `/` → `ToolPage.vue`

#### Step 8: `src/App.vue` — 修改

挂载 `<router-view />`。

---

## 五、跨仓接口契约对齐清单

| # | 接口 | 方法 | 路径 | 请求体 | 响应体 | 前端调用方式 | 对齐状态 |
|---|------|------|------|--------|--------|-------------|----------|
| 1 | helloworld | GET | `/api/helloworld` | 无 | `{"message": "string"}` | `axios.get` | ✅ 已定稿 |
| 2 | hash | POST | `/api/hash` | `{"text":"...","algorithm":"sha256\|md5"}` | `{"algorithm":"...","input":"...","hash":"..."}` | `axios.post` | ✅ 已定稿 |
| 3 | bubble_sort | POST | `/api/bubble_sort` | `{"array":[...],"order":"asc\|desc"}` | `{"original":[...],"sorted":[...],"order":"..."}` | `axios.post` | ✅ 已定稿 |
| 4 | export | POST | `/api/export` | `{"tab":"...","data":...}` | JSON 文件流 (attachment) | `axios.post` + blob | ✅ 已定稿 |

### 跨仓对齐检查点

| 检查项 | 预期 | 实现阶段验证 |
|--------|------|-------------|
| 后端 CORS 配置 | 允许前端 origin 跨域 | main.py 中 `CORSMiddleware` |
| 字段名大小写一致 | 全小写 snake_case | 前后端统一 `algorithm`, `order`, `array` |
| HTTP 状态码 | 200 成功 / 422 校验失败 | FastAPI 自动处理 Pydantic 校验 |
| 导出 Content-Type | `application/json` + attachment | StreamingResponse headers |
| 前端 baseURL | 可配置，默认 `http://localhost:8000` | axios 实例 |
| 冒泡排序复用 | 不修改 `bubble_sort.py`，仅 import | `from bubble_sort import ...` |

---

## 六、执行顺序（依赖关系）

```
Phase 1: 后端 (manyu_test)
  ├── 1.1 创建 requirements.txt
  ├── 1.2 创建 routers/__init__.py
  ├── 1.3 创建 routers/helloworld.py
  ├── 1.4 创建 routers/hash.py
  ├── 1.5 创建 routers/bubble_sort.py
  ├── 1.6 创建 routers/export.py
  ├── 1.7 创建 main.py
  └── 1.8 启动验证: uvicorn main:app --reload
        ↓ (后端就绪后)
Phase 2: 前端 (manyu_test1)
  ├── 2.1 初始化 Vue 3 + Vite 项目
  ├── 2.2 安装 axios / vue-router
  ├── 2.3 创建 src/api/index.js
  ├── 2.4 创建 src/components/HelloWorldTab.vue
  ├── 2.5 创建 src/components/HashTab.vue
  ├── 2.6 创建 src/components/BubbleSortTab.vue
  ├── 2.7 创建 src/views/ToolPage.vue
  ├── 2.8 创建 src/router/index.js
  ├── 2.9 修改 src/App.vue + src/main.js
  └── 2.10 启动验证: npm run dev
```

---

## 七、风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| Vue 3 项目初始化需交互式选择 | 阻塞自动化 | 使用 `--template vue` 非交互模式，或手动创建 `package.json` + `vite.config.js` |
| 冒泡排序模块导入路径问题 | 后端启动失败 | 在 `main.py` 同级目录，`routers/` 使用 `sys.path` 或相对导入 `from ..bubble_sort` |
| 前端 axios 跨域被浏览器拦截 | 前端无法调用后端 | 后端 CORS 配置 + 前端开发代理 (`vite.config.js` proxy) |
| 导出文件名中文乱码 | 用户体验差 | 使用 ASCII 文件名 `export.json`，Content-Disposition 正确编码 |

---

## 八、验收标准

| # | 验收项 | 验证方式 |
|---|--------|----------|
| 1 | `GET /api/helloworld` 返回 `{"message":"Hello World!"}` | `curl http://localhost:8000/api/helloworld` |
| 2 | `POST /api/hash` 正确计算 SHA-256 / MD5 | `curl -X POST ... -d '{"text":"hello","algorithm":"sha256"}'` |
| 3 | `POST /api/bubble_sort` 正确排序 asc/desc | `curl -X POST ... -d '{"array":[5,3,8],"order":"asc"}'` |
| 4 | `POST /api/export` 返回 JSON 文件下载 | `curl -X POST ... -d '{"tab":"hash","data":{...}}' -o export.json` |
| 5 | 前端三 Tab 切换正常，各 Tab 执行按钮可用 | 浏览器访问 `http://localhost:5173` |
| 6 | 导出按钮下载当前 Tab 结果 JSON | 点击导出 → 检查下载文件内容 |
| 7 | 不改动 `bubble_sort.py` 原有代码 | `git diff bubble_sort.py` 为空 |

---

## 九、下一步

1. ✅ 实施计划完成 — 后端 7 文件 + 前端 9 文件 + 项目初始化
2. ➡️ 进入 **编码执行** 阶段：按 Phase 1 → Phase 2 顺序实施
3. ➡️ 编码完成后执行验收标准 §8 全部 7 项