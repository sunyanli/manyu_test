# 方案设计（G2）：日常待办事项 H5

> 输入：`.agents/workflow/requirements.md`（G1 已澄清，无未决问题）。
> 本文档为 G2 设计选型产物，是 G3 编码与 G4 验收的输入。两处关键取舍（渲染方式、视觉方向）经用户确认后已按推荐项定稿。

---

## 0. 结论速览

- **形态**：支付宝开放平台「网页 & 移动应用」下的**第三方纯 H5**，React + TypeScript + AntD Mobile v5 + Next.js(App Router)。非小程序，不使用 `my.*`、`AlipayJSBridge.call`、`ap.tradePay`。
- **收录**：纯内部工具，**全站 B 类 noindex**，无 sitemap、无 og 分享、无结构化数据（Q1 已确认）。
- **渲染**（已定稿）：方案 A「SSR 壳 + CSR 数据」（Next 默认渲染壳，数据客户端拉取，全站 noindex）。
- **视觉**（已定稿）：方向一「清爽专注·支付宝蓝」；签名元素＝「倒计时截止 chip」。
- **闭环**：仅创建——列表可见 + 新建（名称/描述/截止时间）+ 成功反馈 + 空状态引导；完成/编辑/删除/详情为 Should 预留。
- **图片**：logo/头像占位、空态插画、表单图标、成功图标、favicon；**无照片类 WebP、无 og:image**（内部工具无实拍/无外部分享）。

---

## 1. URL 结构（A/B/C）

### 1.1 页面清单

| 路径 | 页面 | 分类 | 说明 |
|---|---|---|---|
| `/` | 待办列表页 | **B** | 登录后使用；按创建时间倒序展示我的待办；noindex |
| `/create` | 新建待办页 | **B** | 登录后使用；名称+描述+截止时间表单；noindex |
| `/detail/:id` | 待办详情页（Should 预留） | B | 单条完整字段；本期不实现 |
| `/login` + `/auth/callback` | 登录/OAuth 回调（Should 预留） | B | publicAppAuthorize 跳转回跳；本期 mock 固定 uid 暂不接真实 |
| （可选）浏览器引导页 | 请在支付宝中打开 | C | 本期内嵌在 AlipayGuard 组件中实现，不单设路由（见 §3.2） |

### 1.2 分类口径

- **A 类（公开可索引）**：需要 SSR/SSG 吐可抓 HTML、进 sitemap、配 og 分享。**本项目无 A 类页面**——内部工具，不外露、不收录、不分享（Q1）。
- **B 类（App 内功能页）**：noindex、不进 sitemap；浏览器打开给「请在支付宝中打开」降级屏；支付宝内渲染真实内容。
- **C 类（引导）**：把浏览器用户唤起进支付宝。本期以 `AlipayGuard` 组件承载，不单独成页。

### 1.3 关键取舍：导航模型（已定，说明理由）

第三方纯 H5 **没有多 WebView**，跨屏＝标准多页（`location` + 历史后退），不做 SPA 全站多屏路由。

- **选项 1（推荐）标准多页**：`/` 与 `/create` 为两个独立 URL，跳转用 `location`/`history`，返回用 `history.back()`。理由：符合 skill 容器范式；无原生转场/标题栏可依赖，独立 URL 反而让「返回、刷新、前进」行为最可预期；后续接真实 OAuth 时回跳 URL 天然清晰。
- **选项 2 SPA 客户端路由（单壳多视图）**：仅在「多步流程共享状态」时才用（如复杂发布流），本项目新建表单一步完成、无需跨步骤共享状态，不采用。
- **选项 3 单页全内嵌**：把新建做成列表页内弹层/抽屉。优点是无跳转；缺点是表单+键盘+截止时间选择器在弹层内体验差、URL 无法直达。不采用。

> 结论：选项 1。`/create` 成功后 `location.replace('/')`（或客户端 `router.replace`）回列表，避免历史栈里残留新建页。

---

## 2. 渲染决策

### 2.1 逐页渲染

| 页面 | 渲染方式 | 理由 |
|---|---|---|
| `/` 列表页 | **CSR 数据 + SSR 壳** | 数据强个人化（登录后拉取），无法无登录拼出；首屏壳（标题栏+骨架）SSR 直出保 LCP/CLS，数据 hydrate 后客户端拉取 |
| `/create` 新建页 | **CSR + SSR 壳** | 纯表单功能页，无公开内容；SSR 壳保首屏，提交/校验全客户端 |
| `/detail/:id`（Should） | CSR + noindex | 登录后单条数据，同列表逻辑 |
| `/login` + 回调（Should） | 服务端重定向 | publicAppAuthorize 是服务端 302，非页面渲染 |

### 2.2 关键取舍：渲染方式（已定稿：A）

- **方案 A（推荐）SSR 壳 + CSR 数据**：Next App Router，页面组件标 `'use client'`，Next 仍把「标题栏 + 骨架屏 + 空态占位」直出进 HTML，数据用 `useEffect`/SWR 客户端拉。全站 `robots: noindex`。
  - 理由：既守 skill「功能页 CSR + noindex」的结论，又借 Next 默认 SSR 壳消灭白屏、压低 CLS；不额外引入 SSG 复杂度；后续要接真实登录也能平滑升级。
- **方案 B 纯 CSR（客户端全渲染，next build 静态输出或 CSR-only）**：最简，但首屏 HTML 为空，白屏更长，骨架屏要客户端先跑 JS 才画，LCP/CLS 预算更难守。
- **方案 C 全站 SSG/ISR**：为「无公开内容、登录后数据」的内部工具引入 SSG 无 SEO 收益，还增加构建/再生成成本，不推荐。

