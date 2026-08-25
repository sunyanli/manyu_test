# 代码评审报告 (Code Review Report)

> 生成时间：2026-07-16
> 任务节点：代码评审 (loop-2)
> 涉及仓库：`manyu_test` (后端), `manyu_test1` (前端)
> 评审方式：静态审查 (pip install 超时，按降级协议切换)

---

## 一、评审范围

| 仓库 | 文件 | 类型 |
|------|------|------|
| manyu_test | `main.py` | FastAPI 入口 |
| manyu_test | `requirements.txt` | 依赖声明 |
| manyu_test | `routers/__init__.py` | 包声明 |
| manyu_test | `routers/helloworld.py` | GET /api/helloworld |
| manyu_test | `routers/hash.py` | POST /api/hash |
| manyu_test | `routers/bubble_sort.py` | POST /api/bubble_sort |
| manyu_test | `routers/export.py` | POST /api/export |
| manyu_test | `bubble_sort.py` | [存量] 冒泡排序算法 |
| manyu_test1 | `index.html` / `package.json` / `vite.config.js` | 项目骨架 |
| manyu_test1 | `src/main.js` / `src/App.vue` | 应用入口 |
| manyu_test1 | `src/router/index.js` | 路由配置 |
| manyu_test1 | `src/api/index.js` | axios 封装 |
| manyu_test1 | `src/views/ToolPage.vue` | Tab 页面容器 |
| manyu_test1 | `src/components/HelloWorldTab.vue` | HelloWorld Tab |
| manyu_test1 | `src/components/HashTab.vue` | 哈希 Tab |
| manyu_test1 | `src/components/BubbleSortTab.vue` | 冒泡排序 Tab |

---

## 二、需求对照检查

| # | 需求 | 实现状态 | 证据 |
|---|------|---------|------|
| 1 | helloworld 接口 | ✅ 已完成 | `GET /api/helloworld` → `{"message": "Hello World!"}` |
| 2 | 哈希算法接口 (SHA-256/MD5) | ✅ 已完成 | `POST /api/hash`，Pydantic 校验 algorithm |
| 3 | 冒泡排序接口 (asc/desc) | ✅ 已完成 | `POST /api/bubble_sort`，复用存量 `bubble_sort.py` |
| 4 | 导出接口 | ✅ 已完成 | `POST /api/export`，StreamingResponse + Content-Disposition |
| 5 | 前端三 Tab 页面 | ✅ 已完成 | ToolPage.vue + 3 个子组件，Tab 切换 |
| 6 | 导出按钮 | ✅ 已完成 | 全局导出按钮，调用 `/api/export`，Blob 下载 |
| 7 | 冒泡排序存量代码不改动 | ✅ 已验证 | `git diff bubble_sort.py` 无输出 |

---

## 三、跨仓接口契约对齐检查

| # | 契约项 | 后端 | 前端 | 对齐 |
|---|--------|------|------|------|
| 1 | GET /api/helloworld → `{"message":"string"}` | `routers/helloworld.py:7-9` | `api/index.js:9` + `HelloWorldTab.vue:33-34` | ✅ |
| 2 | POST /api/hash body: `{text, algorithm}` | `routers/hash.py:10-12` | `api/index.js:11-12` + `HashTab.vue:51` | ✅ |
| 3 | POST /api/hash response: `{algorithm, input, hash}` | `routers/hash.py:15-18` | `HashTab.vue:25-27` | ✅ |
| 4 | POST /api/bubble_sort body: `{array, order}` | `routers/bubble_sort.py:15-17` | `api/index.js:14-15` + `BubbleSortTab.vue:83` | ✅ |
| 5 | POST /api/bubble_sort response: `{original, sorted, order}` | `routers/bubble_sort.py:20-23` | `BubbleSortTab.vue:32-34` | ✅ |
| 6 | POST /api/export body: `{tab, data}` | `routers/export.py:12-14` | `api/index.js:17-18` + `ToolPage.vue:75` | ✅ |
| 7 | 导出 responseType: blob | `StreamingResponse` | `api/index.js:18` responseType:'blob' | ✅ |
| 8 | CORS 允许跨域 | `main.py:9-14` allow_origins=["*"] | N/A | ✅ |
| 9 | 字段命名 snake_case | algorithm, order, array, text, tab, data | 同 | ✅ |
| 10 | HTTP 状态码 | 200/422 (Pydantic 自动) | 前端 catch 处理 | ✅ |

---

## 四、问题清单

### 🔴 Blocker (0 个)

无阻断性问题。

### 🟠 Warning (3 个)

| # | 文件 | 行号 | 问题描述 | 建议 |
|---|------|------|---------|------|
| W1 | `routers/bubble_sort.py` | 9-10 | 使用 `sys.path.insert` 动态修改模块搜索路径来导入根目录 `bubble_sort` 模块。这是一种脆弱模式，在复杂部署环境（如多级包、Docker）中可能失效。 | 改为将 `bubble_sort.py` 放入 `routers/` 或使用相对导入 `from ..bubble_sort import ...`（需将仓库转为包）。当前可用但不推荐。 |
| W2 | `routers/hash.py` | 17 | `HashResponse.input` 字段名与 Python 内置函数 `input()` 同名，虽不导致运行时错误，但 IDE 可能产生警告且不符合最佳实践。 | 建议重命名为 `input_text` 或保持（需同步修改前端 `HashTab.vue:26`）。 |
| W3 | `routers/export.py` | 14 | `data: Any` 允许任意类型数据通过，无校验。若前端传入超大或非预期数据，后端无保护。 | 考虑添加 `max_length` 限制或使用 JSON Schema 约束。当前为设计决策，非缺陷。 |

