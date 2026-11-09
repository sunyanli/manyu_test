"use client";

import { useI18n } from "@/i18n";
import { deadlineInfo } from "@/lib/format";

/** 截止时间 chip（设计签名元素：无=灰—、今天/明天=蓝、已逾期=红、更远=中性）。 */
export function DeadlineChip({ dueAt }: { dueAt: string | null }) {
  const { lang } = useI18n();
  const info = deadlineInfo(dueAt, lang);
  return <span className={`deadline-chip deadline-chip--${info.kind}`}>{info.label}</span>;
}
