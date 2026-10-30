package com.alipay.xchain.web.demo.service;

import com.alipay.xchain.web.demo.model.ApiCallRecordDO;

/**
 * 接口调用埋点记录服务
 *
 * @author AiWork
 * @since 2026-10-30
 */
public interface ApiCallRecordService {

    /**
     * 异步记录埋点（不阻塞主流程）
     *
     * @param record 埋点记录
     */
    void record(ApiCallRecordDO record);

    /**
     * 按维度聚合统计调用次数
     *
     * @param dimension 统计维度 personType/personLevel/personDept
     * @param startDate 开始日期 yyyy-MM-dd
     * @param endDate   结束日期 yyyy-MM-dd
     * @return 统计结果
     */
    com.alipay.xchain.web.demo.response.StatsResultVO stats(String dimension, String startDate, String endDate);

    /**
     * 按类型导出执行结果数据
     *
     * @param type hello/hash/sort
     * @return Excel 字节流
     */
    byte[] export(String type);
}