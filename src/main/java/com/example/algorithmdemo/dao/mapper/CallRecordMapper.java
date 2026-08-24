package com.example.algorithmdemo.dao.mapper;

import com.example.algorithmdemo.model.entity.CallRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 调用记录 Mapper
 */
@Mapper
public interface CallRecordMapper {

    /**
     * 插入调用记录
     */
    int insert(CallRecord record);

    /**
     * 按维度统计调用次数
     *
     * @param dimension 统计维度 (user_type/user_level/user_dept_id)
     * @return 统计结果列表，每项包含 label 和 count
     */
    List<Map<String, Object>> countByDimension(@Param("dimension") String dimension);

    /**
     * 按时间范围统计调用次数（折线图用）
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 按天统计的调用次数
     */
    List<Map<String, Object>> countByTimeRange(@Param("startTime") String startTime,
                                                @Param("endTime") String endTime);

    /**
     * 查询调用记录列表
     */
    List<CallRecord> selectList(@Param("apiName") String apiName,
                                @Param("startTime") String startTime,
                                @Param("endTime") String endTime,
                                @Param("offset") int offset,
                                @Param("limit") int limit);

    /**
     * 查询总记录数
     */
    long countTotal(@Param("apiName") String apiName,
                    @Param("startTime") String startTime,
                    @Param("endTime") String endTime);
}