> 结论（已定稿）：A。与 requirements 的「CSR + noindex」一致，只是进一步明确「壳层 SSR 直出、数据 CSR」。

### 2.3 数据与缓存

- 数据：客户端经统一 `lib/api` 调 `/api/*`（开发期 MSW 拦截 + Next route handler 兜底）。
- HTML 短缓存回源；静态资产哈希文件名长缓存；不引入离线包（纯 H5 无此能力）。

---

## 3. 容器能力

> 原则：第三方纯 H5 拿不到小程序容器能力，只用「开放平台服务端接口 + alipays scheme + 标准 Web」。

### 3.1 能力边界

| 能力 | 是否使用 | 本期落地方式 |
|---|---|---|
| 支付宝 OAuth 授权 | 预留（Should） | 服务端 `publicAppAuthorize` 302 跳转 → 回调拿 auth_code → `alipay.system.oauth.token` 换 uid → 自签会话 cookie。可选 `ap.getAuthCode` 半屏（默认不引入） |
| 手机网站支付 `alipay.trade.wap.pay` | **本期不做**（Q7） | 契约预留：服务端生成支付 URL → 前端 `location.assign(url)`；**禁用 `ap.tradePay`**（公网无 H5 版） |
| alipays scheme 唤起 | 使用（浏览器降级） | `alipays://platformapi/startapp?appId=20000067&url=<encodeURIComponent(本页URL)>`，经 `https://render.alipay.com/p/s/i/?scheme=<encodeURIComponent(scheme)>` landing 跳（非 alipay.com 域名需开放平台配白名单） |
| 标准 Web（Level 1） | 使用（主路径） | `location`/`history`/`fetch`/`localStorage`/`document`/`visibilitychange` |

**明确禁用（不写、不留 opt-in 占位）**：

- `my.*`（小程序 API）：`my.navigateTo`、`my.share`、`my.tradePay`、`my.getAuthCode` 等。
- `AlipayJSBridge.call(...)` 容器 JSAPI：`pushWindow`/`popWindow`/`setTitle`/`setShareContent` 等（第三方不可用）。
- `ap.tradePay`（公网未确认为第三方 H5 公开能力；H5 支付一律服务端 `wap.pay` 跳转）。
- 离线包、客户端开关、原生标题栏、App 内自定义分享卡片。

### 3.2 环境判定与降级

```ts
// container/env.ts
export function isAlipayClient(): boolean {
  if (typeof window !== 'undefined' && typeof (window as any).AlipayJSBridge !== 'undefined') return true;
  return /AlipayClient|AliApp|Alipay/i.test(typeof navigator !== 'undefined' ? navigator.userAgent : '');
}
```

- 判定**只用于**：启用公开能力、给浏览器用户做降级。绝不据此调 `AlipayJSBridge` 容器 JSAPI。
- React 侧用 `useInAlipay()` 钩子（SSR 返回 null，挂载后判定）防闪烁。
- `AlipayGuard` 组件：B 类功能页在**浏览器**打开时不渲染整页 CSR，显示「请在支付宝中打开」+ `openPageInAlipay()` 唤起按钮；在支付宝内渲染子内容。
- 反 cloaking：公开 HTML 对爬虫/用户一致；本项目全站 B 类本就不公开内容，服务端 `isAlipayClientSSR` 仅用于极轻分流（如不预取登录态数据），不改可见内容。

### 3.3 `container/*` 封装契约

业务页只调人类可读 API，不直穿底层：

```ts
// container/* 对业务暴露（本期实现子集）
openPageInAlipay(targetUrl)   // scheme + landing 唤起
isAlipayClient()              // 环境判定
startOAuth()                  // Should：location.assign(publicAppAuthorize 服务端 URL)
startPay(tradeNo)             // 预留：location.assign(服务端 wap.pay URL)，本期空实现 + 注释
```

---

## 4. SEO 方案

### 4.1 结论

全站 **B 类 noindex**（Q1：纯内部工具，不需收录/分享）。因此：

- **robots.txt**：`User-agent: *` + `Disallow: /`（全站 noindex 双保险）。
- **sitemap.xml**：不产出（无 A 类 URL）。
- **og:\*/结构化数据（JSON-LD）**：不产出（无外部分享、无公开可索引内容）。
- **canonical**：不设（无公开 URL 需要规范化）。

### 4.2 每页 head（仍保留的最小集合）

虽 noindex，仍保留站内可用性/书签所需的基础 meta：

```html
<!-- 全站 -->
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover" />
<meta name="robots" content="noindex,nofollow" />
<!-- 列表页 -->
<title>我的待办 - 日常待办</title>
<meta name="description" content="记录日常待办事项，快速新建与查看清单。" />
<!-- 新建页 -->
<title>新建待办 - 日常待办</title>
<meta name="description" content="新建一条待办事项。" />
```

- `<title>` 同时充当分享/书签标题（无原生 setTitle）。
- i18n 下 `title`/`description` 随语言包切换（客户端更新 `document.title`）。

### 4.3 为什么不做 og/sitemap/结构化数据

内部工具无公开页面、无外部分享落地（Q1 已确认）；og:image、sitemap、JSON-LD 只对 A 类公开页有收益，为 B 类内部工具引入是无效开销。若未来要「分享单条待办」或「公开落地页」，再按 skill 的 A 类方案补 head 与 og 图（见 §7.5 预留说明）。

---

## 5. 视觉设计

