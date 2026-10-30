import { Column, Line, Pie } from '@ant-design/charts';
import { Card, Col, DatePicker, Empty, Row, Select, Spin, Statistic } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import { useCallback, useEffect, useState } from 'react';

import { fetchStats } from '@/services/xchain/demoController';
import type { StatsDimension, StatsReport } from '@/services/xchain/typing';

interface CallStatsReportProps {
  /** 报表刷新触发器，页面每次执行接口后自增 */
  refreshToken: number;
  /** 日期区间，与导出按钮共用 */
  range: [Dayjs, Dayjs];
}

const DIMENSION_OPTIONS: { value: StatsDimension; label: string }[] = [
  { value: 'personType', label: '人员类型' },
  { value: 'personLevel', label: '人员层级' },
  { value: 'personDept', label: '人员部门' },
];

/**
 * 调用统计报表：折线图（趋势）、饼图（占比）、柱状图（对比）。
 */
export default function CallStatsReport({ refreshToken, range }: CallStatsReportProps) {
  const [dimension, setDimension] = useState<StatsDimension>('personType');
  const [report, setReport] = useState<StatsReport>();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>();

  const load = useCallback(async () => {
    setLoading(true);
    setError(undefined);
    try {
      const data = await fetchStats(
        dimension,
        range[0].format('YYYY-MM-DD'),
        range[1].format('YYYY-MM-DD'),
      );
      setReport(data);
    } catch (e) {
      setError(e instanceof Error ? e.message : '统计查询失败');
      setReport(undefined);
    } finally {
      setLoading(false);
    }
  }, [dimension, range]);

  useEffect(() => {
    load();
  }, [load, refreshToken]);

  const items = report?.items ?? [];
  const trend = report?.trend ?? [];
  const isEmpty = items.length === 0 && trend.length === 0;

  return (
    <Card
      title="调用统计报表"
      extra={
        <Select
          value={dimension}
          style={{ width: 140 }}
          options={DIMENSION_OPTIONS}
          onChange={(value: StatsDimension) => setDimension(value)}
        />
      }
    >
      <Spin spinning={loading}>
        {error ? (
          <Empty description={error} />
        ) : isEmpty ? (
          <Empty description="暂无调用数据" />
        ) : (
          <>
            <Row gutter={16} style={{ marginBottom: 16 }}>
              <Col span={8}>
                <Statistic title="区间总调用次数" value={report?.totalCount ?? 0} />
              </Col>
              <Col span={8}>
                <Statistic title="统计维度" value={report?.dimensionName ?? '-'} />
              </Col>
              <Col span={8}>
                <Statistic
                  title="统计区间"
                  value={`${report?.startDate ?? ''} ~ ${report?.endDate ?? ''}`}
                />
              </Col>
            </Row>
            <Row gutter={16}>
              <Col span={8}>
                <Card type="inner" title="每日调用趋势（折线图）" size="small">
                  {trend.length === 0 ? (
                    <Empty description="暂无趋势数据" />
                  ) : (
                    <Line
                      data={trend}
                      xField="date"
                      yField="callCount"
                      seriesField="name"
                      height={260}
                      legend={{ color: { position: 'bottom' } }}
                    />
                  )}
                </Card>
              </Col>
              <Col span={8}>
                <Card type="inner" title="调用占比（饼图）" size="small">
                  {items.length === 0 ? (
                    <Empty description="暂无占比数据" />
                  ) : (
                    <Pie
                      data={items}
                      angleField="callCount"
                      colorField="name"
                      height={260}
                      innerRadius={0.4}
                      legend={{ color: { position: 'bottom' } }}
                    />
                  )}
                </Card>
              </Col>
              <Col span={8}>
                <Card type="inner" title="调用次数对比（柱状图）" size="small">
                  {items.length === 0 ? (
                    <Empty description="暂无对比数据" />
                  ) : (
                    <Column data={items} xField="name" yField="callCount" height={260} />
                  )}
                </Card>
              </Col>
            </Row>
          </>
        )}
      </Spin>
    </Card>
  );
}
