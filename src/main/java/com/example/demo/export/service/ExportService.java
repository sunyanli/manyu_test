package com.example.demo.export.service;

import com.example.demo.export.model.request.ExportRequest;

/**
 * 导出服务接口
 *
 * @author AiWork
 */
public interface ExportService {

    /**
     * 导出数据为 Excel 文件
     *
     * @param request 导出请求
     * @return Excel 文件字节数组
     */
    byte[] exportData(ExportRequest request);
}