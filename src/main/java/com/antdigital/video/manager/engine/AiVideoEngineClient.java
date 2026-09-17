package com.antdigital.video.manager.engine;

/**
 * 文生视频外部引擎客户端。提交作业 → 查询状态。
 */
public interface AiVideoEngineClient {

    /**
     * 提交文生视频作业，返回外部引擎作业 ID。
     */
    String submit(EngineGenerateRequest request);

    /**
     * 查询引擎作业状态。
     */
    EngineJobStatus query(String engineJobId);
}