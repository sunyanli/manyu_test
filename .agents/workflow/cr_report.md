# 代码评审报告（G4 · verify）：日常待办事项 H5

> 评审节点：代码评审（code-review-skill）
> 评审对象：编码实现阶段产物（commit `749493d` [auto-dev] 编码实现，含 23 个源文件 + 5 个工程配置）
> 评审输入：`.agents/workflow/requirements.md`（G1）、`.agents/workflow/design.md`（G2）、`.agents/workflow/coding_summary.md`、`.agents/workflow/assets_manifest.md`
> 需求最小闭环：仅创建——新建待办（名称 + 描述 + 截止时间）+ 列表可见 + 成功反馈 + 空状态引导

---

## 1. 评审结论

**结论：✅ 通过（无阻断项），附 1 个重要问题 + 若干改进建议，建议在下一迭代修复。**

| 项 | 结论 |
|---|---|
| 🔴 Blocking（必须修复） | 0 |
| 🟡 Important（应当修复） | 1 |
| 🟢 Nit / Suggestion | 6 |

核心「仅创建」闭环（新建 → 列表可见 → 成功反馈 → 空态引导）实现完整、分层清晰、与 G2 定稿（方案 A「SSR 壳 + CSR 数据」、全站 B 类 noindex、支付宝蓝方向一、截止 chip）一致，未发现破坏闭环或安全性的阻断问题。

---

## 2. 评审范围与方法

**代码结构**（已逐文件通读）：

- `src/lib/`：`types.ts` / `constants.ts` / `seed.ts` / `api.ts` / `format.ts` —— 类型契约、常量、20 条 mock 种子、统一接口层、时间/截止格式化。
- `src/container/env.ts`：容器抽象（环境判定 + alipays scheme 唤起 + 标准导航）。
- `src/hooks/useInAlipay.ts`：SSR 安全环境判定（null → true/false）。
- `src/i18n/index.tsx`：中英双语 provider + antd-mobile locale + `document.title`/`<html lang>` 同步。
- `src/components/`：icons / AlipayGuard / SiteHeader / DeadlineChip / TodoCard / EmptyState / SkeletonList / TodoList / CreateForm。
- `src/app/`：`layout.tsx` / `robots.ts` / `page.tsx` / `create/page.tsx` / `globals.css` / `api/todos/route.ts`。
- 工程配置：`package.json` / `tsconfig.json` / `next.config.mjs`。

**验证方式（本次实际执行）**：

1. 静态逐文件审查（逻辑、边界、类型一致性、安全、可维护性）。
2. 针对疑点核对了依赖源码：`rc-field-form` 的 `Form.useWatch` 实现与类型声明、`antd-mobile` 的 `requiredMarkStyle`/`DatePicker.onConfirm` 支持。
3. 扫描 `src/` 中的禁止容器 API（`my.*` / `AlipayJSBridge.call` / `ap.tradePay`）与敏感串（私钥 / secret），确认无违规、无密钥泄露。
4. 检查既有构建产物 `.next/server/app/`（`index.html`、`create.html`、`robots.txt.body`）作为独立可观测证据。

> ⚠️ 诚实声明：本环境 `node` / `npm` 不在 PATH（执行 `npm run typecheck` 返回 exit 127 `command not found`），**未能独立复跑 typecheck/build**。`coding_summary.md` 中「typecheck 0 错误 / build 成功」为编码阶段自述记录，本次以其 + 既有 `.next` 产物佐证，未重新执行。运行时冒烟（GET/POST）本次亦未复跑。

---

## 3. 需求符合性核对

| 需求（Must） | 实现 | 结论 |
|---|---|---|
| F1 新增待办（名称/描述/截止时间） | `CreateForm.tsx` + `POST /api/todos`（服务端校验 name/description/dueAt） | ✅ |
| F2 列表页（createdAt 倒序） | `TodoList.tsx` + `GET /api/todos` 游标分页 | ✅ |
| F3 创建成功反馈 + 回列表置顶 | 成功 Toast + `setTimeout(()=>location.replace('/'),400)`；新项 `unshift` 后 createdAt 最新置顶 | ✅ |
| F4 空状态引导 | `EmptyState.tsx`（插画 + CTA） | ✅ |
| F11a 截止时间（选填） | `DatePicker precision="minute"` 可选可清除 + `DeadlineChip` 语义色 | ✅ |
| F20 中英双语 | `i18n/index.tsx` 词典 + 语言切换 + locale | ✅ |
| 全站 B 类 noindex | `layout.tsx` metadata robots + `app/robots.ts`（`Disallow: /`）双保险 | ✅ |
| 非小程序、禁容器 JSAPI | 仅 UA 判定引用 `AlipayJSBridge`，未调用任何 JSAPI | ✅ |

范围控制良好：完成/编辑/删除/详情仅做契约预留，未擅自扩围，符合「仅创建」最小闭环。

---

## 4. 问题清单（按严重度）

### 🟡 Important（应当修复）

**I1. `CreateForm` 的「离开未保存确认」是死代码，脏检测恒为 false，存在静默丢失用户输入**

- 位置：`src/components/CreateForm.tsx:29`（配合 `:31-48`）
- 现象：

```ts
const watched = Form.useWatch(["name", "description", "dueAt"], form) as FormValues | undefined;
// ...
const dirty = Boolean(watched?.name || watched?.description || watched?.dueAt);
```

