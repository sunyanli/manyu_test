package com.antdigital.todo.common.trace;

import org.slf4j.MDC;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 链路追踪上下文：保存当前线程的 traceId，并同步到 MDC 供日志输出。
 *
 * <p>traceId 只接受字母、数字、下划线与中划线，且长度不超过 {@link #TRACE_ID_MAX_LEN}，
 * 非法或缺失时由应用生成，避免日志注入与超长 Header 打爆日志。</p>
 *
 * @author AiWork
 */
public final class TraceContext {

    /** MDC 中的 traceId 键名。 */
    public static final String MDC_KEY = "traceId";

    /** traceId 最大长度。 */
    public static final int TRACE_ID_MAX_LEN = 64;

    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<String>();

    private TraceContext() {
    }

    /**
     * 初始化 traceId：优先沿用上游透传值，缺失或非法时生成新的 UUID。
     *
     * @param headerValue 上游透传的 traceId
     * @return 最终生效的 traceId
     */
    public static String initTraceId(String headerValue) {
        String traceId = normalize(headerValue);
        if (traceId == null) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        TRACE_ID.set(traceId);
        MDC.put(MDC_KEY, traceId);
        return traceId;
    }

    /**
     * 获取当前线程的 traceId。
     *
     * @return 未初始化时返回 null
     */
    public static String currentTraceId() {
        return TRACE_ID.get();
    }

    /**
     * 清理当前线程的 traceId，必须在请求结束时调用，避免线程池复用导致串号。
     */
    public static void clear() {
        TRACE_ID.remove();
        MDC.remove(MDC_KEY);
    }

    private static String normalize(String headerValue) {
        if (headerValue == null) {
            return null;
        }
        String trimmed = headerValue.trim();
        if (!TRACE_ID_PATTERN.matcher(trimmed).matches()) {
            return null;
        }
        return trimmed;
    }
}
