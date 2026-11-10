import { NextRequest, NextResponse } from 'next/server';
import { buildAuthorizeUrl } from '@/lib/auth-server';

// GET /api/auth/authorize — 服务端构建 publicAppAuthorize 跳转 URL（私钥/换 uid 仅在服务端）
export async function GET(req: NextRequest) {
  const origin = new URL(req.url).origin;
  const redirectUri = `${origin}/login`;
  return NextResponse.json({ url: buildAuthorizeUrl(redirectUri) });
}
