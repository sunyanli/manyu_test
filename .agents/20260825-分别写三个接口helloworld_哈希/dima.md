# 需求澄清 Brainstorming — 三个接口 + 前端 Tab 页面 + 导出

> 生成时间：2026-07-16  
> 任务节点：需求澄清 (loop-1)  
> 涉及仓库：`manyu_test` (后端), `manyu_test1` (前端)

---

## 一、跨仓依赖与现状摘要

### 1.1 manyu_test (后端仓库)

| 项目 | 详情 |
|------|------|
| 远程地址 | `https://github.com/sunyanli/manyu_test` |
| 当前分支 | `AI/task-DEV-...` (base: `cred-test-20260716022903`) |
| 语言生态 | Python 3 |
| 现有资产 | `bubble_sort.py` — 冒泡排序（标准版、优化版、降序版），含 doctest + 单元测试 |
| 现有框架 | 无（无 Web 框架、无 API 入口） |
| 关键依赖 | 仅标准库 `typing` |

### 1.2 manyu_test1 (前端仓库)

| 项目 | 详情 |
|------|------|
| 远程地址 | `https://github.com/sunyanli/manyu_test1` |
| 当前分支 | `AI/task-DEV-...` (base: `main`) |
| 语言生态 | 空仓（仅 `README.md`） |
| 现有资产 | 无 |
| 现有框架 | 无 |

### 1.3 仓间关系

```
┌─────────────────────┐        HTTP/REST        ┌──────────────────────┐
│   manyu_test (后端)  │ ◄──────────────────────► │  manyu_test1 (前端)   │
│                     │                          │                      │
│ 已有: bubble_sort   │   /api/helloworld        │ 需新建:              │
│ 需新增:             │   /api/hash              │  - Tab 页面          │
│  - helloworld 接口  │   /api/bubble_sort       │  - 导出按钮          │
│  - hash 接口        │   /api/export            │  - 调用后端接口       │
│  - Web 框架搭建     │                          │                      │
│  - export 接口      │                          │                      │
└─────────────────────┘                          └──────────────────────┘
```

---

## 二、需求拆解

### 2.1 后端接口

| # | 接口 | 当前状态 | 需新建/改造 |
|---|------|----------|-------------|
| 1 | **helloworld** | 不存在 | 新建 |
| 2 | **哈希算法** | 不存在 | 新建 |
| 3 | **冒泡排序** | 算法已实现 (`bubble_sort.py`) | 包装为 API |
| 4 | **导出接口** | 不存在 | 新建 |

### 2.2 前端页面

| # | 组件 | 说明 |
|---|------|------|
| 1 | Tab 页面 | 三个 Tab：helloworld、哈希、冒泡排序 |
| 2 | 执行结果展示 | 每个 Tab 内展示对应接口返回结果 |
| 3 | 导出按钮 | 支持导出当前 Tab 展示的结果 |

---

## 三、已确认方案（用户已答复）

| 决策点 | 确认方案 |
|--------|----------|
| 后端框架 | **FastAPI** |
| 前端框架 | **Vue 3** |
| 仓库划分 | manyu_test = 后端，manyu_test1 = 前端 |
| helloworld | `GET /api/helloworld` → `{"message": "Hello World!"}`（最简方案，无参数） |
| 哈希算法 | 支持 SHA-256 / MD5，`POST /api/hash`，输入 `{"text": "...", "algorithm": "sha256\|md5"}`，返回 hex 摘要 |
| 冒泡排序 | `POST /api/bubble_sort`，输入 `{"array": [5,3,8], "order": "asc\|desc"}`，返回排序后数组 |
| 导出 | `POST /api/export`，后端接收当前结果数据，返回 JSON 文件下载 |
| 前端交互 | 每个 Tab：输入区 + 执行按钮 + 结果展示区；全局一个导出按钮，导出当前 Tab 结果 |

---

## 四、接口契约（定稿）

### 4.1 helloworld
```
GET /api/helloworld
Response 200: {"message": "Hello World!"}
```

### 4.2 哈希算法
```
POST /api/hash
Body: {"text": "<string>", "algorithm": "sha256" | "md5"}
Response 200: {"algorithm": "sha256", "input": "<string>", "hash": "<hex>"}
```

### 4.3 冒泡排序
```
POST /api/bubble_sort
Body: {"array": [<int>, ...], "order": "asc" | "desc"}
Response 200: {"original": [<int>, ...], "sorted": [<int>, ...], "order": "asc"}
```

### 4.4 导出
```
POST /api/export
Body: {"tab": "helloworld" | "hash" | "bubble_sort", "data": <any>}
Response 200: Content-Disposition: attachment; filename="export.json"
```

---

## 五、风险与依赖

| 风险 | 影响 | 状态 |
|------|------|------|
| ~~技术栈不确定~~ | ~~代码返工~~ | ✅ 已解决：FastAPI + Vue 3 |
| ~~接口规格模糊~~ | ~~前后端对接失败~~ | ✅ 已解决：接口契约已定稿 |
| 仓间接口契约未对齐 | 集成困难 | ⚠️ 需在实现阶段严格按契约开发 |

---

## 六、下一步

1. ✅ 需求澄清完成 — 8 项决策已确认
2. ➡️ 进入 **方案设计** 阶段：输出各仓详细改动清单
3. ➡️ 编码执行：manyu_test 后端 4 接口 + manyu_test1 Vue 3 前端