'use client';

import { useCallback, useEffect, useState } from 'react';
import { t, type Locale } from '@/lib/i18n';

const KEY = 'todo_locale';

export function useLocale() {
  const [locale, setLocaleState] = useState<Locale>('zh-CN');

  useEffect(() => {
    try {
      const v = localStorage.getItem(KEY);
      if (v === 'zh-CN' || v === 'en') setLocaleState(v);
    } catch {
      /* ignore */
    }
  }, []);

  const setLocale = useCallback((l: Locale) => {
    setLocaleState(l);
    try {
      localStorage.setItem(KEY, l);
    } catch {
      /* ignore */
    }
  }, []);

  const tr = useCallback(
    (key: string, vars?: Record<string, string | number>) => t(locale, key, vars),
    [locale],
  );

  return { locale, setLocale, t: tr };
}