### 5.1 视觉方向备选（已定稿：方向一）

#### 方向一（推荐）「清爽专注 · 支付宝蓝」

- **配色**：主色支付宝蓝 `#1677FF`；浅灰底 `#F5F6FA` + 白卡片；成功绿 `#00B578` 点缀；CTA 用蓝青渐变 `linear-gradient(135deg,#1677FF,#00C2FF)`。
- **版式**：单列卡片流 + 顶部吸顶标题栏 + 底部悬浮「＋ 新建」FAB；大量留白。
- **氛围关键词**：干净、轻量、高效、可信（支付宝原生感）。
- **参考产品**：Apple Reminders、Things 3、Microsoft To Do、支付宝首页。
- **签名元素**：「倒计时截止 chip」——每条待办一枚等宽字体截止标签：无截止＝灰 `—`；今天/明天＝蓝 `今天 18:00`；已过期＝红 `已逾期 1 天`；更远＝中性 `11-12 15:00`。把待办的「时间承诺」显式化，一眼看清「该不该现在做」。
- **推荐理由**：与 Q6 已确认的「支付宝蓝清爽风」一致；内部工具零学习成本；在支付宝 WebView 内视觉无缝；单 FAB 主操作最贴合「仅创建」闭环；等宽字体 + 语义色签名既克制又有辨识度，不落入通用模板。

#### 方向二「温暖手账 · 便签纸」

- **配色**：米白纸感底 `#FAF7F0` + 焦糖/琥珀强调 `#C96F4A` + 衬线标题。
- **版式**：便签卡片错落、描边/柔和阴影如贴纸。
- **氛围关键词**：温暖、亲切、记录感、慢。
- **参考产品**：Notion（纸感）、幕布、纸质手账。
- **不推荐理由**：情绪化，但「内部效率工具」要的是快与可信；纸感与支付/金融语境略冲突；也易落入 AI 常见「奶油底 + 衬线 + 焦糖色」套路。

#### 方向三「暗夜效率 · 高对比」

- **配色**：近黑底 `#0B0E14` + 酸性绿 `#22C55E` 或荧光点缀 + 等宽数据字体。
- **版式**：高密度、数据表感、状态点。
- **氛围关键词**：极客、专注、性能感、夜行。
- **参考产品**：Linear、Arc、VSCode 深色。
- **不推荐理由**：个性强但内部广泛用户（含新人、职能同学）接受度分化；支付宝内以明亮环境为主，暗色不统一；深浅双主题成本超出本期最小闭环。

> **结论（已定稿）**：方向一。在方向一内部用「截止 chip + 等宽字体」做唯一视觉风险点，其余保持克制。

### 5.2 设计令牌（方向一定稿基准）

**颜色**

| Token | 值 | 用途 |
|---|---|---|
| `--todo-primary` | `#1677FF` | 主色、链接、选中态 |
| `--todo-primary-deep` | `#0958D9` | 按下/hover |
| `--todo-primary-soft` | `#E6F4FF` | 选中底、浅强调 |
| `--todo-gradient` | `linear-gradient(135deg,#1677FF,#00C2FF)` | 主 CTA / logo 标记 / 顶栏强调（克制使用） |
| `--todo-success` | `#00B578` | 成功/已完成 |
| `--todo-warning` | `#FF8F1F` | 临近截止、警示 |
| `--todo-danger` | `#FF3141` | 已逾期、错误 |
| `--todo-bg` | `#F5F6FA` | 页面底 |
| `--todo-card` | `#FFFFFF` | 卡片底 |
| `--todo-text` | `#1A1A1A` | 主文字 |
| `--todo-text-2` | `#666666` | 次级文字 |
| `--todo-text-3` | `#999999` | 占位/弱化 |
| `--todo-border` | `#EEEEEE` | 分割线 |

**字体**

- 正文：系统字体栈 `-apple-system, BlinkMacSystemFont, "PingFang SC", "SF Pro Text", "Segoe UI", Roboto, "Helvetica Neue", Arial, "Microsoft YaHei", sans-serif`。
- **数据/时间（签名）**：等宽 `ui-monospace, "SF Mono", "Roboto Mono", "JetBrains Mono", monospace`——仅用于 `createdAt`/`dueAt` 时间与截止 chip，形成「效率工具的数据感」。

**间距 / 圆角 / 阴影（8pt 栅格）**

- 间距：4 / 8 / 12 / 16 / 20 / 24 / 32。
- 圆角：卡片 16px；输入框/弹层 12px；按钮 999px（胶囊）；小 chip 999px。
- 阴影：卡片 `0 1px 2px rgba(16,24,40,.04)`；FAB/浮层 `0 8px 24px rgba(22,119,255,.18)`。
- 触控热区 ≥ 44×44px；文字最小可读 14px；1px 边框用 `0.5px`/`scale(.5)`。

**AntD Mobile 主题变量映射（`ConfigProvider` / CSS 变量覆盖）**

```css
:root {
  --adm-color-primary: #1677FF;
  --adm-color-success: #00B578;
  --adm-color-warning: #FF8F1F;
  --adm-color-danger:  #FF3141;
  --adm-color-text:          #1A1A1A;
  --adm-color-text-secondary:#666666;
  --adm-color-weak:          #999999;
  --adm-color-border:        #EEEEEE;
  --adm-color-background:    #FFFFFF;
  --adm-color-box:           #F5F6FA;
  --adm-radius-s: 8px;
  --adm-radius-m: 12px;
  --adm-radius-l: 16px;
  --adm-font-size-main: 17px;
}
```

