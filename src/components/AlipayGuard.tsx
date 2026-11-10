'use client';

import { Button, Result } from 'antd-mobile';
import { useInAlipay } from '@/hooks/useInAlipay';
import { openPageInAlipay } from '@/container/container';

/**
 * noindex 功能页（B 类）的浏览器降级屏：
 * 支付宝内渲染子内容；浏览器内显示「请在支付宝中打开」+ scheme/landing 唤起。
 */
export function AlipayGuard({ children }: { children: React.ReactNode }) {
  const inApp = useInAlipay();
  if (inApp === null) return null;
  if (inApp) return <>{children}</>;
  return (
    <div className="guard" role="main">
      <Result status="info" title="请在支付宝中打开" description="本功能需在支付宝客户端内使用" />
      <Button color="primary" onClick={() => openPageInAlipay(window.location.href)}>
        在支付宝中打开
      </Button>
    </div>
  );
}
