import type { Todo } from "./types";

/**
 * mock 固定种子（20 条，贴近内部业务，见 design.md §6.2.2）。
 * createdAt 倒序；本期以 open 为主，含 5 条 done 供 Should 预留演示。
 * 真实后端接入时仅替换 lib/api 与路由 handler，种子数据即废弃。
 */
export const seedTodos: Todo[] = [
  { id: "td_1001", name: "整理本周迭代周会纪要", description: "把周一迭代周会录音整理成纪要，同步到项目群", dueAt: "2026-11-10T12:00:00+08:00", status: "open", createdAt: "2026-11-09T09:12:00+08:00", updatedAt: "2026-11-09T09:12:00+08:00" },
  { id: "td_1002", name: "走查 PR #1024 支付模块", description: "按编码规范走查支付模块，重点核对私钥不入前端", dueAt: "2026-11-09T18:00:00+08:00", status: "open", createdAt: "2026-11-08T18:40:00+08:00", updatedAt: "2026-11-08T18:40:00+08:00" },
  { id: "td_1003", name: "提交 10 月差旅报销单", description: "整理杭州出差发票，走 OA 报销流程", dueAt: "2026-11-08T17:00:00+08:00", status: "done", createdAt: "2026-11-08T11:05:00+08:00", updatedAt: "2026-11-08T11:05:00+08:00" },
  { id: "td_1004", name: "跟进客户 A 登录问题反馈", description: "复现支付宝内登录偶发失败并定位", dueAt: "2026-11-11T18:00:00+08:00", status: "open", createdAt: "2026-11-08T10:22:00+08:00", updatedAt: "2026-11-08T10:22:00+08:00" },
  { id: "td_1005", name: "更新 sitemap 与 robots 配置", description: "确认 A 类页面收录、B 类 noindex", dueAt: "2026-11-07T17:00:00+08:00", status: "done", createdAt: "2026-11-07T17:30:00+08:00", updatedAt: "2026-11-07T17:30:00+08:00" },
  { id: "td_1006", name: "写 H5 授权联调自测清单", description: "覆盖 publicAppAuthorize 回跳、code 换 uid", dueAt: "2026-11-12T15:00:00+08:00", status: "open", createdAt: "2026-11-07T15:48:00+08:00", updatedAt: "2026-11-07T15:48:00+08:00" },
  { id: "td_1007", name: "准备周五技术分享 PPT", description: "主题《第三方 H5 的能力边界》，约 20 页", dueAt: "2026-11-13T10:00:00+08:00", status: "open", createdAt: "2026-11-07T10:01:00+08:00", updatedAt: "2026-11-07T10:01:00+08:00" },
  { id: "td_1008", name: "核对灰度发布 checklist", description: "确认 CDN 权重、回滚方案、监控告警", dueAt: "2026-11-10T18:00:00+08:00", status: "open", createdAt: "2026-11-06T19:15:00+08:00", updatedAt: "2026-11-06T19:15:00+08:00" },
  { id: "td_1009", name: "给新同事开通测试环境权限", description: "在开放平台授权测试应用", dueAt: "2026-11-06T14:00:00+08:00", status: "done", createdAt: "2026-11-06T14:33:00+08:00", updatedAt: "2026-11-06T14:33:00+08:00" },
  { id: "td_1010", name: "归档上季度待办并复盘", description: "清理已完成事项，输出复盘小结", dueAt: "2026-11-13T18:00:00+08:00", status: "open", createdAt: "2026-11-06T09:55:00+08:00", updatedAt: "2026-11-06T09:55:00+08:00" },
  { id: "td_1011", name: "补充 README 本地运行说明", description: "加启动命令与 mock 开关说明", dueAt: "2026-11-05T16:00:00+08:00", status: "done", createdAt: "2026-11-05T16:20:00+08:00", updatedAt: "2026-11-05T16:20:00+08:00" },
  { id: "td_1012", name: "升级 antd-mobile 并回归", description: "升级到最新版，跑一遍核心链路", dueAt: "2026-11-14T18:00:00+08:00", status: "open", createdAt: "2026-11-05T13:12:00+08:00", updatedAt: "2026-11-05T13:12:00+08:00" },
  { id: "td_1013", name: "处理微信分享卡片 og:image 不显示", description: "排查分享图 URL 与尺寸", dueAt: "2026-11-04T20:00:00+08:00", status: "open", createdAt: "2026-11-04T20:08:00+08:00", updatedAt: "2026-11-04T20:08:00+08:00" },
  { id: "td_1014", name: "配置域名白名单", description: "在开放平台配 render.alipay.com landing 白名单", dueAt: "2026-11-04T11:00:00+08:00", status: "done", createdAt: "2026-11-04T11:40:00+08:00", updatedAt: "2026-11-04T11:40:00+08:00" },
  { id: "td_1015", name: "写单元测试覆盖空态与重试", description: "列表空态、接口失败重试用例", dueAt: "2026-11-14T17:00:00+08:00", status: "open", createdAt: "2026-11-03T17:26:00+08:00", updatedAt: "2026-11-03T17:26:00+08:00" },
  { id: "td_1016", name: "沟通分享封面图需求", description: "与设计确认分享卡片封面素材", dueAt: "2026-11-03T10:00:00+08:00", status: "open", createdAt: "2026-11-03T10:15:00+08:00", updatedAt: "2026-11-03T10:15:00+08:00" },
  { id: "td_1017", name: "整理接口 mock 数据字段说明", description: "输出字段含义与示例，供前端对齐", dueAt: "2026-11-02T18:00:00+08:00", status: "done", createdAt: "2026-11-02T18:02:00+08:00", updatedAt: "2026-11-02T18:02:00+08:00" },
  { id: "td_1018", name: "排查 CI check-seo 偶发超时", description: "定位脚本超时原因并加缓存", dueAt: "2026-11-12T18:00:00+08:00", status: "open", createdAt: "2026-11-02T14:50:00+08:00", updatedAt: "2026-11-02T14:50:00+08:00" },
  { id: "td_1019", name: "准备上线公告文案", description: "写一段内部上线通知", dueAt: "2026-11-01T16:00:00+08:00", status: "open", createdAt: "2026-11-01T16:44:00+08:00", updatedAt: "2026-11-01T16:44:00+08:00" },
  { id: "td_1020", name: "记录本周个人待办并规划下周", description: "复盘本周，排下周计划", dueAt: "2026-10-31T21:00:00+08:00", status: "open", createdAt: "2026-10-31T21:00:00+08:00", updatedAt: "2026-10-31T21:00:00+08:00" },
];
