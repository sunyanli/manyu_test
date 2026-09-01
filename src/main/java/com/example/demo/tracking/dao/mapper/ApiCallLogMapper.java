package com.example.demo.tracking.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.tracking.model.entity.ApiCallLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 接口调用日志 Mapper
 *
 * @author AiWork
 */
@Mapper
public interface ApiCallLogMapper extends BaseMapper<ApiCallLog> {

    /**
     * 按天聚合调用统计（时序数据）
     *
     * @param apiName   接口名称（可为 null 表示全部）
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 时间序列统计结果
     */
    @Select("<script>"
            + "SELECT DATE_FORMAT(gmt_create, '%Y-%m-%d') AS time, COUNT(*) AS count "
            + "FROM api_call_log "
            + "WHERE gmt_create BETWEEN #{startTime} AND #{endTime} "
            + "<if test='apiName != null'>AND api_name = #{apiName}</if> "
            + "GROUP BY DATE_FORMAT(gmt_create, '%Y-%m-%d') "
            + "ORDER BY time ASC"
            + "</script>")
    List<Map<String, Object>> callStatsByDay(@Param("apiName") String apiName,
                                             @Param("startTime") String startTime,
                                             @Param("endTime") String endTime);

    /**
     * 按维度聚合统计
     *
     * @param dimension 维度字段名（user_type/user_level/user_department）
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 维度统计结果
     */
    @Select("SELECT ${dimension} AS label, COUNT(*) AS count "
            + "FROM api_call_log "
            + "WHERE gmt_create BETWEEN #{startTime} AND #{endTime} "
            + "GROUP BY ${dimension} "
            + "ORDER BY count DESC")
    List<Map<String, Object>> dimensionStats(@Param("dimension") String dimension,
                                             @Param("startTime") String startTime,
                                             @Param("endTime") String endTime);

    /**
     * 按接口名称和时间范围查询调用日志（用于导出）
     *
     * @param apiName   接口名称
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param limit     最大返回条数
     * @return 日志列表
     */
    @Select("<script>"
            + "SELECT id, api_name, user_id, user_name, gmt_create, response_code "
            + "FROM api_call_log "
            + "WHERE api_name = #{apiName} "
            + "<if test='startTime != null and endTime != null'>"
            + "AND gmt_create BETWEEN #{startTime} AND #{endTime} "
            + "</if>"
            + "ORDER BY gmt_create DESC "
            + "LIMIT #{limit}"
            + "</script>")
    List<ApiCallLog> selectForExport(@Param("apiName") String apiName,
                                     @Param("startTime") String startTime,
                                     @Param("endTime") String endTime,
                                     @Param("limit") int limit);
}