import type { NextRequest } from "next/server";
import { NextResponse } from "next/server";
import { DESC_MAX, MAX_PAGE_SIZE, NAME_MAX, TEST_UID } from "@/lib/constants";
import { seedTodos } from "@/lib/seed";
import type { ApiErrorBody, CreateTodoInput, Todo, TodosPage } from "@/lib/types";

/**
 * mock 接口（Next route handler 兜底）：与真实后端同签名，见 design.md §6。
 * 鉴权为 Should 预留：本期固定测试 uid 隔离，真实 OAuth 接入后按会话 uid 取数。
 */
const store = new Map<string, Todo[]>();
store.set(TEST_UID, seedTodos.map((t) => ({ ...t })));

function genId(): string {
  return `td_${Math.random().toString(36).slice(2, 10)}`;
}

function isValidDate(value: string): boolean {
  return !Number.isNaN(new Date(value).getTime());
}

function apiError(status: number, code: string, message: string, details?: Record<string, unknown>) {
  const body: ApiErrorBody = { code, message, details };
  return NextResponse.json(body, { status });
}

function encodeCursor(createdAt: string): string {
  return Buffer.from(JSON.stringify({ createdAt })).toString("base64url");
}

function decodeCursor(cursor: string): string | null {
  try {
    const obj = JSON.parse(Buffer.from(cursor, "base64url").toString("utf8")) as { createdAt?: string };
    return typeof obj.createdAt === "string" ? obj.createdAt : null;
  } catch {
    return null;
  }
}

export async function GET(request: NextRequest) {
  const list = store.get(TEST_UID) ?? [];
  const sorted = [...list].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());

  const rawLimit = Number.parseInt(request.nextUrl.searchParams.get("limit") ?? "20", 10);
  const limit = Math.min(Math.max(Number.isFinite(rawLimit) ? rawLimit : 20, 1), MAX_PAGE_SIZE);
  const cursor = request.nextUrl.searchParams.get("cursor");

  let startIndex = 0;
  if (cursor) {
    const createdAt = decodeCursor(cursor);
    if (createdAt) {
      const idx = sorted.findIndex((t) => t.createdAt === createdAt);
      if (idx >= 0) startIndex = idx + 1;
    }
  }

  const items = sorted.slice(startIndex, startIndex + limit);
  const last = items[items.length - 1];
  const nextCursor = startIndex + limit < sorted.length && last ? encodeCursor(last.createdAt) : null;
  const page: TodosPage = { items, nextCursor };
  return NextResponse.json(page);
}

export async function POST(request: NextRequest) {
  let body: Partial<CreateTodoInput>;
  try {
    body = (await request.json()) as Partial<CreateTodoInput>;
  } catch {
    return apiError(400, "VALIDATION_ERROR", "无效的请求体");
  }

  const name = typeof body.name === "string" ? body.name.trim() : "";
  const description = typeof body.description === "string" ? body.description.trim() : "";
  const dueAt = body.dueAt == null ? null : typeof body.dueAt === "string" ? body.dueAt : null;

  if (!name) return apiError(400, "VALIDATION_ERROR", "事项名称不能为空", { field: "name" });
  if (name.length > NAME_MAX) return apiError(400, "VALIDATION_ERROR", "事项名称不能超过 60 字", { field: "name" });
  if (description.length > DESC_MAX) return apiError(400, "VALIDATION_ERROR", "描述不能超过 500 字", { field: "description" });
  if (dueAt != null && !isValidDate(dueAt)) return apiError(400, "VALIDATION_ERROR", "截止时间格式无效", { field: "dueAt" });

  const now = new Date().toISOString();
  const todo: Todo = {
    id: genId(),
    name,
    description,
    dueAt,
    status: "open",
    createdAt: now,
    updatedAt: now,
  };

  const list = store.get(TEST_UID) ?? [];
  list.unshift(todo);
  store.set(TEST_UID, list);

  return NextResponse.json(todo, { status: 201 });
}
