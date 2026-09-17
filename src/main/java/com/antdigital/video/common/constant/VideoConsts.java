package com.antdigital.video.common.constant;

/**
 * 视频生成服务常量。
 */
public final class VideoConsts {

    private VideoConsts() {
    }

    /** 默认视频时长（秒） */
    public static final int DEFAULT_DURATION_SEC = 3;
    /** 最小视频时长（秒） */
    public static final int MIN_DURATION_SEC = 1;
    /** 最大视频时长（秒） */
    public static final int MAX_DURATION_SEC = 30;

    /** 默认分辨率 */
    public static final String DEFAULT_RESOLUTION = "720p";
    /** 默认画面比例 */
    public static final String DEFAULT_ASPECT_RATIO = "16:9";
    /** 默认视频格式 */
    public static final String DEFAULT_FORMAT = "mp4";

    /** 任务编号日期前缀长度：yyyyMMddHHmmss */
    public static final String TASK_NO_PREFIX = "VT";

    /** 分页默认值 */
    public static final int DEFAULT_PAGE_NUM = 1;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    /** 引擎提交最大重试次数 */
    public static final int MAX_ENGINE_SUBMIT_RETRY = 3;
    /** 引擎类型默认值 */
    public static final String DEFAULT_ENGINE_TYPE = "default";
}