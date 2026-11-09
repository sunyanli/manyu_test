# 编码实现总结（G3）：日常待办事项 H5

> 输入：`.agents/workflow/requirements.md`（G1）、`.agents/workflow/design.md`（G2）、`.agents/workflow/assets_manifest.md`。
> 输出：支付宝第三方纯 H5「日常待办」可运行 demo（React + TypeScript + AntD Mobile v5 + Next.js App Router）。
> 说明：`${spec_dir}` 按既有工作流产物目录解析为 `.agents/workflow`，本文件与其他 G1/G2 产物同目录落地。

---

## 1. 概览

严格按 G2 定稿结论实现「仅创建」最小闭环：

- 形态：支付宝「网页 & 移动应用」下第三方纯 H5，**非小程序**，不使用 `my.*`、`AlipayJSBridge.call`、`ap.tradePay`。
- 渲染：方案 A「SSR 壳 + CSR 数据」，全站 B 类 `noindex`（`layout` metadata robots + `app/robots.ts` 双保险）。
- 视觉：方向一「清爽专注 · 支付宝蓝」+ 签名元素「倒计时截止 chip」。
- 闭环：列表可见 + 新建（名称/描述/截止时间）+ 成功反馈 + 空状态引导；完成/编辑/删除/详情为 Should 预留（仅接口契约，不实现 UI）。
- 数据：全 mock（Next route handler + 固定测试 uid），与真实后端同签名，后续仅换 `lib/api` 与鉴权头即可替换。

## 2. 技术栈与目录结构

- 依赖：`next@14.2.35`、`react@18.3.1`、`antd-mobile@5.43.0`、`typescript@5.9.3`。
- 图标与 logo/空态插画：复用「图片素材采集」阶段落地的 `public/images/**`；表单/成功类图标以自绘内联 SVG（`src/components/icons.tsx`）承载，避免引入额外图标包名（对应 assets_manifest 的「自绘」口径）。

```
src/
  lib/            types.ts / constants.ts / seed.ts / api.ts / format.ts
  container/      env.ts            # 环境判定 + scheme 唤起 + 标准导航
  hooks/          useInAlipay.ts    # SSR 安全环境判定（null → true/false）
  i18n/           index.tsx         # 中英双语 provider + ConfigProvider locale
  components/     icons / AlipayGuard / SiteHeader / DeadlineChip /
                  TodoCard / EmptyState / SkeletonList / TodoList / CreateForm
  app/
    layout.tsx robots.ts page.tsx globals.css
    create/page.tsx
    api/todos/route.ts              # GET / POST mock
```

## 3. 代码变更清单

新增 23 个源文件 + 5 个工程配置：

