import type { Metadata } from 'next';
import { AlipayGuard } from '@/components/AlipayGuard';
import CreateClient from './CreateClient';

export const metadata: Metadata = {
  robots: { index: false, follow: false },
};

export default function Page() {
  return (
    <AlipayGuard>
      <CreateClient />
    </AlipayGuard>
  );
}
