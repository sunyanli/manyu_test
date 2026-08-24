package com.example.algorithmdemo.service;

import com.example.algorithmdemo.model.vo.BubbleSortVO;
import com.example.algorithmdemo.model.vo.HashVO;
import com.example.algorithmdemo.model.vo.HelloWorldVO;

/**
 * 导出服务接口
 */
public interface ExportService {

    /**
     * 导出数据为字节数组
     *
     * @param apiName 接口名称 (hello/hash/bubble-sort/all)
     * @param format  导出格式 (xlsx/csv)
     * @return Excel/CSV 文件字节数组
     */
    byte[] exportData(String apiName, String format);
}