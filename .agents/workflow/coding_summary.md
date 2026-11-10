# 编码实现总结（coding_summary）

> 任务节点：编码实现（G3）
> 采用技能：alipay-h5-program-generator（支付宝第三方纯 H5）
> 需求：帮助内部用户记录日常待办；核心=新增待办；字段=名称+描述；最小闭环=仅创建
> 输出路径约定：`${spec_dir}` 解析为 `.agents/workflow`（与 `requirements.md` 同目录）

---

## 1. 概述（Overview）

- **产品形态**：支付宝开放平台「网页&移动应用」下的第三方纯 H5，React + TypeScript + AntD Mobile v5 + Next.js(App Router, SSR/SSG)。**不是**支付宝小程序。
- **能力边界**：不使用 `my.*`、`AlipayJSBridge` 容器 JSAPI（pushWindow/setTitle/setShareContent）、离线包；授权走服务端 `publicAppAuthorize` 跳转；支付本期不需要。
- **本期范围**：按 G1 定稿实现 Must + Should（需求单 §5 第 1–12 项）：
  - 新增待办（名称+描述，校验+保存）— **核心闭环**
  - 待办列表首页（卡片：名称/描述摘要/时间 + 快速新增 + 空状态 + 状态勾选）
  - 数据持久化（接入 mock 后端，与契约同签名）
  - 登录态（支付宝 OAuth 入口 + 本地演示登录兜底）
  - 详情页、历史/已完成筛选、分页加载、输入校验、外部分享卡片（og）、中英双语
- **关键依赖关系**：`container/*`（环境判定+跳转+降级）→ `hooks/*`（useInAlipay/useLocale）→ `components/*` + 页面客户端 → `lib/api`（接口层）→ `app/api/*` Route Handler → `lib/mock-store`（内存 mock）。

---

## 2. 规划与变更清单（Planning）

### 2.1 目录结构（遵循 coding-standards：lib/container/hooks/components/app 分层）

```
src/
  lib/             # 接口与纯逻辑：types / constants / i18n / api / format / session / mock-store / auth-server
  container/       # 容器抽象层（环境判定 + 跳转 + scheme 唤起 + 跨页事件）
  hooks/           # useInAlipay / useLocale（SSR 安全）
  components/      # AlipayGuard / TodoCard / TodoList / QuickAddBar / EmptyState
  app/
    layout.tsx globals.css robots.ts sitemap.ts
    page.tsx + HomeClient.tsx          # / 待办首页（B 类 noindex）
    create/page.tsx + CreateClient.tsx # /create 新增（B 类）
    todo/[id]/page.tsx + TodoDetailClient.tsx # /todo/:id 详情（B 类）
    history/page.tsx + HistoryClient.tsx      # /history 历史（B 类）
    login/page.tsx + LoginClient.tsx          # /login 授权/降级（B 类）
    guide/page.tsx + GuideClient.tsx          # /guide 分享落地（C 类 SSG + og）
    api/todos/route.ts          # POST /api/todos、GET /api/todos
    api/todos/[id]/route.ts     # GET / PATCH /api/todos/:id
    api/me/route.ts             # GET /api/me
    api/auth/authorize/route.ts # GET /api/auth/authorize（服务端建 publicAppAuthorize URL）
根目录
  package.json tsconfig.json next.config.mjs next-env.d.ts .eslintrc.json .gitignore .env.example
```

### 2.2 页面渲染决策（逐页）

| 路径 | 分类 | 渲染 | noindex | 说明 |
|---|---|---|---|---|
| `/guide` | C | SSG | ✅ | 公开可抓 HTML + og:title/description/image + JSON-LD + scheme 唤起 |
| `/` `/create` `/todo/:id` `/history` `/login` | B | CSR | ✅ | `export const metadata.robots.index=false` + `AlipayGuard` 浏览器降级 |

### 2.3 接口契约（mock 与真实后端同签名，见 requirements.md §8）

| 方法/路径 | 说明 |
|---|---|
| `POST /api/todos` | `{name, description}` → 201 `{id,name,description,status:"todo",createdAt,updatedAt}`；校验：名称必填≤100、描述≤500 |
| `GET /api/todos?status=&page=&pageSize=` | `{items,total,page,pageSize}` |
| `GET /api/todos/:id` | 详情 / 404 |
| `PATCH /api/todos/:id` | 状态切换/编辑（Should/Could） |
| `GET /api/me` | `{uid,nickname}`（真实后端 OAuth 换 uid 后返回） |
| `GET /api/auth/authorize` | 服务端构建 publicAppAuthorize 跳转 URL |

---

## 3. 执行（Execution）关键实现要点

