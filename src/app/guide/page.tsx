import type { Metadata } from 'next';
import { SHARE_DESC, SHARE_TITLE, SITE_NAME, SITE_URL } from '@/lib/constants';
import GuideClient from './GuideClient';

// C 类公开落地页：SSG 直出可抓 HTML + og 分享卡片；noindex（本产品不收录）。
const url = `${SITE_URL}/guide`;

export const metadata: Metadata = {
  title: SHARE_TITLE,
  description: SHARE_DESC,
  alternates: { canonical: url },
  openGraph: {
    type: 'website',
    url,
    title: SHARE_TITLE,
    description: SHARE_DESC,
    siteName: SITE_NAME,
    locale: 'zh_CN',
    images: [{ url: `${SITE_URL}/og.png`, width: 1200, height: 630, alt: SHARE_TITLE }],
  },
  twitter: { card: 'summary_large_image' },
  robots: { index: false, follow: false },
};

const jsonLd = {
  '@context': 'https://schema.org',
  '@type': 'WebSite',
  name: SITE_NAME,
  url,
  description: SHARE_DESC,
};

export default function Page() {
  return (
    <main className="guide">
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }} />
      <section className="guide-hero">
        <h1 className="guide-title">{SHARE_TITLE}</h1>
        <p className="guide-desc">{SHARE_DESC}</p>
      </section>
      <section className="guide-features">
        <div className="guide-feature">
          <span className="guide-feature-ico" aria-hidden="true">
            ＋
          </span>
          <strong>打开即记</strong>
          <p>顶部输入，回车即存</p>
        </div>
        <div className="guide-feature">
          <span className="guide-feature-ico" aria-hidden="true">
            ☑
          </span>
          <strong>名称 + 描述</strong>
          <p>简单清晰，记录不丢</p>
        </div>
        <div className="guide-feature">
          <span className="guide-feature-ico" aria-hidden="true">
            ⏱
          </span>
          <strong>即时可见</strong>
          <p>创建后列表即时回显</p>
        </div>
      </section>
      <GuideClient />
      <footer className="guide-foot">隐私与数据安全：仅收集必要待办信息 · Daily Todos</footer>
    </main>
  );
}
