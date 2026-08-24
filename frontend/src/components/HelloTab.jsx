import React, { useState } from 'react';
import { Input, Button, Card, message, Spin, Typography } from 'antd';
import { callHello } from '../services/api';

const { TextArea } = Input;
const { Title, Text } = Typography;

function HelloTab() {
  const [name, setName] = useState('');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleCall = async () => {
    setLoading(true);
    try {
      const res = await callHello(name || 'World');
      if (res.data.code === 200) {
        setResult(res.data.data);
        message.success('调用成功');
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
    <Card title="HelloWorld 接口" style={{ marginBottom: 16 }}>
      <div style={{ marginBottom: 16 }}>
        <Text>输入名称：</Text>
        <Input
          style={{ width: 300, marginLeft: 8 }}
          placeholder="请输入名称（默认 World）"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
      </div>
      <Button type="primary" onClick={handleCall} loading={loading}>
        调用接口
      </Button>
      {loading && <Spin style={{ marginLeft: 16 }} />}
      {result && (
        <Card style={{ marginTop: 16, backgroundColor: '#f6ffed' }}>
          <Text strong>返回结果：</Text>
          <div style={{ marginTop: 8 }}>
            <Text>{result.greeting}</Text>
          </div>
        </Card>
      )}
    </Card>
  );
}

export default HelloTab;