- 根因：`Form.useWatch` 来自 `rc-field-form`。传入**数组** `["name","description","dueAt"]` 时，它被解析为**单一嵌套 namePath**（`values.name.description.dueAt`），而非「三个字段的值列表」。其类型声明即映射为嵌套访问 `GetGeneric[T1][T2][T3]`，运行时 `getValue(values, ["name","description","dueAt"])` 恒为 `undefined`。
- 后果：`watched` 恒为 `undefined`，`dirty` 恒为 `false`，`goBack` 中「放弃未保存的内容？」`Dialog.confirm`（`:38-47`）永远不会触发。用户填写名称/描述后点返回，内容被静默丢弃，与 design.md §5.4「有未保存内容返回时 Dialog.confirm」的设计不符。
- 建议：改为逐字段监听或直接取表单值，例如：

```ts
const watchedName = Form.useWatch("name", form);
const watchedDesc = Form.useWatch("description", form);
const watchedDueAt = Form.useWatch("dueAt", form);
const dirty = Boolean(watchedName || watchedDesc || watchedDueAt);
// 或回调内：const vals = form.getFieldsValue(true); const dirty = Boolean(vals.name || vals.description || vals.dueAt);
```

- 影响：不影响「仅创建」核心闭环，故不列为阻断；但属已交付功能（数据保护）失效，应在下一迭代修复。

### 🟢 Nit / Suggestion

**N1. `/create` 页面 `<title>` 未按设计区分**
`src/app/layout.tsx:7-11` 的全局 metadata 标题为「我的待办 - 日常待办」，`/create` 未覆盖；design.md §4.2 要求新建页标题为「新建待办 - 日常待办」。i18n 的 `document.title` 同步（`src/i18n/index.tsx:134`）也只写列表页标题。建议为 `/create` 增加 per-page metadata / generateMetadata。

**N2. `npm run lint` 实际不可运行**
`package.json:9` 声明 `"lint": "next lint"`，但未声明 `eslint` / `eslint-config-next` 依赖，且 `next.config.mjs:5` 设了 `eslint.ignoreDuringBuilds: true`。当前 `npm run lint` 会提示缺依赖或失败，lint 链路名存实亡。

**N3. mock id 生成与游标唯一性**
`src/app/api/todos/route.ts:14-16` 的 `genId()` 用 `Math.random()`（非加密、理论可碰撞）；游标分页（`:52`）依赖 `createdAt` 唯一（`findIndex(t => t.createdAt === createdAt)`），同一毫秒创建两条会产生相同 createdAt，可能跳页/重复。mock 场景可接受，接真实后端时由服务端生成 id/游标即可。

**N4. 截止 chip 的日差计算在 DST 边界可能偏差**
`src/lib/format.ts:40` 用 `Math.round((startOfDay(d)-startOfDay(now))/86400000)`。23/25 小时的夏令时切换日会被四舍五入到错误的一天；且「更早一天」即计为「已逾期 N 天」。对 +08:00 场景无影响，属可接受的 demo 简化。

**N5. POST 对 `dueAt` 非字符串值静默降级**
`src/app/api/todos/route.ts:74` 中 `dueAt` 为非法类型（如数字）时被静默置为 `null`，而非按契约返回 400 `VALIDATION_ERROR`；同时服务端错误 message 为硬编码中文（前端已用 i18n 兜底展示，故影响较低）。

**N6. `limit` 解析接受部分数字字符串**
`src/app/api/todos/route.ts:44` 的 `Number.parseInt("30abc", 10)` 会得到 30。边界宽松，mock 场景无实质危害，建议 `Number(...)` + `Number.isInteger` 校验。

---

## 5. 亮点（做得好的部分）

- **分层与契约清晰**：`lib` / `container` / `hooks` / `i18n` / `components` / `app` 职责分明；统一 `lib/api` 收敛接口，替换真实后端仅改 baseURL 与鉴权头，契约不变。
- **B 类 noindex 双保险**：`metadata.robots` + `robots.ts` 均生效，实测 SSR HTML 含 `noindex, nofollow`、`viewport-fit=cover`，`robots.txt` 为 `Disallow: /`，与设计一致。
- **安全基线良好**：POST 服务端二次校验（name/description 长度、dueAt 格式）；文本经 React 转义（无 `dangerouslySetInnerHTML`）；全仓库扫描未发现 `my.*` / `AlipayJSBridge.call` / `ap.tradePay` / 私钥串，`appId=20000067` 为支付宝公开唤起 appId、非机密。
- **SSR/CSR 边界处理到位**：`useInAlipay` 返回 null → 渲染中性壳，避免 hydration mismatch；`startedRef` 规避 React StrictMode 下 `useEffect` 双调导致的重复请求。
- **a11y 基础齐全**：按钮 `aria-label`、`sr-only` 加载提示、`:focus-visible`、44px 触控热区、`prefers-reduced-motion` 降级动画。

---

## 6. 决策

- 无 🔴 阻断项，**核心「仅创建」闭环可用，同意通过**。
- 建议后续迭代优先修复 I1（离开确认失效）；N1–N6 择机处理。
- 遗留风险（与 coding_summary 一致）：数据为内存 mock、UA 判定需真机复核、`alipays` 唤起白名单需开放平台配置、性能预算与 a11y 需真机 + Lighthouse/axe 复核。
