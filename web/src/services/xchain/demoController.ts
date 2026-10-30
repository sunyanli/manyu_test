import type {
  ApiResponse,
  DemoResult,
  HashRequest,
  SortRequest,
  StatsDimension,
  StatsReport,
} from './typing';

const API_BASE = '/api/demo';

/** 解析后端统一响应体，失败时抛出带用户提示的错误 */
async function parseResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    throw new Error(`请求失败（HTTP ${response.status}）`);
  }
  const body = (await response.json()) as ApiResponse<T>;
  if (!body.success) {
    throw new Error(body.userTip || body.errorMessage || '请求失败');
  }
  return body.data;
}

/** HelloWorld 演示 */
export async function fetchHello(): Promise<DemoResult> {
  const response = await fetch(`${API_BASE}/hello`, { method: 'GET' });
  return parseResponse<DemoResult>(response);
}

/** 哈希算法演示 */
export async function fetchHash(request: HashRequest): Promise<DemoResult> {
  const response = await fetch(`${API_BASE}/hash`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });
  return parseResponse<DemoResult>(response);
}

/** 冒泡排序演示 */
export async function fetchSort(request: SortRequest): Promise<DemoResult> {
  const response = await fetch(`${API_BASE}/sort`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });
  return parseResponse<DemoResult>(response);
}

/** 查询调用统计报表 */
export async function fetchStats(
  dimension: StatsDimension,
  startDate: string,
  endDate: string,
): Promise<StatsReport> {
  const params = new URLSearchParams({ dimension, startDate, endDate });
  const response = await fetch(`${API_BASE}/stats?${params.toString()}`, { method: 'GET' });
  return parseResponse<StatsReport>(response);
}

/** 导出页面展示结果，返回 Blob */
export async function exportDemoResult(
  type: 'hello' | 'hash' | 'sort' | 'all',
  startDate: string,
  endDate: string,
): Promise<Blob> {
  const params = new URLSearchParams({ type, startDate, endDate });
  const response = await fetch(`${API_BASE}/export?${params.toString()}`, { method: 'GET' });
  if (!response.ok) {
    throw new Error(`导出失败（HTTP ${response.status}）`);
  }
  return response.blob();
}

/** 触发浏览器下载 */
export function downloadBlob(blob: Blob, fileName: string): void {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
}
