# 代码评审报告（cr_report）

> 任务节点：代码评审
> 采用技能：code-review-skill
> 评审对象：commit `23609d6`「[auto-dev] 编码实现 (stage: 编码实现, round: 1)」
> 需求：帮助内部用户记录日常待办；核心 = 新增待办；字段 = 名称 + 描述；最小闭环 = 仅创建
> 技术栈：Next.js 14 (App Router) + React 18 + TypeScript + AntD Mobile v5（支付宝第三方纯 H5）

---

## 1. 审查结论

**🔴 Request Changes（需修改后合入）**

- 阻塞项（🔴 blocking）：**1**
- 重要项（🟡 important）：**6**
- 可选项（🟢 nit）：**6**
- 好评（🎉 praise）：**7**

核心结论：本期「仅创建」最小闭环已完整实现（`POST /api/todos` 校验 + 创建 + 首页/列表即时回显），分层清晰、类型严格、安全边界正确。但首页「快速新增」存在回车双重提交风险，可能污染核心创建路径，需优先修复。此外 OAuth 登录闭环尚未打通（仅 demo 登录可用），需在真实后端联调前补齐。

---

## 2. 发现清单

### 🔴 Blocking（必须修复）

#### B1. 快速新增「回车即存」存在重复提交风险 — `src/components/QuickAddBar.tsx:44-58`

`Input` 的 `onEnterPress` 与 `<form onSubmit>` 同时绑定了同一个 `submit()` 动作：

```tsx
<form onSubmit={(e) => { e.preventDefault(); void submit(); }}>
  <Input onEnterPress={() => void submit()} ... />
  <button type="submit" ...>+</button>
</form>
```

当用户在输入框按回车时，可能同时触发 `onEnterPress` 与表单隐式提交（`onSubmit`），导致两次 `submit()`。而 `submit()` 内的去重依赖 React 异步状态 `busy`（`if (busy) return`），同一事件帧内两次调用读到的都是旧的 `busy=false`，无法去重，最终产生**两条重复待办**（两次 `POST /api/todos`）。

这直接污染「回车即存」这条核心创建路径，属于数据正确性缺陷。修复建议（任选其一）：
- 只保留一条提交路径：去掉 `onEnterPress`，仅保留 `<form onSubmit>`；或
- 在 `onEnterPress` 中调用 `e.preventDefault()` 阻止表单隐式提交，并只保留 `onEnterPress`；或
- 用 `useRef` 做同步 in-flight 守卫（`const inflight = useRef(false)`）替代仅靠 state 的 `busy` 判断。

> 说明：当前环境无 Node/npm，无法运行验证；此结论基于对触发链路的静态分析，属高风险项，建议真机/浏览器实测确认后修复。

---

### 🟡 Important（应当修复）

#### I1. OAuth 登录闭环未打通 — `src/app/api/auth/authorize/route.ts`、`src/lib/auth-server.ts`、`src/app/login/LoginClient.tsx`

`GET /api/auth/authorize` 构建跳转 URL 后 redirect_uri 回到 `/login`，但：
- `/login` 页面没有读取 URL 上的 `auth_code` 参数，也没有换取 uid 的逻辑；
- 仓库内没有 `auth_code → uid` 的 exchange / callback route；
- `/api/me` 硬编码返回 `{ uid: 'internal-user' }`。

因此真实支付宝授权登录目前**无法端到端走通**，仅有「演示登录」可用。需求 §5 将登录列为 Must，需在真实后端联调前补齐服务端换 uid + 登录态下发链路（当前阶段以 mock 兜底可接受，但需显式标注为未完成）。

#### I2. OAuth `state` 为固定值，缺少 CSRF 防护 — `src/lib/auth-server.ts:9`

```ts
state: 'todo-login',
```

`state` 应为每次授权请求随机生成的 nonce，并在回调时校验一致，用于防 CSRF。固定值存在登录 CSRF 风险，接入真实后端前必须改为随机 + 服务端校验。

#### I3. 分享封面图 404，`public/` 目录缺失 — `src/app/guide/page.tsx:19`

`og:image` 指向 `${SITE_URL}/og.png`，但仓库不存在 `public/` 目录，`/og.png` 与站点 favicon 均会 404，外部分享卡片封面无法展示。需补齐图片素材（与 `assets_manifest.md` 缺失已记录一致），或临时移除 og:image 避免死链。

#### I4. `pageSize` 无上限 — `src/app/api/todos/route.ts:15-16`

```ts
const pageSize = Number(sp.get('pageSize') ?? String(PAGE_SIZE)) || PAGE_SIZE;
```

未对 `pageSize` 做上限约束，传入超大值（如 `999999`）可一次返回全量数据，放大响应体与序列化开销。建议 clamp（如 `Math.min(pageSize, 100)` 或 `PAGE_SIZE * 5`），并在 `mock-store.listTodosMock` 同样约束。

#### I5. 空 patch / 非法 status 触发 no-op 更新 — `src/app/api/todos/[id]/route.ts:23-45` + `src/lib/mock-store.ts:76-90`

`PATCH` 时：
- `body = {}`（合法 JSON 空对象）会通过校验，最终 `updateTodoMock` 返回原对象但把 `updatedAt` 刷新为当前时间；
- `body.status = 'x'`（非法值）被静默忽略，同样刷新 `updatedAt`。

对无有效字段的 patch 建议返回 400（`NO_FIELDS`），非法 `status` 建议返回 400，避免无意义的时间戳变更与困惑。

#### I6. 登录/降级提示相互矛盾（可访问性死角）— `src/app/login/page.tsx` + `src/components/AlipayGuard.tsx`

