package com.antdigital.video.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.antdigital.video.common.constant.ErrorCodes;
import com.antdigital.video.common.exception.BusinessException;
import com.antdigital.video.dao.mapper.AiEngineJobMapper;
import com.antdigital.video.dao.mapper.VideoResultMapper;
import com.antdigital.video.dao.mapper.VideoTaskMapper;
import com.antdigital.video.manager.engine.AiVideoEngineClient;
import com.antdigital.video.manager.storage.VideoStorageService;
import com.antdigital.video.model.dto.CreateVideoTaskCommand;
import com.antdigital.video.model.dto.EngineCallbackPayload;
import com.antdigital.video.model.entity.AiEngineJobDO;
import com.antdigital.video.model.entity.VideoTaskDO;
import com.antdigital.video.model.enums.TaskStatusEnum;

/**
 * {@link VideoTaskServiceImpl} 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class VideoTaskServiceImplTest {

    private static final String TENANT_ID = "tenant-001";

    @Mock
    private VideoTaskMapper videoTaskMapper;
    @Mock
    private VideoResultMapper videoResultMapper;
    @Mock
    private AiEngineJobMapper aiEngineJobMapper;
    @Mock
    private AiVideoEngineClient aiVideoEngineClient;
    @Mock
    private VideoStorageService videoStorageService;

    private VideoTaskServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VideoTaskServiceImpl(videoTaskMapper, videoResultMapper, aiEngineJobMapper,
                aiVideoEngineClient, videoStorageService, "default", "callback-secret");
    }

    @Test
    @DisplayName("创建任务：参数合法时落库并返回任务 ID")
    void should_createTask_when_commandValid() {
        CreateVideoTaskCommand command = command("小猫喵喵叫", null, null);
        when(aiVideoEngineClient.submit(any())).thenReturn("mock-job-1");

        Long taskId = service.createVideoTask(TENANT_ID, command);

        assertThat(taskId).isNotNull();
        verify(videoTaskMapper).insert(any(VideoTaskDO.class));
        verify(videoTaskMapper).updateEngineJobId(eq(taskId), eq("mock-job-1"));
        verify(aiEngineJobMapper).insert(any(AiEngineJobDO.class));
    }

    @Test
    @DisplayName("创建任务：幂等键命中时返回已有任务")
    void should_returnExistingTask_when_idempotencyKeyHit() {
        CreateVideoTaskCommand command = command("小猫喵喵叫", null, "idem-1");
        VideoTaskDO existed = new VideoTaskDO();
        existed.setId(100L);
        when(videoTaskMapper.selectByIdempotencyKey(TENANT_ID, "idem-1")).thenReturn(existed);

        Long taskId = service.createVideoTask(TENANT_ID, command);

        assertThat(taskId).isEqualTo(100L);
        verify(videoTaskMapper, never()).insert(any());
        verify(aiVideoEngineClient, never()).submit(any());
    }

    @Test
    @DisplayName("创建任务：prompt 为空时抛 VT_001")
    void should_throwVT001_when_promptBlank() {
        CreateVideoTaskCommand command = command("   ", null, null);

        assertThatThrownBy(() -> service.createVideoTask(TENANT_ID, command))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCodes.VT_001));
    }

    @Test
    @DisplayName("创建任务：时长超范围时抛 VT_001")
    void should_throwVT001_when_durationOutOfRange() {
        CreateVideoTaskCommand command = command("小猫喵喵叫", 60, null);

        assertThatThrownBy(() -> service.createVideoTask(TENANT_ID, command))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCodes.VT_001));
    }

    @Test
    @DisplayName("查询任务：租户匹配时返回 VO")
    void should_getTask_when_tenantMatch() {
        VideoTaskDO task = task(1L, TaskStatusEnum.CREATED);
        when(videoTaskMapper.selectById(1L)).thenReturn(task);
        when(videoResultMapper.selectByTaskId(1L)).thenReturn(Collections.emptyList());

        var vo = service.getByTaskId(TENANT_ID, 1L);

        assertThat(vo.getTaskId()).isEqualTo("1");
        assertThat(vo.getStatus()).isEqualTo(TaskStatusEnum.CREATED.name());
    }

    @Test
    @DisplayName("查询任务：租户不匹配时抛 VT_005")
    void should_throwVT005_when_tenantMismatch() {
        VideoTaskDO task = task(1L, TaskStatusEnum.CREATED);
        task.setTenantId("another-tenant");
        when(videoTaskMapper.selectById(1L)).thenReturn(task);

        assertThatThrownBy(() -> service.getByTaskId(TENANT_ID, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCodes.VT_005));
    }

    @Test
    @DisplayName("查询任务：任务不存在时抛 VT_004")
    void should_throwVT004_when_taskNotExist() {
        when(videoTaskMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> service.getByTaskId(TENANT_ID, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCodes.VT_004));
    }

    @Test
    @DisplayName("取消任务：非终态时成功")
    void should_cancel_when_statusNonTerminal() {
        VideoTaskDO task = task(1L, TaskStatusEnum.SUBMITTED);
        when(videoTaskMapper.selectById(1L)).thenReturn(task);
        when(videoTaskMapper.cancel(1L)).thenReturn(1);

        service.cancel(TENANT_ID, 1L);

        verify(videoTaskMapper).cancel(1L);
    }

    @Test
    @DisplayName("取消任务：终态时抛 VT_006")
    void should_throwVT006_when_cancelTerminalStatus() {
        VideoTaskDO task = task(1L, TaskStatusEnum.SUCCEEDED);
        when(videoTaskMapper.selectById(1L)).thenReturn(task);

        assertThatThrownBy(() -> service.cancel(TENANT_ID, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCodes.VT_006));
    }

    @Test
    @DisplayName("回调：成功回调推进任务至 SUCCEEDED 并写结果")
    void should_advanceToSucceeded_when_callbackSuccess() {
        VideoTaskDO task = task(1L, TaskStatusEnum.SUBMITTED);
        AiEngineJobDO engineJob = engineJob(10L, task.getId(), "mock-job-1", "PENDING");
        EngineCallbackPayload payload = successPayload("mock-job-1");

        when(aiEngineJobMapper.selectByEngineJobId("mock-job-1")).thenReturn(engineJob);
        when(videoTaskMapper.selectById(1L)).thenReturn(task);
        when(videoTaskMapper.updateStatus(1L, "SUBMITTED", "SUCCEEDED")).thenReturn(1);
        when(videoResultMapper.insert(any())).thenReturn(1);

        service.handleEngineCallback(payload);

        verify(videoTaskMapper).updateStatus(1L, "SUBMITTED", "SUCCEEDED");
        verify(videoResultMapper).insert(any());
    }

    @Test
    @DisplayName("回调：已处理回调幂等忽略")
    void should_ignoreCallback_when_alreadyReceived() {
        AiEngineJobDO engineJob = engineJob(10L, 1L, "mock-job-1", "RECEIVED");
        EngineCallbackPayload payload = successPayload("mock-job-1");
        when(aiEngineJobMapper.selectByEngineJobId("mock-job-1")).thenReturn(engineJob);

        service.handleEngineCallback(payload);

        verify(videoTaskMapper, never()).selectById(any());
        verify(videoResultMapper, never()).insert(any());
    }

    @Test
    @DisplayName("回调：验签失败抛 VT_008")
    void should_throwVT008_when_signatureInvalid() {
        EngineCallbackPayload payload = successPayload("mock-job-1");
        payload.setSignature("wrong-signature");

        assertThatThrownBy(() -> service.handleEngineCallback(payload))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCodes.VT_008));
    }

    @Test
    @DisplayName("创建任务：引擎提交重试失败后标记 FAILED 并抛 VT_007")
    void should_markFailed_when_engineSubmitFailsAllRetries() {
        CreateVideoTaskCommand command = command("小猫喵喵叫", null, null);
        when(aiVideoEngineClient.submit(any())).thenThrow(new RuntimeException("引擎不可用"));

        assertThatThrownBy(() -> service.createVideoTask(TENANT_ID, command))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCodes.VT_007));

        ArgumentCaptor<VideoTaskDO> captor = ArgumentCaptor.forClass(VideoTaskDO.class);
        verify(videoTaskMapper).insert(captor.capture());
        verify(videoTaskMapper).updateError(eq(captor.getValue().getId()), eq(TaskStatusEnum.FAILED.name()), eq(ErrorCodes.VT_007), anyString());
    }

    private static CreateVideoTaskCommand command(String prompt, Integer duration, String idempotencyKey) {
        CreateVideoTaskCommand command = new CreateVideoTaskCommand();
        command.setPrompt(prompt);
        command.setDurationSec(duration);
        command.setIdempotencyKey(idempotencyKey);
        return command;
    }

    private static VideoTaskDO task(long id, TaskStatusEnum status) {
        VideoTaskDO task = new VideoTaskDO();
        task.setId(id);
        task.setTenantId(TENANT_ID);
        task.setTaskNo("VT202609170001");
        task.setPrompt("小猫喵喵叫");
        task.setDurationSec(3);
        task.setStatus(status.name());
        return task;
    }

    private static AiEngineJobDO engineJob(long id, long taskId, String engineJobId, String callbackStatus) {
        AiEngineJobDO job = new AiEngineJobDO();
        job.setId(id);
        job.setTenantId(TENANT_ID);
        job.setTaskId(taskId);
        job.setEngineJobId(engineJobId);
        job.setEngineType("default");
        job.setCallbackStatus(callbackStatus);
        return job;
    }

    private static EngineCallbackPayload successPayload(String engineJobId) {
        EngineCallbackPayload payload = new EngineCallbackPayload();
        payload.setEngineJobId(engineJobId);
        payload.setStatus("SUCCEEDED");
        payload.setResultUrl("http://localhost:8080/videos/video.mp4");
        payload.setObjectKey("video.mp4");
        payload.setDurationSec(3);
        payload.setSignature(secureSign(engineJobId + "|SUCCEEDED"));
        return payload;
    }

    private static String secureSign(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(("callback-secret" + raw).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}