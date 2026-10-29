package com.example.algorithm.service;

import com.example.algorithm.model.dto.ExportStatusResponse;

/**
 * Export service for algorithm results.
 */
public interface ExportService {

    /**
     * Create an async export task.
     *
     * @param algorithmType the algorithm type
     * @param params        the request params
     * @param operatorId    the operator ID
     * @return the task ID
     */
    String createExportTask(String algorithmType, Object params, String operatorId);

    /**
     * Query export task status.
     *
     * @param taskId the task ID
     * @return the status response
     */
    ExportStatusResponse queryExportStatus(String taskId);
}
