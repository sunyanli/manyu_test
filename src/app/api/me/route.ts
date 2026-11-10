import { NextResponse } from 'next/server';

// GET /api/me — 当前用户（真实后端：OAuth 换 uid 后返回）
export async function GET() {
  return NextResponse.json({ uid: 'internal-user', nickname: '内部员工' });
}
