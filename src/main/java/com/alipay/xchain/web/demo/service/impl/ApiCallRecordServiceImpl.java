package com.alipay.xchain.web.demo.service.impl;

import com.alipay.xchain.web.demo.common.DemoException;
import com.alipay.xchain.web.demo.dao.mapper.ApiCallRecordMapper;
import com.alipay.xchain.web.demo.model.ApiCallRecordDO;
import com.alipay.xchain.web.demo.response.StatsResultVO;
import com.alipay.xchain.web.demo.service.ApiCallRecordService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 接口调用埋点记录服务实现
 *
 * @author AiWork
 * @since 2026-10-30
 */
@Service
public class ApiCallRecordServiceImpl implements ApiCallRecordService {

    private static final Logger logger = LoggerFactory.getLogger(ApiCallRecordServiceImpl.class);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 维度列名映射 */
    private static final Map<String, String> DIMENSION_COLUMN_MAP = new HashMap<>();

    static {
        DIMENSION_COLUMN_MAP.put("personType", "person_type");
        DIMENSION_COLUMN_MAP.put("personLevel", "person_level");
        DIMENSION_COLUMN_MAP.put("personDept", "person_dept");
    }

    @Autowired
    private ApiCallRecordMapper apiCallRecordMapper;

    @Override
    @Async
    public void record(ApiCallRecordDO record) {
        try {
            apiCallRecordMapper.insert(record);
        } catch (Exception e) {
            logger.error("埋点写入数据库失败, apiName: {}, callerId: {}", record.getApiName(), record.getCallerId(), e);
        }
    }

    @Override
    public StatsResultVO stats(String dimension, String startDate, String endDate) {
        // 校验维度
        if (!DIMENSION_COLUMN_MAP.containsKey(dimension)) {
            throw new DemoException("DEMO_040", "统计维度非法: " + dimension + "，可选 personType/personLevel/personDept");
        }

        String column = DIMENSION_COLUMN_MAP.get(dimension);

        // 默认时间窗：近 7 天
        if (startDate == null || startDate.isEmpty()) {
            startDate = LocalDate.now().minusDays(7).format(DATE_FMT);
        }
        if (endDate == null || endDate.isEmpty()) {
            endDate = LocalDate.now().format(DATE_FMT);
        }

        String startTime = startDate + " 00:00:00";
        String endTime = endDate + " 23:59:59";

        // 按维度聚合
        List<Map<String, Object>> dimensionRows = apiCallRecordMapper.statsByDimension(column, startTime, endTime);
        List<StatsResultVO.StatsItem> items = new ArrayList<>();
        for (Map<String, Object> row : dimensionRows) {
            String name = String.valueOf(row.get("name"));
            Object callCountObj = row.get("callCount");
            Integer callCount = 0;
            if (callCountObj instanceof Number) {
                callCount = ((Number) callCountObj).intValue();
            }
            // 空值归入"未知"
            if (name == null || name.trim().isEmpty() || "null".equals(name)) {
                name = "未知";
            }
            items.add(new StatsResultVO.StatsItem(name, callCount));
        }

        // 日趋势
        List<Map<String, Object>> trendRows = apiCallRecordMapper.dailyTrend(startTime, endTime);
        List<StatsResultVO.TrendItem> trend = new ArrayList<>();
        for (Map<String, Object> row : trendRows) {
            String date = String.valueOf(row.get("date"));
            Object callCountObj = row.get("callCount");
            Integer callCount = 0;
            if (callCountObj instanceof Number) {
                callCount = ((Number) callCountObj).intValue();
            }
            trend.add(new StatsResultVO.TrendItem(date, callCount));
        }

        StatsResultVO result = new StatsResultVO();
        result.setDimension(dimension);
        result.setItems(items);
        result.setTrend(trend);

        if (logger.isInfoEnabled()) {
            logger.info("统计查询完成, dimension: {}, items: {}, trend: {}", dimension, items.size(), trend.size());
        }
        return result;
    }

    @Override
    public byte[] export(String type) {
        // 校验 type
        if (!"hello".equals(type) && !"hash".equals(type) && !"sort".equals(type)) {
            throw new DemoException("DEMO_030", "导出类型非法: " + type + "，可选 hello/hash/sort");
        }

        List<ApiCallRecordDO> records = apiCallRecordMapper.listByApiName(type, 1000);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("导出数据");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            // 表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {"序号", "接口名", "调用人工号", "调用人姓名", "人员类型", "人员层级", "人员部门", "调用时间", "调用结果"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 数据行
            int rowIdx = 1;
            for (ApiCallRecordDO record : records) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(rowIdx - 1);
                row.createCell(1).setCellValue(record.getApiName() != null ? record.getApiName() : "");
                row.createCell(2).setCellValue(record.getCallerId() != null ? record.getCallerId() : "");
                row.createCell(3).setCellValue(record.getCallerName() != null ? record.getCallerName() : "");
                row.createCell(4).setCellValue(record.getPersonType() != null ? record.getPersonType() : "");
                row.createCell(5).setCellValue(record.getPersonLevel() != null ? record.getPersonLevel() : "");
                row.createCell(6).setCellValue(record.getPersonDept() != null ? record.getPersonDept() : "");
                row.createCell(7).setCellValue(record.getCallTime() != null ? record.getCallTime().format(DATETIME_FMT) : "");
                row.createCell(8).setCellValue(record.getResultStatus() != null ? record.getResultStatus() : "");

                for (int i = 0; i < headers.length; i++) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            logger.error("导出Excel失败, type: {}", type, e);
            throw new DemoException("DEMO_031", "导出失败: " + e.getMessage());
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}