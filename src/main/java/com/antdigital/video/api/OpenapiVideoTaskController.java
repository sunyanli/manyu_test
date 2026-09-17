package com.antdigital.video.api;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.antdigital.video.model.dto.CreateVideoTaskCommand;
import com.antdigital.video.model.vo.VideoTaskVO;
import com.antdigital.video.service.VideoTaskService;

import jakarta.validation.Valid;

/**
 * OpenAPI 接口。
 */
@RestController
@RequestMapping("/openapi/v1/video-tasks")
@Validated
public class OpenapiVideoTaskController {

    private final VideoTaskService videoTaskService;

    public OpenapiVideoTaskController(VideoTaskService videoTaskService) {
        this.videoTaskService = videoTaskService;
    }

    @PostMapping
    public Object create(@RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody CreateVideoTaskRequest request) {
        Long taskId = videoTaskService.createVideoTask(tenantId, toCommand(request));
        return Responses.createSuccess(taskId, null, "CREATED");
    }

    @GetMapping("/{taskId}")
    public Object get(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable("taskId") Long taskId) {
        return Responses.wrap("OK", "SUCCESS", videoTaskService.getByTaskId(tenantId, taskId));
    }

    private CreateVideoTaskCommand toCommand(CreateVideoTaskRequest request) {
        CreateVideoTaskCommand command = new CreateVideoTaskCommand();
        command.setPrompt(request.getPrompt());
        command.setDurationSec(request.getDurationSec());
        command.setResolution(request.getResolution());
        command.setAspectRatio(request.getAspectRatio());
        command.setFormat(request.getFormat());
        command.setIdempotencyKey(request.getIdempotencyKey());
        return command;
    }
}