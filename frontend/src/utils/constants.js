// 调用人信息（模拟登录用户）
export const CALLER_INFO = {
  name: '张三',
  personType: '研发',
  personLevel: '高级',
  department: '技术部',
};

// 支持的可选算法列表
export const HASH_ALGORITHMS = ['SHA-256', 'MD5', 'SHA-512'];

// 分析维度
export const DIMENSIONS = [
  { key: 'personType', label: '人员类型' },
  { key: 'personLevel', label: '人员层级' },
  { key: 'department', label: '人员部门' },
  { key: 'timeTrend', label: '时间趋势' },
];

// 图表类型映射
export const CHART_TYPE_MAP = {
  personType: 'pie',
  personLevel: 'pie',
  department: 'bar',
  timeTrend: 'line',
};