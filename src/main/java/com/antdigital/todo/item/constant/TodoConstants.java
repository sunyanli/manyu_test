package com.antdigital.todo.item.constant;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 待办事项模块常量，禁止在业务代码中散落魔法值。
 *
 * @author AiWork
 */
public final class TodoConstants {

    /** 事项名称最大字符数。 */
    public static final int TITLE_MAX_LEN = 100;

    /** 事项描述最大字符数。 */
    public static final int DESC_MAX_LEN = 500;

    /** 事项名称列宽上限，与 todo_item.title varchar(200) 对齐。 */
    public static final int TITLE_COLUMN_MAX_LEN = 200;

    /** 事项描述列宽上限，与 todo_item.description varchar(1000) 对齐。 */
    public static final int DESC_COLUMN_MAX_LEN = 1000;

    /** 登录态 Header：用户 ID。 */
    public static final String HEADER_USER_ID = "X-User-Id";

    /** 登录态 Header：用户展示名。 */
    public static final String HEADER_USER_NAME = "X-User-Name";

    /** 登录态 Header：租户 ID。 */
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";

    /** 链路追踪 Header。 */
    public static final String HEADER_TRACE_ID = "X-Trace-Id";

    /** 登录态字段最大长度。 */
    public static final int OPERATOR_FIELD_MAX_LEN = 64;

    /** 单条 INSERT 超时时间（毫秒），与 application.yml 中 default-statement-timeout=3 秒保持一致。 */
    public static final int INSERT_TIMEOUT_MS = 3000;

    /** 展示与存储统一使用 UTC+8。 */
    public static final ZoneId ZONE_UTC8 = ZoneId.of("Asia/Shanghai");

    /** 出参时间格式。 */
    public static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TodoConstants() {
    }
}
