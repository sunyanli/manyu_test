'use client';

import { useState } from 'react';
import { Button, Result } from 'antd-mobile';
import { useLocale } from '@/hooks/useLocale';
import { getAuthorizeUrl, getMe } from '@/lib/api';
import { openPage, redirectToAuth } from '@/container/container';
import { setStoredSession } from '@/lib/session';

export default function LoginClient() {
  const { t } = useLocale();
  const [busy, setBusy] = useState(false);

  const authorize = async () => {
    if (busy) return;
    setBusy(true);
    try {
      const { url } = await getAuthorizeUrl();
      redirectToAuth(url);
    } finally {
      setBusy(false);
    }
  };

  const demo = async () => {
    if (busy) return;
    setBusy(true);
    try {
      const me = await getMe();
      setStoredSession(me);
      openPage('/');
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
        <h1 className="header-title">{t('loginTitle')}</h1>
        <span className="header-spacer" />
      </header>

      <div className="login-card">
        <Result status="success" title={t('appName')} description={t('loginDesc')} />
        <Button block color="primary" loading={busy} onClick={() => void authorize()}>
          {t('loginButton')}
        </Button>
        <Button block fill="outline" style={{ marginTop: 12 }} onClick={() => void demo()}>
          {t('demoLogin')}
        </Button>
        <p className="login-hint">{t('loginBrowserHint')}</p>
      </div>
    </div>
  );
}
