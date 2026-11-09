"use client";

import { useEffect, useState } from "react";
import { isAlipayClient } from "@/container/env";

/**
 * SSR 期返回 null，挂载后判定真实环境，防闪烁与 hydration mismatch。
 * 用法：inApp === null 渲染中性壳（SSR 直出），true 渲染真实内容，false 渲染降级屏。
 */
export function useInAlipay(): boolean | null {
  const [inApp, setInApp] = useState<boolean | null>(null);
  useEffect(() => {
    setInApp(isAlipayClient());
  }, []);
  return inApp;
}
