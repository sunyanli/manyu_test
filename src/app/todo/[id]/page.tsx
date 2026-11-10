import type { Metadata } from 'next';
import { AlipayGuard } from '@/components/AlipayGuard';
import TodoDetailClient from './TodoDetailClient';

export const metadata: Metadata = {
  robots: { index: false, follow: false },
};

export default function Page({ params }: { params: { id: string } }) {
  return (
    <AlipayGuard>
      <TodoDetailClient id={params.id} />
    </AlipayGuard>
  );
}
