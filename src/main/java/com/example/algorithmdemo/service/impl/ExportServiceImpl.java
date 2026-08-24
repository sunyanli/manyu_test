package com.example.algorithmdemo.service.impl;

import com.example.algorithmdemo.common.exception.BusinessException;
import com.example.algorithmdemo.model.vo.BubbleSortVO;
import com.example.algorithmdemo.model.vo.HashVO;
import com.example.algorithmdemo.model.vo.HelloWorldVO;
import com.example.algorithmdemo.service.AlgorithmService;
import com.example.algorithmdemo.service.ExportService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * 导出服务实现
 */
@Service
public class ExportServiceImpl implements ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportServiceImpl.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final AlgorithmService algorithmService;

    public ExportServiceImpl(AlgorithmService algorithmService) {
        this.algorithmService = algorithmService;
    }

    @Override
    public byte[] exportData(String apiName, String format) {
        if (format == null || format.isBlank()) {
            format = "xlsx";
        }

        if (!"xlsx".equalsIgnoreCase(format) && !"csv".equalsIgnoreCase(format)) {
            throw BusinessException.unsupportedExportFormat();
        }

        if ("xlsx".equalsIgnoreCase(format)) {
            return exportToExcel(apiName);
        } else {
            return exportToCsv(apiName);
        }
    }

    private byte[] exportToExcel(String apiName) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("导出数据");

            // 创建表头
            String[] headers = {"接口名称", "输入参数", "执行结果", "导出时间"};
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            String exportTime = LocalDateTime.now().format(FORMATTER);

            if ("hello".equalsIgnoreCase(apiName) || "all".equalsIgnoreCase(apiName)) {
                HelloWorldVO hello = algorithmService.helloWorld("World");
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue("helloworld");
                row.createCell(1).setCellValue("name=World");
                row.createCell(2).setCellValue(hello.getMessage());
                row.createCell(3).setCellValue(exportTime);
            }

            if ("hash".equalsIgnoreCase(apiName) || "all".equalsIgnoreCase(apiName)) {
                HashVO hash = algorithmService.hash("Hello World", "SHA-256");
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue("hash");
                row.createCell(1).setCellValue("input=Hello World, algorithm=SHA-256");
                row.createCell(2).setCellValue(hash.getHashValue());
                row.createCell(3).setCellValue(exportTime);
            }

            if ("bubble-sort".equalsIgnoreCase(apiName) || "all".equalsIgnoreCase(apiName)) {
                int[] testArray = {5, 3, 8, 4, 2};
                BubbleSortVO sort = algorithmService.bubbleSort(testArray, "asc");
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue("bubble-sort");
                row.createCell(1).setCellValue("array=" + Arrays.toString(testArray));
                row.createCell(2).setCellValue("sorted=" + Arrays.toString(sort.getSortedArray()));
                row.createCell(3).setCellValue(exportTime);
            }

            if (rowNum == 1) {
                throw BusinessException.exportDataEmpty();
            }

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("导出Excel失败", e);
            throw new RuntimeException("导出Excel失败", e);
        }
    }

    private byte[] exportToCsv(String apiName) {
        StringBuilder sb = new StringBuilder();
        sb.append("接口名称,输入参数,执行结果,导出时间\n");

        String exportTime = LocalDateTime.now().format(FORMATTER);

        if ("hello".equalsIgnoreCase(apiName) || "all".equalsIgnoreCase(apiName)) {
            HelloWorldVO hello = algorithmService.helloWorld("World");
            sb.append("helloworld, name=World, ").append(hello.getMessage()).append(", ").append(exportTime).append("\n");
        }

        if ("hash".equalsIgnoreCase(apiName) || "all".equalsIgnoreCase(apiName)) {
            HashVO hash = algorithmService.hash("Hello World", "SHA-256");
            sb.append("hash, input=Hello World, ").append(hash.getHashValue()).append(", ").append(exportTime).append("\n");
        }

        if ("bubble-sort".equalsIgnoreCase(apiName) || "all".equalsIgnoreCase(apiName)) {
            int[] testArray = {5, 3, 8, 4, 2};
            BubbleSortVO sort = algorithmService.bubbleSort(testArray, "asc");
            sb.append("bubble-sort, array=").append(Arrays.toString(testArray))
                    .append(", sorted=").append(Arrays.toString(sort.getSortedArray()))
                    .append(", ").append(exportTime).append("\n");
        }

        if (sb.length() == 0) {
            throw BusinessException.exportDataEmpty();
        }

        return sb.toString().getBytes();
    }
}