'use client';

import { Button } from 'antd-mobile';
import { openPageInAlipay } from '@/container/container';

export default function GuideClient() {
  return (
    <div className="guide-cta">
      <Button color="primary" size="large" onClick={() => openPageInAlipay(window.location.href)}>
        在支付宝中打开
      </Button>
      <p className="guide-sub">打开即可记录 · 回车即存</p>
    </div>
  );
}
