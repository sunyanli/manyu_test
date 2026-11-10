import type { Metadata } from 'next';
import { AlipayGuard } from '@/components/AlipayGuard';
import HomeClient from './HomeClient';

export const metadata: Metadata = {
  robots: { index: false, follow: false },
};

export default function Page() {
  return (
    <AlipayGuard>
      <HomeClient />
    </AlipayGuard>
  );
}
