'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { Button, ErrorBlock, InfiniteScroll, PullToRefresh, SpinLoading } from 'antd-mobile';
import { listTodos, updateTodo } from '@/lib/api';
import { PAGE_SIZE } from '@/lib/constants';
import type { Todo } from '@/lib/types';
import { TodoCard } from './TodoCard';
import { EmptyState } from './EmptyState';

type Status = 'loading' | 'error' | 'ready';

export function TodoList({
  t,
  onEmptyAction,
  refreshKey,
}: {
  t: (key: string) => string;
  onEmptyAction?: () => void;
  refreshKey?: number;
}) {
  const [items, setItems] = useState<Todo[]>([]);
  const [total, setTotal] = useState(0);
  const [status, setStatus] = useState<Status>('loading');
  const [busy, setBusy] = useState(false);
  const pageRef = useRef(1);

  const load = useCallback(async (page: number, replace: boolean) => {
    try {
      const res = await listTodos({ page, pageSize: PAGE_SIZE });
      setItems((prev) => (replace ? res.items : [...prev, ...res.items]));
      setTotal(res.total);
      pageRef.current = page;
      setStatus('ready');
    } catch {
      setStatus('error');
    }
  }, []);

  useEffect(() => {
    setStatus('loading');
    void load(1, true);
  }, [load, refreshKey]);

  const hasMore = items.length < total;

  const toggle = async (id: string) => {
    setBusy(true);
    try {
      const prev = items.find((x) => x.id === id);
      if (!prev) return;
      const next = prev.status === 'done' ? 'todo' : 'done';
      const updated = await updateTodo(id, { status: next });
      setItems((cur) => cur.map((x) => (x.id === id ? updated : x)));
    } catch {
      /* 保持原状 */
    } finally {
      setBusy(false);
    }
  };

  if (status === 'loading') {
    return (
      <div className="list-loading" aria-live="polite">
        <SpinLoading />
      </div>
    );
  }

  if (status === 'error') {
    return (
      <ErrorBlock status="default" title={t('listError')} description={t('listError')}>
        <Button color="primary" size="small" onClick={() => void load(1, true)}>
          {t('retry')}
        </Button>
      </ErrorBlock>
    );
  }

  if (items.length === 0) {
    return <EmptyState title={t('emptyTitle')} desc={t('emptyDesc')} actionText={t('emptyCta')} onAction={onEmptyAction} />;
  }

  return (
    <PullToRefresh
      onRefresh={async () => {
        await load(1, true);
      }}
    >
      <ul className="todo-list" aria-label={t('appName')}>
        {items.map((todo) => (
          <li key={todo.id}>
            <TodoCard todo={todo} t={t} onToggle={toggle} busy={busy} />
          </li>
        ))}
      </ul>
      <InfiniteScroll
        loadMore={async () => {
          await load(pageRef.current + 1, false);
        }}
        hasMore={hasMore}
      />
    </PullToRefresh>
  );
}