**组件风格**：胶囊主按钮（全宽吸底 / FAB）、白卡片列表、`Dialog`/`Toast` 用 antd-mobile 内置、`Skeleton` 骨架、`Empty` 空态、`DatePicker` 日期时间、`InfiniteScroll`/`PullToRefresh` 列表加载。

### 5.3 逐页线框与内容

#### 页面一：待办列表页 `/`

**线框（自上而下）**

```
┌─────────────────────────────────┐
│ [logo 我的待办]           [头像A] │  ← 吸顶标题栏，safe-area-top
├─────────────────────────────────┤
│ 今天 · 3 件待办            (副标) │  ← 日期/摘要条（可选首屏）
├─────────────────────────────────┤
│ ● 走查 PR #1024 支付模块   [今天18:00] │  ← 卡片1：状态点+名称
│   按编码规范走查支付模块…     09:12 │     +描述摘要+创建时间(mono)
│ ● 整理本周迭代周会纪要      [明天12:00] │  ← 卡片2
│   把周一迭代周会录音整理成…   10:22 │
│ ───────── 加载中… ─────────       │  ← InfiniteScroll 触底加载
└─────────────────────────────────┘
                  [＋ 新建待办]      ← 悬浮 FAB，safe-area-bottom
```

**区块与组件**

1. **吸顶标题栏**：自绘 Header（不用原生栏），左＝内联 SVG logo + 站点名「我的待办」（中/英 i18n），右＝头像（登录后支付宝头像 URL / mock 首字母头像）。`position: sticky; top: 0` + `env(safe-area-inset-top)`；滚动超过阈值加投影。
2. **今日摘要条（可选）**：显示「今天 · N 件待办」/ "Today · N todos"，数据来自列表统计（open 且 dueAt 为今天），无数据时隐藏。
3. **待办列表（主区）**：antd-mobile `List` + `InfiniteScroll`，`key` 用稳定业务 id。每卡片：
   - 左：状态点（本期全 open，绿点；done 预留灰对勾）+ 名称（1–2 行，超长省略）+ 描述摘要（1 行省略）+ 创建时间（mono，次级色）。
   - 右：截止时间 chip（mono，语义色：蓝=今天/明天、红=已逾期、灰=无截止、中性=更远）。
   - **交互**：整行点击 → 预留详情（本期 Toast「详情将在下个版本开放」）；触底 → `InfiniteScroll` 加载下一页（cursor 游标，每页 20）；下拉 → `PullToRefresh` 刷新；卡片 `:active` 反馈。
4. **空状态**（无数据时替换列表）：antd-mobile `Empty` + 空态插画 + 「记下第一件事」+ 主按钮「新建待办」→ `/create`。
5. **悬浮 FAB**：「＋ 新建待办」胶囊按钮，`position: fixed; right:16px; bottom: calc(env(safe-area-inset-bottom)+24px)`，点击 `location.assign('/create')`。

**真实内容示例（列表项）**

| 名称 | 描述摘要 | 截止 chip | 创建时间 |
|---|---|---|---|
| 走查 PR #1024 支付模块 | 按编码规范走查支付模块，重点核对私钥不入前端 | `今天 18:00`（蓝） | 11-09 09:12 |
| 整理本周迭代周会纪要 | 把周一迭代周会录音整理成纪要，同步到项目群 | `明天 12:00`（蓝） | 11-09 10:22 |
| 归档上季度待办并复盘 | 清理已完成事项，输出复盘小结 | `—`（灰） | 11-06 09:55 |

#### 页面二：新建待办页 `/create`

**线框（自上而下）**

```
┌─────────────────────────────────┐
│ ← 返回                新建待办     │  ← 标题栏
├─────────────────────────────────┤
│ 事项名称 *                     (0/60)│  ← Input 必填
│ ┌─────────────────────────────┐ │
│ │ 例如：整理本周周会纪要        │ │
│ └─────────────────────────────┘ │
│ 描述（选填）                  (0/500)│  ← TextArea 选填
│ ┌─────────────────────────────┐ │
│ │ 把录音整理成纪要同步到群       │ │
│ └─────────────────────────────┘ │
│ 截止时间（选填）                  │  ← DatePicker 选填可清除
│ [ 未设置 ▸ ]                     │
└─────────────────────────────────┘
      [        保存        ]        ← 全宽胶囊，吸底 safe-area-bottom
```

**区块与组件**

1. **标题栏**：返回按钮（`history.back()`，`history.length<=1` 时回 `/`）+ 标题「新建待办」。
2. **表单**：antd-mobile `Form`：
   - 名称：`Input`，必填、`maxLength=60`、实时字数计数器、`placeholder` 中/英示例。
   - 描述：`TextArea`，选填、`maxLength=500`、`autoSize={{minRows:3,maxRows:8}}`、计数器。
   - 截止时间：`DatePicker`（日期时间，precision 到分钟）、选填、可清除；显示「未设置 ▸」。
   - 聚焦 `scrollIntoView({block:'center'})` 防键盘遮挡；输入 `font-size≥16px` 防 iOS 自动放大。
3. **吸底提交栏**：全宽胶囊主按钮「保存」/「创建」，`padding-bottom: calc(env(safe-area-inset-bottom)+16px)`。
   - **交互**：点击 → 前端校验（name 非空/长度）→ POST `/api/todos` → 提交中按钮 disabled+loading（防重复）→ 成功 Toast「已创建」→ `router.replace('/')` 回列表并置顶新项；失败 Toast「创建失败，请重试」+ 保留已填内容。

### 5.4 状态与体验细节

