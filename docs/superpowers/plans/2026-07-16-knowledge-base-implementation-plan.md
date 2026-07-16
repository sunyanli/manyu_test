# 知识库系统 — 实施计划

> 关联规范: docs/superpowers/specs/2026-07-16-knowledge-base-design.md
> 版本: v1.0 | 日期: 2026-07-16 | 状态: 待执行

---

## 1. 项目结构概览

目标: 基于 Turbo monorepo 构建前后端分离的知识库平台。

```
knowledge-base/
├── apps/
│   ├── server/                     # NestJS 后端
│   │   ├── src/
│   │   │   ├── main.ts
│   │   │   ├── app.module.ts
│   │   │   ├── common/             # 守卫、拦截器、装饰器、DTO 基类
│   │   │   ├── prisma/             # PrismaService + schema
│   │   │   │   └── schema.prisma
│   │   │   ├── auth/               # 认证模块 (JWT)
│   │   │   ├── spaces/             # 空间模块
│   │   │   ├── documents/          # 文档模块
│   │   │   ├── tags/               # 标签模块
│   │   │   ├── attachments/        # 附件模块 (Phase 2)
│   │   │   ├── versions/           # 版本模块 (Phase 2)
│   │   │   ├── comments/           # 评论模块 (Phase 3)
│   │   │   ├── search/             # 搜索模块
│   │   │   ├── users/              # 用户模块
│   │   │   └── permissions/        # 权限模块
│   │   └── test/
│   └── web/                        # React 前端
│       ├── src/
│       │   ├── main.tsx
│       │   ├── App.tsx
│       │   ├── router.tsx
│       │   ├── api/                # API 客户端层
│       │   ├── hooks/              # 自定义 Hooks
│       │   ├── stores/             # 状态管理 (Zustand)
│       │   ├── pages/
│       │   │   ├── spaces/         # 空间列表/详情
│       │   │   ├── documents/      # 文档编辑/查看
│       │   │   ├── search/         # 搜索页
│       │   │   └── auth/           # 登录/注册
│       │   ├── components/
│       │   │   ├── layout/         # AppLayout, Sidebar, Header
│       │   │   ├── editor/         # 富文本编辑器 (Tiptap)
│       │   │   ├── tree/           # 文档树组件
│       │   │   └── common/         # 通用组件
│       │   └── utils/
│       └── test/
├── packages/
│   ├── shared/                     # 共享类型、常量、校验
│   │   ├── src/
│   │   │   ├── types/              # DTO 类型定义
│   │   │   ├── constants/          # 共享常量
│   │   │   └── validators/         # Zod 校验 schema
│   │   └── package.json
│   └── eslint-config/              # 共享 ESLint 配置
│       └── package.json
├── turbo.json
├── package.json
└── docker-compose.yml              # PostgreSQL + Redis
```

---

## 2. Phase 1: MVP — 基础文档管理 (预计 2 周)

### 目标
用户可创建空间、编写/管理文档、全文搜索、基础权限控制。

### 2.1 基础设施搭建

- [ ] **2.1.1** 初始化 monorepo: `pnpm` + `turbo`，创建 `apps/server`、`apps/web`、`packages/shared`
- [ ] **2.1.2** 配置 `turbo.json` 构建流水线 (lint → build → test)
- [ ] **2.1.3** Docker Compose: PostgreSQL 16 + Redis 7，挂载数据卷
- [ ] **2.1.4** `packages/shared`: 定义核心 DTO 类型 (Space, Document, Tag, User, ApiResponse)，Zod 校验 schema
- [ ] **2.1.5** ESLint/Prettier 共享配置，Husky pre-commit 钩子

### 2.2 数据库层

- [ ] **2.2.1** Prisma schema: User, Space, Document, Tag, DocumentTag, Permission, SpaceMember (共 7 张表)
- [ ] **2.2.2** 初始迁移 + seed 脚本 (创建默认 demo 空间 + 示例文档)
- [ ] **2.2.3** 全文搜索索引: `tsvector` 触发器自动更新 `documents.search_vector`

### 2.3 后端 — 认证与用户

- [ ] **2.3.1** `AuthModule`: 注册 (`POST /api/v1/auth/register`)、登录 (`POST /api/v1/auth/login`)、JWT 签发
- [ ] **2.3.2** `JwtAuthGuard`: 全局守卫，解析 `req.user`
- [ ] **2.3.3** `UsersModule`: 用户信息查询/更新 (`GET/PATCH /api/v1/users/me`)

