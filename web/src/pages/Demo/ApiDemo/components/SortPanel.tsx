import { Alert, Button, Descriptions, Form, Input, Select, Space } from 'antd';
import { useState } from 'react';

import { fetchSort } from '@/services/xchain/demoController';
import type { DemoResult } from '@/services/xchain/typing';

interface SortFormValues {
  numbers: string;
  order: 'asc' | 'desc';
}

/**
 * 将逗号分隔的数字串解析为数组，非法输入返回 null。
 */
function parseNumbers(text: string): number[] | null {
  const segments = text
    .split(',')
    .map((item) => item.trim())
    .filter((item) => item.length > 0);
  if (segments.length === 0) {
    return null;
  }
  const numbers: number[] = [];
  for (const segment of segments) {
    const value = Number(segment);
    if (!Number.isInteger(value)) {
      return null;
    }
    numbers.push(value);
  }
  return numbers;
}

/**
 * 冒泡排序 Tab：输入数字数组，展示排序结果与交换次数。
 */
export default function SortPanel() {
  const [form] = Form.useForm<SortFormValues>();
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<DemoResult>();
  const [error, setError] = useState<string>();

  const onFinish = async (values: SortFormValues) => {
    const numbers = parseNumbers(values.numbers);
    if (!numbers) {
      setError('请输入以英文逗号分隔的整数，例如 5,3,8,1,9,2');
      return;
    }
    setLoading(true);
    setError(undefined);
    try {
      setResult(await fetchSort({ numbers, order: values.order }));
    } catch (e) {
      setError(e instanceof Error ? e.message : '排序失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ padding: '16px 0' }}>
      <Form<SortFormValues>
        form={form}
        layout="inline"
        initialValues={{ numbers: '5,3,8,1,9,2', order: 'asc' }}
        onFinish={onFinish}
        style={{ marginBottom: 16 }}
      >
        <Form.Item
          name="numbers"
          label="数组"
          rules={[{ required: true, message: '请输入待排序的数字数组' }]}
        >
          <Input style={{ width: 320 }} placeholder="5,3,8,1,9,2" allowClear />
        </Form.Item>
        <Form.Item name="order" label="顺序">
          <Select
            style={{ width: 120 }}
            options={[
              { value: 'asc', label: '升序' },
              { value: 'desc', label: '降序' },
            ]}
          />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit" loading={loading}>
              排序
            </Button>
            <Button onClick={() => form.resetFields()}>重置</Button>
          </Space>
        </Form.Item>
      </Form>
      {error && <Alert type="error" showIcon message={error} style={{ marginBottom: 16 }} />}
      {result && (
        <Descriptions bordered column={1} size="middle">
          <Descriptions.Item label="原始数组">{result.original?.join(', ')}</Descriptions.Item>
          <Descriptions.Item label="排序后">{result.sorted?.join(', ')}</Descriptions.Item>
          <Descriptions.Item label="排序顺序">{result.order === 'desc' ? '降序' : '升序'}</Descriptions.Item>
          <Descriptions.Item label="交换次数">{result.swapCount}</Descriptions.Item>
          <Descriptions.Item label="执行时间">{result.timestamp}</Descriptions.Item>
        </Descriptions>
      )}
    </div>
  );
}
