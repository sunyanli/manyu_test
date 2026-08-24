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

@Service
public class ExportService {

    @Autowired
    private HelloService helloService;

    @Autowired
    private HashService hashService;

    @Autowired
    private BubbleService bubbleService;

    public byte[] exportHello(String format) {
        HelloResult result = helloService.greet("Sample");
        if ("xlsx".equalsIgnoreCase(format)) {
            return exportHelloExcel(result);
        }
        return exportHelloCsv(result);
    }

    public byte[] exportHash(String format) {
        HashResult result = hashService.computeHash("sample-data", "SHA-256");
        if ("xlsx".equalsIgnoreCase(format)) {
            return exportHashExcel(result);
        }
        return exportHashCsv(result);
    }

    public byte[] exportBubble(String format) {
        BubbleResult result = bubbleService.sort(Arrays.asList(5, 3, 8, 1, 2));
        if ("xlsx".equalsIgnoreCase(format)) {
            return exportBubbleExcel(result);
        }
        return exportBubbleCsv(result);
    }

    private byte[] exportHelloCsv(HelloResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("greeting\n");
        sb.append(result.getGreeting()).append("\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] exportHashCsv(HashResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("input,algorithm,hash\n");
        sb.append(result.getInput()).append(",")
          .append(result.getAlgorithm()).append(",")
          .append(result.getHash()).append("\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] exportBubbleCsv(BubbleResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("originalArray,sortedArray,swapCount,comparisonCount\n");
        sb.append(result.getOriginalArray()).append(",")
          .append(result.getSortedArray()).append(",")
          .append(result.getSwapCount()).append(",")
          .append(result.getComparisonCount()).append("\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
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
            row.createCell(0).setCellValue(result.getOriginalArray().toString());
            row.createCell(1).setCellValue(result.getSortedArray().toString());
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