### 2.4 后端 — 空间管理

- [ ] **2.4.1** `SpacesModule`: CRUD (`POST/GET/PATCH/DELETE /api/v1/spaces`)
- [ ] **2.4.2** `SpaceMemberGuard`: 成员权限校验，注入到文档路由
- [ ] **2.4.3** `PermissionsModule`: 角色 CRUD + 成员邀请 (`POST /api/v1/spaces/:slug/members`)

### 2.5 后端 — 文档管理

- [ ] **2.5.1** `DocumentsModule`: 文档 CRUD (`POST/GET/PATCH/DELETE /api/v1/spaces/:slug/docs`)
- [ ] **2.5.2** 文档树查询: 递归 CTE 查询子树 (`GET /api/v1/spaces/:slug/docs/tree`)
- [ ] **2.5.3** 文档移动: `POST /api/v1/spaces/:slug/docs/:id/move` (更新 parentId + sortOrder)
- [ ] **2.5.4** `TagsModule`: 标签 CRUD + 文档绑定 (`POST/GET/DELETE /api/v1/spaces/:slug/docs/:id/tags`)

### 2.6 后端 — 搜索

- [ ] **2.6.1** `SearchModule`: 全文搜索 (`GET /api/v1/search?q=&spaceId=&limit=&offset=`)
- [ ] **2.6.2** PostgreSQL `ts_rank` + `ts_headline` 结果高亮片段

### 2.7 前端 — 基础设施

- [ ] **2.7.1** Vite + React 18 + TypeScript 项目初始化，配置代理到后端
- [ ] **2.7.2** React Router v6 路由表: `/login`, `/spaces`, `/spaces/:slug`, `/spaces/:slug/docs/:id`, `/search`
- [ ] **2.7.3** Axios 实例 + 拦截器 (JWT 注入、401 跳转登录)
- [ ] **2.7.4** Zustand store: `authStore`, `spaceStore`, `documentStore`

### 2.8 前端 — 页面

- [ ] **2.8.1** 登录/注册页 (`/login`)
- [ ] **2.8.2** 空间列表页 (`/spaces`): 卡片网格 + 创建空间弹窗
- [ ] **2.8.3** 空间详情页 (`/spaces/:slug`): 左侧文档树 + 右侧文档内容
- [ ] **2.8.4** 文档树组件: Ant Design Tree 递归渲染，支持拖拽排序
- [ ] **2.8.5** Tiptap 富文本编辑器: 基础工具栏 (标题、加粗、列表、链接、图片)
- [ ] **2.8.6** 搜索页 (`/search`): 搜索框 + 结果列表 (高亮片段)

### 2.9 Phase 1 验证

- [ ] **2.9.1** 后端集成测试: Space CRUD、Document CRUD、Tree 查询、全文搜索
- [ ] **2.9.2** 前端 E2E (Playwright): 注册 → 创建空间 → 创建文档 → 搜索 → 编辑 → 删除
- [ ] **2.9.3** Docker Compose 一键启动脚本，README 环境搭建说明

---

## 3. Phase 2: 增强 — 版本管理 + 附件 + 收藏 (预计 1.5 周)

### 目标
文档版本回溯、附件上传管理、个人收藏、Markdown 导入导出。

### 3.1 后端

- [ ] **3.1.1** `VersionsModule`: 版本列表/详情 (`GET .../versions`)、回滚 (`POST .../versions/:vid/rollback`)
- [ ] **3.1.2** Prisma 新增 `versions`、`attachments`、`favorites`、`starred_documents` 表
- [ ] **3.1.3** `AttachmentsModule`: 文件上传 (`POST .../docs/:id/attachments`)、下载 (`GET .../attachments/:id/download`)、删除
- [ ] **3.1.4** 本地存储: Multer 磁盘存储，路径 `uploads/:spaceId/:docId/:filename`
- [ ] **3.1.5** `FavoritesModule`: 收藏/取消 (`POST/DELETE /api/v1/users/me/favorites`)
- [ ] **3.1.6** 导入导出: `POST .../docs/import` (Markdown → 文档)、`GET .../docs/:id/export` (文档 → Markdown)

### 3.2 前端

- [ ] **3.2.1** 版本历史面板: 时间线 UI + 版本对比 (diff 视图)
- [ ] **3.2.2** 附件管理: 拖拽上传区域 + 附件列表 + 下载按钮
- [ ] **3.2.3** 收藏按钮 + 收藏列表页
- [ ] **3.2.4** 导入导出按钮: 文件选择器 + 下载触发