**全状态覆盖**

| 状态 | 页面 | 表现 |
|---|---|---|
| 加载中 | 列表 | antd-mobile `Skeleton` 模拟 3 条卡片（圆点+两行文字+右侧 chip）；接口 <300ms 直接出结果不闪骨架 |
| 加载中 | 新建 | 提交按钮 `loading` + 文案「保存中…」 |
| 空态 | 列表 | 插画 + 「记下第一件事」+ CTA「新建待办」 |
| 错误/断网 | 列表 | `ErrorBlock`/自绘「加载失败」+ 区分文案（网络异常 vs 服务异常）+「重试」按钮；弱网骨架 + 超时兜底 |
| 错误 | 新建 | 提交失败 Toast + 保留表单内容，可再试 |
| 未登录 | 列表/新建 | 预留：引导登录（真实 OAuth 后才出现）；本期 mock 以固定测试 uid 直接进入，UI 预留未登录态文案「登录后查看我的待办」 |
| 无权限 | 列表/新建 | 预留：uid 无数据访问权限（真实后端才出现），显示「无权限访问」+ 联系管理员 |
| 支付成功/失败/处理中 | 全局 | **本期不做支付（Q7）**。仅接口契约预留：处理中＝骨架/轮询；成功＝结果 toast；失败＝回退重试。UI 不实现 |

**微交互与体验细节**

- **转场**：页间为整页加载（无原生转场），用骨架 + 数据直出降白；首屏列表项淡入+上移（stagger ≤5 项）；`prefers-reduced-motion` 时关闭动画。
- **反馈 Toast**：antd-mobile `Toast`（成功「已创建」/失败「创建失败，请重试」/网络「网络异常，请重试」）；不出现裸 `alert/confirm`。
- **吸顶**：列表标题栏 sticky top + 滚动加投影。
- **返回顶部**：列表滚动较长时出现「回到顶部」浮钮（Could 预留，本期不实现）。
- **下拉刷新**：`PullToRefresh` 本期启用（列表可见闭环的一部分）。
- **加载更多**：`InfiniteScroll` 游标分页，底部「加载中…/没有更多了」。
- **点击防抖/防重**：提交按钮提交即禁用；可点元素 `:active` 反馈 + 热区 ≥44px。
- **分享引导**：本期无外部分享（Q1），不设分享按钮；未来如启用靠 URL + scheme 唤起（预留说明）。
- **收藏/历史**：本期不做（F15/F13 Could），仅在列表长按/详情预留入口设计，不实现。
- **语言切换**：中英双语；标题栏或设置入口切换，`localStorage` + cookie 持久化；`document.title` 随语言更新。
- **键盘**：失焦点空白收键盘；提交后收起键盘；输入聚焦防遮挡。
- **离开未保存表单**：WebView 对 `onbeforeunload` 支持弱，以应用内确认弹层为主（有未保存内容返回时 `Dialog.confirm`）。

---

## 6. 接口契约与 mock

> 开发期优先 mock（MSW 拦截 fetch + Next route handler 兜底），与真实后端同签名；SSR 期 mock 不得抛错致整页降级。统一 `lib/api` 收敛。所有数据按 uid 隔离（本期 mock 用固定测试 uid `test_uid_0001`）。

### 6.1 通用约定

- 基础路径：`/api`。
- 时间字段：ISO 8601 带偏移，示例 `2026-11-10T12:00:00+08:00`；前端展示时按语言/时区格式化。
- 列表排序：`createdAt` 倒序（新在前）。
- 分页：游标（`cursor` + `limit`），返回 `items` + `nextCursor`（null 表示没有更多）。
- 错误统一形状：

```json
{ "code": "VALIDATION_ERROR", "message": "事项名称不能为空", "details": { "field": "name" } }
```

- 错误码：`VALIDATION_ERROR` / `UNAUTHORIZED` / `NOT_FOUND` / `RATE_LIMITED` / `INTERNAL`。

**核心数据类型**

```ts
type TodoStatus = 'open' | 'done';
interface Todo {
  id: string;            // 稳定业务 id，如 "td_8f3a1c9e"
  name: string;          // 必填 1–60 字
  description: string;   // 选填 0–500 字
  dueAt: string | null;  // 选填 ISO 8601 或 null
  status: TodoStatus;    // 本期固定 'open'，'done' 为 Should 预留
  createdAt: string;     // ISO 8601
  updatedAt: string;     // ISO 8601
}
```

### 6.2 接口明细与示例

#### 6.2.1 `POST /api/todos` 新建待办（Must）

**入参**

| 字段 | 类型 | 必填 | 约束 |
|---|---|---|---|
| name | string | 是 | 1–60 字 |
| description | string | 否 | 0–500 字 |
| dueAt | string \| null | 否 | ISO 8601 或 null |

**返回**：`201` 创建的 `Todo`（`status='open'`，`id/createdAt/updatedAt` 由服务端生成）。

**示例 1（全字段）**

```json
// 请求
{ "name": "整理本周迭代周会纪要", "description": "把周一迭代周会录音整理成纪要，同步到项目群", "dueAt": "2026-11-10T12:00:00+08:00" }
// 响应 201
{ "id": "td_8f3a1c9e", "name": "整理本周迭代周会纪要", "description": "把周一迭代周会录音整理成纪要，同步到项目群", "dueAt": "2026-11-10T12:00:00+08:00", "status": "open", "createdAt": "2026-11-09T10:22:00+08:00", "updatedAt": "2026-11-09T10:22:00+08:00" }
```

**示例 2（最小字段）**

