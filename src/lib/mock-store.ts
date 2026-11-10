// 服务端内存 mock 存储：与真实后端同契约（POST/GET /api/todos，字段见 requirements.md §8）。
// 真实后端就绪后，由 route handler 切换实现，接口层(src/lib/api.ts)签名不变。
import type { ListResult, Todo, TodoCreateInput, TodoStatus, TodoUpdateInput } from './types';

const now = Date.now();
const HOUR = 3600_000;
const DAY = 24 * HOUR;
const iso = (msAgo: number): string => new Date(now - msAgo).toISOString();

type Seed = [name: string, description: string, createdAtMsAgo: number, updatedAtMsAgo: number];

// 贴近真实业务的种子数据（≥20 条，覆盖工作/生活/协作场景）
const seeds: Seed[] = [
  ['整理 Q3 周报数据', '汇总三组口径，周五前发给主管', 1 * HOUR, 1 * HOUR],
  ['回复客户合同邮件', '确认付款节点与发票抬头', 3 * HOUR, 3 * HOUR],
  ['预约年度体检', '选工作日下午，避开会议', 5 * HOUR, 5 * HOUR],
  ['缴纳水电费', '物业通知本月底前', 7 * HOUR, 7 * HOUR],
  ['准备季度复盘 PPT', '10 页以内，含数据看板', 9 * HOUR, 9 * HOUR],
  ['跟进供应商合同续签', '联系采购部确认报价单', 12 * HOUR, 12 * HOUR],
  ['整理报销票据', '差旅+办公耗材，本月 20 日前提交', 14 * HOUR, 14 * HOUR],
  ['更新项目排期表', '同步给前端与测试', 16 * HOUR, 16 * HOUR],
  ['预订下周会议室', '需求评审，约 12 人，半天', 20 * HOUR, 20 * HOUR],
  ['阅读新版本发布说明', '记录接口变更点', 1 * DAY, 1 * DAY],
  ['给客户发周报', '附数据与风险点', 1 * DAY + 2 * HOUR, 1 * DAY + 2 * HOUR],
  ['采购团队下午茶', '预算内，含饮品与轻食', 1 * DAY + 4 * HOUR, 1 * DAY + 4 * HOUR],
  ['更新个人 OKR', '对齐季度目标', 1 * DAY + 8 * HOUR, 1 * DAY + 8 * HOUR],
  ['整理会议纪要', '同步到项目群', 2 * DAY, 2 * DAY],
  ['预约牙医复诊', '确认报销范围', 2 * DAY + 3 * HOUR, 2 * DAY + 3 * HOUR],
  ['提交差旅申请', '下月行业峰会', 2 * DAY + 6 * HOUR, 2 * DAY + 6 * HOUR],
  ['备份工作文档', '云盘与本地双备份', 3 * DAY, 3 * DAY],
  ['阅读行业报告', '摘录可复用的结论', 3 * DAY + 5 * HOUR, 3 * DAY + 5 * HOUR],
  ['约导师复盘', '准备三个问题', 4 * DAY, 4 * DAY],
  ['整理收藏夹', '归档与分类', 5 * DAY, 5 * DAY],
];

const todos: Todo[] = seeds.map(([name, description, createdAtMsAgo, updatedAtMsAgo], i) => ({
  id: `t${String(i + 1).padStart(3, '0')}`,
  name,
  description,
  status: i % 5 === 0 ? 'done' : 'todo',
  createdAt: iso(createdAtMsAgo),
  updatedAt: iso(updatedAtMsAgo),
}));

let seq = todos.length;
function nextId(): string {
  seq += 1;
  return `t${String(seq).padStart(3, '0')}`;
}

export function createTodoMock(input: TodoCreateInput): Todo {
  const created: Todo = {
    id: nextId(),
    name: input.name.trim(),
    description: input.description.trim(),
    status: 'todo',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };
  todos.unshift(created);
  return created;
}

export function listTodosMock(params: { status?: TodoStatus | ''; page?: number; pageSize?: number }): ListResult {
  const page = Math.max(1, params.page ?? 1);
  const pageSize = Math.max(1, params.pageSize ?? 10);
  const filtered = params.status ? todos.filter((x) => x.status === params.status) : todos;
  const start = (page - 1) * pageSize;
  return { items: filtered.slice(start, start + pageSize), total: filtered.length, page, pageSize };
}

export function getTodoMock(id: string): Todo | null {
  return todos.find((x) => x.id === id) ?? null;
}

export function updateTodoMock(id: string, patch: TodoUpdateInput): Todo | null {
  const idx = todos.findIndex((x) => x.id === id);
  if (idx < 0) return null;
  const prev = todos[idx];
  if (!prev) return null;
  const next: Todo = {
    ...prev,
    ...patch,
    name: patch.name !== undefined ? patch.name.trim() : prev.name,
    description: patch.description !== undefined ? patch.description.trim() : prev.description,
    updatedAt: new Date().toISOString(),
  };
  todos[idx] = next;
  return next;
}
