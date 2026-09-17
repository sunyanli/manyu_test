package com.antdigital.video.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.antdigital.video.model.dto.EngineCallbackPayload;
import com.antdigital.video.service.VideoTaskService;

/**
 * 引擎结果回调入口。
 */
@RestController
@RequestMapping("/internal/v1/video-callback")
public class VideoEngineCallbackController {

    private final VideoTaskService videoTaskService;

    public VideoEngineCallbackController(VideoTaskService videoTaskService) {
        this.videoTaskService = videoTaskService;
    }

    @PostMapping
    public Object callback(@RequestBody EngineCallbackPayload payload) {
        videoTaskService.handleEngineCallback(payload);
        return Responses.wrap("OK", "SUCCESS", null);
    }
}