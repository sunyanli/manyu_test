package com.example.demo.tracking.service;

import com.example.demo.tracking.model.request.CallStatsRequest;
import com.example.demo.tracking.model.request.DimensionStatsRequest;
import com.example.demo.tracking.model.vo.CallStatsVO;
import com.example.demo.tracking.model.vo.DimensionStatsVO;

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

    /**
     * 查询调用时序统计
     *
     * @param request 统计请求
     * @return 时序统计结果
     */
    CallStatsVO queryCallStats(CallStatsRequest request);

    /**
     * 查询维度统计
     *
     * @param request 维度统计请求
     * @return 维度统计结果
     */
    DimensionStatsVO queryDimensionStats(DimensionStatsRequest request);
}