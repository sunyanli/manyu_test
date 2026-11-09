/** 站点名（i18n 文案在 src/i18n 中，这里仅为常量引用/兜底）。 */
export const SITE_NAME = "日常待办";

/** mock 固定测试 uid（真实 OAuth 登录为 Should 预留，见 requirements.md Q2/Q3）。 */
export const TEST_UID = "test_uid_0001";
export const TEST_NICKNAME = "测试同事";

/** 分页：每页条数与上限。 */
export const PAGE_SIZE = 20;
export const MAX_PAGE_SIZE = 50;

/** 字段长度约束（与 design.md §6.2.1 对齐）。 */
export const NAME_MAX = 60;
export const DESC_MAX = 500;

/** 语言偏好持久化 key（localStorage / cookie）。 */
export const LANG_STORAGE_KEY = "todo.lang";
export const LANG_COOKIE = "todo_lang";
