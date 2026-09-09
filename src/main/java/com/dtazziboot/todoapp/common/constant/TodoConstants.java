package com.dtazziboot.todoapp.common.constant;

import java.time.ZoneId;

/**
 * 待办事项模块常量
 *
 * @author AiWork
 * @date 2026/09/09
 */
public final class TodoConstants {

    /**
     * 业务默认时区（东八区）
     */
    public static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("Asia/Shanghai");

    /**
     * 模块错误码前缀
     */
    public static final String ERROR_CODE_PREFIX = "TODO_";

    /**
     * 事项名称最大长度
     */
    public static final int TITLE_MAX_LENGTH = 128;

    /**
     * 事项描述最大长度
     */
    public static final int DESCRIPTION_MAX_LENGTH = 1024;

    /**
     * 成功结果码
     */
    public static final String RESULT_OK = "OK";

    /**
     * 失败结果码
     */
    public static final String RESULT_FAIL = "FAIL";

    /**
     * 成功提示信息
     */
    public static final String MSG_SUCCESS = "SUCCESS";

    /**
     * 登录用户 ID 请求头
     */
    public static final String HEADER_USER_ID = "X-User-Id";

    /**
     * 租户 ID 请求头
     */
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";

    /**
     * 私有构造，禁止实例化
     */
    private TodoConstants() {
    }
}
