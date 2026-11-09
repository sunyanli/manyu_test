"use client";

// B 类页：全站 noindex（layout metadata robots + app/robots.ts 兜底）；浏览器经 AlipayGuard 降级。
import { AlipayGuard } from "@/components/AlipayGuard";
import { CreateForm } from "@/components/CreateForm";
import { CreateSkeleton } from "@/components/SkeletonList";

export default function CreatePage() {
  return (
    <AlipayGuard fallback={<CreateSkeleton />}>
      <CreateForm />
    </AlipayGuard>
  );
}
