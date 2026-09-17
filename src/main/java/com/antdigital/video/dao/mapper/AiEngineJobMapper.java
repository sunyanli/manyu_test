package com.antdigital.video.dao.mapper;

import org.apache.ibatis.annotations.Param;

import com.antdigital.video.model.entity.AiEngineJobDO;

/**
 * ai_engine_job 表访问。
 */
public interface AiEngineJobMapper {

    int insert(AiEngineJobDO record);

    AiEngineJobDO selectByEngineJobId(@Param("engineJobId") String engineJobId);

    int updateCallbackStatus(@Param("id") Long id, @Param("callbackStatus") String callbackStatus,
            @Param("rawPayload") String rawPayload);
}