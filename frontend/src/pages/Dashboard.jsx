import React from 'react';
import { Tabs, Typography, Divider } from 'antd';
import { CodeOutlined, KeyOutlined, SortAscendingOutlined } from '@ant-design/icons';
import HelloTab from '../components/HelloTab';
import HashTab from '../components/HashTab';
import BubbleTab from '../components/BubbleTab';
import ExportButton from '../components/ExportButton';
import AnalyticsChart from '../components/AnalyticsChart';

const { Title } = Typography;

function Dashboard() {
  const tabItems = [
    {
      key: 'hello',
      label: (
        <span>
          <CodeOutlined /> HelloWorld
        </span>
      ),
      children: <HelloTab />,
    },
    {
      key: 'hash',
      label: (
        <span>
          <KeyOutlined /> 哈希算法
        </span>
      ),
      children: <HashTab />,
    },
    {
      key: 'bubble',
      label: (
        <span>
          <SortAscendingOutlined /> 冒泡排序
        </span>
      ),
      children: <BubbleTab />,
    },
  ];

  return (
    <div style={{ maxWidth: 1200, margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <Title level={2} style={{ margin: 0 }}>
          三接口展示与调用分析报表系统
        </Title>
        <ExportButton />
      </div>

      <Tabs defaultActiveKey="hello" items={tabItems} />

      <Divider />

      <AnalyticsChart />
    </div>
  );
}

export default Dashboard;