package com.antdigital.todo.common.constant;

/**
 * 待办模块常量定义：长度约束、默认值、编号规则、请求头与限流参数。
 *
 * <p>常量按功能归类，禁止在业务代码中出现魔法值。
 *
 * @author AiWork
 * @date 2026/10/29
 */
public final class TodoConstants {

    /**
     * 事项名称最大长度（按字符计，非字节）
     */
    public static final int MAX_ITEM_NAME_LENGTH = 128;

    /**
     * 事项描述最大长度（按字符计，非字节）
     */
    public static final int MAX_ITEM_DESC_LENGTH = 512;

    /**
     * 幂等键最大长度
     */
    public static final int MAX_REQUEST_ID_LENGTH = 64;

    /**
     * 创建人展示名最大长度（与表字段 creator_name 对齐）
     */
    public static final int MAX_CREATOR_NAME_LENGTH = 64;

    /**
     * 默认租户标识（内部单租户场景）
     */
    public static final String DEFAULT_TENANT_ID = "0";

    /**
     * 默认创建来源：Web 控制台
     */
    public static final String DEFAULT_SOURCE = "web";

    /**
     * 待办编号前缀
     */
    public static final String TODO_NO_PREFIX = "TD";

    /**
     * 待办编号日期部分格式
     */
    public static final String TODO_NO_DATE_PATTERN = "yyyyMMdd";

    /**
     * 待办编号当日序号位数
     */
    public static final int TODO_NO_SEQ_LENGTH = 6;

    /**
     * 待办编号当日序号回绕基数（10 的序号位数次幂），回绕后由唯一索引兜底
     */
    public static final long TODO_NO_SEQ_MODULUS = 1000000L;

    /**
     * 待办编号冲突重试次数（R09：冲突时重试 1 次）
     */
    public static final int TODO_NO_CONFLICT_RETRY_TIMES = 1;

    /**
     * 单用户默认每分钟创建次数上限（R11）
     */
    public static final int DEFAULT_RATE_LIMIT_PER_MINUTE = 30;

    /**
     * 限流滑动窗口长度（毫秒）
     */
    public static final long RATE_LIMIT_WINDOW_MILLIS = 60L * 1000L;

    /**
     * 网关注入：用户 ID 请求头
     */
    public static final String HEADER_USER_ID = "X-User-Id";

    /**
     * 网关注入：用户展示名请求头
     */
    public static final String HEADER_USER_NAME = "X-User-Name";

    /**
     * 网关注入：租户标识请求头
     */
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";

    /**
     * 网关注入：角色列表请求头，多个角色以英文逗号分隔
     */
    public static final String HEADER_USER_ROLES = "X-User-Roles";

    /**
     * 网关注入：登录票据请求头（仅开启二次票据校验时使用）
     */
    public static final String HEADER_AUTH_TICKET = "X-Auth-Ticket";

    /**
     * 角色列表分隔符
     */
    public static final String ROLE_SEPARATOR = ",";

    /**
     * 「待办-创建」权限点标识（R06）
     */
    public static final String PERMISSION_TODO_CREATE = "TODO_CREATE";

    /**
     * 全量权限标识，角色列表含该值视为拥有全部权限点
     */
    public static final String PERMISSION_ALL = "*";

    /**
     * 统一时间格式（前后端交互）
     */
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 成功结果码
     */
    public static final String RESULT_CODE_OK = "OK";

    /**
     * 成功提示信息
     */
    public static final String RESULT_MSG_SUCCESS = "SUCCESS";

    /**
     * 雪花算法起始纪元：2024-01-01 00:00:00 UTC
     */
    public static final long SNOWFLAKE_EPOCH_MILLIS = 1704067200000L;

    /**
     * 雪花算法工作节点 ID 上限（2 的 5 次幂）
     */
    public static final long SNOWFLAKE_WORKER_ID_MAX = 32L;

    /**
     * 雪花算法数据中心 ID 上限（2 的 5 次幂）
     */
    public static final long SNOWFLAKE_DATACENTER_ID_MAX = 32L;

    /**
     * 雪花算法序列号位数
     */
    public static final int SNOWFLAKE_SEQUENCE_BITS = 12;

    /**
     * 雪花算法工作节点 ID 位数
     */
    public static final int SNOWFLAKE_WORKER_ID_BITS = 5;

    /**
     * 雪花算法数据中心 ID 位数
     */
    public static final int SNOWFLAKE_DATACENTER_ID_BITS = 5;

    /**
     * 待办编号降级后缀长度（取雪花 ID 后 6 位，与序号位数对齐）
     */
    public static final int TODO_NO_FALLBACK_SUFFIX_LENGTH = 6;

    private TodoConstants() {
        throw new IllegalStateException("Utility class");
    }
}
