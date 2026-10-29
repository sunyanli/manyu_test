package com.antdigital.todo.common.trace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TraceContext} 单元测试。
 *
 * @author AiWork
 */
class TraceContextTest {

    @AfterEach
    void tearDown() {
        TraceContext.clear();
    }

    @Test
    @DisplayName("缺失 Header：生成 traceId 并写入 MDC")
    void should_generateTraceId_when_headerAbsent() {
        // Act
        String traceId = TraceContext.initTraceId(null);

        // Assert
        assertThat(traceId).isNotBlank().hasSize(32);
        assertThat(TraceContext.currentTraceId()).isEqualTo(traceId);
        assertThat(MDC.get(TraceContext.MDC_KEY)).isEqualTo(traceId);
    }

    @Test
    @DisplayName("合法 Header：沿用上游 traceId")
    void should_reuseTraceId_when_headerValid() {
        // Act
        String traceId = TraceContext.initTraceId("  abc-123_DEF  ");

        // Assert
        assertThat(traceId).isEqualTo("abc-123_DEF");
    }

    @Test
    @DisplayName("非法 Header：拒绝日志注入并重新生成")
    void should_regenerateTraceId_when_headerIllegal() {
        // Act
        String injected = TraceContext.initTraceId("abc\nfake-log-line");
        String tooLong = TraceContext.initTraceId(repeat('a', 65));

        // Assert
        assertThat(injected).isNotEqualTo("abc\nfake-log-line").hasSize(32);
        assertThat(tooLong).hasSize(32);
    }

    @Test
    @DisplayName("clear：线程变量与 MDC 同时清理")
    void should_clearTraceIdAndMdc() {
        // Arrange
        TraceContext.initTraceId("abc");

        // Act
        TraceContext.clear();

        // Assert
        assertThat(TraceContext.currentTraceId()).isNull();
        assertThat(MDC.get(TraceContext.MDC_KEY)).isNull();
    }

    private String repeat(char value, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }
}
