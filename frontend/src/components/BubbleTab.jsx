import React, { useState } from 'react';
import { Input, Button, Card, message, Spin, Typography, Tag } from 'antd';
import { callBubbleSort } from '../services/api';

const { Text } = Typography;

function BubbleTab() {
  const [arrayInput, setArrayInput] = useState('');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleCall = async () => {
    let array;
    try {
      array = JSON.parse(`[${arrayInput}]`);
      if (!Array.isArray(array) || array.length === 0) {
        message.warning('请输入有效的数字数组，例如: 5, 3, 8, 1, 2');
        return;
      }
    } catch {
      message.warning('请输入有效的数字数组，例如: 5, 3, 8, 1, 2');
      return;
    }

    setLoading(true);
    try {
      const res = await callBubbleSort(array);
      if (res.data.code === 200) {
        setResult(res.data.data);
        message.success('排序成功');
      } else {
        message.error(res.data.message);
      }
    } catch (err) {
      message.error('调用失败: ' + (err.message || '未知错误'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card title="冒泡排序接口" style={{ marginBottom: 16 }}>
      <div style={{ marginBottom: 16 }}>
        <Text>输入数组（逗号分隔）：</Text>
        <Input
          style={{ width: 300, marginLeft: 8 }}
          placeholder="例如: 5, 3, 8, 1, 2"
          value={arrayInput}
          onChange={(e) => setArrayInput(e.target.value)}
        />
      </div>
      <Button type="primary" onClick={handleCall} loading={loading}>
        排序
      </Button>
      {loading && <Spin style={{ marginLeft: 16 }} />}
      {result && (
        <Card style={{ marginTop: 16, backgroundColor: '#f6ffed' }}>
          <Text strong>排序结果：</Text>
          <div style={{ marginTop: 8 }}>
            <Text>原始数组: [{result.originalArray.join(', ')}]</Text>
          </div>
          <div style={{ marginTop: 4 }}>
            <Text>排序后: </Text>
            <Tag color="green">[{result.sortedArray.join(', ')}]</Tag>
          </div>
          <div style={{ marginTop: 4 }}>
            <Text>交换次数: {result.swapCount}</Text>
          </div>
          <div style={{ marginTop: 4 }}>
            <Text>比较次数: {result.comparisonCount}</Text>
          </div>
        </Card>
      )}
    </Card>
  );
}

export default BubbleTab;