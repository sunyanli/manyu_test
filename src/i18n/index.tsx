"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { ConfigProvider } from "antd-mobile";
import enUS from "antd-mobile/es/locales/en-US";
import zhCN from "antd-mobile/es/locales/zh-CN";
import { LANG_COOKIE, LANG_STORAGE_KEY } from "@/lib/constants";
import type { Lang } from "@/lib/format";

const zh = {
  appName: "我的待办",
  newTodo: "新建待办",
  createTitle: "新建待办",
  back: "返回",
  nameLabel: "事项名称",
  namePlaceholder: "例如：整理本周周会纪要",
  descLabel: "描述（选填）",
  descPlaceholder: "把录音整理成纪要同步到群",
  dueLabel: "截止时间（选填）",
  dueUnset: "未设置",
  dueClear: "清除",
  save: "保存",
  saving: "保存中…",
  created: "已创建",
  createFailed: "创建失败，请重试",
  nameRequired: "请输入事项名称",
  nameTooLong: "事项名称不能超过 60 字",
  descTooLong: "描述不能超过 500 字",
  emptyTitle: "记下第一件事",
  emptyDesc: "你还没有待办事项，点击下方按钮快速记录一条。",
  emptyCta: "新建待办",
  todaySummary: "今天 · {n} 件待办",
  loading: "加载中…",
  noMore: "没有更多了",
  loadFailed: "加载失败",
  networkError: "网络异常，请重试",
  serverError: "服务异常，请稍后重试",
  retry: "重试",
  detailComingSoon: "详情将在下个版本开放",
  openInAlipayTitle: "请在支付宝中打开",
  openInAlipayDesc: "本页面为支付宝内功能页，请在支付宝 App 中打开以获得完整体验。",
  openInAlipayBtn: "在支付宝中打开",
  switchLang: "切换语言",
  discardTitle: "放弃未保存的内容？",
  discardDesc: "当前填写的内容尚未保存，确定要离开吗？",
  discardOk: "离开",
  discardCancel: "继续编辑",
} as const;

export type MessageKey = keyof typeof zh;

const en: Record<MessageKey, string> = {
  appName: "My Todos",
  newTodo: "New todo",
  createTitle: "New Todo",
  back: "Back",
  nameLabel: "Task name",
  namePlaceholder: "e.g. Organize weekly meeting notes",
  descLabel: "Description (optional)",
  descPlaceholder: "Turn the recording into meeting notes",
  dueLabel: "Due time (optional)",
  dueUnset: "Not set",
  dueClear: "Clear",
  save: "Save",
  saving: "Saving…",
  created: "Created",
  createFailed: "Failed to create, please retry",
  nameRequired: "Please enter a task name",
  nameTooLong: "Task name must be within 60 characters",
  descTooLong: "Description must be within 500 characters",
  emptyTitle: "Write down your first task",
  emptyDesc: "No todos yet. Tap the button below to add one quickly.",
  emptyCta: "New todo",
  todaySummary: "Today · {n} todos",
  loading: "Loading…",
  noMore: "No more",
  loadFailed: "Failed to load",
  networkError: "Network error, please retry",
  serverError: "Service error, please retry later",
  retry: "Retry",
  detailComingSoon: "Details coming in the next release",
  openInAlipayTitle: "Please open in Alipay",
  openInAlipayDesc: "This is an in-Alipay page. Open it in the Alipay app for the full experience.",
  openInAlipayBtn: "Open in Alipay",
  switchLang: "Switch language",
  discardTitle: "Discard unsaved changes?",
  discardDesc: "Your changes are not saved. Leave this page?",
  discardOk: "Leave",
  discardCancel: "Keep editing",
};

const dictionaries: Record<Lang, Record<MessageKey, string>> = { zh, en };

export type I18nParams = Record<string, string | number>;

export interface I18nApi {
  lang: Lang;
  setLang: (lang: Lang) => void;
  t: (key: MessageKey, params?: I18nParams) => string;
}

const I18nContext = createContext<I18nApi | null>(null);

function readSavedLang(): Lang {
  try {
    const saved = typeof localStorage !== "undefined" ? localStorage.getItem(LANG_STORAGE_KEY) : null;
    if (saved === "en" || saved === "zh") return saved;
  } catch {
    /* ignore */
  }
  return "zh";
}

export function I18nProvider({ children }: { children: ReactNode }) {
  const [lang, setLangState] = useState<Lang>("zh");

  useEffect(() => {
    setLangState(readSavedLang());
  }, []);

  const setLang = useCallback((next: Lang) => {
    setLangState(next);
    try {
      localStorage.setItem(LANG_STORAGE_KEY, next);
      document.cookie = `${LANG_COOKIE}=${next};path=/;max-age=31536000`;
    } catch {
      /* ignore */
    }
  }, []);

  useEffect(() => {
    document.documentElement.lang = lang === "zh" ? "zh-CN" : "en";
    document.title = lang === "zh" ? "我的待办 - 日常待办" : "My Todos - Daily Todo";
  }, [lang]);

  const t = useCallback(
    (key: MessageKey, params?: I18nParams) => {
      let s: string = dictionaries[lang][key];
      if (params) {
        for (const [k, v] of Object.entries(params)) {
          s = s.replaceAll(`{${k}}`, String(v));
        }
      }
      return s;
    },
    [lang],
  );

  const value = useMemo<I18nApi>(() => ({ lang, setLang, t }), [lang, setLang, t]);

  return (
    <ConfigProvider locale={lang === "zh" ? zhCN : enUS}>
      <I18nContext.Provider value={value}>{children}</I18nContext.Provider>
    </ConfigProvider>
  );
}

export function useI18n(): I18nApi {
  const ctx = useContext(I18nContext);
  if (!ctx) throw new Error("useI18n must be used within I18nProvider");
  return ctx;
}
