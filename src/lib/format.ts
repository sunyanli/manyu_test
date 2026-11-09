import type { Todo } from "./types";

export type Lang = "zh" | "en";

export function pad2(n: number): string {
  return String(n).padStart(2, "0");
}

/** 创建时间展示：MM-DD HH:mm（本地时区）。 */
export function formatCreateTime(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "";
  return `${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(d.getMinutes())}`;
}

/** 截止时间 chip 的语义分类（设计签名元素：倒计时截止 chip）。 */
export type DeadlineKind = "none" | "overdue" | "today" | "tomorrow" | "future";

export interface DeadlineInfo {
  kind: DeadlineKind;
  label: string;
}

function startOfDay(d: Date): number {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
}

export function isSameDay(a: Date, b: Date): boolean {
  return (
    a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate()
  );
}

/** 截止 chip：无=灰—、今天/明天=蓝、已逾期=红、更远=中性。 */
export function deadlineInfo(dueAt: string | null, lang: Lang, now: Date = new Date()): DeadlineInfo {
  if (!dueAt) return { kind: "none", label: "—" };
  const d = new Date(dueAt);
  if (Number.isNaN(d.getTime())) return { kind: "none", label: "—" };

  const diffDays = Math.round((startOfDay(d) - startOfDay(now)) / 86400000);
  const hm = `${pad2(d.getHours())}:${pad2(d.getMinutes())}`;

  if (diffDays < 0) {
    const n = -diffDays;
    return { kind: "overdue", label: lang === "zh" ? `已逾期 ${n} 天` : `${n}d overdue` };
  }
  if (diffDays === 0) return { kind: "today", label: lang === "zh" ? `今天 ${hm}` : `Today ${hm}` };
  if (diffDays === 1) return { kind: "tomorrow", label: lang === "zh" ? `明天 ${hm}` : `Tmrw ${hm}` };
  return { kind: "future", label: `${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${hm}` };
}

/** 今日待办数（open 且 dueAt 为今天），用于列表摘要条。 */
export function countDueToday(items: Todo[], now: Date = new Date()): number {
  return items.filter((it) => it.status === "open" && it.dueAt && isSameDay(new Date(it.dueAt), now))
    .length;
}
