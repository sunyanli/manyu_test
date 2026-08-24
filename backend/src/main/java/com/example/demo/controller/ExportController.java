package com.example.demo.controller;

import com.example.demo.annotation.Traceable;
import com.example.demo.service.ExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    @Autowired
    private ExportService exportService;

    @GetMapping
    @Traceable(apiName = "export")
    public ResponseEntity<byte[]> export(@RequestParam String type,
                                         @RequestParam(defaultValue = "csv") String format,
                                         @RequestParam(required = false) String name,
                                         @RequestParam(required = false) String input,
                                         @RequestParam(required = false) String algorithm,
                                         @RequestParam(required = false) String array) {
        byte[] data;
        String filename;
        String contentType;

        if ("xlsx".equalsIgnoreCase(format)) {
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            filename = type + "_export.xlsx";
        } else {
            contentType = "text/csv; charset=UTF-8";
            filename = type + "_export.csv";
        }

        switch (type) {
            case "hello":
                data = exportService.exportHello(name, format);
                break;
            case "hash":
                data = exportService.exportHash(input, algorithm, format);
                break;
            case "bubble":
                data = exportService.exportBubble(array, format);
                break;
            default:
                return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(data);
    }
}