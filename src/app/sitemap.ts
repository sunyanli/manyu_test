import type { MetadataRoute } from 'next';

export default function sitemap(): MetadataRoute.Sitemap {
  // 全站 noindex（不收录），无可索引 URL。
  return [];
}
