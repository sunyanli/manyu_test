import React, { useState, useEffect, useCallback } from 'react';
import { Card, Select, Row, Col, Spin, message, Typography, Empty } from 'antd';
import ReactECharts from 'echarts-for-react';
import * as echarts from 'echarts';
import { getAnalytics } from '../services/api';
import { DIMENSIONS, CHART_TYPE_MAP } from '../utils/constants';

const { Title, Text } = Typography;

function AnalyticsChart() {
  const [dimension, setDimension] = useState('personType');
  const [chartData, setChartData] = useState(null);
  const [loading, setLoading] = useState(false);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getAnalytics(dimension);
      if (res.data.code === 200) {
        setChartData(res.data.data);
      }
    } catch (err) {
      message.error('获取报表数据失败');
    } finally {
      setLoading(false);
    }
  }, [dimension]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const getChartOption = (chartType, chartDimension) => {
    if (!chartData || !chartData.series) return {};

    const seriesData = chartData.series.map((item) => ({
      name: item.label,
      value: item.value,
    }));

    const baseOption = {
      title: {
        text: `${chartData.dimension} - 调用分布`,
        left: 'center',
        textStyle: { fontSize: 14 },
      },
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c} ({d}%)',
      },
    };

    if (chartType === 'pie') {
      return {
        ...baseOption,
        series: [
          {
            type: 'pie',
            radius: ['30%', '60%'],
            center: ['50%', '55%'],
            data: seriesData,
            label: {
              formatter: '{b}: {c}',
            },
          },
        ],
      };
    }

    if (chartType === 'line') {
      return {
        ...baseOption,
        xAxis: {
          type: 'category',
          data: chartData.series.map((item) => item.label),
          axisLabel: { rotate: 45 },
        },
        yAxis: { type: 'value' },
        series: [
          {
            type: 'line',
            data: chartData.series.map((item) => item.value),
            smooth: true,
            areaStyle: { opacity: 0.3 },
          },
        ],
      };
    }

    // bar
    return {
      ...baseOption,
      xAxis: {
        type: 'category',
        data: chartData.series.map((item) => item.label),
        axisLabel: { rotate: 45 },
      },
      yAxis: { type: 'value' },
      series: [
        {
          type: 'bar',
          data: chartData.series.map((item) => item.value),
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#83bff6' },
              { offset: 1, color: '#188df0' },
            ]),
          },
        },
      ],
    };
  };

  const chartType = CHART_TYPE_MAP[dimension] || 'bar';

  return (
    <Card
      title="调用分析报表"
      extra={
        <Select
          value={dimension}
          onChange={setDimension}
          style={{ width: 150 }}
          options={DIMENSIONS.map((d) => ({ label: d.label, value: d.key }))}
        />
      }
    >
      {loading ? (
        <div style={{ textAlign: 'center', padding: 40 }}>
          <Spin size="large" />
        </div>
      ) : chartData && chartData.series && chartData.series.length > 0 ? (
        <>
          <div style={{ textAlign: 'center', marginBottom: 16 }}>
            <Text>总调用次数: <strong>{chartData.totalCalls}</strong></Text>
          </div>
          <Row gutter={16}>
            <Col span={12}>
              <ReactECharts
                option={getChartOption(chartType, dimension)}
                style={{ height: 350 }}
              />
            </Col>
            <Col span={12}>
              <ReactECharts
                option={getChartOption('pie', dimension)}
                style={{ height: 350 }}
              />
            </Col>
          </Row>
          <Row gutter={16} style={{ marginTop: 16 }}>
            <Col span={24}>
              <ReactECharts
                option={getChartOption('bar', dimension)}
                style={{ height: 300 }}
              />
            </Col>
          </Row>
        </>
      ) : (
        <Empty description="暂无调用数据" />
      )}
    </Card>
  );
}

export default AnalyticsChart;