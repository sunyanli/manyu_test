package com.example.algorithmdemo.common.constant;

/**
 * 错误码常量
 */
public final class ErrorCodeConstant {

    private ErrorCodeConstant() {}

    /** 成功 */
    public static final String SUCCESS = "SUCCESS";

    /** 系统内部错误 */
    public static final String SYS_001 = "SYS_001";

    /** 参数校验失败 */
    public static final String PARAM_001 = "PARAM_001";

    /** 不支持的哈希算法 */
    public static final String ALGO_001 = "ALGO_001";

    /** 导出数据为空 */
    public static final String EXPORT_001 = "EXPORT_001";

    /** 不支持的导出格式 */
    public static final String EXPORT_002 = "EXPORT_002";

    /** 不支持的统计维度 */
    public static final String TRACK_001 = "TRACK_001";
}