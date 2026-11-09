import type { ApiErrorBody, CreateTodoInput, Todo, TodosPage } from "./types";

/** 统一接口错误。 */
export class ApiError extends Error {
  code: string;
  details?: Record<string, unknown>;

  constructor(body: ApiErrorBody) {
    super(body.message);
    this.name = "ApiError";
    this.code = body.code;
    this.details = body.details;
  }
}

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url, init);
  if (!res.ok) {
    let body: ApiErrorBody = { code: "INTERNAL", message: res.statusText };
    try {
      body = (await res.json()) as ApiErrorBody;
    } catch {
      /* 非 JSON 响应，保留默认错误 */
    }
    throw new ApiError(body);
  }
  return (await res.json()) as T;
}

/** 待办列表（游标分页）。 */
export function fetchTodos(params?: { cursor?: string; limit?: number }): Promise<TodosPage> {
  const qs = new URLSearchParams();
  if (params?.cursor) qs.set("cursor", params.cursor);
  if (params?.limit) qs.set("limit", String(params.limit));
  const q = qs.toString();
  return request<TodosPage>(`/api/todos${q ? `?${q}` : ""}`);
}

/** 新建待办。 */
export function createTodo(input: CreateTodoInput): Promise<Todo> {
  return request<Todo>("/api/todos", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  });
}
