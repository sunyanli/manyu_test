package com.alipay.xchain.web.demo.dao.mapper;

import com.alipay.xchain.web.demo.model.ApiCallRecordDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 接口调用埋点记录 Mapper
 *
 * @author AiWork
 * @since 2026-10-30
 */
@Mapper
public interface ApiCallRecordMapper {

    /**
     * 插入埋点记录
     */
    int insert(ApiCallRecordDO record);

    /**
     * 按维度聚合统计
     *
     * @param column    数据库列名
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 分桶统计结果
     */
    List<Map<String, Object>> statsByDimension(@Param("column") String column,
                                                @Param("startTime") String startTime,
                                                @Param("endTime") String endTime);

    /**
     * 按日期聚合日趋势
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 日趋势
     */
    List<Map<String, Object>> dailyTrend(@Param("startTime") String startTime,
                                          @Param("endTime") String endTime);

    /**
     * 按类型查询最近记录（用于导出）
     *
     * @param apiName 接口名
     * @param limit   限制条数
     * @return 埋点记录列表
     */
    List<ApiCallRecordDO> listByApiName(@Param("apiName") String apiName,
                                         @Param("limit") int limit);
}