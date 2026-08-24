package com.example.algorithmdemo.tracking.aop;

import com.example.algorithmdemo.common.constant.ApiNameConstant;
import com.example.algorithmdemo.tracking.service.TrackingService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 埋点切面 - 自动记录接口调用
 */
@Aspect
@Component
public class TrackingAspect {

    private static final Logger log = LoggerFactory.getLogger(TrackingAspect.class);

    private final TrackingService trackingService;

    public TrackingAspect(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @AfterReturning(pointcut = "execution(* com.example.algorithmdemo.controller.AlgorithmController.*(..))", returning = "result")
    public void trackAlgorithmCall(JoinPoint joinPoint, Object result) {
        try {
            String methodName = joinPoint.getSignature().getName();
            String apiName = mapMethodToApiName(methodName);

            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            String userId = "anonymous";
            String userName = "anonymous";
            String userType = "unknown";
            String userLevel = "unknown";
            Long userDeptId = 0L;

            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                userId = getHeader(request, "X-User-Id", "anonymous");
                userName = getHeader(request, "X-User-Name", "anonymous");
                userType = getHeader(request, "X-User-Type", "unknown");
                userLevel = getHeader(request, "X-User-Level", "unknown");
                String deptIdStr = request.getHeader("X-User-Dept-Id");
                if (deptIdStr != null && !deptIdStr.isBlank()) {
                    try {
                        userDeptId = Long.parseLong(deptIdStr);
                    } catch (NumberFormatException e) {
                        log.warn("解析部门ID失败: {}", deptIdStr);
                    }
                }
            }

            String callResult = "SUCCESS";
            trackingService.recordCall(userId, userName, userType, userLevel, userDeptId, apiName, callResult);
        } catch (Exception e) {
            log.error("埋点切面处理异常", e);
        }
    }

    private String mapMethodToApiName(String methodName) {
        switch (methodName) {
            case "hello":
                return ApiNameConstant.HELLO;
            case "hash":
                return ApiNameConstant.HASH;
            case "bubbleSort":
                return ApiNameConstant.BUBBLE_SORT;
            default:
                return methodName;
        }
    }

    private String getHeader(HttpServletRequest request, String name, String defaultValue) {
        String value = request.getHeader(name);
        return value != null && !value.isBlank() ? value : defaultValue;
    }
}