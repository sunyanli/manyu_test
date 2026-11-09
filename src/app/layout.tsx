import type { Metadata, Viewport } from "next";
import type { ReactNode } from "react";
import { I18nProvider } from "@/i18n";
import "./globals.css";

// 全站 B 类：noindex（配合 app/robots.ts 双保险）。无 A 类公开页、无 og 分享、无 sitemap。
export const metadata: Metadata = {
  title: "我的待办 - 日常待办",
  description: "记录日常待办事项，快速新建与查看清单。",
  robots: { index: false, follow: false },
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  viewportFit: "cover",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="zh-CN">
      <body>
        <I18nProvider>{children}</I18nProvider>
      </body>
    </html>
  );
}
