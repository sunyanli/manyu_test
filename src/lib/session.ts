// 登录态前端补偿（mock 联调）：服务端 OAuth 换取 uid 为主路径，这里仅缓存 uid/nickname。
import type { Me } from './types';

const KEY = 'todo_session';

export function getStoredSession(): Me | null {
  if (typeof window === 'undefined') return null;
  try {
    const raw = localStorage.getItem(KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as Me;
    return typeof parsed?.uid === 'string' ? parsed : null;
  } catch {
    return null;
  }
}

export function setStoredSession(me: Me): void {
  try {
    localStorage.setItem(KEY, JSON.stringify(me));
  } catch {
    /* 私有模式/空间不足：以服务端持久化为主 */
  }
}

export function clearSession(): void {
  try {
    localStorage.removeItem(KEY);
  } catch {
    /* ignore */
  }
}
