package com.antdigital.video.dao.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.antdigital.video.model.entity.VideoResultDO;

/**
 * video_result 表访问。
 */
public interface VideoResultMapper {

    int insert(VideoResultDO record);

    List<VideoResultDO> selectByTaskId(@Param("taskId") Long taskId);
}