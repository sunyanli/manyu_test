/**
 * 支付宝第三方纯 H5 — 容器抽象层。
 * 约定见 references/container.md：默认 Level 1(标准 Web) + Level 2(开放平台服务端接口/scheme)。
 * 不依赖容器 JSAPI（pushWindow/setTitle/setShareContent 等第三方不可用）。
 */

// ---------- env ----------
function hasAlipayBridge(): boolean {
  if (typeof window === 'undefined') return false;
  return typeof (window as unknown as { AlipayJSBridge?: unknown }).AlipayJSBridge !== 'undefined';
}

export function isAlipayClient(): boolean {
  if (hasAlipayBridge()) return true;
  return /AlipayClient|AliApp|Alipay/i.test(typeof navigator !== 'undefined' ? navigator.userAgent : '');
}

/**
 * SSR 安全判定（传 request 的 UA）：仅在需要「是否在支付宝内」做极轻分流时用。
 * ⚠ 反 cloaking：绝不要据此给爬虫与用户出不同内容/head。
 */
export function isAlipayClientSSR(ua: string | undefined): boolean {
  if (!ua) return false;
  return /AlipayClient|AliApp|Alipay/i.test(ua);
}

// ---------- navigation（第三方无多 webview -> 标准多页） ----------
export function openPage(url: string, params?: Record<string, string | number | boolean>): void {
  location.assign(appendQuery(url, params));
}

export function popWindow(): void {
  history.back();
}

export function openPageInAlipay(targetUrl: string): void {
  const scheme = `alipays://platformapi/startapp?appId=20000067&url=${encodeURIComponent(targetUrl)}`;
  const landing = `https://render.alipay.com/p/s/i/?scheme=${encodeURIComponent(scheme)}`;
  location.assign(landing); // 非 alipay.com 域名需在开放平台配白名单
}

// ---------- pay / auth（服务端建 URL，前端只跳转） ----------
export function startPay(payServerUrl: string): void {
  location.assign(payServerUrl);
}

export function redirectToAuth(authServerUrl: string): void {
  location.assign(authServerUrl);
}

// ---------- cross-load state: localStorage + storage 事件 + visibilitychange ----------
const BUS_KEY = '__xc_bus__';
interface BusMessage {
  event?: string;
  payload?: unknown;
  ts?: number;
  from?: string;
  to?: string;
}

export function emit(event: string, payload: unknown, opts?: { from?: string; to?: string }): void {
  try {
    localStorage.setItem(BUS_KEY, JSON.stringify({ event, payload, ts: Date.now(), from: opts?.from, to: opts?.to }));
  } catch {
    /* 私有模式/空间不足：以服务端持久化为主 */
  }
}

export function on(event: string, handler: (payload: unknown, from?: string) => void, opts?: { currentPage?: string }): () => void {
  const fn = (e: StorageEvent): void => {
    if (e.key !== BUS_KEY || !e.newValue) return;
    try {
      const m = JSON.parse(e.newValue) as BusMessage;
      if (m.event !== event) return;
      if (m.to && opts?.currentPage && m.to !== opts.currentPage) return;
      handler(m.payload, m.from);
    } catch {
      /* ignore */
    }
  };
  window.addEventListener('storage', fn);
  return () => window.removeEventListener('storage', fn);
}

export function onResume(handler: () => void): () => void {
  const fn = (): void => {
    if (document.visibilityState === 'visible') handler();
  };
  document.addEventListener('visibilitychange', fn);
  return () => document.removeEventListener('visibilitychange', fn);
}

// ---------- helpers ----------
function appendQuery(url: string, p?: Record<string, string | number | boolean>): string {
  if (!p || Object.keys(p).length === 0) return url;
  const qs = new URLSearchParams();
  for (const [k, v] of Object.entries(p)) qs.set(k, String(v));
  const q = qs.toString();
  return url.includes('?') ? `${url}&${q}` : `${url}?${q}`;
}
