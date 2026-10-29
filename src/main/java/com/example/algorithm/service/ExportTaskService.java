package com.example.algorithm.service;

import com.example.algorithm.mapper.AlgorithmExportTaskMapper;
import com.example.algorithm.model.entity.AlgorithmExportTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * Async task service for export operations.
 */
@Service
public class ExportTaskService {

    private static final Logger LOG = LoggerFactory.getLogger(ExportTaskService.class);

    private final AlgorithmExportTaskMapper exportTaskMapper;

    public ExportTaskService(AlgorithmExportTaskMapper exportTaskMapper) {
        this.exportTaskMapper = exportTaskMapper;
    }

    @Async
    public void executeExportAsync(String taskId, String algorithmType, Object params) {
        try {
            updateTaskStatus(taskId, "PROCESSING", null, null);
            String content = generateExportContent(algorithmType, params);
            Path exportDir = Paths.get("exports");
            if (!Files.exists(exportDir)) {
                Files.createDirectories(exportDir);
            }
            String fileName = taskId + ".csv";
            Path filePath = exportDir.resolve(fileName);
            Files.writeString(filePath, content, java.nio.charset.StandardCharsets.UTF_8);
            updateTaskStatus(taskId, "SUCCESS", filePath.toString(), null);
        } catch (Exception e) {
            LOG.error("Export task failed: " + taskId, e);
            updateTaskStatus(taskId, "FAILED", null, e.getMessage());
        }
    }

    private void updateTaskStatus(String taskId, String status, String fileUrl, String errorMsg) {
        AlgorithmExportTask task = new AlgorithmExportTask();
        task.setTaskId(taskId);
        task.setStatus(status);
        task.setFileUrl(fileUrl);
        task.setErrorMsg(errorMsg);
        task.setGmtModified(LocalDateTime.now());
        exportTaskMapper.updateStatus(task);
    }

    private String generateExportContent(String algorithmType, Object params) {
        return "algorithm_type,params,export_time\n"
                + algorithmType + "," + params + "," + LocalDateTime.now() + "\n";
    }
}
