import React, { useState } from 'react';
import { Input, Button, Card, Select, message, Spin, Typography, Tag } from 'antd';
import { callHash } from '../services/api';
import { HASH_ALGORITHMS } from '../utils/constants';

const { TextArea } = Input;
const { Title, Text } = Typography;

function HashTab() {
  const [input, setInput] = useState('');
  const [algorithm, setAlgorithm] = useState('SHA-256');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleCall = async () => {
    if (!input) {
      message.warning('请输入要计算哈希的文本');
      return;
    }
    setLoading(true);
    try {
      const res = await callHash(input, algorithm);
      if (res.data.code === 200) {
        setResult(res.data.data);
        message.success('计算成功');
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
    <Card title="哈希算法接口" style={{ marginBottom: 16 }}>
      <div style={{ marginBottom: 16 }}>
        <Text>输入文本：</Text>
        <Input
          style={{ width: 300, marginLeft: 8 }}
          placeholder="请输入要计算哈希的文本"
          value={input}
          onChange={(e) => setInput(e.target.value)}
        />
      </div>
      <div style={{ marginBottom: 16 }}>
        <Text>选择算法：</Text>
        <Select
          style={{ width: 200, marginLeft: 8 }}
          value={algorithm}
          onChange={setAlgorithm}
          options={HASH_ALGORITHMS.map((algo) => ({ label: algo, value: algo }))}
        />
      </div>
      <Button type="primary" onClick={handleCall} loading={loading}>
        计算哈希
      </Button>
      {loading && <Spin style={{ marginLeft: 16 }} />}
      {result && (
        <Card style={{ marginTop: 16, backgroundColor: '#f6ffed' }}>
          <Text strong>计算结果：</Text>
          <div style={{ marginTop: 8 }}>
            <Text>输入: {result.input}</Text>
          </div>
          <div style={{ marginTop: 4 }}>
            <Text>算法: </Text>
            <Tag color="blue">{result.algorithm}</Tag>
          </div>
          <div style={{ marginTop: 4, wordBreak: 'break-all' }}>
            <Text>哈希值: {result.hash}</Text>
          </div>
        </Card>
      )}
    </Card>
  );
}

export default HashTab;