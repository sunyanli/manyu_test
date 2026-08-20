package com.manyu.algodemo.tracking.aspect;

import com.manyu.algodemo.common.context.CallContextResolver;
import com.manyu.algodemo.tracking.annotation.TrackCall;
import com.manyu.algodemo.tracking.async.CallRecordQueue;
import com.manyu.algodemo.tracking.model.entity.CallRecordDO;
import com.manyu.algodemo.tracking.model.enums.BizType;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 埋点切面测试：成功/失败路径均异步入队，摘要不含敏感原文。
 */
@ExtendWith(MockitoExtension.class)
class TrackAspectTest {

    @Mock
    private CallRecordQueue callRecordQueue;

    private TrackAspect trackAspect;

    @BeforeEach
    void setUp() {
        trackAspect = new TrackAspect(callRecordQueue, new CallContextResolver());
    }

    @Test
    @DisplayName("成功路径记录 SUCCESS 埋点并返回结果")
    void should_recordSuccess() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        TrackCall trackCall = mock(TrackCall.class);
        when(signature.getName()).thenReturn("hello");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"Alice"});
        when(joinPoint.proceed()).thenReturn("ok");
        when(trackCall.type()).thenReturn(BizType.HELLO_WORLD);

        Object result = trackAspect.around(joinPoint, trackCall);

        assertThat(result).isEqualTo("ok");
        ArgumentCaptor<CallRecordDO> captor = ArgumentCaptor.forClass(CallRecordDO.class);
        verify(callRecordQueue).offer(captor.capture());
        CallRecordDO record = captor.getValue();
        assertThat(record.getBizType()).isEqualTo("HELLO_WORLD");
        assertThat(record.getResultStatus()).isEqualTo("SUCCESS");
        assertThat(record.getCallerId()).isEqualTo("anonymous");
        assertThat(record.getReqSummary()).isEqualTo("name=Alice");
    }

    @Test
    @DisplayName("异常路径记录 FAIL 埋点并继续抛出")
    void should_recordFailure() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        TrackCall trackCall = mock(TrackCall.class);
        when(signature.getName()).thenReturn("hash");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"secret-text", "SHA256"});
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("boom"));
        when(trackCall.type()).thenReturn(BizType.HASH);

        assertThatThrownBy(() -> trackAspect.around(joinPoint, trackCall))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");

        ArgumentCaptor<CallRecordDO> captor = ArgumentCaptor.forClass(CallRecordDO.class);
        verify(callRecordQueue).offer(captor.capture());
        CallRecordDO record = captor.getValue();
        assertThat(record.getResultStatus()).isEqualTo("FAIL");
        // R03：哈希入参不落原文，仅记算法与字节数
        assertThat(record.getReqSummary()).doesNotContain("secret-text");
        assertThat(record.getReqSummary()).startsWith("algorithm=SHA256,textBytes=");
    }
}
