/** 接口类型 */
export type DemoApiType = 'hello' | 'hash' | 'sort';

/** 统计维度 */
export type StatsDimension = 'personType' | 'personLevel' | 'personDept';

/** 统一响应体 */
export interface ApiResponse<T> {
  success: boolean;
  errorCode?: string;
  errorMessage?: string;
  userTip?: string;
  data: T;
}

/** 演示接口返回 */
export interface DemoResult {
  type: DemoApiType;
  message?: string;
  algorithm?: string;
  input?: string;
  output?: string;
  original?: number[];
  sorted?: number[];
  order?: 'asc' | 'desc';
  swapCount?: number;
  timestamp: string;
}

/** 哈希请求 */
export interface HashRequest {
  input: string;
  algorithm?: 'MD5' | 'SHA-1' | 'SHA-256';
}

/** 排序请求 */
export interface SortRequest {
  numbers: number[];
  order?: 'asc' | 'desc';
}

/** 维度分类调用次数 */
export interface StatsItem {
  name: string;
  callCount: number;
}

/** 每日调用趋势 */
export interface StatsTrend {
  date: string;
  name: string;
  callCount: number;
}

/** 调用统计报表 */
export interface StatsReport {
  dimension: StatsDimension;
  dimensionName: string;
  startDate: string;
  endDate: string;
  totalCount: number;
  items: StatsItem[];
  trend: StatsTrend[];
}
