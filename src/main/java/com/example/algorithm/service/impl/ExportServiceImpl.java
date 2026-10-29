package com.example.algorithm.service.impl;

import com.example.algorithm.mapper.AlgorithmExportTaskMapper;
import com.example.algorithm.model.dto.ExportStatusResponse;
import com.example.algorithm.model.entity.AlgorithmExportTask;
import com.example.algorithm.service.ExportService;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Implementation of export service.
 */
@Service
public class ExportServiceImpl implements ExportService {

    private final AlgorithmExportTaskMapper exportTaskMapper;

    public ExportServiceImpl(AlgorithmExportTaskMapper exportTaskMapper) {
        this.exportTaskMapper = exportTaskMapper;
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
}
