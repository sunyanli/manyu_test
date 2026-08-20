package com.manyu.algodemo.tracking.aspect;

import com.manyu.algodemo.common.context.CallContextResolver;
import com.manyu.algodemo.common.context.CallerInfo;
import com.manyu.algodemo.tracking.annotation.TrackCall;
import com.manyu.algodemo.tracking.async.CallRecordQueue;
import com.manyu.algodemo.tracking.model.entity.CallRecordDO;
import com.manyu.algodemo.tracking.model.enums.BizType;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 埋点切面：对标注 {@link TrackCall} 的方法做环绕通知，异步记录调用次数与调用人（F06）。
 *
 * <p>入参/出参仅记录摘要，不落原文与密钥（R03）。</p>
 */
@Aspect
@Component
public class TrackAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrackAspect.class);

    private static final int HASH_PREFIX_LEN = 16;
    private static final int SORT_PREFIX_LEN = 10;
    private static final int RESP_SUMMARY_LIMIT = 300;

    private final CallRecordQueue callRecordQueue;
    private final CallContextResolver callContextResolver;

    /**
     * 构造器注入。
     *
     * @param callRecordQueue     埋点队列
     * @param callContextResolver 调用人解析器
     */
    public TrackAspect(CallRecordQueue callRecordQueue, CallContextResolver callContextResolver) {
        this.callRecordQueue = callRecordQueue;
        this.callContextResolver = callContextResolver;
    }

    /**
     * 环绕通知：执行目标方法并异步落埋点；目标方法异常时记 FAIL 并继续抛出。
     *
     * @param joinPoint 连接点
     * @param trackCall 埋点注解
     * @return 目标方法返回值
     * @throws Throwable 目标方法异常
     */
    @Around("@annotation(trackCall)")
    public Object around(ProceedingJoinPoint joinPoint, TrackCall trackCall) throws Throwable {
        long start = System.currentTimeMillis();
        Object[] args = joinPoint.getArgs();
        CallerInfo caller = callContextResolver.resolve();
        try {
            Object result = joinPoint.proceed();
            CallRecordDO record = buildRecord(trackCall.type(), caller, args, result,
                    System.currentTimeMillis() - start, null, null);
            callRecordQueue.offer(record);
            return result;
        } catch (Throwable t) {
            CallRecordDO record = buildRecord(trackCall.type(), caller, args, null,
                    System.currentTimeMillis() - start, "FAIL", t.getClass().getSimpleName());
            callRecordQueue.offer(record);
            throw t;
        }
    }

    private CallRecordDO buildRecord(BizType bizType, CallerInfo caller, Object[] args,
                                     Object result, long costTimeMs, String status, String errorCode) {
        CallRecordDO record = new CallRecordDO();
        record.setBizType(bizType.name());
        record.setCallerId(caller.getCallerId());
        record.setCallerName(caller.getCallerName());
        record.setCallerType(caller.getCallerType());
        record.setCallerLevel(caller.getCallerLevel());
        record.setCallerDeptCode(caller.getCallerDeptCode());
        record.setCallerDeptName(caller.getCallerDeptName());
        record.setReqSummary(buildReqSummary(bizType, args));
        record.setRespSummary(status == null ? buildRespSummary(bizType, result) : null);
        record.setCostTimeMs(costTimeMs);
        record.setResultStatus(status == null ? "SUCCESS" : status);
        record.setErrorCode(errorCode);
        return record;
    }

    private String buildReqSummary(BizType bizType, Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        return switch (bizType) {
            case HELLO_WORLD -> "name=" + safe(args[0]);
            case HASH -> "algorithm=" + safe(args[1]) + ",textBytes=" + textBytes(safe(args[0]));
            case BUBBLE_SORT -> "size=" + listSize(args[0]) + ",order=" + safe(args[1])
                    + ",optimized=" + safe(args[2]);
            case EXPORT -> "target=" + exportField(args[0], "target") + ",format=" + exportField(args[0], "format");
            default -> "args=" + truncate(args[0], RESP_SUMMARY_LIMIT);
        };
    }

    private String buildRespSummary(BizType bizType, Object result) {
        if (result == null) {
            return "";
        }
        return switch (bizType) {
            case HELLO_WORLD, EXPORT -> truncate(result.toString(), RESP_SUMMARY_LIMIT);
            case HASH -> truncate(result.toString(), HASH_PREFIX_LEN + 60);
            case BUBBLE_SORT -> truncate(result.toString(), SORT_PREFIX_LEN * 6 + 30);
            default -> truncate(result.toString(), RESP_SUMMARY_LIMIT);
        };
    }

    private String exportField(Object arg, String field) {
        if (arg == null) {
            return "";
        }
        String text = arg.toString();
        String prefix = field + "=";
        if (!text.contains(prefix)) {
            return "";
        }
        String rest = text.substring(text.indexOf(prefix) + prefix.length());
        int comma = rest.indexOf(',');
        return comma > 0 ? rest.substring(0, comma) : rest;
    }

    private long listSize(Object arg) {
        if (arg instanceof List<?> list) {
            return list.size();
        }
        return -1;
    }

    private int textBytes(String text) {
        return text == null ? 0 : text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }
}
