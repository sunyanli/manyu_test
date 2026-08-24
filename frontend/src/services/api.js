import axios from 'axios';
import { CALLER_INFO } from '../utils/constants';

const apiClient = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
    'X-Caller-Name': CALLER_INFO.name,
    'X-Person-Type': CALLER_INFO.personType,
    'X-Person-Level': CALLER_INFO.personLevel,
    'X-Department': CALLER_INFO.department,
  },
});

// HelloWorld 接口
export function callHello(name) {
  return apiClient.get('/hello', { params: { name } });
}

// 哈希计算接口
export function callHash(input, algorithm) {
  return apiClient.post('/hash', { input, algorithm });
}

// 冒泡排序接口
export function callBubbleSort(array) {
  return apiClient.post('/bubble-sort', { array });
}

// 导出接口
export function getExportUrl(type, format = 'csv') {
  return `/api/export?type=${type}&format=${format}`;
}

// 分析报表接口
export function getAnalytics(dimension, startTime, endTime) {
  const params = { dimension };
  if (startTime) params.startTime = startTime;
  if (endTime) params.endTime = endTime;
  return apiClient.get('/analytics/summary', { params });
}

export default apiClient;