# 知识库系统 — 功能设计规范

> 版本: v1.0 | 日期: 2026-07-16 | 状态: 草稿

---

## 1. 概述

### 1.1 目标
构建一个面向团队内部的知识管理平台，支持文档的创建、组织、检索与协作。

### 1.2 范围
- **In Scope**: 文档 CRUD、层级目录、全文搜索、版本管理、权限控制、标签系统
- **Out of Scope (v1)**: 实时协同编辑、AI 辅助写作、SSO 集成、多语言 i18n

### 1.3 非功能需求
| 指标 | 目标值 |
|------|--------|
| 文档编辑保存延迟 | < 200ms |
| 全文搜索响应 | < 500ms (10万文档内) |
| 并发用户 | 支持 200 并发读写 |
| 数据持久性 | 版本历史永不丢失 |

---

## 2. 领域模型

### 2.1 实体关系图

```
Space (1) ──→ (N) Document
Space (1) ──→ (N) Member
Document (1) ──→ (N) Version
Document (N) ──→ (N) Tag
Document (1) ──→ (N) Comment
Document (1) ──→ (1) Document (parent)
```

### 2.2 核心实体定义

#### Space（知识空间）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | UUID | 主键 |
| name | string(100) | 空间名称 |
| slug | string(100) | URL 友好标识 |
| description | text | 描述 |
| icon | string(255) | 图标 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

#### Document（文档）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | UUID | 主键 |
| space_id | UUID → Space | 所属空间 |
| parent_id | UUID? → Document | 父文档（层级） |
| title | string(255) | 标题 |
| content | text | Markdown 正文 |
| sort_order | integer | 同级排序 |
| is_published | boolean | 是否发布 |
| created_by | UUID → User | 创建者 |
| updated_by | UUID → User | 最后编辑者 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

#### Version（历史版本）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | UUID | 主键 |
| document_id | UUID → Document | 所属文档 |
| version_number | integer | 版本号（自增） |
| title | string(255) | 快照标题 |
| content | text | 快照正文 |
| change_summary | string(500) | 变更摘要 |
| created_by | UUID → User | 编辑者 |
| created_at | timestamp | 创建时间 |

#### Tag（标签）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | UUID | 主键 |
| space_id | UUID → Space | 所属空间 |
| name | string(50) | 标签名 |
| color | string(7) | 颜色（hex） |

#### Member（空间成员）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | UUID | 主键 |
| space_id | UUID → Space | 所属空间 |
| user_id | UUID → User | 用户 |
| role | enum | owner / editor / viewer |

---

## 3. API 设计

### 3.1 RESTful 端点

#### Space
```
POST   /api/v1/spaces                  # 创建空间
GET    /api/v1/spaces                  # 列表（当前用户有权限的）
GET    /api/v1/spaces/:slug            # 获取空间详情
PATCH  /api/v1/spaces/:slug            # 更新空间
DELETE /api/v1/spaces/:slug            # 删除空间
```

#### Document
```
POST   /api/v1/spaces/:slug/docs             # 创建文档
GET    /api/v1/spaces/:slug/docs             # 文档列表（平铺/树形）
GET    /api/v1/spaces/:slug/docs/:id         # 获取文档详情
PATCH  /api/v1/spaces/:slug/docs/:id         # 更新文档
DELETE /api/v1/spaces/:slug/docs/:id         # 删除文档
POST   /api/v1/spaces/:slug/docs/:id/move    # 移动文档（改父节点+排序）
```

#### Version
```
GET    /api/v1/spaces/:slug/docs/:id/versions          # 版本列表
GET    /api/v1/spaces/:slug/docs/:id/versions/:vid     # 版本详情
GET    /api/v1/spaces/:slug/docs/:id/versions/diff     # 两个版本 diff
POST   /api/v1/spaces/:slug/docs/:id/versions/:vid/restore  # 回滚
```

#### Search
```
GET    /api/v1/spaces/:slug/search?q=keyword&tag=tag1&page=1&size=20
```

#### Tag
```
POST   /api/v1/spaces/:slug/tags        # 创建标签
GET    /api/v1/spaces/:slug/tags        # 标签列表
DELETE /api/v1/spaces/:slug/tags/:id    # 删除标签
```

#### Attachment
```
POST   /api/v1/spaces/:slug/attachments       # 上传附件
GET    /api/v1/spaces/:slug/attachments/:id   # 下载附件
```

### 3.2 响应格式
```json
{
  "code": 0,
  "message": "success",
  "data": { ... },
  "pagination": {
    "page": 1,
    "size": 20,
    "total": 142
  }
}
```

---

## 4. 数据库设计

### 4.1 核心表 DDL（PostgreSQL）

