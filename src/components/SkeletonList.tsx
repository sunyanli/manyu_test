"use client";

import { useI18n } from "@/i18n";
import { SiteHeader } from "./SiteHeader";

/** 列表加载骨架（SSR 直出的中性壳 + hydrate 后骨架）。 */
export function SkeletonList() {
  const { t } = useI18n();
  return (
    <div className="page">
      <SiteHeader />
      <main className="page__main" aria-busy="true">
        <span className="sr-only">{t("loading")}</span>
        {[0, 1, 2].map((i) => (
          <div key={i} className="todo-card todo-card--skeleton" aria-hidden>
            <span className="sk sk-dot" />
            <span className="todo-card__body">
              <span className="sk sk-line" />
              <span className="sk sk-line sk-line--short" />
              <span className="sk sk-line sk-line--thin" />
            </span>
            <span className="sk sk-chip" />
          </div>
        ))}
      </main>
    </div>
  );
}

/** 新建页骨架。 */
export function CreateSkeleton() {
  const { t } = useI18n();
  return (
    <div className="page">
      <SiteHeader title={t("createTitle")} showBack />
      <main className="page__main form-main" aria-busy="true">
        <span className="sr-only">{t("loading")}</span>
        <div className="sk sk-label" aria-hidden />
        <div className="sk sk-input" aria-hidden />
        <div className="sk sk-label" aria-hidden />
        <div className="sk sk-input sk-input--tall" aria-hidden />
        <div className="sk sk-label" aria-hidden />
        <div className="sk sk-input" aria-hidden />
      </main>
    </div>
  );
}
