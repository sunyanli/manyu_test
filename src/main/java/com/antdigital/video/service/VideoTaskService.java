package com.antdigital.video.service;

import com.antdigital.video.common.model.PageResult;
import com.antdigital.video.model.dto.CreateVideoTaskCommand;
import com.antdigital.video.model.dto.EngineCallbackPayload;
import com.antdigital.video.model.vo.VideoTaskVO;

/**
 * 视频任务服务接口。
 */
public interface VideoTaskService {

    /**
     * 创建文生视频任务，返回任务主键。
     */
    Long createVideoTask(String tenantId, CreateVideoTaskCommand command);

    /**
     * 按任务 ID 查询（租户隔离）。
     */
    VideoTaskVO getByTaskId(String tenantId, Long taskId);

    /**
     * 分页查询任务列表（租户隔离）。
     */
    PageResult<VideoTaskVO> list(String tenantId, String status, int pageNum, int pageSize);

    /**
     * 取消任务。
     */
    void cancel(String tenantId, Long taskId);

    /**
     * 处理引擎回调。
     */
    void handleEngineCallback(EngineCallbackPayload payload);
}