```json
// 请求
{ "name": "提交 10 月差旅报销单" }
// 响应 201
{ "id": "td_91c2b7d4", "name": "提交 10 月差旅报销单", "description": "", "dueAt": null, "status": "open", "createdAt": "2026-11-09T11:00:00+08:00", "updatedAt": "2026-11-09T11:00:00+08:00" }
```

**错误示例**

```json
// 名称为空
{ "code": "VALIDATION_ERROR", "message": "事项名称不能为空", "details": { "field": "name" } }
// 名称超长
{ "code": "VALIDATION_ERROR", "message": "事项名称不能超过 60 字", "details": { "field": "name" } }
```

#### 6.2.2 `GET /api/todos` 待办列表（Must）

**入参**

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| cursor | string | 否 | — | 游标（首次为空） |
| limit | number | 否 | 20 | 每页条数，最大 50 |

**返回**：`200` `{ items: Todo[], nextCursor: string | null }`。

**示例（前 3 条 + 分页）**

```json
{
  "items": [
    { "id": "td_8f3a1c9e", "name": "走查 PR #1024 支付模块", "description": "按编码规范走查支付模块，重点核对私钥不入前端", "dueAt": "2026-11-09T18:00:00+08:00", "status": "open", "createdAt": "2026-11-09T09:12:00+08:00", "updatedAt": "2026-11-09T09:12:00+08:00" },
    { "id": "td_7e2b0d8a", "name": "整理本周迭代周会纪要", "description": "把周一迭代周会录音整理成纪要，同步到项目群", "dueAt": "2026-11-10T12:00:00+08:00", "status": "open", "createdAt": "2026-11-09T08:40:00+08:00", "updatedAt": "2026-11-09T08:40:00+08:00" },
    { "id": "td_6d1c9f7b", "name": "跟进客户 A 登录问题反馈", "description": "复现支付宝内登录偶发失败并定位", "dueAt": "2026-11-11T18:00:00+08:00", "status": "open", "createdAt": "2026-11-08T18:40:00+08:00", "updatedAt": "2026-11-08T18:40:00+08:00" }
  ],
  "nextCursor": "eyJjcmVhdGVkQXQiOiIyMDI2LTExLTA4VDE4OjQwOjAwKzA4OjAwIn0"
}
```

**mock 列表完整 20 条**（`mock/todos.ts` 固定种子，`createdAt` 倒序，本期全 `status:'open'`；含 5 条 `done` 供 Should 预留演示）

```json
[
  { "id": "td_1001", "name": "整理本周迭代周会纪要", "description": "把周一迭代周会录音整理成纪要，同步到项目群", "dueAt": "2026-11-10T12:00:00+08:00", "status": "open", "createdAt": "2026-11-09T09:12:00+08:00" },
  { "id": "td_1002", "name": "走查 PR #1024 支付模块", "description": "按编码规范走查支付模块，重点核对私钥不入前端", "dueAt": "2026-11-09T18:00:00+08:00", "status": "open", "createdAt": "2026-11-08T18:40:00+08:00" },
  { "id": "td_1003", "name": "提交 10 月差旅报销单", "description": "整理杭州出差发票，走 OA 报销流程", "dueAt": "2026-11-08T17:00:00+08:00", "status": "done", "createdAt": "2026-11-08T11:05:00+08:00" },
  { "id": "td_1004", "name": "跟进客户 A 登录问题反馈", "description": "复现支付宝内登录偶发失败并定位", "dueAt": "2026-11-11T18:00:00+08:00", "status": "open", "createdAt": "2026-11-08T10:22:00+08:00" },
  { "id": "td_1005", "name": "更新 sitemap 与 robots 配置", "description": "确认 A 类页面收录、B 类 noindex", "dueAt": "2026-11-07T17:00:00+08:00", "status": "done", "createdAt": "2026-11-07T17:30:00+08:00" },
  { "id": "td_1006", "name": "写 H5 授权联调自测清单", "description": "覆盖 publicAppAuthorize 回跳、code 换 uid", "dueAt": "2026-11-12T15:00:00+08:00", "status": "open", "createdAt": "2026-11-07T15:48:00+08:00" },
  { "id": "td_1007", "name": "准备周五技术分享 PPT", "description": "主题《第三方 H5 的能力边界》，约 20 页", "dueAt": "2026-11-13T10:00:00+08:00", "status": "open", "createdAt": "2026-11-07T10:01:00+08:00" },
  { "id": "td_1008", "name": "核对灰度发布 checklist", "description": "确认 CDN 权重、回滚方案、监控告警", "dueAt": "2026-11-10T18:00:00+08:00", "status": "open", "createdAt": "2026-11-06T19:15:00+08:00" },
  { "id": "td_1009", "name": "给新同事开通测试环境权限", "description": "在开放平台授权测试应用", "dueAt": "2026-11-06T14:00:00+08:00", "status": "done", "createdAt": "2026-11-06T14:33:00+08:00" },
  { "id": "td_1010", "name": "归档上季度待办并复盘", "description": "清理已完成事项，输出复盘小结", "dueAt": "2026-11-13T18:00:00+08:00", "status": "open", "createdAt": "2026-11-06T09:55:00+08:00" },
  { "id": "td_1011", "name": "补充 README 本地运行说明", "description": "加启动命令与 mock 开关说明", "dueAt": "2026-11-05T16:00:00+08:00", "status": "done", "createdAt": "2026-11-05T16:20:00+08:00" },
  { "id": "td_1012", "name": "升级 antd-mobile 并回归", "description": "升级到最新版，跑一遍核心链路", "dueAt": "2026-11-14T18:00:00+08:00", "status": "open", "createdAt": "2026-11-05T13:12:00+08:00" },
  { "id": "td_1013", "name": "处理微信分享卡片 og:image 不显示", "description": "排查分享图 URL 与尺寸", "dueAt": "2026-11-04T20:00:00+08:00", "status": "open", "createdAt": "2026-11-04T20:08:00+08:00" },
  { "id": "td_1014", "name": "配置域名白名单", "description": "在开放平台配 render.alipay.com landing 白名单", "dueAt": "2026-11-04T11:00:00+08:00", "status": "done", "createdAt": "2026-11-04T11:40:00+08:00" },
  { "id": "td_1015", "name": "写单元测试覆盖空态与重试", "description": "列表空态、接口失败重试用例", "dueAt": "2026-11-14T17:00:00+08:00", "status": "open", "createdAt": "2026-11-03T17:26:00+08:00" },
  { "id": "td_1016", "name": "沟通分享封面图需求", "description": "与设计确认分享卡片封面素材", "dueAt": "2026-11-03T10:00:00+08:00", "status": "open", "createdAt": "2026-11-03T10:15:00+08:00" },
  { "id": "td_1017", "name": "整理接口 mock 数据字段说明", "description": "输出字段含义与示例，供前端对齐", "dueAt": "2026-11-02T18:00:00+08:00", "status": "done", "createdAt": "2026-11-02T18:02:00+08:00" },
  { "id": "td_1018", "name": "排查 CI check-seo 偶发超时", "description": "定位脚本超时原因并加缓存", "dueAt": "2026-11-12T18:00:00+08:00", "status": "open", "createdAt": "2026-11-02T14:50:00+08:00" },
  { "id": "td_1019", "name": "准备上线公告文案", "description": "写一段内部上线通知", "dueAt": "2026-11-01T16:00:00+08:00", "status": "open", "createdAt": "2026-11-01T16:44:00+08:00" },
  { "id": "td_1020", "name": "记录本周个人待办并规划下周", "description": "复盘本周，排下周计划", "dueAt": "2026-10-31T21:00:00+08:00", "status": "open", "createdAt": "2026-10-31T21:00:00+08:00" }
]
```

