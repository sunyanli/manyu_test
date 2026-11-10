// 服务端专用：构建支付宝 OAuth 网页授权跳转 URL（publicAppAuthorize）。
// 私钥签名/换 uid 仅在服务端进行，绝不入前端 bundle。
export function buildAuthorizeUrl(redirectUri: string): string {
  const appId = process.env.ALIPAY_APP_ID ?? '2021000000000000';
  const params = new URLSearchParams({
    app_id: appId,
    scope: 'auth_user',
    redirect_uri: redirectUri,
    state: 'todo-login',
  });
  return `https://openauth.alipay.com/oauth2/publicAppAuthorize.htm?${params.toString()}`;
}
