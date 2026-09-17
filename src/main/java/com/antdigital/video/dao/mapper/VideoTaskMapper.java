package com.antdigital.video.dao.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.antdigital.video.model.entity.VideoTaskDO;

/**
 * video_task 表访问。
 */
public interface VideoTaskMapper {

    int insert(VideoTaskDO record);

    VideoTaskDO selectById(@Param("id") Long id);

    VideoTaskDO selectByIdempotencyKey(@Param("tenantId") String tenantId, @Param("idempotencyKey") String idempotencyKey);

    List<VideoTaskDO> selectByTenantAndStatus(@Param("tenantId") String tenantId, @Param("status") String status,
            @Param("offset") int offset, @Param("pageSize") int pageSize);

    long countByTenantAndStatus(@Param("tenantId") String tenantId, @Param("status") String status);

    int updateStatus(@Param("id") Long id, @Param("expectStatus") String expectStatus, @Param("status") String status);

    int updateEngineJobId(@Param("id") Long id, @Param("engineJobId") String engineJobId);

    int updateError(@Param("id") Long id, @Param("status") String status,
            @Param("errorCode") String errorCode, @Param("errorMsg") String errorMsg);

    int cancel(@Param("id") Long id);
}