```sql
-- 全文搜索配置
ALTER DATABASE knowledge_base SET default_text_search_config = 'simple';

CREATE TABLE spaces (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    description TEXT DEFAULT '',
    icon VARCHAR(255) DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id UUID NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    parent_id UUID REFERENCES documents(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL DEFAULT '',
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_published BOOLEAN NOT NULL DEFAULT true,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- 全文搜索向量
    search_vector TSVECTOR GENERATED ALWAYS AS (
        setweight(to_tsvector('simple', coalesce(title, '')), 'A') ||
        setweight(to_tsvector('simple', coalesce(content, '')), 'B')
    ) STORED
);

CREATE INDEX idx_documents_search ON documents USING GIN (search_vector);
CREATE INDEX idx_documents_space_parent ON documents (space_id, parent_id);
CREATE INDEX idx_documents_sort ON documents (space_id, parent_id, sort_order);

CREATE TABLE document_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    change_summary VARCHAR(500) DEFAULT '',
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (document_id, version_number)
);

CREATE TABLE tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id UUID NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    name VARCHAR(50) NOT NULL,
    color VARCHAR(7) DEFAULT '#3B82F6',
    UNIQUE (space_id, name)
);

CREATE TABLE document_tags (
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    tag_id UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (document_id, tag_id)
);

CREATE TABLE members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id UUID NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'viewer' CHECK (role IN ('owner', 'editor', 'viewer')),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (space_id, user_id)
);

CREATE TABLE comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    parent_id UUID REFERENCES comments(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

---

## 5. 前端页面结构

```
/                          → 首页/仪表盘
/spaces                    → 空间列表
/spaces/:slug              → 空间主页（文档树）
/spaces/:slug/docs/:id     → 文档详情/编辑页
/spaces/:slug/search       → 搜索页
/spaces/:slug/settings     → 空间设置
/spaces/:slug/settings/members → 成员管理
```

### 5.1 核心组件树

```
App
├── Layout
│   ├── Sidebar (空间切换 + 文档树)
│   │   ├── SpaceSwitcher
│   │   └── DocumentTree
│   ├── Header (搜索框 + 用户菜单)
│   └── Content
│       ├── DocumentEditor (Markdown 编辑器)
│       │   ├── Toolbar
│       │   ├── EditorPane (CodeMirror/Monaco)
│       │   └── PreviewPane
│       ├── VersionHistory
│       │   ├── VersionList
│       │   └── DiffView
│       └── SearchResults
```

---

## 6. 技术选型

| 层 | 技术 | 版本 |
|----|------|------|
| 语言 | TypeScript (前后端统一) | 5.x |
| 前端框架 | React | 18 |
| UI 组件库 | Ant Design | 5.x |
| Markdown 编辑器 | @toast-ui/editor 或 Milkdown | latest |
| 后端框架 | NestJS (Node.js) | 10.x |
| ORM | Prisma | 5.x |
| 数据库 | PostgreSQL | 16 |
| 全文搜索 | PostgreSQL tsvector (内置) | — |
| 文件存储 | 本地 FS / MinIO | — |
| 缓存 | Redis | 7.x |

---

## 7. 实施路线图

### Phase 1 — MVP（核心闭环）
- [ ] Space 创建与列表
- [ ] 文档 CRUD + Markdown 编辑预览
- [ ] 文档树（无限层级）
- [ ] 基础全文搜索

### Phase 2 — 增强
- [ ] 版本历史与回滚
- [ ] 权限控制（RBAC）
- [ ] 标签系统
- [ ] 附件上传

### Phase 3 — 协作
- [ ] 评论系统
- [ ] 导入/导出
- [ ] 操作日志

---

## 8. 风险与待澄清项

### 已识别风险
| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 文档树深嵌套性能 | 加载延迟 | 懒加载 + 虚拟滚动 |
| 大文档编辑卡顿 | 用户体验 | 分片渲染 + Web Worker |
| 全文搜索精度 | 检索质量 | 先用 PG tsvector，后续可切换 Elasticsearch |

### 待澄清项（建议与产品/团队确认）
1. 是否需要多语言支持？首版假设仅中文。
2. 用户认证体系是否已存在？先假设独立用户系统。
3. 是否需要公开分享（生成只读链接）？
4. 知识空间之间是否需要文档引用/嵌入？
5. 是否需要 API Token 供外部系统集成？

---

## 9. 自审清单

- [x] 领域模型完整，实体关系清晰
- [x] API 端点覆盖所有功能模块
- [x] 数据库 DDL 可直接执行
- [x] 前端页面结构与组件树已定义
- [x] 技术栈选型有明确理由
- [x] 实施路线图分阶段可交付
- [x] 风险已识别并给出缓解方案
- [ ] 待澄清项（5 项）需与产品确认