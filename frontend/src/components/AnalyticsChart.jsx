import React, { useState, useEffect, useCallback } from 'react';
import { Card, Row, Col, Spin, message, Typography, Empty } from 'antd';
import ReactECharts from 'echarts-for-react';
import * as echarts from 'echarts';
import { getAnalytics } from '../services/api';

const { Text } = Typography;

function AnalyticsChart() {
  const [timeTrendData, setTimeTrendData] = useState(null);
  const [personTypeData, setPersonTypeData] = useState(null);
  const [departmentData, setDepartmentData] = useState(null);
  const [loading, setLoading] = useState(false);

  const fetchAllData = useCallback(async () => {
    setLoading(true);
    try {
      const [timeRes, typeRes, deptRes] = await Promise.all([
        getAnalytics('timeTrend'),
        getAnalytics('personType'),
        getAnalytics('department'),
      ]);
      if (timeRes.data.code === 200) setTimeTrendData(timeRes.data.data);
      if (typeRes.data.code === 200) setPersonTypeData(typeRes.data.data);
      if (deptRes.data.code === 200) setDepartmentData(deptRes.data.data);
    } catch (err) {
      message.error('获取报表数据失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAllData();
  }, [fetchAllData]);

  const getLineOption = (data) => {
    if (!data || !data.series || data.series.length === 0) return {};
    return {
      title: {
        text: '调用趋势（按时间）',
        left: 'center',
        textStyle: { fontSize: 14 },
      },
      tooltip: {
        trigger: 'axis',
        formatter: '{b}<br/>调用次数: {c}',
      },
      xAxis: {
        type: 'category',
        data: data.series.map((item) => item.label),
        axisLabel: { rotate: 45 },
      },
      yAxis: { type: 'value' },
      series: [
        {
          type: 'line',
          data: data.series.map((item) => item.value),
          smooth: true,
          areaStyle: { opacity: 0.3 },
          lineStyle: { width: 2 },
          itemStyle: { color: '#5470c6' },
        },
      ],
      grid: { bottom: 60 },
    };
  };

  const getPieOption = (data) => {
    if (!data || !data.series || data.series.length === 0) return {};
    return {
      title: {
        text: '人员类型分布',
        left: 'center',
        textStyle: { fontSize: 14 },
      },
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c} ({d}%)',
      },
      series: [
        {
          type: 'pie',
          radius: ['30%', '60%'],
          center: ['50%', '55%'],
          data: data.series.map((item) => ({
            name: item.label,
            value: item.value,
          })),
          label: {
            formatter: '{b}: {c}',
          },
        },
      ],
    };
  };

  const getBarOption = (data) => {
    if (!data || !data.series || data.series.length === 0) return {};
    return {
      title: {
        text: '部门调用分布',
        left: 'center',
        textStyle: { fontSize: 14 },
      },
      tooltip: {
        trigger: 'axis',
        formatter: '{b}<br/>调用次数: {c}',
      },
      xAxis: {
        type: 'category',
        data: data.series.map((item) => item.label),
        axisLabel: { rotate: 45 },
      },
      yAxis: { type: 'value' },
      series: [
        {
          type: 'bar',
          data: data.series.map((item) => item.value),
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#83bff6' },
              { offset: 1, color: '#188df0' },
            ]),
          },
          barMaxWidth: 50,
        },
      ],
      grid: { bottom: 60 },
    };
  };

  const hasData = timeTrendData || personTypeData || departmentData;
  const hasAnySeries = (timeTrendData?.series?.length > 0) ||
    (personTypeData?.series?.length > 0) ||
    (departmentData?.series?.length > 0);

  return (
    <Card title="调用分析报表">
      {loading ? (
        <div style={{ textAlign: 'center', padding: 40 }}>
          <Spin size="large" />
        </div>
      ) : hasData && hasAnySeries ? (
        <>
          <div style={{ textAlign: 'center', marginBottom: 16 }}>
            <Text>总调用次数: <strong>{timeTrendData?.totalCalls || personTypeData?.totalCalls || departmentData?.totalCalls || 0}</strong></Text>
          </div>
          <Row gutter={16}>
            <Col span={8}>
              <ReactECharts
                option={getLineOption(timeTrendData)}
                style={{ height: 350 }}
              />
            </Col>
            <Col span={8}>
              <ReactECharts
                option={getPieOption(personTypeData)}
                style={{ height: 350 }}
              />
            </Col>
            <Col span={8}>
              <ReactECharts
                option={getBarOption(departmentData)}
                style={{ height: 350 }}
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