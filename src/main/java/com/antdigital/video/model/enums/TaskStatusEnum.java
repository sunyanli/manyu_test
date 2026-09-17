package com.antdigital.video.model.enums;

/**
 * 视频生成任务状态。合法流转：
 * <pre>
 * CREATED -&gt; SUBMITTED -&gt; PROCESSING -&gt; SUCCEEDED
 *                      \-> FAILED
 * CREATED/SUBMITTED/PROCESSING -&gt; CANCELLED
 * </pre>
 */
public enum TaskStatusEnum {

    CREATED,
    SUBMITTED,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    CANCELLED;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED || this == CANCELLED;
    }

    public boolean isCancellable() {
        return this == CREATED || this == SUBMITTED || this == PROCESSING;
    }
}