package com.antdigital.video.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.antdigital.video.common.constant.VideoConsts;
import com.antdigital.video.common.model.PageResult;
import com.antdigital.video.model.dto.CreateVideoTaskCommand;
import com.antdigital.video.model.vo.VideoTaskVO;
import com.antdigital.video.service.VideoTaskService;

import jakarta.validation.Valid;

/**
 * Web 控制台接口。
 */
@RestController
@RequestMapping("/api/v1/video-tasks")
@Validated
public class VideoTaskController {

    private static final Logger logger = LoggerFactory.getLogger(VideoTaskController.class);

    private final VideoTaskService videoTaskService;

    public VideoTaskController(VideoTaskService videoTaskService) {
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

    @GetMapping
    public Object list(@RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        int resolvedPageNum = pageNum < 1 ? VideoConsts.DEFAULT_PAGE_NUM : pageNum;
        int resolvedPageSize = pageSize < 1 ? VideoConsts.DEFAULT_PAGE_SIZE : pageSize;
        PageResult<VideoTaskVO> result = videoTaskService.list(tenantId, status, resolvedPageNum, resolvedPageSize);
        return Responses.wrap("OK", "SUCCESS", result);
    }

    @PostMapping("/{taskId}/cancel")
    public Object cancel(@RequestHeader("X-Tenant-Id") String tenantId, @PathVariable("taskId") Long taskId) {
        videoTaskService.cancel(tenantId, taskId);
        return Responses.wrap("OK", "SUCCESS", null);
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