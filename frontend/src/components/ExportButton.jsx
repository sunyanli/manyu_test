import React, { useState } from 'react';
import { Button, Dropdown, message, Space } from 'antd';
import { DownloadOutlined } from '@ant-design/icons';
import { getExportUrl } from '../services/api';

function ExportButton() {
  const [exporting, setExporting] = useState(false);

  const handleExport = async (type, format) => {
    setExporting(true);
    try {
      const url = getExportUrl(type, format);
      // 使用临时链接下载
      const link = document.createElement('a');
      link.href = url;
      link.download = `${type}_export.${format === 'xlsx' ? 'xlsx' : 'csv'}`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      message.success(`导出 ${type} 成功`);
    } catch (err) {
      message.error('导出失败: ' + (err.message || '未知错误'));
    } finally {
      setExporting(false);
    };
  };

  const items = [
    {
      key: 'hello',
      label: 'HelloWorld 结果',
      children: [
        { key: 'hello-csv', label: 'CSV 格式', onClick: () => handleExport('hello', 'csv') },
        { key: 'hello-xlsx', label: 'Excel 格式', onClick: () => handleExport('hello', 'xlsx') },
      ],
    },
    {
      key: 'hash',
      label: '哈希结果',
      children: [
        { key: 'hash-csv', label: 'CSV 格式', onClick: () => handleExport('hash', 'csv') },
        { key: 'hash-xlsx', label: 'Excel 格式', onClick: () => handleExport('hash', 'xlsx') },
      ],
    },
    {
      key: 'bubble',
      label: '排序结果',
      children: [
        { key: 'bubble-csv', label: 'CSV 格式', onClick: () => handleExport('bubble', 'csv') },
        { key: 'bubble-xlsx', label: 'Excel 格式', onClick: () => handleExport('bubble', 'xlsx') },
      ],
    },
  ];

  return (
    <Dropdown menu={{ items }} disabled={exporting}>
      <Button type="primary" icon={<DownloadOutlined />} loading={exporting}>
        导出结果
      </Button>
    </Dropdown>
  );
}

export default ExportButton;