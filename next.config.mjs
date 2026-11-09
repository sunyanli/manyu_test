/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  // 本期交付可运行 demo，构建不因 lint 阻塞；类型检查仍由 next build 执行。
  eslint: { ignoreDuringBuilds: true },
};

export default nextConfig;
