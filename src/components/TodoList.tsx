"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { Button, InfiniteScroll, PullToRefresh, Toast } from "antd-mobile";
import { ApiError, fetchTodos } from "@/lib/api";
import { PAGE_SIZE } from "@/lib/constants";
import { countDueToday } from "@/lib/format";
import type { Todo } from "@/lib/types";
import { useI18n } from "@/i18n";
import { SiteHeader, LangToggle, UserAvatar } from "./SiteHeader";
import { TodoCard } from "./TodoCard";
import { EmptyState } from "./EmptyState";
import { SkeletonList } from "./SkeletonList";
import { AlertIcon, PlusIcon } from "./icons";

type ErrorKind = "network" | "server";

function errorKind(e: unknown): ErrorKind {
  return e instanceof ApiError ? "server" : "network";
}

/** 待办列表（CSR 数据 + SSR 壳）：加载/空态/错误/下拉刷新/触底加载。 */
export function TodoList() {
  const { t } = useI18n();
  const [items, setItems] = useState<Todo[]>([]);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<ErrorKind | null>(null);
  const startedRef = useRef(false);

  const loadFirst = useCallback(async () => {
    try {
      const page = await fetchTodos({ limit: PAGE_SIZE });
      setItems(page.items);
      setNextCursor(page.nextCursor);
      setHasMore(page.nextCursor != null);
      setError(null);
    } catch (e) {
      setError(errorKind(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!startedRef.current) {
      startedRef.current = true;
      void loadFirst();
    }
  }, [loadFirst]);

  const loadMore = useCallback(async () => {
    if (!nextCursor) {
      setHasMore(false);
      return;
    }
    try {
      const page = await fetchTodos({ cursor: nextCursor, limit: PAGE_SIZE });
      setItems((prev) => [...prev, ...page.items]);
      setNextCursor(page.nextCursor);
      setHasMore(page.nextCursor != null);
    } catch {
      Toast.show({ icon: "fail", content: t("networkError") });
    }
  }, [nextCursor, t]);

  const onRefresh = useCallback(async () => {
    try {
      const page = await fetchTodos({ limit: PAGE_SIZE });
      setItems(page.items);
      setNextCursor(page.nextCursor);
      setHasMore(page.nextCursor != null);
      setError(null);
    } catch {
      Toast.show({ icon: "fail", content: t("networkError") });
    }
  }, [t]);

  const right = (
    <>
      <LangToggle />
      <UserAvatar />
    </>
  );

  if (loading) {
    return <SkeletonList />;
  }

  if (error) {
    return (
      <div className="page">
        <SiteHeader right={right} />
        <main className="page__main">
          <div className="error-state" role="alert">
            <AlertIcon className="error-state__icon" />
            <p className="error-state__title">{error === "network" ? t("networkError") : t("serverError")}</p>
            <Button
              color="primary"
              onClick={() => {
                setLoading(true);
                setError(null);
                void loadFirst();
              }}
            >
              {t("retry")}
            </Button>
          </div>
        </main>
      </div>
    );
  }

  const todayCount = countDueToday(items);

  return (
    <div className="page">
      <SiteHeader right={right} />
      <main className="page__main">
        {todayCount > 0 ? <p className="today-summary">{t("todaySummary", { n: todayCount })}</p> : null}
        {items.length === 0 ? (
          <EmptyState />
        ) : (
          <PullToRefresh onRefresh={onRefresh}>
            <ul className="todo-list" aria-label={t("appName")}>
              {items.map((todo) => (
                <li key={todo.id}>
                  <TodoCard todo={todo} />
                </li>
              ))}
            </ul>
            <InfiniteScroll loadMore={loadMore} hasMore={hasMore} threshold={100}>
              <span className="load-more">{hasMore ? t("loading") : t("noMore")}</span>
            </InfiniteScroll>
          </PullToRefresh>
        )}
      </main>
      <button type="button" className="fab" aria-label={t("newTodo")} onClick={() => window.location.assign("/create")}>
        <PlusIcon />
        <span>{t("newTodo")}</span>
      </button>
    </div>
  );
}
