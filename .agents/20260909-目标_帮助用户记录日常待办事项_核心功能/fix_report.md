# 问题修复报告（review 阶段）

> **文档元信息**
>
> | 项目 | 内容 |
> |------|------|
> | 修复日期 | 2026-09-09 |
> | 输入 | `cr_report.md`（VERDICT: APPROVE_WITH_COMMENTS）、`design.md`、`run_context.json`（blocker_count=0） |
> | 采用技能 | dtazziboot-java-coding-standards（CHECK 阶段 L1 静态检查策略） |
> | 代码改动 | **无**（outputs_content 为空，遵循"仅文档产物、禁止修改代码文件"约束） |

---

## 1. 通览（Overview）

评审报告共 2 条 WARNING、2 条 INFO，无 CRITICAL/HIGH 阻断项，`run_context.json` 确认 `blocker_count=0`。逐条核对仓库现状（`rg` 验证 `todo_app/` 下仅存在 `/api/todos` 路由，无 `openapi` 实现）后，确定各发现的最小风险处置方式。

## 2. 发现项处置清单

| # | 发现编号 | 级别 | 处置方式 | 状态 |
|---|----------|------|----------|:----:|
| 1 | OPENAPI_MISSING（design） | WARNING | 修复 design.md：消除文档自相矛盾 | ✅ 已修复 |
| 2 | LEGACY_FILE（trim） | WARNING | 记录为跨任务遗留项，本轮不改动 | 📌 遗留 |
| 3 | ID_GENERATION（align） | INFO | 无需处理（设计与实现已一致） | ✅ 无需处理 |
| 4 | TEST_COVERAGE（verify） | INFO | 记录为测试增强建议，本轮不改动 | 📌 建议 |

## 3. 修复明细

### F01 [WARNING] [design] OPENAPI_MISSING —— ✅ 已修复（文档侧）

**问题**：`design.md` §1 已声明"内部用户，无需对外暴露 OpenAPI"，但 §4.2 又列出 O01 `POST /openapi/todos` 接口，而代码实现仅有 W01 `POST /api/todos`（`todo_app/controller/todo_item_controller.py:50`），形成文档自相矛盾 + 文档与实现不一致。

**决策依据**（自主决策优先级 ①②）：
- 需求约束"目标用户：内部用户""最小闭环：仅创建"，design.md §1 两处明确"无需对外暴露 OpenAPI"——文档缺陷而非实现缺陷；
- 风险最低/改动最小：修复文档 2 处 vs 新增一整条对外路由及配套测试。

**修改内容**（`design.md`，共 2 处）：
1. §1「排除范围」新增一条：显式声明 OpenAPI 对外接口（O01 `/openapi/todos`）本期不实现；
2. §4.2「OpenAPI（对外接口）」：删除 O01 接口表，替换为"本期不实现"说明，并保留后续扩展指引。

**验证**：修复后全文检索 `/openapi/todos` 仅存于排除范围与 §4.2 的"不实现"声明中，与代码实现（仅 `/api/todos`）完全一致。

### F02 [WARNING] [trim] LEGACY_FILE —— 📌 跨任务遗留项

**问题**：仓库根目录 `hello.py`（Hello World 程序）与待办事项功能无关。

**本轮不处置原因**：本阶段 `outputs_content` 为空，按硬性约束"仅允许生成期望产物中声明的文件，禁止修改任何代码文件"，删除 `hello.py` 属于代码文件变更，超出本阶段授权范围。

**建议**：在后续 coding/cleanup 阶段删除或移至归档目录。

### F03 [INFO] [verify] TEST_COVERAGE —— 📌 测试增强建议

**建议内容**：补充双边界组合用例（`name` 长度 101 + `description` 长度 501 同时出现，断言返回首个错误码 TODO_002）及 `tenant_id` 边界值用例。

**本轮不处置原因**：同上，涉及 `.py` 测试代码修改，超出本阶段授权范围。现有 13/13 测试已通过，不构成阻断。

## 4. 规范符合性检查（L1 静态检查）

| 检查项 | 规范要求 | 符合情况 |
|--------|----------|:--------:|
| 文档一致性 | 系分文档与代码实现一致 | ✅ 修复后一致 |
| 排除范围完整性 | 不实现的功能显式排除 | ✅ O01 已显式排除 |
| 产物路径 | 产物写入 cwd（仓库根目录）内 | ✅ 均位于 `.agents/` 下 |
| 代码修改约束 | 仅文档产物，不改动代码文件 | ✅ 未触碰任何 `.py` 文件 |

## 5. 汇总（Summary）

- **变更文件清单**：
  - `.agents/20260909-目标_帮助用户记录日常待办事项_核心功能/design.md`（2 处修复：§1 排除范围 + §4.2 OpenAPI 章节）
  - `.agents/20260909-目标_帮助用户记录日常待办事项_核心功能/fix_report.md`（本报告，新增）
- **代码变更**：无（0 个代码文件改动）
- **阻断项**：0（与 `run_context.json` blocker_count=0 一致）
- **遗留建议**（不阻断流水线）：清理 `hello.py`；补充双边界组合测试用例。

**修复结论**：评审发现的可闭环项已全部闭环，review 阶段问题修复完成。
