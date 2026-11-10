'use client';

import { Checkbox } from 'antd-mobile';
import type { Todo } from '@/lib/types';
import { formatDateTime } from '@/lib/format';

export function TodoCard({
  todo,
  t,
  onToggle,
  busy,
}: {
  todo: Todo;
  t: (key: string) => string;
  onToggle: (id: string) => void;
  busy?: boolean;
}) {
  const done = todo.status === 'done';
  return (
    <div className={`todo-card${done ? ' is-done' : ''}`}>
      <Checkbox
        className="todo-check"
        checked={done}
        disabled={busy}
        onChange={() => onToggle(todo.id)}
      >
        <span className="sr-only">{done ? t('markUndo') : t('markDone')}</span>
      </Checkbox>
      <a className="todo-main" href={`/todo/${encodeURIComponent(todo.id)}`}>
        <span className="todo-name">{todo.name}</span>
        <span className="todo-desc">{todo.description}</span>
        <time className="todo-time" dateTime={todo.createdAt}>
          {formatDateTime(todo.createdAt)}
        </time>
      </a>
    </div>
  );
}
