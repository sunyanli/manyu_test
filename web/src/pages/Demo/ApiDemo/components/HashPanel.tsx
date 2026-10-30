import { Alert, Button, Descriptions, Form, Input, Select, Space } from 'antd';
import { useState } from 'react';

import { fetchHash } from '@/services/xchain/demoController';
import type { DemoResult } from '@/services/xchain/typing';

interface HashFormValues {
  input: string;
  algorithm: 'MD5' | 'SHA-1' | 'SHA-256';
}

/**
 * 哈希算法 Tab：输入原文并选择算法，展示哈希结果。
 */
export default function HashPanel() {
  const [form] = Form.useForm<HashFormValues>();
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<DemoResult>();
  const [error, setError] = useState<string>();

  const onFinish = async (values: HashFormValues) => {
    setLoading(true);
    setError(undefined);
    try {
      setResult(await fetchHash(values));
    } catch (e) {
      setError(e instanceof Error ? e.message : '计算失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ padding: '16px 0' }}>
      <Form<HashFormValues>
        form={form}
        layout="inline"
        initialValues={{ algorithm: 'SHA-256' }}
        onFinish={onFinish}
        style={{ marginBottom: 16 }}
      >
        <Form.Item
          name="input"
          label="原文"
          rules={[{ required: true, message: '请输入待计算哈希的原文' }]}
        >
          <Input style={{ width: 320 }} placeholder="请输入待计算哈希的字符串" allowClear />
        </Form.Item>
        <Form.Item name="algorithm" label="算法">
          <Select
            style={{ width: 140 }}
            options={[
              { value: 'SHA-256', label: 'SHA-256' },
              { value: 'SHA-1', label: 'SHA-1' },
              { value: 'MD5', label: 'MD5' },
            ]}
          />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit" loading={loading}>
              计算哈希
            </Button>
            <Button onClick={() => form.resetFields()}>重置</Button>
          </Space>
        </Form.Item>
      </Form>
      {error && <Alert type="error" showIcon message={error} style={{ marginBottom: 16 }} />}
      {result && (
        <Descriptions bordered column={1} size="middle">
          <Descriptions.Item label="算法">{result.algorithm}</Descriptions.Item>
          <Descriptions.Item label="原文">{result.input}</Descriptions.Item>
          <Descriptions.Item label="哈希结果">
            <span style={{ wordBreak: 'break-all' }}>{result.output}</span>
          </Descriptions.Item>
          <Descriptions.Item label="执行时间">{result.timestamp}</Descriptions.Item>
        </Descriptions>
      )}
    </div>
  );
}
