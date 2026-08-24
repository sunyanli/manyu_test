package com.example.demo.service;

import com.example.demo.model.BubbleResult;
import com.example.demo.model.HashResult;
import com.example.demo.model.HelloResult;
import com.opencsv.CSVWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExportService {

    @Autowired
    private HelloService helloService;

    @Autowired
    private HashService hashService;

    @Autowired
    private BubbleService bubbleService;

    public byte[] exportHello(String name, String format) {
        HelloResult result = helloService.greet(name != null ? name : "World");
        if ("xlsx".equalsIgnoreCase(format)) {
            return exportHelloExcel(result);
        }
        return exportHelloCsv(result);
    }

    public byte[] exportHash(String input, String algorithm, String format) {
        HashResult result = hashService.computeHash(
                input != null ? input : "sample-data",
                algorithm != null ? algorithm : "SHA-256");
        if ("xlsx".equalsIgnoreCase(format)) {
            return exportHashExcel(result);
        }
        return exportHashCsv(result);
    }

    public byte[] exportBubble(String arrayStr, String format) {
        List<Integer> array = parseArray(arrayStr);
        BubbleResult result = bubbleService.sort(array);
        if ("xlsx".equalsIgnoreCase(format)) {
            return exportBubbleExcel(result);
        }
        return exportBubbleCsv(result);
    }

    private List<Integer> parseArray(String arrayStr) {
        if (arrayStr == null || arrayStr.isBlank()) {
            return Arrays.asList(5, 3, 8, 1, 2);
        }
        try {
            return Arrays.stream(arrayStr.split(","))
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("数组格式无效，请使用逗号分隔的数字，例如: 5,3,8,1,2", e);
        }
    }

    private byte[] exportHelloCsv(HelloResult result) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             OutputStreamWriter osw = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
             CSVWriter writer = new CSVWriter(osw)) {
            writer.writeNext(new String[]{"greeting"});
            writer.writeNext(new String[]{result.getGreeting()});
            writer.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("CSV 导出失败", e);
        }
    }

    private byte[] exportHashCsv(HashResult result) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             OutputStreamWriter osw = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
             CSVWriter writer = new CSVWriter(osw)) {
            writer.writeNext(new String[]{"input", "algorithm", "hash"});
            writer.writeNext(new String[]{result.getInput(), result.getAlgorithm(), result.getHash()});
            writer.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("CSV 导出失败", e);
        }
    }

    private byte[] exportBubbleCsv(BubbleResult result) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             OutputStreamWriter osw = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
             CSVWriter writer = new CSVWriter(osw)) {
            writer.writeNext(new String[]{"originalArray", "sortedArray", "swapCount", "comparisonCount"});
            writer.writeNext(new String[]{
                    result.getOriginalArray().stream().map(String::valueOf).collect(Collectors.joining(",")),
                    result.getSortedArray().stream().map(String::valueOf).collect(Collectors.joining(",")),
                    String.valueOf(result.getSwapCount()),
                    String.valueOf(result.getComparisonCount())
            });
            writer.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("CSV 导出失败", e);
        }
    }

    private byte[] exportHelloExcel(HelloResult result) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("HelloWorld");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("greeting");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(result.getGreeting());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Excel 导出失败", e);
        }
    }

    private byte[] exportHashExcel(HashResult result) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Hash");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("input");
            header.createCell(1).setCellValue("algorithm");
            header.createCell(2).setCellValue("hash");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(result.getInput());
            row.createCell(1).setCellValue(result.getAlgorithm());
            row.createCell(2).setCellValue(result.getHash());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Excel 导出失败", e);
        }
    }

    private byte[] exportBubbleExcel(BubbleResult result) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("BubbleSort");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("originalArray");
            header.createCell(1).setCellValue("sortedArray");
            header.createCell(2).setCellValue("swapCount");
            header.createCell(3).setCellValue("comparisonCount");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(result.getOriginalArray().stream().map(String::valueOf).collect(Collectors.joining(",")));
            row.createCell(1).setCellValue(result.getSortedArray().stream().map(String::valueOf).collect(Collectors.joining(",")));
            row.createCell(2).setCellValue(result.getSwapCount());
            row.createCell(3).setCellValue(result.getComparisonCount());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Excel 导出失败", e);
        }
    }
}