package com.example.algorithm.mapper;

import com.example.algorithm.model.entity.AlgorithmCallRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * Mapper for algorithm_call_record table.
 */
@Mapper
public interface AlgorithmCallRecordMapper {

    @Insert("INSERT INTO algorithm_call_record "
            + "(request_id, api_path, algorithm_type, caller_id, caller_name, "
            + "caller_type, caller_level, caller_dept, request_params, response_result, cost_time_ms) "
            + "VALUES (#{requestId}, #{apiPath}, #{algorithmType}, #{callerId}, #{callerName}, "
            + "#{callerType}, #{callerLevel}, #{callerDept}, #{requestParams}, #{responseResult}, #{costTimeMs})")
    int insert(AlgorithmCallRecord record);

    @Select("SELECT "
            + "  ${dimension} AS dimension_key, COUNT(*) AS count "
            + "FROM algorithm_call_record "
            + "WHERE gmt_create >= #{startTime} AND gmt_create <= #{endTime} "
            + "GROUP BY ${dimension}")
    List<Map<String, Object>> selectStatisticsByDimension(
            @Param("dimension") String dimension,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime);

    @Select("SELECT "
            + "  DATE_FORMAT(gmt_create, #{format}) AS time_label, COUNT(*) AS count "
            + "FROM algorithm_call_record "
            + "WHERE gmt_create >= #{startTime} AND gmt_create <= #{endTime} "
            + "  AND (#{algorithmType} IS NULL OR algorithm_type = #{algorithmType}) "
            + "GROUP BY DATE_FORMAT(gmt_create, #{format}) "
            + "ORDER BY time_label")
    List<Map<String, Object>> selectTrend(
            @Param("format") String format,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime,
            @Param("algorithmType") String algorithmType);

    @Select("SELECT COUNT(*) FROM algorithm_call_record")
    long countAll();
}
