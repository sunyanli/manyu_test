# 需求澄清文档（dima）— 三接口 + 前端三 Tab + 导出 + 埋点报表

- 任务ID: DEV-eecb99b0-9b79-11f1-a438-852be3c2a082-501fbda9-7573-4879-8fdd-ef19f9680994
- 阶段: loop-1 / 需求澄清
- 采用技能: /brainstorming
- 日期: 2026-08-20
- 状态: 澄清中（Q1–Q4 待用户确认，见第 5 节；未答复时按第 5 节“建议默认”推进，与 plan.md 既有裁定一致）

---

## 1. 需求理解

原始需求拆解为 4 个子需求：

| # | 子需求 | 归属端 | 说明 |
|---|--------|--------|------|
| R1 | 三个接口：helloworld、哈希算法、冒泡排序 | 后端 | 提供 3 个可独立调用的 API |
| R2 | 前端新增一个页面，3 个 Tab 分别展示三个接口的执行结果 | 前端 | 每个 Tab 内可触发调用并展示结果 |
| R3 | 新增导出按钮 + 后端导出接口 | 前/后端 | 支持导出各个页面（Tab）的展示结果 |
| R4 | 后端埋点（调用次数 + 调用人）+ 前端报表可视化 | 前/后端 | 维度：人员类型 / 人员层级 / 人员部门；图形：折线图 / 饼图 / 柱状图 |

验收口径（初步）：
- R1：三个接口均可通过 HTTP 调用并返回正确结果；冒泡排序需复用/对齐已有算法实现。
- R2：单页面三 Tab，切换 Tab 展示对应接口的执行结果。
- R3：任一 Tab 下点击导出，可下载该 Tab 展示结果文件。
- R4：每次接口调用被记录（时间、接口、调用人）；报表页可按 3 个维度切换聚合，并以折线图/饼图/柱状图三种形式展示。

## 2. 跨仓依赖与现状摘要

### [manyu_test]（后端候选仓，分支基线 cred-test-20260716022903）
- `[manyu_test] bubble_sort.py:18-103`：已有冒泡排序 Python 实现（标准版 / 优化版 / 降序版 + 自测用例），可直接作为 R1 冒泡排序接口的算法内核复用。
- 仓内无 Web 框架、无依赖清单（无 requirements.txt / pom.xml / package.json），API 服务需从零搭建。
- `[manyu_test] cred-helper-test.txt:1`：凭据写入测试遗留文件，与本需求无关，不做处理。

### [manyu_test1]（前端候选仓，分支基线 main）
- 仅有 `[manyu_test1] README.md`，为空仓库，前端工程需从零初始化。

### 预期数据流
```
浏览器（manyu_test1 前端）
 ├─ GET  /api/hello            ─┐
 ├─ POST /api/hash              ├─→ 后端（manyu_test）执行业务 + 统一埋点落库
 ├─ POST /api/bubble-sort      ─┘
 ├─ GET  /api/export?tab=...      → 后端按 Tab 导出文件流
 └─ GET  /api/metrics?...         → 埋点聚合数据 → 前端渲染折线/饼图/柱状图
```

## 3. 初步方案建议（待澄清后定稿）

推荐倾向（理由：manyu_test 已有 Python 代码可零成本复用；两仓均为空仓，选轻量栈交付最快）：
- **后端（manyu_test）**：Python + FastAPI；埋点记录落 SQLite（单文件、无外部依赖）。
- **前端（manyu_test1）**：React + Vite + Ant Design（Tab/按钮组件）+ ECharts（折线/饼图/柱状图）。
- **埋点方式**：后端对三个业务接口做统一拦截，记录 `{时间, 接口名, 调用人ID, 调用人姓名, 人员类型, 人员层级, 部门, 耗时, 状态}`。

### 接口契约草案（前后端对齐基准）

| 接口 | 方法/路径 | 入参 | 出参 |
|------|-----------|------|------|
| HelloWorld | `GET /api/hello` | `name`（可选，默认 World） | `{code,data:{greeting}}` |
| 哈希算法 | `POST /api/hash` | `{text, algorithm}` | `{code,data:{algorithm,input,digest}}` |
| 冒泡排序 | `POST /api/bubble-sort` | `{numbers:[], order:"asc\|desc"}` | `{code,data:{input,sorted,duration_ms}}` |
| 导出 | `GET /api/export` | `tab=hello\|hash\|bubble`，`format` | 文件流（attachment） |
| 报表聚合 | `GET /api/metrics` | `dimension=user_type\|user_level\|department`，`chart=line\|pie\|bar`，`range` | `{code,data:{labels,series}}` |

### 备选方案
- 方案 B：后端 Python Flask + 前端 Vue3/Vite（更简单的路由模型，图表同样用 ECharts）。
- 方案 C：后端 Java Spring Boot + 前端 React（如团队技术栈强制 Java，成本最高，冒泡排序需重写为 Java）。

## 4. 跨仓对齐点

1. **API 契约**：上表路径与出入参为前后端唯一对齐基准，变更需双仓同步。
2. **埋点字段**：“人员类型 / 人员层级 / 部门”三维度字段的来源与取值枚举必须前后端一致（影响报表筛选与图例）。
3. **导出格式**：文件类型、命名规范（建议 `{tab}_{yyyyMMddHHmmss}.{ext}`）。
4. **部署约定**：后端端口（建议 8000）、前端开发代理与 CORS 放行。

## 5. 待澄清问题（阻塞项，已向用户提问）

| # | 问题 | 影响 | 建议默认（未答复时的推进基线，与 plan.md 既有裁定一致） |
|---|------|------|------|
| Q1 | 仓库分工与技术栈选型（后端框架 / 前端框架） | 决定两仓工程骨架与工作量 | 后端 Python + FastAPI + SQLite（manyu_test，复用既有 bubble_sort.py）；前端 React + Vite + AntD + ECharts（manyu_test1） |
| Q2 | 哈希算法范围（仅 SHA-256，还是支持多算法可选） | R1 接口入参设计 | 支持 md5 / sha1 / sha256 / sha512，默认 sha256 |
| Q3 | 导出文件格式（CSV / Excel / JSON） | R3 后端实现与前端交互 | CSV（默认，带 BOM 便于 Excel 打开）与 JSON 两种，由 `format` 参数选择；不引入 Excel 库 |
| Q4 | 调用人身份与人员属性（类型/层级/部门）数据来源：是否有现成用户体系，还是请求头自报 + mock 用户表 | R4 埋点数据模型与报表可信度 | `X-User-Id` 请求头自报 + 仓内 mock 用户表（u001..u004）补全三维度；缺失按匿名兜底（演示级可信度，见第 6 节风险） |

## 6. 风险与假设

- 两仓均为空/脚本仓，工作量集中在工程初始化（依赖安装、目录骨架、构建配置）。
- 若无真实用户体系，调用人信息只能由调用方自报（如 `X-User-*` 请求头）或依赖本地 mock 用户表补全维度属性，报表为演示级可信度。
- 埋点采用 SQLite 本地存储为单机方案；如后续多实例部署需升级为集中式存储（当前按单机假设推进）。
