package com.manyu.algodemo.tracking.service;

import com.manyu.algodemo.tracking.model.dto.OverviewVO;
import com.manyu.algodemo.tracking.model.dto.StatsVO;
import com.manyu.algodemo.tracking.model.dto.TrendVO;
import com.manyu.algodemo.tracking.model.entity.CallRecordDO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 埋点统计服务：概况 / 维度统计 / 时间趋势 / 页面导出数据源。
 */
public interface TrackingService {

    /**
     * 调用概况查询（W05）。
     *
     * @param start 起始时间
     * @param end   截止时间
     * @return 概况视图对象
     */
    OverviewVO overview(LocalDateTime start, LocalDateTime end);

    /**
     * 维度统计查询（W06）。
     *
     * @param dimension 维度（CALLER_TYPE/CALLER_LEVEL/CALLER_DEPT/BIZ_TYPE）
     * @param start     起始时间
     * @param end       截止时间
     * @return 维度统计视图对象
     */
    StatsVO stats(String dimension, LocalDateTime start, LocalDateTime end);

    /**
     * 时间趋势查询（W07）。
     *
     * @param granularity 粒度（HOUR/DAY/MONTH）
     * @param start       起始时间
     * @param end         截止时间
     * @return 趋势视图对象
     */
    TrendVO trend(String granularity, LocalDateTime start, LocalDateTime end);

    /**
     * 时间趋势查询（W07），支持可选维度细分。
     *
     * @param granularity 粒度（HOUR/DAY/MONTH）
     * @param dimension   可选维度细分（如 CALLER_TYPE=EMPLOYEE），为空表示不细分
     * @param start       起始时间
     * @param end         截止时间
     * @return 趋势视图对象
     */
    TrendVO trend(String granularity, String dimension, LocalDateTime start, LocalDateTime end);

    /**
     * 查询某业务类型最近记录（页面导出数据源）。
     *
     * @param bizType 业务类型
     * @param start   起始时间
     * @param end     截止时间
     * @param limit   条数上限
     * @return 记录列表
     */
    List<CallRecordDO> pageRecords(String bizType, LocalDateTime start, LocalDateTime end, int limit);
}