### 3.3 Phase 2 验证

- [ ] **3.3.1** 版本回滚集成测试
- [ ] **3.3.2** 附件上传/下载 E2E
- [ ] **3.3.3** 导入导出格式正确性验证

---

## 4. Phase 3: 协作 — 评论 + 协同编辑 + 通知 (预计 2 周)

### 目标
文档评论、实时协同编辑、操作通知、权限细化。

### 4.1 后端

- [ ] **4.1.1** `CommentsModule`: 评论 CRUD + 嵌套回复 (`POST/GET/DELETE .../docs/:id/comments`)
- [ ] **4.1.2** Prisma 新增 `comments`、`notifications` 表
- [ ] **4.1.3** WebSocket Gateway: 文档协同编辑 (Yjs 协议)、评论实时推送
- [ ] **4.1.4** `NotificationsModule`: 通知列表 (`GET /api/v1/users/me/notifications`)、已读标记 (`PATCH`)
- [ ] **4.1.5** 权限细化: 文档级权限 (view/edit/admin) 替代仅空间级

### 4.2 前端

- [ ] **4.2.1** 评论面板: 文档右侧评论区 + 嵌套回复
- [ ] **4.2.2** Tiptap 协同编辑: Yjs WebSocket 同步，光标同步显示
- [ ] **4.2.3** 通知中心: Header 铃铛图标 + 未读计数 + 下拉列表
- [ ] **4.2.4** 文档锁定 UI: 锁图标 + 编辑权限提示

### 4.3 Phase 3 验证

- [ ] **4.3.1** 评论 CRUD 集成测试
- [ ] **4.3.2** WebSocket 协同编辑并发测试 (2 用户同时编辑)
- [ ] **4.3.3** 通知推送 E2E

---

## 5. 风险与缓解

| 风险 | 影响 | 缓解 |
|------|------|------|
| 文档树深层嵌套性能 | 树查询慢、前端渲染卡顿 | 递归 CTE 分页 + 虚拟滚动；限制嵌套深度 ≤ 5 层 |
| 大文档编辑性能 | Tiptap 编辑器卡顿 | 分块加载 + 增量保存 (debounce 2s) |
| 全文搜索精度 | 中文分词不准确 | 使用 `zhparser` 扩展或 `pg_jieba`；Phase 2 引入 Elasticsearch 可选 |
| WebSocket 连接不稳定 | 协同编辑冲突 | CRDT (Yjs) 内置冲突解决；断线重连 + 状态同步 |
| 文件上传安全 | 恶意文件上传 | 文件类型白名单 + 大小限制 (10MB) + 病毒扫描 (可选) |

---

## 6. 待澄清项 (阻塞 Phase 1 启动)

| # | 问题 | 影响范围 |
|---|------|----------|
| 1 | 用户认证方式: 仅本地 JWT 还是需要对接 SSO/OAuth? | Auth 模块设计 |
| 2 | 富文本编辑器: 确认 Tiptap 还是 Slate/Quill? | 前端编辑器选型 |
| 3 | 文件存储: 仅本地磁盘还是需要 S3/OSS? | Attachment 模块 |
| 4 | 搜索: Phase 1 仅 PG 全文搜索，还是直接引入 Elasticsearch? | Search 模块 |
| 5 | 部署方式: Docker Compose 单机还是 K8s? | DevOps 配置 |

---

## 7. 技术栈确认

| 层 | 技术 | 版本 |
|----|------|------|
| 前端框架 | React | 18+ |
| UI 组件库 | Ant Design | 5.x |
| 富文本编辑器 | Tiptap | 2.x |
| 状态管理 | Zustand | 4.x |
| 后端框架 | NestJS | 10+ |
| ORM | Prisma | 5.x |
| 数据库 | PostgreSQL | 16 |
| 缓存 | Redis | 7 |
| 协同协议 | Yjs | 13.x |
| 构建工具 | Turbo + Vite | latest |
| 测试 | Vitest + Playwright | latest |

---

## 8. 执行检查清单

- [ ] Phase 1 基础设施搭建完成
- [ ] Phase 1 数据库 + 后端 API 完成
- [ ] Phase 1 前端页面完成
- [ ] Phase 1 集成测试通过
- [ ] Phase 2 版本 + 附件 + 收藏完成
- [ ] Phase 2 验证通过
- [ ] Phase 3 协作功能完成
- [ ] Phase 3 验证通过
- [ ] 待澄清项全部确认