1. **容器层纯 H5 范式**（`src/container/container.ts`）：只做 `isAlipayClient`（仅用对象存在性判定，**不调用**容器 JSAPI）、标准多页 `openPage`/`history.back()`、scheme+landing 唤起 `openPageInAlipay`、服务端建 URL 前端跳转（`redirectToAuth`/`startPay`）、localStorage+storage 事件跨页通信。全仓 0 处 `AlipayJSBridge.call`/`pushWindow`/`setTitle`/`setShareContent`/`ap.tradePay`/`my.tradePay`/`@alipay/jssdk`。
2. **多页导航退化为标准多页**：跨屏用 `<a href>`/`location`，不做 SPA 全站路由；详情页经 `/todo/:id` 公网 URL 打开。
3. **B 类功能页降级**：`AlipayGuard`（`useInAlipay` SSR 期返回 null 防闪烁）——支付宝内渲染子内容，浏览器给「请在支付宝中打开」+ 唤起。
4. **C 类 `/guide` 分享落地**：SSG 直出 `canonical == og:url == 公网 URL` 三处一致 + `twitter:card` + JSON-LD(WebSite) + og:image(1200×630)，`robots` noindex；CTA 按钮走 `openPageInAlipay(location.href)`。
5. **SEO 一致性**：全站 noindex（产品不收录）；`sitemap.ts` 返回空数组、`robots.ts` `Disallow: /`；页面级 metadata 均显式 `index:false`，无页面直接判 user-agent（cloaking 风险为 0）。
6. **环境变量边界（安全）**：服务端密钥 `ALIPAY_APP_ID/ALIPAY_PRIVATE_KEY/...` 仅服务端读取（`auth-server.ts`）；前端仅白名单 `NEXT_PUBLIC_SITE_URL`；`app/api/*` 0 处引用 `NEXT_PUBLIC_*`；`.env.example` 无 `NEXT_PUBLIC_`+secret。
7. **接口层统一**（`lib/api.ts`）：mock 与真实后端同签名，SSR/CSR 同源；错误统一抛 `Error`，UI 走空态/错误态+重试。
8. **非功能**：
   - i18n：zh-CN 默认 + en，`useLocale` + localStorage，UI/校验/空态/分享文案双语。
   - a11y：语义化标签（`a`/`button`/`nav` 区段/有序 heading）、表单 `label`、`sr-only` 替代文本、`:focus-visible`、触控热区 ≥44px。
   - 响应式：`viewport-fit=cover` + `env(safe-area-inset-*)` + 桌面 `max-width:560px` 收敛 + 高 DPI SVG。
   - 交互：骨架/空态+重试、提交防抖（busy 禁用）、回车即存、`PullToRefresh`+`InfiniteScroll`、Toast 反馈。

---

## 4. 验证结果（Verification）

> 环境事实：当前容器**无 Node.js/npm**（`command -v node/npm` 均不存在），Next.js `build/lint/typecheck` 无法执行。按「构建可用性不阻塞任务」协议，**降级为静态代码审查**。

**静态护栏（以 Python 复现 skill 的 check-project / check-seo / check-build 确定性逻辑，规则与脚本一致）：**

| 检查 | 结果 | 说明 |
|---|---|---|
| check-project（禁用 API 静态扫） | ✅ PASS | 0 命中（37 个源码文件，无容器/小程序/私有 API 调用遗址） |
| check-seo（SEO 一致性） | ✅ PASS | 6 个 page.tsx：canonical/og 一致、sitemap/robots 在场、无 cloaking、B 类 AlipayGuard+noindex 齐全 |
| check-build（secret/产物） | ✅ PASS | `.env.example` 无 `NEXT_PUBLIC_`+secret；`app/api/*` 0 处 `NEXT_PUBLIC_*` |

**未执行（依赖缺失）**：`npm install` + `next build` + `next lint` + `tsc --noEmit` + Vitest 单测 + 真机核对。

---

## 5. 剩余风险与后续（Remaining Risks / Next）

1. **构建/单测未跑**：需在具备 Node 18+ 的环境执行 `npm install && npm run build`（`next build` 内含 tsc/lint）；当前仅完成静态审查。
2. **后端联调**：`/api/*` 为内存 mock（进程重启即重置），需按 requirements.md §8 契约替换为真实后端（OAuth uid 鉴权、`PATCH` 字段名逐项核对）。
3. **图片素材缺失**：`og.png`（1200×630 分享封面）、空状态插画、品牌 Logo 待联调阶段提供（`assets_manifest.md` 不存在，未阻塞编码）。
4. **真机验证**：支付宝 WebView（iOS WKWebView / Android）下 scheme 唤起白名单、安全区、键盘遮挡、Service Worker 是否生效需真机核对。
5. **本期明确不做**（Could/Won't）：编辑/删除、优先级/截止/提醒、搜索、统计看板、离线缓存。

---

## 6. 产物清点

- 交付文档：`.agents/workflow/coding_summary.md`（本文件）
- 新增源码：37 个源文件 + 8 个工程配置（`package.json` 等），全部位于仓库根目录内，可被 git 追踪。
