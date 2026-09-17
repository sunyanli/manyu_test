package com.antdigital.video.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.antdigital.video.common.constant.ErrorCodes;
import com.antdigital.video.common.constant.VideoConsts;
import com.antdigital.video.common.exception.BusinessException;
import com.antdigital.video.common.model.PageResult;
import com.antdigital.video.dao.mapper.AiEngineJobMapper;
import com.antdigital.video.dao.mapper.VideoResultMapper;
import com.antdigital.video.dao.mapper.VideoTaskMapper;
import com.antdigital.video.manager.engine.AiVideoEngineClient;
import com.antdigital.video.manager.engine.EngineGenerateRequest;
import com.antdigital.video.manager.storage.VideoStorageService;
import com.antdigital.video.model.dto.CreateVideoTaskCommand;
import com.antdigital.video.model.dto.EngineCallbackPayload;
import com.antdigital.video.model.entity.AiEngineJobDO;
import com.antdigital.video.model.entity.VideoResultDO;
import com.antdigital.video.model.entity.VideoTaskDO;
import com.antdigital.video.model.enums.CallbackStatusEnum;
import com.antdigital.video.model.enums.TaskStatusEnum;
import com.antdigital.video.model.vo.VideoTaskVO;
import com.antdigital.video.service.VideoTaskService;

/**
 * 视频任务服务实现。承载创建（幂等）、查询（租户隔离）、取消、回调推进状态机。
 */
@Service
public class VideoTaskServiceImpl implements VideoTaskService {

    private static final Logger logger = LoggerFactory.getLogger(VideoTaskServiceImpl.class);

    private static final DateTimeFormatter TASK_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VideoTaskMapper videoTaskMapper;
    private final VideoResultMapper videoResultMapper;
    private final AiEngineJobMapper aiEngineJobMapper;
    private final AiVideoEngineClient aiVideoEngineClient;
    private final VideoStorageService videoStorageService;

    private final AtomicLong taskNoSequence = new AtomicLong(0L);

    private String engineType;
    private String callbackSecret;

    public VideoTaskServiceImpl(VideoTaskMapper videoTaskMapper,
            VideoResultMapper videoResultMapper,
            AiEngineJobMapper aiEngineJobMapper,
            AiVideoEngineClient aiVideoEngineClient,
            VideoStorageService videoStorageService,
            @Value("${video.engine.type:default}") String engineType,
            @Value("${video.callback.secret:callback-secret}") String callbackSecret) {
        this.videoTaskMapper = videoTaskMapper;
        this.videoResultMapper = videoResultMapper;
        this.aiEngineJobMapper = aiEngineJobMapper;
        this.aiVideoEngineClient = aiVideoEngineClient;
        this.videoStorageService = videoStorageService;
        this.engineType = engineType;
        this.callbackSecret = callbackSecret;
    }

