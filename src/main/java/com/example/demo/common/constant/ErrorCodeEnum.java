package com.example.demo.common.constant;

/**
 * 错误码枚举
 *
 * @author AiWork
 */
public enum ErrorCodeEnum {

    /** 算法模块 - 系统内部错误 */
    ALG_001("ALG_001", "系统内部错误"),
    /** 算法模块 - 输入参数为空 */
    ALG_002("ALG_002", "输入不能为空"),
    /** 算法模块 - 输入数组为空或非法 */
    ALG_003("ALG_003", "数组不能为空"),
    /** 算法模块 - 排序方向参数非法 */
    ALG_004("ALG_004", "排序方向非法，仅支持 asc/desc"),

    /** 导出模块 - 导出类型非法 */
    EXP_001("EXP_001", "导出类型非法"),
    /** 导出模块 - 时间范围参数非法 */
    EXP_002("EXP_002", "时间范围参数非法"),
    /** 导出模块 - 系统内部错误 */
    EXP_003("EXP_003", "导出系统内部错误"),

    /** 埋点模块 - 时间范围参数非法 */
    TRK_001("TRK_001", "时间范围参数非法，时间范围不能超过90天"),
    /** 埋点模块 - 维度参数非法 */
    TRK_002("TRK_002", "维度参数非法");

    /** 错误码 */
    private final String code;

    /** 错误描述 */
    private final String message;

    ErrorCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}