package com.example.algorithm.mapper;

import com.example.algorithm.model.entity.AlgorithmExportTask;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Mapper for algorithm_export_task table.
 */
@Mapper
public interface AlgorithmExportTaskMapper {

    @Insert("INSERT INTO algorithm_export_task "
            + "(task_id, algorithm_type, params, status, operator_id) "
            + "VALUES (#{taskId}, #{algorithmType}, #{params}, #{status}, #{operatorId})")
    int insert(AlgorithmExportTask task);

    @Select("SELECT * FROM algorithm_export_task WHERE task_id = #{taskId}")
    AlgorithmExportTask selectByTaskId(String taskId);

    @Update("UPDATE algorithm_export_task SET status = #{status}, file_url = #{fileUrl}, "
            + "error_msg = #{errorMsg}, gmt_modified = NOW() WHERE task_id = #{taskId}")
    int updateStatus(AlgorithmExportTask task);
}
