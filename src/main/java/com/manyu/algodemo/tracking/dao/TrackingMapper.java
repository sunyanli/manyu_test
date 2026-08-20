package com.manyu.algodemo.tracking.dao;

import com.manyu.algodemo.tracking.model.entity.CallRecordDO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 埋点记录数据访问：批量写入 + 多维度聚合 + 最近记录查询（参数化 SQL）。
 */
public interface TrackingMapper {

    /**
     * 批量插入调用记录。
     *
     * @param records 记录列表
     * @return 影响行数
     */
    int batchInsert(@Param("records") List<CallRecordDO> records);

    /**
     * 查询概况聚合（总数/人数/成功数/平均耗时）。
     *
     * @param start 起始时间
     * @param end   截止时间
     * @return 聚合结果 Map（totalCalls/totalCallers/successCount/avgCostTimeMs）
     */
    Map<String, Object> selectOverview(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 查询调用最多的人。
     *
     * @param start 起始时间
     * @param end   截止时间
     * @return 调用量与姓名（name/calls）
     */
    Map<String, Object> selectTopCaller(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 按维度聚合（饼图/柱状图数据源）。
     *
     * @param dimColumn 维度列（service 白名单映射后传入，禁止直接拼用户输入）
     * @param start     起始时间
     * @param end       截止时间
     * @param groupBy   分组列（维度列，部门维度为 caller_dept_code）
     * @return 聚合结果（name/value）
     */
    List<Map<String, Object>> selectStatsByDimension(
            @Param("dimColumn") String dimColumn,
            @Param("groupBy") String groupBy,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * 按时间粒度聚合（折线图数据源）。
     *
     * @param timeExpr 时间格式化表达式（service 白名单映射）
     * @param start    起始时间
     * @param end      截止时间
     * @return 聚合结果（time/calls/successCount）
     */
    List<Map<String, Object>> selectTrend(
            @Param("timeExpr") String timeExpr,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * 查询某业务类型最近记录（页面导出数据源）。
     *
     * @param bizType 业务类型
     * @param start   起始时间
     * @param end     截止时间
     * @param limit   条数上限
     * @return 记录列表
     */
    List<CallRecordDO> selectRecentRecords(
            @Param("bizType") String bizType,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("limit") int limit);
}