| 文件 | 职责 |
|---|---|
| `package.json` / `tsconfig.json` / `next.config.mjs` / `next-env.d.ts` / `.gitignore` | 工程配置（strict、noUncheckedIndexedAccess、`@/*` 别名、构建跳过 lint） |
| `src/lib/types.ts` | `Todo` / `TodosPage` / `CreateTodoInput` / `ApiErrorBody` |
| `src/lib/constants.ts` | 站点名、固定测试 uid、分页与字段长度、语言持久化 key |
| `src/lib/seed.ts` | 20 条 mock 种子（贴近内部业务，createdAt 倒序） |
| `src/lib/api.ts` | 统一接口层 `fetchTodos` / `createTodo` + `ApiError` |
| `src/lib/format.ts` | 创建时间格式化、截止 chip 语义分类、今日待办统计 |
| `src/container/env.ts` | `isAlipayClient` / `isAlipayClientSSR` / `openPage` / `goBack` / `openPageInAlipay` |
| `src/hooks/useInAlipay.ts` | SSR 返回 null、挂载后判定，防 hydration mismatch |
| `src/i18n/index.tsx` | 中英双语词典 + provider + `document.title`/`<html lang>` 同步 + antd-mobile locale |
| `src/components/icons.tsx` | 自绘内联 SVG 图标 |
| `src/components/AlipayGuard.tsx` | B 类页浏览器降级（请在支付宝中打开 + scheme 唤起） |
| `src/components/SiteHeader.tsx` | 吸顶标题栏 + 语言切换 + mock 首字母头像 |
| `src/components/DeadlineChip.tsx` | 截止时间语义色 chip |
| `src/components/TodoCard.tsx` | 待办卡片（状态点/名称/描述/时间/chip） |
| `src/components/EmptyState.tsx` | 空态插画 + 引导 + CTA |
| `src/components/SkeletonList.tsx` | 列表/新建页骨架（SSR 直出中性壳） |
| `src/components/TodoList.tsx` | 列表数据拉取 + 下拉刷新 + 触底加载 + 空/错态 |
| `src/components/CreateForm.tsx` | 新建表单（名称必填 + 描述选填 + 截止时间选填）+ 校验 + 防重 + 离开确认 |
| `src/app/layout.tsx` / `robots.ts` / `globals.css` | 根布局、noindex 双保险、设计令牌 + 响应式/安全区/a11y 样式 |
| `src/app/page.tsx` / `create/page.tsx` | 列表页 `/` 与新建页 `/create`（B 类 + AlipayGuard） |
| `src/app/api/todos/route.ts` | GET（游标分页）/ POST（校验 + 创建）mock |

## 4. 验证结果（本次实际执行）

| 检查 | 命令 | 结果 |
|---|---|---|
| 类型检查 | `npm run typecheck`（tsc --noEmit） | ✅ 0 错误 |
| 生产构建 | `npm run build`（next build） | ✅ 编译成功；路由 `/`、`/create`、`/api/todos`、`/robots.txt`；First Load JS ≈ 179 kB |
| 项目护栏 | skill `check-project.mjs` | ✅ 未发现禁止的容器/小程序/私有 API |
| SEO 一致性 | skill `check-seo.mjs` | ✅ canonical/og 一致、noindex/robots 在场、无 cloaking |
| 发布前门 | skill `check-build.mjs` | ✅ env 边界安全、产物无私钥串 |
| 能力矩阵 | skill `check-env.mjs` | ✅ 打印能力矩阵（无 rc 判定） |
| 运行时冒烟 | `next start` + curl | ✅ GET 列表返回 20 条种子（游标分页）、POST 创建成功（201）、空名称返回 400 |
| SSR HTML | curl `/`、`/create`、`/robots.txt` | ✅ title=`我的待办 - 日常待办`、`<meta name="robots" content="noindex, nofollow">`、viewport-fit=cover、骨架壳直出；robots=`Disallow: /` |

## 5. 已实现 vs 预留

**已实现（Must，仅创建闭环）**：F1 新增待办、F2 列表页、F3 创建成功反馈、F4 空态引导、F11a 截止时间、F20 中英双语 i18n。

**Should 预留（接口契约已定，UI 不实现）**：详情 `/detail/:id`、完成/编辑/删除、支付宝 OAuth 登录（数据按 uid 隔离，本期固定 `test_uid_0001`）。

**明确不做**：支付、外部分享/SEO/og、离线包、客户端开关、浏览器独立引导页（以内嵌 `AlipayGuard` 承载）。

## 6. 剩余风险与后续建议

- 数据为内存 mock（route handler 模块级存储），生产需替换为真实后端持久化 + 会话鉴权（`lib/api` 契约不变）。
- `useInAlipay` 的 UA 判定需在支付宝真机（iOS WKWebView / Android WebView）复核；`alipays` scheme 唤起需在开放平台配置 `render.alipay.com` 白名单。
- 性能预算（LCP<2.5s / CLS<0.1 / INP<200ms）与 a11y 需在真机 + Lighthouse/axe 复核（本期无真机环境）。
- `create` 成功后以 `setTimeout(()=>location.replace('/'),400)` 回列表，Toast 展示约 0.4s；如需更强反馈可延时至 Toast 关闭回调。
