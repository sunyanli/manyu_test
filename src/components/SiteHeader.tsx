"use client";

import type { ReactNode } from "react";
import { useI18n } from "@/i18n";
import { TEST_NICKNAME } from "@/lib/constants";
import { ChevronLeftIcon } from "./icons";

function defaultBack(): void {
  if (window.history.length > 1) window.history.back();
  else window.location.assign("/");
}

interface SiteHeaderProps {
  /** 中心标题（新建页等）；缺省则渲染 logo + 站点名。 */
  title?: string;
  showBack?: boolean;
  onBack?: () => void;
  right?: ReactNode;
}

/** 吸顶标题栏（自绘，不用原生标题栏）。 */
export function SiteHeader({ title, showBack, onBack, right }: SiteHeaderProps) {
  const { t } = useI18n();

  return (
    <header className="site-header">
      {showBack ? (
        <button type="button" className="header-back" aria-label={t("back")} onClick={onBack ?? defaultBack}>
          <ChevronLeftIcon />
        </button>
      ) : (
        <div className="header-brand">
          <img src="/images/common/logo.svg" width={28} height={28} alt="" aria-hidden className="header-brand__logo" />
          <span className="header-title">{t("appName")}</span>
        </div>
      )}
      {title ? <span className="header-center">{title}</span> : null}
      <div className="header-right">{right ?? <LangToggle />}</div>
    </header>
  );
}

/** 中英切换按钮。 */
export function LangToggle() {
  const { lang, setLang, t } = useI18n();
  const next = lang === "zh" ? "en" : "zh";
  return (
    <button type="button" className="lang-toggle" aria-label={t("switchLang")} onClick={() => setLang(next)}>
      {lang === "zh" ? "EN" : "中"}
    </button>
  );
}

/** mock 首字母头像（登录后支付宝头像为 Should 预留）。 */
export function UserAvatar() {
  return (
    <span className="user-avatar" aria-hidden title={TEST_NICKNAME}>
      {TEST_NICKNAME.slice(0, 1)}
    </span>
  );
}
