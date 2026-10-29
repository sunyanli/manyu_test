package com.example.algorithm.service.impl;

import com.example.algorithm.mapper.AlgorithmExportTaskMapper;
import com.example.algorithm.model.dto.ExportStatusResponse;
import com.example.algorithm.model.entity.AlgorithmExportTask;
import com.example.algorithm.service.ExportService;
import com.example.algorithm.service.ExportTaskService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Implementation of export service.
 */
@Service
public class ExportServiceImpl implements ExportService {

    private final AlgorithmExportTaskMapper exportTaskMapper;
    private final ExportTaskService exportTaskService;

    public ExportServiceImpl(AlgorithmExportTaskMapper exportTaskMapper, ExportTaskService exportTaskService) {
        this.exportTaskMapper = exportTaskMapper;
        this.exportTaskService = exportTaskService;
    }

    @Override
    public String createExportTask(String algorithmType, Object params, String operatorId) {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        AlgorithmExportTask task = new AlgorithmExportTask();
        task.setTaskId(taskId);
        task.setAlgorithmType(algorithmType);
        task.setParams(params != null ? params.toString() : null);
        task.setStatus("PENDING");
        task.setOperatorId(operatorId != null ? operatorId : "UNKNOWN");
        exportTaskMapper.insert(task);
        exportTaskService.executeExportAsync(taskId, algorithmType, params);
        return taskId;
    }

    @Override
    public ExportStatusResponse queryExportStatus(String taskId) {
        AlgorithmExportTask task = exportTaskMapper.selectByTaskId(taskId);
        if (task == null) {
            return null;
        }
        return new ExportStatusResponse(task.getTaskId(), task.getStatus(), task.getFileUrl());
    }

    @Override
    public InputStream downloadExportFile(String taskId) {
        AlgorithmExportTask task = exportTaskMapper.selectByTaskId(taskId);
        if (task == null || task.getFileUrl() == null) {
            return new ByteArrayInputStream(new byte[0]);
        }
        try {
            return new FileInputStream(new File(task.getFileUrl()));
        } catch (IOException e) {
            return new ByteArrayInputStream(new byte[0]);
        }
    }
}
