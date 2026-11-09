import type { MetadataRoute } from "next";

/** 全站 noindex（内部工具，无 A 类公开页，见 design.md §4.1）。 */
export default function robots(): MetadataRoute.Robots {
  return {
    rules: [{ userAgent: "*", disallow: "/" }],
  };
}
