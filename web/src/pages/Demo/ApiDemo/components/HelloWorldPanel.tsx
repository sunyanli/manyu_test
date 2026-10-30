import { Alert, Button, Descriptions, Spin } from 'antd';
import { useCallback, useEffect, useState } from 'react';

import { fetchHello } from '@/services/xchain/demoController';
import type { DemoResult } from '@/services/xchain/typing';

/**
 * HelloWorld Tab：进入即自动调用，展示返回消息与时间戳。
 */
export default function HelloWorldPanel() {
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<DemoResult>();
  const [error, setError] = useState<string>();

  const run = useCallback(async () => {
    setLoading(true);
    setError(undefined);
    try {
      setResult(await fetchHello());
    } catch (e) {
      setError(e instanceof Error ? e.message : '调用失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    run();
  }, [run]);

  return (
    <Spin spinning={loading}>
      <div style={{ padding: '16px 0' }}>
        <Button type="primary" onClick={run} style={{ marginBottom: 16 }}>
          重新调用
        </Button>
        {error && <Alert type="error" showIcon message={error} style={{ marginBottom: 16 }} />}
        {result && (
          <Descriptions bordered column={1} size="middle">
            <Descriptions.Item label="返回消息">{result.message}</Descriptions.Item>
            <Descriptions.Item label="执行时间">{result.timestamp}</Descriptions.Item>
          </Descriptions>
        )}
      </div>
    </Spin>
  );
}
