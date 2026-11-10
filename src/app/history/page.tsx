import type { Metadata } from 'next';
import { AlipayGuard } from '@/components/AlipayGuard';
import HistoryClient from './HistoryClient';

export const metadata: Metadata = {
  robots: { index: false, follow: false },
};

export default function Page() {
  return (
    <AlipayGuard>
      <HistoryClient />
    </AlipayGuard>
  );
}
