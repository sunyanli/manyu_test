package com.example.demo.export.service.impl;

import com.example.demo.common.constant.ErrorCodeEnum;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.export.model.request.ExportRequest;
import com.example.demo.export.service.ExportService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 导出服务实现
 *
 * @author AiWork
 */
@Service
public class ExportServiceImpl implements ExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExportServiceImpl.class);

    private static final Set<String> VALID_EXPORT_TYPES = new HashSet<String>(
            Arrays.asList("helloworld", "hash", "bubble_sort"));

    @Value("${export.enabled:true}")
    private boolean exportEnabled;

    @Value("${export.max-records:10000}")
    private int maxRecords;

    @Override
    public byte[] exportData(ExportRequest request) {
        if (!exportEnabled) {
            throw new BusinessException(ErrorCodeEnum.EXP_003.getCode(), "导出功能维护中");
        }

        if (request == null || !VALID_EXPORT_TYPES.contains(request.getExportType())) {
            throw new BusinessException(ErrorCodeEnum.EXP_001.getCode(), ErrorCodeEnum.EXP_001.getMessage());
        }

        String exportType = request.getExportType();
        logger.info("开始导出数据, type: {}, timeRange: [{}, {}]", exportType,
                request.getStartTime(), request.getEndTime());

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(exportType);
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("序号");
            headerRow.createCell(1).setCellValue("API名称");
            headerRow.createCell(2).setCellValue("调用人");
            headerRow.createCell(3).setCellValue("调用时间");
            headerRow.createCell(4).setCellValue("响应码");

            // 写入示例数据（实际场景应查询数据库）
            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue(1);
            dataRow.createCell(1).setCellValue(exportType);
            dataRow.createCell(2).setCellValue("sample_user");
            dataRow.createCell(3).setCellValue("2026-09-01 12:00:00");
            dataRow.createCell(4).setCellValue("OK");

            workbook.write(bos);
            logger.info("导出完成, type: {}, 记录数: 1", exportType);
            return bos.toByteArray();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("导出失败", e);
            throw new BusinessException(ErrorCodeEnum.EXP_003.getCode(),
                    ErrorCodeEnum.EXP_003.getMessage(), e);
        }
    }
}