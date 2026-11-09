/** 待办状态：本期固定 open，done 为 Should 预留。 */
export type TodoStatus = "open" | "done";

/** 与后端对齐的待办实体（见 .agents/workflow/design.md §6.1）。 */
export interface Todo {
  id: string; // 稳定业务 id，如 "td_8f3a1c9e"
  name: string; // 必填 1–60 字
  description: string; // 选填 0–500 字
  dueAt: string | null; // 选填 ISO 8601 或 null
  status: TodoStatus;
  createdAt: string; // ISO 8601
  updatedAt: string; // ISO 8601
}

/** GET /api/todos 分页返回。 */
export interface TodosPage {
  items: Todo[];
  nextCursor: string | null;
}

/** POST /api/todos 入参。 */
export interface CreateTodoInput {
  name: string;
  description?: string;
  dueAt?: string | null;
}

/** 统一错误形状（见 design.md §6.1）。 */
export interface ApiErrorBody {
  code: string;
  message: string;
  details?: Record<string, unknown>;
}
