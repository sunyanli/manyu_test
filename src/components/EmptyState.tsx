"use client";

import { Button } from "antd-mobile";
import { useI18n } from "@/i18n";
import { PlusIcon } from "./icons";

/** 列表空态：插画 + 引导 + 主操作。 */
export function EmptyState() {
  const { t } = useI18n();
  return (
    <div className="empty-state">
      <img src="/images/todos/empty.svg" width={240} height={258} alt={t("emptyTitle")} loading="lazy" decoding="async" className="empty-state__img" />
      <p className="empty-state__title">{t("emptyTitle")}</p>
      <p className="empty-state__desc">{t("emptyDesc")}</p>
      <Button color="primary" className="empty-state__cta" onClick={() => window.location.assign("/create")}>
        <PlusIcon />
        <span>{t("emptyCta")}</span>
      </Button>
    </div>
  );
}
