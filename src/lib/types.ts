export type TodoStatus = 'todo' | 'done';

export interface Todo {
  id: string;
  name: string;
  description: string;
  status: TodoStatus;
  createdAt: string; // ISO 8601
  updatedAt: string; // ISO 8601
}

export interface TodoCreateInput {
  name: string;
  description: string;
}

export interface TodoUpdateInput {
  status?: TodoStatus;
  name?: string;
  description?: string;
}

export interface ListResult {
  items: Todo[];
  total: number;
  page: number;
  pageSize: number;
}

export interface Me {
  uid: string;
  nickname?: string;
}