    @Override
    @Transactional
    public Long createVideoTask(String tenantId, CreateVideoTaskCommand command) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new BusinessException(ErrorCodes.VT_001, "tenantId 不能为空");
        }
        validateCommand(command);

        String idempotencyKey = command.getIdempotencyKey();
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            VideoTaskDO existed = videoTaskMapper.selectByIdempotencyKey(tenantId, idempotencyKey);
            if (existed != null) {
                logger.info("命中幂等键, tenantId: {}, idempotencyKey: {}, taskId: {}", tenantId, idempotencyKey, existed.getId());
                return existed.getId();
            }
        }

        int duration = command.getDurationSec() == null ? VideoConsts.DEFAULT_DURATION_SEC : command.getDurationSec();
        String resolution = defaultIfBlank(command.getResolution(), VideoConsts.DEFAULT_RESOLUTION);
        String aspectRatio = defaultIfBlank(command.getAspectRatio(), VideoConsts.DEFAULT_ASPECT_RATIO);
        String format = defaultIfBlank(command.getFormat(), VideoConsts.DEFAULT_FORMAT);

        VideoTaskDO task = new VideoTaskDO();
        task.setTenantId(tenantId);
        task.setTaskNo(generateTaskNo());
        task.setPrompt(command.getPrompt());
        task.setDurationSec(duration);
        task.setResolution(resolution);
        task.setAspectRatio(aspectRatio);
        task.setFormat(format);
        task.setStatus(TaskStatusEnum.CREATED.name());
        task.setIdempotencyKey(blankToNull(idempotencyKey));
        videoTaskMapper.insert(task);

        String engineJobId = submitToEngine(task);
        task.setEngineJobId(engineJobId);
        videoTaskMapper.updateEngineJobId(task.getId(), engineJobId);

        AiEngineJobDO engineJob = new AiEngineJobDO();
        engineJob.setTenantId(tenantId);
        engineJob.setTaskId(task.getId());
        engineJob.setEngineJobId(engineJobId);
        engineJob.setEngineType(engineType);
        engineJob.setCallbackStatus(CallbackStatusEnum.PENDING.name());
        aiEngineJobMapper.insert(engineJob);

        logger.info("视频任务创建并提交引擎成功, taskId: {}, taskNo: {}, engineJobId: {}", task.getId(), task.getTaskNo(), engineJobId);
        return task.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public VideoTaskVO getByTaskId(String tenantId, Long taskId) {
        if (taskId == null) {
            throw new BusinessException(ErrorCodes.VT_001, "taskId 不能为空");
        }
        VideoTaskDO task = requireTask(tenantId, taskId);
        VideoTaskVO vo = toVO(task);
        List<VideoResultDO> results = videoResultMapper.selectByTaskId(taskId);
        if (!results.isEmpty()) {
            vo.setResultUrl(results.get(0).getResultUrl());
        }
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<VideoTaskVO> list(String tenantId, String status, int pageNum, int pageSize) {
        int resolvedPageSize = Math.min(pageSize, VideoConsts.MAX_PAGE_SIZE);
        int offset = (pageNum - 1) * resolvedPageSize;
        long total = videoTaskMapper.countByTenantAndStatus(tenantId, status);
        if (total == 0L) {
            return PageResult.empty();
        }
        List<VideoTaskDO> tasks = videoTaskMapper.selectByTenantAndStatus(tenantId, status, offset, resolvedPageSize);
        List<VideoTaskVO> vos = new ArrayList<>(tasks.size());
        for (VideoTaskDO task : tasks) {
            vos.add(toVO(task));
        }
        return PageResult.of(vos, total);
    }

    @Override
    @Transactional
    public void cancel(String tenantId, Long taskId) {
        VideoTaskDO task = requireTask(tenantId, taskId);
        TaskStatusEnum status = TaskStatusEnum.valueOf(task.getStatus());
        if (!status.isCancellable()) {
            throw new BusinessException(ErrorCodes.VT_006, "任务已处于终态，不可取消", "任务已结束，无法取消");
        }
        videoTaskMapper.cancel(taskId);
        logger.info("任务已取消, taskId: {}", taskId);
    }

    @Override
    @Transactional
    public void handleEngineCallback(EngineCallbackPayload payload) {
        if (payload == null || payload.getEngineJobId() == null || payload.getEngineJobId().isBlank()) {
            throw new BusinessException(ErrorCodes.VT_001, "回调报文非法");
        }
        verifySignature(payload);

        AiEngineJobDO engineJob = aiEngineJobMapper.selectByEngineJobId(payload.getEngineJobId());
        if (engineJob == null) {
            logger.warn("回调对应引擎作业不存在, engineJobId: {}", payload.getEngineJobId());
            throw new BusinessException(ErrorCodes.VT_004, "引擎作业不存在", "回调对应任务不存在");
        }
        if (CallbackStatusEnum.RECEIVED.name().equals(engineJob.getCallbackStatus())) {
            logger.info("回调已处理过，忽略, engineJobId: {}, callbackStatus: {}", payload.getEngineJobId(), engineJob.getCallbackStatus());
            return;
        }

        VideoTaskDO task = videoTaskMapper.selectById(engineJob.getTaskId());
        if (task == null) {
            throw new BusinessException(ErrorCodes.VT_004, "任务不存在", "回调对应任务不存在");
        }
        TaskStatusEnum current = TaskStatusEnum.valueOf(task.getStatus());
        if (current != TaskStatusEnum.SUBMITTED && current != TaskStatusEnum.PROCESSING) {
            logger.info("任务状态不可回调推进，忽略, taskId: {}, status: {}", task.getId(), task.getStatus());
            aiEngineJobMapper.updateCallbackStatus(engineJob.getId(), CallbackStatusEnum.RECEIVED.name(), null);
            return;
        }

        String callbackStatus = payload.getStatus();
        if ("SUCCEEDED".equals(callbackStatus)) {
            completeTask(task, payload);
            aiEngineJobMapper.updateCallbackStatus(engineJob.getId(), CallbackStatusEnum.RECEIVED.name(), payload.getResultUrl());
        } else if ("FAILED".equals(callbackStatus)) {
            videoTaskMapper.updateError(task.getId(), TaskStatusEnum.FAILED.name(), payload.getErrorCode(), payload.getErrorMsg());
            aiEngineJobMapper.updateCallbackStatus(engineJob.getId(), CallbackStatusEnum.RECEIVED.name(), null);
        } else {
            // PROCESSING 等中间状态：仅推进 SUBMITTED -> PROCESSING，不置 RECEIVED，
            // 以便后续终态回调仍可处理
            if (current == TaskStatusEnum.SUBMITTED) {
                videoTaskMapper.updateStatus(task.getId(), TaskStatusEnum.SUBMITTED.name(), TaskStatusEnum.PROCESSING.name());
            }
        }
        logger.info("引擎回调处理完成, engineJobId: {}, callbackStatus: {}", payload.getEngineJobId(), callbackStatus);
    }

    private void completeTask(VideoTaskDO task, EngineCallbackPayload payload) {
        videoTaskMapper.updateStatus(task.getId(), task.getStatus(), TaskStatusEnum.SUCCEEDED.name());

        VideoResultDO result = new VideoResultDO();
        result.setTenantId(task.getTenantId());
        result.setTaskId(task.getId());
        result.setObjectKey(payload.getObjectKey());
        result.setResultUrl(payload.getResultUrl() != null
                ? payload.getResultUrl()
                : videoStorageService.buildAccessUrl(payload.getObjectKey()));
        result.setFileSize(payload.getFileSize());
        result.setDurationSec(payload.getDurationSec() == null ? task.getDurationSec() : payload.getDurationSec());
        result.setChecksum(payload.getChecksum());
        videoResultMapper.insert(result);
    }

    private String submitToEngine(VideoTaskDO task) {
        EngineGenerateRequest request = new EngineGenerateRequest();
        request.setPrompt(task.getPrompt());
        request.setDurationSec(task.getDurationSec());
        request.setResolution(task.getResolution());
        request.setAspectRatio(task.getAspectRatio());
        request.setFormat(task.getFormat());
        request.setTaskNo(task.getTaskNo());

        RuntimeException last = null;
        for (int attempt = 1; attempt <= VideoConsts.MAX_ENGINE_SUBMIT_RETRY; attempt++) {
            try {
                return aiVideoEngineClient.submit(request);
            } catch (RuntimeException ex) {
                last = ex;
                logger.warn("引擎提交失败, taskId: {}, attempt: {}/{}, errorMessage: {}",
                        task.getId(), attempt, VideoConsts.MAX_ENGINE_SUBMIT_RETRY, ex.getMessage());
            }
        }
        videoTaskMapper.updateError(task.getId(), TaskStatusEnum.FAILED.name(), ErrorCodes.VT_007, "引擎提交失败");
        throw new BusinessException(ErrorCodes.VT_007, "引擎提交失败", last == null ? "视频生成引擎不可用" : last.getMessage());
    }

    private void validateCommand(CreateVideoTaskCommand command) {
        if (command == null || command.getPrompt() == null || command.getPrompt().isBlank()) {
            throw new BusinessException(ErrorCodes.VT_001, "prompt 不能为空", "请输入视频描述");
        }
        if (command.getPrompt().length() > 1024) {
            throw new BusinessException(ErrorCodes.VT_001, "prompt 长度超限", "视频描述过长");
        }
        Integer duration = command.getDurationSec();
        if (duration != null && (duration < VideoConsts.MIN_DURATION_SEC || duration > VideoConsts.MAX_DURATION_SEC)) {
            throw new BusinessException(ErrorCodes.VT_001, "duration_sec 超出 1~30 范围", "视频时长需在 1~30 秒之间");
        }
    }

    private VideoTaskDO requireTask(String tenantId, Long taskId) {
        VideoTaskDO task = videoTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCodes.VT_004, "任务不存在", "任务不存在");
        }
        if (!task.getTenantId().equals(tenantId)) {
            throw new BusinessException(ErrorCodes.VT_005, "无权访问该任务", "无权访问该任务");
        }
        return task;
    }

    private VideoTaskVO toVO(VideoTaskDO task) {
        VideoTaskVO vo = new VideoTaskVO();
        vo.setTaskId(String.valueOf(task.getId()));
        vo.setTaskNo(task.getTaskNo());
        vo.setPrompt(task.getPrompt());
        vo.setDurationSec(task.getDurationSec());
        vo.setResolution(task.getResolution());
        vo.setFormat(task.getFormat());
        vo.setAspectRatio(task.getAspectRatio());
        vo.setStatus(task.getStatus());
        vo.setErrorCode(task.getErrorCode());
        vo.setErrorMsg(task.getErrorMsg());
        vo.setGmtCreate(task.getGmtCreate());
        return vo;
    }

    private String generateTaskNo() {
        String time = LocalDateTime.now().format(TASK_NO_TIME);
        long seq = taskNoSequence.incrementAndGet() % 10000L;
        return VideoConsts.TASK_NO_PREFIX + time + String.format("%04d", seq);
    }

    private void verifySignature(EngineCallbackPayload payload) {
        String signature = payload.getSignature();
        if (signature == null || signature.isBlank()) {
            throw new BusinessException(ErrorCodes.VT_008, "回调验签失败", "回调验签失败");
        }
        String expected = sign(payload.getEngineJobId() + "|" + payload.getStatus());
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(ErrorCodes.VT_008, "回调验签失败", "回调验签失败");
        }
    }

    private String sign(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((callbackSecret + raw).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("签名算法不可用", ex);
        }
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}