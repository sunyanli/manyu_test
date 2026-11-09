/**
 * 支付宝第三方纯 H5 — 容器抽象层（Level 1 标准 Web + Level 2 开放平台能力）。
 * 约定见 skill references/container.md：绝不依赖 AlipayJSBridge 容器 JSAPI。
 */

/** 是否在支付宝内（UA 同步判定，SSR 安全）。只用于能力启用与降级，不据此调用容器 JSAPI。 */
export function isAlipayClient(): boolean {
  if (typeof window !== "undefined" && typeof (window as unknown as { AlipayJSBridge?: unknown }).AlipayJSBridge !== "undefined") {
    return true;
  }
  return /AlipayClient|AliApp|Alipay/i.test(typeof navigator !== "undefined" ? navigator.userAgent : "");
}

/**
 * SSR 安全判定（传 request 的 UA）。⚠ 反 cloaking：仅用于极轻分流（如不预取登录态数据），
 * 绝不给爬虫与用户出不同可见内容/head。
 */
export function isAlipayClientSSR(ua: string | undefined): boolean {
  if (!ua) return false;
  return /AlipayClient|AliApp|Alipay/i.test(ua);
}

/** 进入下一页：无论浏览器还是支付宝都是整页加载本 URL（第三方无多 webview → 标准多页）。 */
export function openPage(url: string): void {
  window.location.assign(url);
}

/** 返回上一页：标准浏览器历史后退（无原生 popWindow）。 */
export function goBack(): void {
  window.history.back();
}

/** 浏览器环境引导「在支付宝中打开」（alipays scheme + render landing）。非 alipay.com 域名需配白名单。 */
export function openPageInAlipay(targetUrl: string): void {
  const scheme = `alipays://platformapi/startapp?appId=20000067&url=${encodeURIComponent(targetUrl)}`;
  const landing = `https://render.alipay.com/p/s/i/?scheme=${encodeURIComponent(scheme)}`;
  window.location.assign(landing);
}
