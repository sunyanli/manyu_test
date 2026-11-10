import type { Metadata } from 'next';
import { AlipayGuard } from '@/components/AlipayGuard';
import LoginClient from './LoginClient';

export const metadata: Metadata = {
  robots: { index: false, follow: false },
};

export default function Page() {
  return (
    <AlipayGuard>
      <LoginClient />
    </AlipayGuard>
  );
}
