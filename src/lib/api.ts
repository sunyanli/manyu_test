import type { ListResult, Me, Todo, TodoCreateInput, TodoStatus, TodoUpdateInput } from './types';

// 统一接口层：mock 与真实后端同签名（SSR/CSR 同源）。真实后端就绪后仅替换 basePath 实现。
async function request<T>(input: string, init?: RequestInit): Promise<T> {
  const res = await fetch(input, {
    headers: { 'Content-Type': 'application/json' },
    ...init,
  });
  if (!res.ok) {
    const data = (await res.json().catch(() => null)) as { message?: string } | null;
    throw new Error(data?.message || `HTTP ${res.status}`);
  }
  return (await res.json()) as T;
}

export function createTodo(input: TodoCreateInput): Promise<Todo> {
  return request<Todo>('/api/todos', { method: 'POST', body: JSON.stringify(input) });
}

export function listTodos(params: { status?: TodoStatus | ''; page?: number; pageSize?: number } = {}): Promise<ListResult> {
  const qs = new URLSearchParams();
  if (params.status) qs.set('status', params.status);
  if (params.page) qs.set('page', String(params.page));
  if (params.pageSize) qs.set('pageSize', String(params.pageSize));
  const q = qs.toString();
  return request<ListResult>(`/api/todos${q ? `?${q}` : ''}`);
}

export function getTodo(id: string): Promise<Todo> {
  return request<Todo>(`/api/todos/${encodeURIComponent(id)}`);
}

export function updateTodo(id: string, patch: TodoUpdateInput): Promise<Todo> {
  return request<Todo>(`/api/todos/${encodeURIComponent(id)}`, {
    method: 'PATCH',
    body: JSON.stringify(patch),
  });
}

export function getMe(): Promise<Me> {
  return request<Me>('/api/me');
}

export function getAuthorizeUrl(): Promise<{ url: string }> {
  return request<{ url: string }>('/api/auth/authorize');
}
