# 图片素材清单（assets manifest）

> 任务：为支付宝 H5「日常待办」准备图片素材。
> 输入：`.agents/workflow/design.md` §7「图片资源方案」、`.agents/workflow/requirements.md` §「图片资源需求」。
> 结论：本项目有图片需求（空态插画 + logo/favicon）。落地到 `public/images/` 的共 5 个文件；图标类走 npm 依赖（antd-mobile-icons / Lucide）组件内 import，不落地 public。

## 采集说明

- **EMPTY-1（空态插画）**：采用 unDraw「notes-list」插画（人物 + 清单白板，契合「记下第一件事」空态语义），按品牌色调整主色后落地。unDraw CDN（`cdn.undraw.co`）可正常访问并下载成功。
- **配色处理**：unDraw 默认主色 `#6c63ff`（紫）→ 品牌主色 `#1677FF`；次要点缀 `#ed9da0`（粉）→ 品牌渐变亮青 `#00C2FF`；其余中性色（深色描边 `#090814`、深灰 `#3f3d56`、浅灰 `#e6e6e6`、白 `#fff`）保持不变。
- **比例偏差**：设计表 EMPTY-1 标注「4:3，约 480×360」。所选 unDraw 插画原生为 744×800（近 1:1 竖版），为免裁剪元素，保留原生比例按 240×258 渲染；已在表中如实标注。
- **许可纠正**：设计表将 unDraw 记为「CC0」，实际为 **unDraw License**（免费商用、无需署名，但不可将素材打包再分发、不可用于 AI/ML 训练）。本清单按实际许可记录。
- **网络备注**：`raw.githubusercontent.com`（free-exercise-db）直连失败、PyPI 拉取超时，但本项目无需照片/健身类素材，未受影响；favicon PNG 由纯 Python（标准库 zlib/struct）离线生成，无外部依赖。
- **图标**：表单/成功图标经 npm 依赖（antd-mobile-icons、Lucide）组件内 import，不复制进 `public/`，其许可文本随 `node_modules` 自带，无需页面署名。

## 图片位清单

| 图片位编号 | 页面 | 本地路径 | 来源站点 | 原始链接 | 作者 | 许可证 | 是否需署名 | 尺寸与体积 | 是否占位图 |
|---|---|---|---|---|---|---|---|---|---|
| LOGO-1 | 列表页顶栏 / 全局 | 代码内联组件（SVG 源码 `public/images/common/logo.svg`） | 自绘（参考 Lucide `ListTodo`/`ClipboardCheck` 造型） | — | 本项目（自绘） | 自绘，无第三方版权 | 否 | SVG 64×64，409 B | 否 |
| AVATAR-1 | 列表页顶栏 | 组件内生成（首字母圆形头像 SVG，不落地 public） | 代码生成 | — | 本项目（自绘） | 自绘，无第三方版权 | 否 | SVG（渲染 40×40，2x 80×80） | 否 |
| EMPTY-1 | 列表页空态 | `public/images/todos/empty.svg` | unDraw | https://undraw.co/illustrations/undraw_notes-list_non4 （下载 https://cdn.undraw.co/illustration/notes-list_non4.svg） | Katerina Limpitsouni（unDraw） | unDraw License（免费商用、无需署名；不可打包再分发、不可用于 AI/ML 训练） | 否 | SVG，源 744×800，渲染 240×258，7.0 KB | 否 |
| ICON-FORM-1 | 新建页表单（事项名称） | 组件内 import `antd-mobile-icons` `EditSOutline`（不落地 public） | antd-mobile-icons（npm） | https://www.npmjs.com/package/antd-mobile-icons | Ant Design Mobile 团队 | MIT | 否 | SVG 24×24 | 否 |
| ICON-FORM-2 | 新建页表单（描述） | 组件内 import `lucide-react` `AlignLeft`（不落地 public） | Lucide（npm） | https://lucide.dev/icons/align-left | Lucide Icons and Contributors | ISC（部分派生自 Feather 的图标为 MIT） | 否 | SVG 24×24 | 否 |
| ICON-FORM-3 | 新建页表单（截止时间） | 组件内 import `antd-mobile-icons` `ClockCircleOutline`（不落地 public） | antd-mobile-icons（npm） | https://www.npmjs.com/package/antd-mobile-icons | Ant Design Mobile 团队 | MIT | 否 | SVG 24×24 | 否 |
| ICON-SUCCESS-1 | 新建页提交成功 / Toast | 组件内 import `antd-mobile-icons` `CheckCircleFill`（不落地 public） | antd-mobile-icons（npm） | https://www.npmjs.com/package/antd-mobile-icons | Ant Design Mobile 团队 | MIT | 否 | SVG 24×24（Toast 内 32×32） | 否 |
| FAVICON-1 | 全局（favicon + PWA 图标） | `public/images/common/icon-192.png`、`public/images/common/icon-512.png`（向量源 `public/images/common/icon.svg`） | 由 LOGO-1 派生（自绘） | — | 本项目（自绘） | 自绘，无第三方版权 | 否 | PNG 192×192（2.0 KB）、512×512（7.0 KB）；SVG 64×64（500 B） | 否 |
| OG-1 | 分享卡片 og:image | 本次无需（0 个，Q1 无外部分享，不产出） | — | — | — | — | — | — | — |

## 落地文件汇总

| 文件 | 体积 |
|---|---|
| `public/images/todos/empty.svg` | 7.0 KB |
| `public/images/common/logo.svg` | 409 B |
| `public/images/common/icon.svg` | 500 B |
| `public/images/common/icon-192.png` | 2.0 KB |
| `public/images/common/icon-512.png` | 7.0 KB |

> 第三方素材来源与许可记录（本项目均无需署名）：见 `public/images/CREDITS.md`。
