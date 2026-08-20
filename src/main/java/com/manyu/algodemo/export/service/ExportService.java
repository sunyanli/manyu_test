package com.manyu.algodemo.export.service;

import com.manyu.algodemo.export.model.dto.ExportRequest;

/**
 * 页面结果导出服务（W04）。
 */
public interface ExportService {

    /**
     * 按页面导出展示结果。
     *
     * @param request 导出请求
     * @return 导出文件
     */
    ExportFile export(ExportRequest request);

    /**
     * 导出文件对象。
     *
     * @param fileName    文件名
     * @param contentType 内容类型
     * @param content     文件字节内容
     */
    record ExportFile(String fileName, String contentType, byte[] content) {
    }
}