#### 6.2.3 Should 预留接口（本期不实现，仅定契约）

| 接口 | 入参 | 返回 | 说明 |
|---|---|---|---|
| `GET /api/todos/:id` | — | 单条 `Todo` | 详情；404 返回 `NOT_FOUND` |
| `PATCH /api/todos/:id` | `{ name?, description?, dueAt?, status? }` | 更新后 `Todo` | 编辑/完成 |
| `DELETE /api/todos/:id` | — | `{ ok: true }` | 删除 |
| `POST /api/auth/code` | `{ authCode }` | `{ uid, token }` | auth_code 服务端换 uid |
| `GET /api/auth/session` | — | `{ uid, nickname }` | 取当前登录态 |

**`GET /api/todos/:id` 示例**

```json
{ "id": "td_1002", "name": "走查 PR #1024 支付模块", "description": "按编码规范走查支付模块，重点核对私钥不入前端；结束后在群里同步结论。", "dueAt": "2026-11-09T18:00:00+08:00", "status": "open", "createdAt": "2026-11-08T18:40:00+08:00", "updatedAt": "2026-11-08T18:40:00+08:00" }
```

**`PATCH /api/todos/:id` 示例**

```json
// 请求（完成）
{ "status": "done" }
// 响应 200
{ "id": "td_1002", "name": "走查 PR #1024 支付模块", "description": "按编码规范走查支付模块，重点核对私钥不入前端", "dueAt": "2026-11-09T18:00:00+08:00", "status": "done", "createdAt": "2026-11-08T18:40:00+08:00", "updatedAt": "2026-11-09T18:12:00+08:00" }
```

**`GET /api/auth/session` 示例**

```json
// 已登录
{ "uid": "test_uid_0001", "nickname": "测试同事" }
// 未登录 401
{ "code": "UNAUTHORIZED", "message": "请先登录" }
```

### 6.3 mock 运行机制

- **MSW**（`mocks/handlers.ts`）拦截浏览器 fetch，命中 `/api/todos*` 返回内存数据（固定种子 20 条 + 新建追加）。
- **Next route handler**（`app/api/todos/route.ts`）兜底，供 SSR/直连/无 MSW 环境使用，同一份种子数据 + 固定测试 uid 隔离。
- mock 开关：环境变量 `NEXT_PUBLIC_USE_MOCK`；SSR 期 mock 返回兜底（空列表或种子），**不抛错致整页降级**。
- 替换真实后端：仅换 `lib/api` 内 baseURL 与鉴权头，契约不变。

---

## 7. 图片资源方案

> 本步骤只列需求，不下载图片；下载由下一步「图片素材采集」节点完成。来源仅限以下免费可商用站点：free-exercise-db、Unsplash/Pexels/Pixabay、Openverse（仅 CC0/CC BY）、Wikimedia Commons、unDraw/Open Peeps/Open Doodles/Storyset、antd-mobile-icons/Lucide/Tabler Icons/Heroicons、Twemoji/Noto Emoji。

### 7.1 图片位清单

