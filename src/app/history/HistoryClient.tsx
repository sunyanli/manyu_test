'use client';

import { useCallback, useEffect, useState } from 'react';
import { Button, ErrorBlock, SpinLoading, Tabs } from 'antd-mobile';
import { useLocale } from '@/hooks/useLocale';
import { listTodos, updateTodo } from '@/lib/api';
import { openPage } from '@/container/container';
import { EmptyState } from '@/components/EmptyState';
import { TodoCard } from '@/components/TodoCard';
import type { Todo, TodoStatus } from '@/lib/types';

type Status = 'loading' | 'error' | 'ready';

const FILTERS: Array<{ key: string; filter: TodoStatus | ''; labelKey: string }> = [
  { key: 'all', filter: '', labelKey: 'tabAll' },
  { key: 'todo', filter: 'todo', labelKey: 'tabTodo' },
  { key: 'done', filter: 'done', labelKey: 'tabDone' },
];

export default function HistoryClient() {
  const { t } = useLocale();
  const [filter, setFilter] = useState<TodoStatus | ''>('');
  const [items, setItems] = useState<Todo[]>([]);
  const [summary, setSummary] = useState({ total: 0, done: 0 });
  const [status, setStatus] = useState<Status>('loading');
  const [busy, setBusy] = useState(false);

  const reloadSummary = useCallback(() => {
    Promise.all([
      listTodos({ page: 1, pageSize: 1 }),
      listTodos({ status: 'done', page: 1, pageSize: 1 }),
    ])
      .then(([all, done]) => setSummary({ total: all.total, done: done.total }))
      .catch(() => {
        /* ignore */
      });
  }, []);

  const loadFiltered = useCallback(async (f: TodoStatus | '') => {
    setStatus('loading');
    try {
      const res = await listTodos({ status: f, page: 1, pageSize: 200 });
      setItems(res.items);
      setStatus('ready');
    } catch {
      setStatus('error');
    }
  }, []);

  useEffect(() => {
    void loadFiltered(filter);
  }, [filter, loadFiltered]);

  useEffect(() => {
    reloadSummary();
  }, [reloadSummary]);

  const toggle = async (id: string) => {
    setBusy(true);
    try {
      const prev = items.find((x) => x.id === id);
      if (!prev) return;
      const next = prev.status === 'done' ? 'todo' : 'done';
      const updated = await updateTodo(id, { status: next });
      setItems((cur) => cur.map((x) => (x.id === id ? updated : x)));
      setSummary((s) => ({
        total: s.total,
        done: next === 'done' ? s.done + 1 : Math.max(0, s.done - 1),
      }));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="page">
      <header className="page-header">
        <button className="header-back" onClick={() => openPage('/')} aria-label={t('cancel')}>
          ‹
        </button>
        <h1 className="header-title">{t('historyTitle')}</h1>
        <span className="header-spacer" />
      </header>

      <p className="history-summary">{t('countSummary', { n: summary.total, d: summary.done })}</p>

      <Tabs activeKey={filter || 'all'} onChange={(k) => setFilter(k === 'all' ? '' : (k as TodoStatus))}>
        {FILTERS.map((f) => (
          <Tabs.Tab title={t(f.labelKey)} key={f.key} />
        ))}
      </Tabs>

      {status === 'loading' ? (
        <div className="list-loading" aria-live="polite">
          <SpinLoading />
        </div>
      ) : status === 'error' ? (
        <ErrorBlock status="default" title={t('listError')} description={t('listError')}>
          <Button color="primary" size="small" onClick={() => void loadFiltered(filter)}>
            {t('retry')}
          </Button>
        </ErrorBlock>
      ) : items.length === 0 ? (
        <EmptyState title={t('emptyTitle')} desc={t('emptyDesc')} actionText={t('emptyCta')} onAction={() => openPage('/create')} />
      ) : (
        <ul className="todo-list" aria-label={t('historyTitle')}>
          {items.map((todo) => (
            <li key={todo.id}>
              <TodoCard todo={todo} t={t} onToggle={toggle} busy={busy} />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
