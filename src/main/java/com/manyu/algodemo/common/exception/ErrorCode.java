package com.manyu.algodemo.common.exception;

/**
 * 统一错误码枚举。
 *
 * <p>格式：{MODULE}_{SEQ}，模块码：COMMON / DEMO / EXPORT / TRACKING。</p>
 */
public enum ErrorCode {

    /** 成功。 */
    OK("OK", "SUCCESS"),
    /** 未登录。 */
    COMMON_401("COMMON_401", "未登录或登录态失效"),
    /** 系统异常。 */
    COMMON_500("COMMON_500", "系统繁忙，请稍后重试"),
    /** demo：参数非法。 */
    DEMO_001("DEMO_001", "参数非法"),
    /** demo：不支持的哈希算法。 */
    DEMO_002("DEMO_002", "不支持的哈希算法"),
    /** demo：排序数组超上限。 */
    DEMO_003("DEMO_003", "排序数组数量超上限"),
    /** export：不支持的导出目标/格式。 */
    EXPORT_001("EXPORT_001", "不支持的导出目标或格式"),
    /** export：导出数据为空。 */
    EXPORT_002("EXPORT_002", "导出数据为空"),
    /** tracking：时间范围非法。 */
    TRACKING_001("TRACKING_001", "时间范围非法"),
    /** tracking：不支持的统计维度。 */
    TRACKING_002("TRACKING_002", "不支持的统计维度"),
    /** tracking：时间范围跨度过大（>90天）。 */
    TRACKING_003("TRACKING_003", "时间范围跨度过大（最大90天）");

    /** 错误码。 */
    private final String code;
    /** 默认提示信息。 */
    private final String defaultMsg;

    ErrorCode(String code, String defaultMsg) {
        this.code = code;
        this.defaultMsg = defaultMsg;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMsg() {
        return defaultMsg;
    }
}
