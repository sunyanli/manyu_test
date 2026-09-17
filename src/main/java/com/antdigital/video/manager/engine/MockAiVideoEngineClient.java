package com.antdigital.video.manager.engine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 引擎客户端本地 Mock 实现。真实环境替换为外部 HTTP 客户端。
 * 用于联调/演示：提交后立即进入处理中，query 时返回 SUCCEEDED 并给出占位结果。
 */
@Component
public class MockAiVideoEngineClient implements AiVideoEngineClient {

    private static final Logger logger = LoggerFactory.getLogger(MockAiVideoEngineClient.class);

    private final Map<String, EngineJobStatus> jobs = new ConcurrentHashMap<>();
    private long sequence = 0L;

    @Override
    public String submit(EngineGenerateRequest request) {
        String jobId = "mock-job-" + (++sequence);
        EngineJobStatus status = new EngineJobStatus();
        status.setEngineJobId(jobId);
        status.setStatus("PROCESSING");
        jobs.put(jobId, status);
        logger.info("mock 引擎已接收任务, jobId: {}, prompt: {}", jobId, request.getPrompt());
        return jobId;
    }

    @Override
    public EngineJobStatus query(String engineJobId) {
        EngineJobStatus status = jobs.get(engineJobId);
        if (status == null) {
            throw new IllegalArgumentException("未知引擎作业ID: " + engineJobId);
        }
        return status;
    }

    /**
     * 演示：将指定作业标记为成功，并给出结果。
     */
    public void markSuccess(String engineJobId, String resultUrl, String objectKey, Integer durationSec) {
        EngineJobStatus status = jobs.get(engineJobId);
        if (status == null) {
            throw new IllegalArgumentException("未知引擎作业ID: " + engineJobId);
        }
        status.setStatus("SUCCEEDED");
        status.setResultUrl(resultUrl);
        status.setObjectKey(objectKey);
        status.setDurationSec(durationSec);
    }
}