`/login` 被 `AlipayGuard` 包裹：浏览器中访问 `/login` 时，`LoginClient`（含 `demoLogin` 演示登录、`loginBrowserHint` 提示）永远不会渲染，用户只会看到「请在支付宝中打开」。这些浏览器降级文案/按钮在浏览器里不可达。建议明确降级策略：要么让 `/login` 在浏览器中展示「支付宝授权说明 + 降级提示」而非被 guard 拦截，要么删除不可达文案。

---

### 🟢 Nit（可选）

#### N1. `EmptyState` SVG `role` 与 `aria-hidden` 冗余 — `src/components/EmptyState.tsx:18`

`role="img"` 与 `aria-hidden="true"` 并存；`aria-hidden` 已将其标记为装饰元素，`role="img"` 冗余，可去掉其一（建议保留 `aria-hidden`）。

#### N2. GET 请求携带 `Content-Type` — `src/lib/api.ts:5-8`

`request()` 对所有请求统一设置 `Content-Type: application/json`，GET 无 body 时该头无意义，可仅在 `body` 存在时设置。

#### N3. 分页失败破坏已加载列表 — `src/components/TodoList.tsx:28-38`

分页 `load(page, false)` 失败时直接把整体状态置为 `error`，丢弃已加载的第一页数据；且 toggle 失败静默无反馈。建议分页失败仅提示并保留已有列表，toggle 失败给 Toast。

#### N4. 历史页硬编码 pageSize + 计数漂移 — `src/app/history/HistoryClient.tsx:42,58-73`

`pageSize: 200` 硬编码（应复用 `PAGE_SIZE` 常量或分页）；`done` 计数在前端手动 ±1，切换 tab 或后端数据变化后可能与真实值漂移，建议以接口返回的 `total` 为准重算。

#### N5. 缺 `prefers-reduced-motion` — `src/app/globals.css`

CSS 未提供 `@media (prefers-reduced-motion: reduce)` 降级，动画/转场对无障碍偏好用户不友好（加分项）。

#### N6. B 类页面 SSR 直出为 null — `src/components/AlipayGuard.tsx:11-13`

`useInAlipay()` 服务端返回 `null` → `AlipayGuard` 返回 `null`，导致 B 类页面（`/` 首页等）服务端首屏为空壳（无骨架/无占位），与 requirements §7「CSR noindex 骨架/空壳」的预期有差距。功能不受影响（noindex），但首屏体验与骨架屏要求未完全落地。

---

### 🎉 Praise（做得好的地方）

1. **类型严格**：全仓开启 `strict` + `noUncheckedIndexedAccess`，`mock-store.updateTodoMock` 等对 `todos[idx]` 做了 `!prev` 判空，空值处理到位。
2. **分层清晰**：`container / hooks / components / lib / app` 职责分明，接口层（`lib/api.ts`）与 mock 后端同契约，后续替换真实后端只需改实现、签名不变。
3. **安全边界正确**：服务端密钥仅 `auth-server.ts` 读取，前端仅白名单 `NEXT_PUBLIC_SITE_URL`；`.env.example` 无 `NEXT_PUBLIC_`+secret；`app/api/*` 无 `NEXT_PUBLIC_*` 引用。
4. **无注入面**：所有用户输入经 React 转义输出，`dangerouslySetInnerHTML` 仅用于静态 JSON-LD，无 XSS 风险；mock 为内存存储，无 SQL 注入面。
5. **SEO 一致性**：全站 noindex、`robots.ts` `Disallow: /`、`sitemap.ts` 空数组、无 UA 分流（cloaking 风险为 0）。
6. **无障碍**：语义化标签、表单 `label`、`sr-only`、`:focus-visible`、safe-area 适配、≥44px 触控热区均有落地。
7. **i18n 完整**：zh-CN 默认 + en，UI/校验/空态/分享文案双语齐全。

---

## 3. 验证情况（Verification）

> 环境事实：当前容器**无 Node.js/npm**（`command -v node/npm` 均不存在），`next build` / `next lint` / `tsc --noEmit` / 单测无法执行。按「构建可用性不阻塞任务」协议降级为**静态代码审查**。

| 检查项 | 结果 | 说明 |
|---|---|---|
| 核心闭环（POST /api/todos 校验 + 创建 + 回显） | ✅ 通过 | 名称必填/trim/≤100、描述≤500，创建后 `unshift` 置顶回显 |
| 类型一致性静态核对 | ✅ 通过 | 未发现明显 TS 编译错误（strict + noUncheckedIndexedAccess 下判空到位） |
| 注入面 / XSS | ✅ 通过 | 输出走 React 转义，无原始 HTML 注入 |
| 安全边界（secret 不进前端） | ✅ 通过 | 服务端密钥与前端白名单分离 |
| SEO 一致性 | ✅ 通过 | noindex / robots / sitemap 一致，无 cloaking |
| 构建/单测运行 | ⛔ 未执行 | 环境缺 Node，无法运行验证（遗留风险，见下） |

**未执行（依赖缺失）**：`npm install` + `next build`（内含 tsc/lint）+ `next lint` + 单测 + 支付宝真机核对。

**遗留风险**：B1（回车双重提交）与部分运行时行为需在有 Node 的环境或真机中实测确认；OAuth 闭环（I1/I2）需真实后端联调。

---

## 4. 最终结论

- **评审结论**：🔴 Request Changes（1 个阻塞项 B1 需修复后合入；6 个重要项建议随真实后端联调一并闭环）。
- **review_blocker_count = 1**。
- 建议合入顺序：先修复 B1 保证「仅创建」路径数据正确，其余 🟡 项在真实后端联调阶段逐项闭环。
