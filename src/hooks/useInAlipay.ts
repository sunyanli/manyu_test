'use client';

import { useEffect, useState } from 'react';
import { isAlipayClient } from '@/container/container';

/** 是否在支付宝内（SSR 期返回 null，挂载后判定，避免首屏闪烁与 hydration mismatch）。 */
export function useInAlipay(): boolean | null {
  const [v, setV] = useState<boolean | null>(null);
  useEffect(() => {
    setV(isAlipayClient());
  }, []);
  return v;
}
