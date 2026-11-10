'use client';

import { useCallback, useEffect, useState } from 'react';
import { useLocale } from '@/hooks/useLocale';
import { getMe } from '@/lib/api';
import { clearSession, getStoredSession, setStoredSession } from '@/lib/session';
import { openPage } from '@/container/container';
import { QuickAddBar } from '@/components/QuickAddBar';
import { TodoList } from '@/components/TodoList';
import type { Me } from '@/lib/types';

export default function HomeClient() {
  const { locale, setLocale, t } = useLocale();
  const [me, setMe] = useState<Me | null>(null);
  const [hydrated, setHydrated] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    setMe(getStoredSession());
    setHydrated(true);
    getMe()
      .then((m) => {
        setMe(m);
        setStoredSession(m);
      })
      .catch(() => {
        /* 后端不可用时保留本地登录态/游客态 */
      });
  }, []);

  const toggleLang = useCallback(
    () => setLocale(locale === 'zh-CN' ? 'en' : 'zh-CN'),
    [locale, setLocale],
  );
  const handleCreated = useCallback(() => setRefreshKey((k) => k + 1), []);
  const logout = () => {
    clearSession();
    setMe(null);
  };

  return (
    <div className="page">
      <header className="page-header">
        <div className="header-left">
          <h1 className="header-title">{t('appName')}</h1>
          {hydrated && me ? <span className="header-user">{me.nickname ?? me.uid}</span> : null}
        </div>
        <div className="header-right">
          <a className="header-action" href="/history">
            {t('navHistory')}
          </a>
          {hydrated && me ? (
            <button className="header-action" onClick={logout}>
              {t('logout')}
            </button>
          ) : (
            <a className="header-action" href="/login">
              {t('login')}
            </a>
          )}
          <button className="header-action" onClick={toggleLang} aria-label="language">
            {t('language')}
          </button>
        </div>
      </header>

      <QuickAddBar t={t} onCreated={handleCreated} />
      <TodoList t={t} onEmptyAction={() => openPage('/create')} refreshKey={refreshKey} />

      <button className="fab" aria-label={t('quickAddSubmit')} onClick={() => openPage('/create')}>
        ＋
      </button>
    </div>
  );
}