| 编号 | 页面/区块 | 用途 | 数量 | 尺寸比例与像素 | 格式 | 风格描述 | 建议搜索关键词（中/英） | 建议来源站点 | 加载策略 | 本地存放路径 |
|---|---|---|---|---|---|---|---|---|---|---|
| LOGO-1 | 列表页顶栏 / 全局 | 站点 logo 标识 | 1 | 1:1，≥96×96（实际渲染 32–40px） | SVG | 简洁线性「勾选+清单」，支付宝蓝单色，与品牌一致 | 待办 清单 勾选 logo / todo list checkmark logo | Lucide（`ListTodo`/`ClipboardCheck`）或自绘 | 内联 SVG（首屏关键，避免额外请求；等效 LCP 预加载） | 代码内联组件，不落地 public |
| AVATAR-1 | 列表页顶栏 | 头像占位（mock/未登录） | 1 | 1:1，40×40（2x 80×80） | SVG | 圆形首字母头像，蓝底白字，简洁 | 用户 头像 占位 / avatar placeholder | 代码生成（首字母 SVG） | 内联 SVG（组件生成） | 不落地 public（组件内生成） |
| EMPTY-1 | 列表页空态 | 空状态插画 | 1 | 4:3，约 480×360（渲染 240×180） | SVG | 清爽线性、大量留白、浅蓝小人 + 便签/清单，与支付宝蓝统一 | 待办 空状态 插画 线性 / empty state illustration to-do line | unDraw（可调主色，CC0） | `<img loading="lazy" decoding="async">`；体积小可内联 | `public/images/todos/empty.svg` |
| ICON-FORM-1 | 新建页表单 | 名称字段图标 | 1 | 1:1，24×24 | SVG | 线性 1.5px 描边，语义＝编辑/文字 | 编辑 文本 图标 / edit text icon | antd-mobile-icons（`EditSOutline`/`TextOutline`） | 内联 SVG（组件 import） | 组件内 import，不落地 public |
| ICON-FORM-2 | 新建页表单 | 描述字段图标 | 1 | 1:1，24×24 | SVG | 线性，语义＝笔记/段落 | 笔记 段落 图标 / notes paragraph icon | Lucide（`AlignLeft`）/ antd-mobile-icons | 内联 SVG（组件 import） | 组件内 import，不落地 public |
| ICON-FORM-3 | 新建页表单 | 截止时间字段图标 | 1 | 1:1，24×24 | SVG | 线性，语义＝日历/时钟 | 日历 时钟 图标 / calendar clock icon | antd-mobile-icons（`ClockCircleOutline`）/ Lucide | 内联 SVG（组件 import） | 组件内 import，不落地 public |
| ICON-SUCCESS-1 | 新建页提交成功 / Toast | 成功对勾 | 1 | 1:1，24×24（Toast 内 32×32） | SVG | 对勾，品牌蓝/绿 | 成功 对勾 图标 / success check icon | antd-mobile-icons（`CheckCircleFill`） | 内联 SVG（组件 import） | 组件内 import，不落地 public |
| FAVICON-1 | 全局 | favicon + PWA 图标 | 1（多尺寸） | 1:1，192×192、512×512（favicon 32/48） | SVG → PNG/ICO | 与 LOGO-1 同源 | 同 LOGO-1 | 由 LOGO-1 派生 | Next metadata（`app/icon.svg` + manifest） | `public/images/common/icon-192.png`、`public/images/common/icon-512.png` |
| OG-1 | 分享卡片 og:image | 外部分享封面 | 0 | — | — | — | — | — | — | **本次无需**（Q1 无外部分享，不产出） |

### 7.2 结论与说明

- **本次无照片类 WebP/实拍素材**：内部工具无实拍需求。
- **本次无 og:image**：无外部分享（Q1）。
- **头像**：登录后使用支付宝 OAuth 返回的头像 URL（若授权了用户信息）；本期 mock 用首字母 SVG 占位，无需实拍头像。
- **logo**：用 Lucide 图标 + 文字标识自绘（内联）；品牌专属 logo 如需，由用户提供（Q6）。

### 7.3 本地存放路径约定

- 统一下载/放置：`public/images/<模块>/<文件名>`，模块按页面分：`common/`、`todos/`、`create/`。
- 图标库（antd-mobile-icons/Lucide）经 npm 引入、组件内 import，**不复制进 public**。
- 需要落地的仅：`EMPTY-1`（`public/images/todos/empty.svg`）、`FAVICON-1`（`public/images/common/icon-*.png`）。

### 7.4 加载策略约定

- **内联 SVG**（logo、表单/成功图标）：体积小、首屏关键，内联或组件 import，零额外请求，等效 LCP 预加载。
- **空态插画**：非首屏关键（仅空态出现），`<img loading="lazy">`；若 SVG 体积 < 4KB 可内联以省请求。
- **favicon/PWA**：Next `app/icon.svg` 自动注入 + `manifest` 引用 192/512 PNG。
- **next/image**：本期图片均为 SVG（Next Image 默认不优化 SVG），故以「内联 + 普通 `<img>`」为主；未来引入照片 WebP 时改用 `next/image` + `priority`（LCP）/ `loading="lazy"`（非首屏）。

---

## 8. 已定稿结论

以下关键取舍已定稿（按推荐项锁定）：

1. **渲染方式**：**A（SSR 壳 + CSR 数据，全站 noindex）**。备选 B（纯 CSR）、C（全站 SSG/ISR）不采用。
2. **视觉方向**：**方向一（清爽专注·支付宝蓝 + 倒计时截止 chip）**。方向二（温暖手账·便签纸）、方向三（暗夜效率·高对比）不采用。

其余均已按 Q1–Q10 澄清结论锁定（B 类 noindex、无支付、无外部分享、中英双语、仅创建闭环、mock + 固定测试 uid），无其他未决问题。
