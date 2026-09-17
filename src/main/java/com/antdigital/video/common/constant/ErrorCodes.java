package com.antdigital.video.common.constant;

/**
 * 错误码常量。格式：来源（A用户/B系统/C第三方）+ 四位数字编号。
 */
public final class ErrorCodes {

    private ErrorCodes() {
    }

    /** 参数非法 */
    public static final String VT_001 = "A0001";
    /** 幂等键冲突 */
    public static final String VT_002 = "A0002";
    /** 创建任务失败 */
    public static final String VT_003 = "B0001";
    /** 任务不存在 */
    public static final String VT_004 = "A0003";
    /** 无权访问该任务（租户越权） */
    public static final String VT_005 = "A0004";
    /** 任务不可取消（已终态） */
    public static final String VT_006 = "B0002";
    /** 引擎提交失败 */
    public static final String VT_007 = "C0001";
    /** 回调验签失败 */
    public static final String VT_008 = "A0005";
    /** 任务非可选状态，回调被忽略 */
    public static final String VT_009 = "B0003";
}