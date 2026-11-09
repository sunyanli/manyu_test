"use client";

import { Toast } from "antd-mobile";
import { useI18n } from "@/i18n";
import { formatCreateTime } from "@/lib/format";
import type { Todo } from "@/lib/types";
import { DeadlineChip } from "./DeadlineChip";

/** 待办卡片：状态点 + 名称 + 描述摘要 + 创建时间（mono）+ 截止 chip。 */
export function TodoCard({ todo }: { todo: Todo }) {
  const { t, lang } = useI18n();

  const onTap = () => {
    Toast.show({ content: t("detailComingSoon") });
  };

  return (
    <button type="button" className="todo-card" onClick={onTap} aria-label={`${todo.name}`}>
      <span className={`todo-card__dot${todo.status === "done" ? " todo-card__dot--done" : ""}`} aria-hidden />
      <span className="todo-card__body">
        <span className="todo-card__name">{todo.name}</span>
        {todo.description ? <span className="todo-card__desc">{todo.description}</span> : null}
        <span className="todo-card__time mono">{formatCreateTime(todo.createdAt)}</span>
      </span>
      <DeadlineChip dueAt={todo.dueAt} />
    </button>
  );
}