### 🟡 Info (3 个)

| # | 文件 | 行号 | 问题描述 | 建议 |
|---|------|------|---------|------|
| I1 | `vite.config.js` | 8-13 | Vite proxy 配置了 `/api` → `localhost:8000`，但 `src/api/index.js` 中 axios baseURL 直接指向 `http://localhost:8000`，导致 proxy 实际未被使用。 | 移除 proxy（或改为 axios 使用相对路径以利用 proxy）。当前无功能影响。 |
| I2 | `src/components/HelloWorldTab.vue` | 36-39 | 请求失败时仅设置 `result=null`，未 `emit('update:result', null)` 通知父组件清除旧数据。若用户之前成功执行过，导出按钮仍可导出旧数据。 | 错误时 emit null 或空对象，使父组件 `tabData` 同步清除。HashTab/BubbleSortTab 同理。 |
| I3 | `src/api/index.js` | 4 | `baseURL` 硬编码为 `http://localhost:8000`，生产环境需手动修改。 | 建议使用环境变量 `import.meta.env.VITE_API_BASE_URL`。 |

---

## 五、代码质量评估

### 5.1 后端 (manyu_test)

| 维度 | 评分 | 说明 |
|------|------|------|
| 架构设计 | ★★★★★ | 路由分文件、单一职责、Pydantic 校验 |
| 代码规范 | ★★★★☆ | 命名清晰，有 docstring；W1/W2 略有瑕疵 |
| 错误处理 | ★★★★☆ | Pydantic 自动 422，FastAPI 默认错误处理 |
| 安全性 | ★★★★☆ | CORS 开发阶段全放通（可接受），export data 无校验 |
| 可维护性 | ★★★★☆ | 模块化清晰，W1 导入方式需改进 |

### 5.2 前端 (manyu_test1)

| 维度 | 评分 | 说明 |
|------|------|------|
| 组件设计 | ★★★★★ | 职责单一、状态内聚、Composition API |
| 代码规范 | ★★★★★ | 结构清晰，命名规范 |
| 错误处理 | ★★★★☆ | 网络错误提示友好，I2 旧数据残留 |
| 用户体验 | ★★★★★ | Tab 切换流畅，loading/disabled 状态完善 |
| 可维护性 | ★★★★★ | 组件独立，API 层封装良好 |

---

## 六、存量代码保护验证

| 文件 | 检查方式 | 结果 |
|------|---------|------|
| `manyu_test/bubble_sort.py` | `git diff bubble_sort.py` | ✅ 无改动（空输出） |
| `manyu_test/bubble_sort.py` 导入测试 | `python3 -c "from bubble_sort import ..."` | ✅ 导入正常，算法正确 |

---

## 七、验收标准对照

| # | 验收项 | 来源 | 静态审查结果 |
|---|--------|------|-------------|
| 1 | `GET /api/helloworld` 返回正确 | plan.md §8.1 | ✅ 代码匹配契约 |
| 2 | `POST /api/hash` 正确计算 SHA-256/MD5 | plan.md §8.2 | ✅ hashlib 调用正确 |
| 3 | `POST /api/bubble_sort` 正确排序 asc/desc | plan.md §8.3 | ✅ 函数调用正确，导入验证通过 |
| 4 | `POST /api/export` 返回 JSON 文件下载 | plan.md §8.4 | ✅ StreamingResponse + Content-Disposition |
| 5 | 前端三 Tab 切换正常 | plan.md §8.5 | ✅ v-if 条件渲染，activeTab 驱动 |
| 6 | 导出按钮下载当前 Tab 结果 JSON | plan.md §8.6 | ✅ Blob + createObjectURL 下载 |
| 7 | 不改动 `bubble_sort.py` | plan.md §8.7 | ✅ git diff 验证通过 |

---

## 八、总结

### 评审结论：✅ 通过（无 Blocker）

- **Blocker**: 0
- **Warning**: 3（均为非功能性改进建议）
- **Info**: 3（优化建议）

### 待改进项优先级

| 优先级 | 编号 | 建议在下一迭代处理 |
|--------|------|-------------------|
| 高 | I2 | 错误时清除父组件 tabData |
| 中 | W1 | 改进 bubble_sort 导入方式 |
| 中 | I3 | baseURL 环境变量化 |
| 低 | W2 | 重命名 input 字段 |
| 低 | W3 | export data 添加校验 |
| 低 | I1 | 清理冗余 vite proxy |

### 跨仓对齐结论

所有 7 项验收标准静态审查通过，4 个接口契约前后端一致，字段命名统一，数据流完整。冒泡排序存量代码未被修改（已验证）。系统可进入集成测试阶段。