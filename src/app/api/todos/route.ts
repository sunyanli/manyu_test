import { NextRequest, NextResponse } from 'next/server';
import { createTodoMock, listTodosMock } from '@/lib/mock-store';
import { DESC_MAX, NAME_MAX, PAGE_SIZE } from '@/lib/constants';
import type { TodoStatus } from '@/lib/types';

function bad(message: string, code = 'BAD_REQUEST'): NextResponse {
  return NextResponse.json({ code, message }, { status: 400 });
}

// GET /api/todos?status=&page=&pageSize= — 列表（登录态，uid 鉴权由真实后端实现）
export async function GET(req: NextRequest) {
  const sp = req.nextUrl.searchParams;
  const raw = sp.get('status') ?? '';
  const status: TodoStatus | '' = raw === 'todo' || raw === 'done' ? raw : '';
  const page = Number(sp.get('page') ?? '1') || 1;
  const pageSize = Number(sp.get('pageSize') ?? String(PAGE_SIZE)) || PAGE_SIZE;
  return NextResponse.json(listTodosMock({ status, page, pageSize }));
}

// POST /api/todos — 新增待办（核心闭环：仅创建）
export async function POST(req: NextRequest) {
  const body = (await req.json().catch(() => null)) as { name?: unknown; description?: unknown } | null;
  if (!body || typeof body.name !== 'string') {
    return bad('请填写事项名称', 'NAME_REQUIRED');
  }
  const name = body.name.trim();
  if (!name) return bad('请填写事项名称', 'NAME_REQUIRED');
  if (name.length > NAME_MAX) return bad(`名称不能超过 ${NAME_MAX} 字`, 'NAME_TOO_LONG');

  const description = typeof body.description === 'string' ? body.description.trim() : '';
  if (description.length > DESC_MAX) return bad(`描述不能超过 ${DESC_MAX} 字`, 'DESC_TOO_LONG');

  const created = createTodoMock({ name, description });
  return NextResponse.json(created, { status: 201 });
}
