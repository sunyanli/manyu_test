import { DownloadOutlined } from '@ant-design/icons';
import { PageContainer } from '@ant-design/pro-components';
import { Button, DatePicker, Dropdown, message, Space, Tabs, type MenuProps } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import { useCallback, useState } from 'react';

import { downloadBlob, exportDemoResult } from '@/services/xchain/demoController';

import CallStatsReport from './components/CallStatsReport';
import HashPanel from './components/HashPanel';
import HelloWorldPanel from './components/HelloWorldPanel';
import SortPanel from './components/SortPanel';

const { RangePicker } = DatePicker;

/** 导出类型 */
type ExportType = 'hello' | 'hash' | 'sort' | 'all';

/** 默认查询近 7 天 */
const DEFAULT_RANGE: [Dayjs, Dayjs] = [dayjs().subtract(6, 'day'), dayjs()];

/**
 * 接口演示中心：三个 Tab 展示接口执行结果，顶部导出，底部调用统计报表。
 */
export default function ApiDemoPage() {
  const [activeTab, setActiveTab] = useState('hello');
  const [range, setRange] = useState<[Dayjs, Dayjs]>(DEFAULT_RANGE);
  const [refreshToken, setRefreshToken] = useState(0);
  const [exporting, setExporting] = useState(false);

  const doExport = useCallback(
    async (type: ExportType) => {
      setExporting(true);
      try {
        const blob = await exportDemoResult(
          type,
          range[0].format('YYYY-MM-DD'),
          range[1].format('YYYY-MM-DD'),
        );
        downloadBlob(blob, `api-demo-export-${type}-${dayjs().format('YYYYMMDD')}.xlsx`);
        message.success('导出成功');
      } catch (e) {
        message.error(e instanceof Error ? e.message : '导出失败');
      } finally {
        setExporting(false);
      }
    },
    [range],
  );

  const exportMenu: MenuProps = {
    items: [
      { key: 'current', label: `导出当前 Tab（${activeTab}）` },
      { key: 'all', label: '导出全部 Tab' },
    ],
    onClick: ({ key }) => doExport(key === 'all' ? 'all' : (activeTab as ExportType)),
  };

  const items = [
    {
      key: 'hello',
      label: 'HelloWorld',
      children: <HelloWorldPanel />,
    },
    {
      key: 'hash',
      label: '哈希算法',
      children: <HashPanel />,
    },
    {
      key: 'sort',
      label: '冒泡排序',
      children: <SortPanel />,
    },
  ];

  return (
    <PageContainer
      title="接口演示中心"
      extra={
        <Space>
          <RangePicker
            value={range}
            allowClear={false}
            onChange={(value) => {
              if (value && value[0] && value[1]) {
                setRange([value[0], value[1]]);
                setRefreshToken((token) => token + 1);
              }
            }}
          />
          <Button onClick={() => setRefreshToken((token) => token + 1)}>刷新报表</Button>
          <Dropdown menu={exportMenu} disabled={exporting}>
            <Button type="primary" icon={<DownloadOutlined />} loading={exporting}>
              导出
            </Button>
          </Dropdown>
        </Space>
      }
    >
      <Tabs
        activeKey={activeTab}
        onChange={(key) => {
          setActiveTab(key);
          setRefreshToken((token) => token + 1);
        }}
        items={items}
      />
      <div style={{ marginTop: 16 }}>
        <CallStatsReport refreshToken={refreshToken} range={range} />
      </div>
    </PageContainer>
  );
}
