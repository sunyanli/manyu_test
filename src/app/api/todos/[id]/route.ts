import { NextRequest, NextResponse } from 'next/server';
import { getTodoMock, updateTodoMock } from '@/lib/mock-store';
import { DESC_MAX, NAME_MAX } from '@/lib/constants';
import type { TodoStatus, TodoUpdateInput } from '@/lib/types';

// GET /api/todos/:id — 详情
export async function GET(_req: NextRequest, { params }: { params: { id: string } }) {
  const todo = getTodoMock(params.id);
  if (!todo) return NextResponse.json({ code: 'NOT_FOUND', message: '待办不存在' }, { status: 404 });
  return NextResponse.json(todo);
}

// PATCH /api/todos/:id — 状态切换/编辑（Should/Could）
export async function PATCH(req: NextRequest, { params }: { params: { id: string } }) {
  if (!getTodoMock(params.id)) {
    return NextResponse.json({ code: 'NOT_FOUND', message: '待办不存在' }, { status: 404 });
  }
  const body = (await req.json().catch(() => null)) as {
    status?: unknown;
    name?: unknown;
    description?: unknown;
  } | null;
  if (!body) return NextResponse.json({ code: 'BAD_REQUEST', message: '请求体无效' }, { status: 400 });

  const patch: TodoUpdateInput = {};
  if (body.status === 'todo' || body.status === 'done') {
    patch.status = body.status as TodoStatus;
  }
  if (typeof body.name === 'string') {
    const name = body.name.trim();
    if (!name) return NextResponse.json({ code: 'NAME_REQUIRED', message: '请填写事项名称' }, { status: 400 });
    if (name.length > NAME_MAX) {
      return NextResponse.json({ code: 'NAME_TOO_LONG', message: `名称不能超过 ${NAME_MAX} 字` }, { status: 400 });
    }
    patch.name = name;
  }
  if (typeof body.description === 'string') {
    const description = body.description.trim();
    if (description.length > DESC_MAX) {
      return NextResponse.json({ code: 'DESC_TOO_LONG', message: `描述不能超过 ${DESC_MAX} 字` }, { status: 400 });
    }
    patch.description = description;
  }

  const updated = updateTodoMock(params.id, patch);
  if (!updated) return NextResponse.json({ code: 'NOT_FOUND', message: '待办不存在' }, { status: 404 });
  return NextResponse.json(updated);
}
