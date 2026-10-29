package com.antdigital.todo.common.constant;

import java.time.ZoneId;

/**
 * 待办模块常量定义：长度约束、编号规则、请求头、权限点与限流参数。
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
     * 触发空闲用户条目回收的用户数阈值（低于该值时不做全量扫描，避免每次请求都遍历）
     */
    public static final int RATE_LIMIT_IDLE_SCAN_THRESHOLD = 1024;

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
     * 网关注入：权限点列表请求头（网关从统一权限平台解析后签名透传），多个值以英文逗号分隔
     */
    public static final String HEADER_USER_PERMISSIONS = "X-User-Permissions";

    /**
     * 网关注入：请求签名请求头（HMAC-SHA256 十六进制小写）
     */
    public static final String HEADER_GATEWAY_SIGN = "X-Gateway-Sign";

    /**
     * 网关注入：签名时间戳请求头（毫秒）
     */
    public static final String HEADER_GATEWAY_TIMESTAMP = "X-Gateway-Timestamp";

    /**
     * 权限点列表分隔符
     */
    public static final String PERMISSION_SEPARATOR = ",";

    /**
     * 灰度租户尾号分隔符
     */
    public static final String GRAY_TAIL_SEPARATOR = ",";

    /**
     * 「待办-创建」权限点标识（R06）
     */
    public static final String PERMISSION_TODO_CREATE = "TODO_CREATE";

    /**
     * 全量权限标识，权限点列表含该值视为拥有全部权限点
     */
    public static final String PERMISSION_ALL = "*";

    /**
     * 统一时间格式（前后端交互）
     */
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 业务时区标识：与数据库连接串 serverTimezone 保持一致，避免依赖 JVM 默认时区
     */
    public static final String BIZ_ZONE_ID = "Asia/Shanghai";

    /**
     * 业务时区
     */
    public static final ZoneId BIZ_ZONE = ZoneId.of(BIZ_ZONE_ID);

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
     * 雪花算法数据中心 ID 位数
     */
    public static final int SNOWFLAKE_DATACENTER_ID_BITS = 5;

    /**
     * 雪花算法允许等待的时钟回拨上限（毫秒），超出即快速失败，避免长时间自旋
     */
    public static final long SNOWFLAKE_MAX_BACKWARD_WAIT_MILLIS = 5000L;

    /**
     * 雪花算法等待时的单次休眠上限（毫秒），替代纯自旋忙等
     */
    public static final long SNOWFLAKE_MAX_WAIT_SLEEP_MILLIS = 1L;

    private TodoConstants() {
        throw new IllegalStateException("Utility class");
    }
}
