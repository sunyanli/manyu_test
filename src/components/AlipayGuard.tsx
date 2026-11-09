"use client";

import type { ReactNode } from "react";
import { Button } from "antd-mobile";
import { useInAlipay } from "@/hooks/useInAlipay";
import { openPageInAlipay } from "@/container/env";
import { useI18n } from "@/i18n";
import { SiteHeader } from "./SiteHeader";

interface AlipayGuardProps {
  children: ReactNode;
  /** 中性壳：SSR 直出（与首次客户端渲染一致，避免 hydration mismatch）。 */
  fallback: ReactNode;
}

/**
 * B 类功能页浏览器降级：
 * - inApp === null：渲染 fallback（SSR 与首次客户端渲染一致，防闪烁）；
 * - 非支付宝浏览器：显示「请在支付宝中打开」+ 唤起；
 * - 支付宝内：渲染真实内容。
 */
export function AlipayGuard({ children, fallback }: AlipayGuardProps) {
  const inApp = useInAlipay();
  const { t } = useI18n();

  if (inApp === null) return <>{fallback}</>;

  if (!inApp) {
    return (
      <div className="page">
        <SiteHeader />
        <main className="page__main degrade">
          <img src="/images/common/logo.svg" width={48} height={48} alt="" aria-hidden className="degrade__logo" />
          <h1 className="degrade__title">{t("openInAlipayTitle")}</h1>
          <p className="degrade__desc">{t("openInAlipayDesc")}</p>
          <Button block color="primary" className="degrade__btn" onClick={() => openPageInAlipay(window.location.href)}>
            {t("openInAlipayBtn")}
          </Button>
        </main>
      </div>
    );
  }

  return <>{children}</>;
}
