package com.example.demo.tracking.service;

/**
 * 埋点服务接口
 *
 * @author AiWork
 */
public interface TrackingService {

    /**
     * 记录接口调用日志
     *
     * @param apiName 接口名称
     * @param userId  调用人 ID
     */
    void recordCall(String apiName, String userId);
}