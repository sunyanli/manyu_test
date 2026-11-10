import type { MetadataRoute } from 'next';

export default function robots(): MetadataRoute.Robots {
  // 全站 noindex（产品不收录），无 A 类可索引页。
  return {
    rules: [{ userAgent: '*', disallow: '/' }],
  };
}
