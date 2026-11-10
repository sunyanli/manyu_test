'use client';

import { useState } from 'react';
import { Button, Input, TextArea, Toast } from 'antd-mobile';
import { useLocale } from '@/hooks/useLocale';
import { createTodo } from '@/lib/api';
import { DESC_MAX, NAME_MAX } from '@/lib/constants';
import { openPage } from '@/container/container';

export default function CreateClient() {
  const { t } = useLocale();
  const [name, setName] = useState('');
  const [desc, setDesc] = useState('');
  const [busy, setBusy] = useState(false);
  const [errors, setErrors] = useState<{ name?: string; desc?: string }>({});

  const validate = (): boolean => {
    const e: { name?: string; desc?: string } = {};
    const n = name.trim();
    if (!n) e.name = t('nameRequired');
    else if (n.length > NAME_MAX) e.name = t('nameTooLong');
    if (desc.trim().length > DESC_MAX) e.desc = t('descTooLong');
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const submit = async () => {
    if (busy) return;
    if (!validate()) return;
    setBusy(true);
    try {
      await createTodo({ name: name.trim(), description: desc.trim() });
      Toast.show({ icon: 'success', content: t('createSuccess') });
      openPage('/');
    } catch {
      Toast.show({ content: t('createFail') });
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
        <h1 className="header-title">{t('createTitle')}</h1>
        <span className="header-spacer" />
      </header>

      <div className="form-card">
        <label className="field">
          <span className="field-label">{t('nameLabel')}</span>
          <Input value={name} onChange={setName} placeholder={t('namePlaceholder')} clearable />
          {errors.name ? (
            <span className="field-error" role="alert">
              {errors.name}
            </span>
          ) : null}
        </label>

        <label className="field">
          <span className="field-label">{t('descLabel')}</span>
          <TextArea
            value={desc}
            onChange={setDesc}
            placeholder={t('descPlaceholder')}
            rows={4}
            showCount
            maxLength={DESC_MAX}
          />
          {errors.desc ? (
            <span className="field-error" role="alert">
              {errors.desc}
            </span>
          ) : null}
        </label>
      </div>

      <div className="form-actions">
        <Button block color="primary" loading={busy} onClick={() => void submit()}>
          {t('submit')}
        </Button>
      </div>
    </div>
  );
}
