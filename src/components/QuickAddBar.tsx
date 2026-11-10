'use client';

import { useState } from 'react';
import { Input, Toast } from 'antd-mobile';
import { createTodo } from '@/lib/api';
import { NAME_MAX } from '@/lib/constants';
import type { Todo } from '@/lib/types';

export function QuickAddBar({
  t,
  onCreated,
}: {
  t: (key: string) => string;
  onCreated: (todo: Todo) => void;
}) {
  const [name, setName] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async () => {
    if (busy) return;
    const value = name.trim();
    if (!value) {
      Toast.show({ content: t('nameRequired') });
      return;
    }
    if (value.length > NAME_MAX) {
      Toast.show({ content: t('nameTooLong') });
      return;
    }
    setBusy(true);
    try {
      const created = await createTodo({ name: value, description: '' });
      setName('');
      Toast.show({ icon: 'success', content: t('createSuccess') });
      onCreated(created);
    } catch {
      Toast.show({ content: t('createFail') });
    } finally {
      setBusy(false);
    }
  };

  return (
    <form
      className="quick-add"
      onSubmit={(e) => {
        e.preventDefault();
        void submit();
      }}
    >
      <Input
        className="quick-add-input"
        value={name}
        onChange={setName}
        placeholder={t('quickAddPlaceholder')}
        clearable
        onEnterPress={() => void submit()}
      />
      <button className="quick-add-btn" type="submit" disabled={busy} aria-label={t('quickAddSubmit')}>
        +
      </button>
    </form>
  );
}
