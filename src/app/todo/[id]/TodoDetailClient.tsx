'use client';

import { useCallback, useEffect, useState } from 'react';
import { Button, ErrorBlock, SpinLoading, Tag } from 'antd-mobile';
import { useLocale } from '@/hooks/useLocale';
import { getTodo, updateTodo } from '@/lib/api';
import { formatDateTime } from '@/lib/format';
import { openPage } from '@/container/container';
import type { Todo } from '@/lib/types';

type Status = 'loading' | 'error' | 'ready';

export default function TodoDetailClient({ id }: { id: string }) {
  const { locale, t } = useLocale();
  const [todo, setTodo] = useState<Todo | null>(null);
  const [status, setStatus] = useState<Status>('loading');
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    setStatus('loading');
    try {
      const d = await getTodo(id);
      setTodo(d);
      setStatus('ready');
    } catch {
      setStatus('error');
    }
  }, [id]);

  useEffect(() => {
    void load();
  }, [load]);

  const toggle = async () => {
    if (!todo || busy) return;
    setBusy(true);
    try {
      const next = todo.status === 'done' ? 'todo' : 'done';
      const updated = await updateTodo(id, { status: next });
      setTodo(updated);
    } finally {
      setBusy(false);
    }
  };

  if (status === 'loading') {
    return (
      <div className="page list-loading" aria-live="polite">
        <SpinLoading />
      </div>
    );
  }

  if (status === 'error' || !todo) {
    return (
      <div className="page">
        <ErrorBlock status="default" title={t('notFound')} description={t('listError')}>
          <Button color="primary" size="small" onClick={() => openPage('/')}>
            {t('retry')}
          </Button>
        </ErrorBlock>
      </div>
    );
  }

  const done = todo.status === 'done';
  return (
    <div className="page">
      <header className="page-header">
        <button className="header-back" onClick={() => openPage('/')} aria-label={t('cancel')}>
          ‹
        </button>
        <h1 className="header-title">{t('detailTitle')}</h1>
        <span className="header-spacer" />
      </header>

      <div className="detail-card">
        <h2 className="detail-name">{todo.name}</h2>
        <p className="detail-desc">{todo.description || t('descPlaceholder')}</p>
        <div className="detail-tag">
          <Tag color={done ? 'success' : 'primary'}>{done ? t('statusDone') : t('statusTodo')}</Tag>
        </div>
        <dl className="detail-meta">
          <div>
            <dt>{t('createdAt')}</dt>
            <dd>{formatDateTime(todo.createdAt, locale)}</dd>
          </div>
          <div>
            <dt>{t('updatedAt')}</dt>
            <dd>{formatDateTime(todo.updatedAt, locale)}</dd>
          </div>
        </dl>
      </div>

      <div className="form-actions">
        <Button block color={done ? 'default' : 'primary'} loading={busy} onClick={() => void toggle()}>
          {done ? t('markUndo') : t('markDone')}
        </Button>
      </div>
    </div>
  );
}
