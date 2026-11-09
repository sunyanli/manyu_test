"use client";

// B 类页：全站 noindex（layout metadata robots + app/robots.ts 兜底）；浏览器经 AlipayGuard 降级。
import { AlipayGuard } from "@/components/AlipayGuard";
import { SkeletonList } from "@/components/SkeletonList";
import { TodoList } from "@/components/TodoList";

export default function HomePage() {
  return (
    <AlipayGuard fallback={<SkeletonList />}>
      <TodoList />
    </AlipayGuard>
  );
}
