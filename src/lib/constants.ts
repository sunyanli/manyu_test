// 前端可见的公开配置（白名单，见 coding-standards.md「环境变量边界」）
export const SITE_URL: string = process.env.NEXT_PUBLIC_SITE_URL ?? 'https://todo.example.com';
export const SITE_NAME = '日常待办';
export const SITE_NAME_EN = 'Daily Todos';
export const BRAND_COLOR = '#1677FF';

// 外部分享卡片三要素（落地 /guide）
export const SHARE_TITLE = '日常待办 · 记下每件要事';
export const SHARE_DESC = '帮助记录日常待办事项';

// 输入校验上限
export const NAME_MAX = 100;
export const DESC_MAX = 500;
export const PAGE_SIZE = 10;
