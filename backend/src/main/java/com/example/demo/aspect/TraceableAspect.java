package com.example.demo.aspect;

import com.example.demo.annotation.Traceable;
import com.example.demo.model.CallLog;
import com.example.demo.service.CallLogAsyncSaver;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Aspect
@Component
public class TraceableAspect {

    @Autowired
    private CallLogAsyncSaver callLogAsyncSaver;

    @Around("@annotation(traceable)")
    public Object logCall(ProceedingJoinPoint joinPoint, Traceable traceable) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取请求头中的调用人信息
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes()).getRequest();

        String callerName = request.getHeader("X-Caller-Name");
        String personType = request.getHeader("X-Person-Type");
        String personLevel = request.getHeader("X-Person-Level");
        String department = request.getHeader("X-Department");

        String apiName = traceable.apiName();
        if (apiName.isEmpty()) {
            apiName = joinPoint.getSignature().getName();
        }

        Object result;
        String status = "success";
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            status = "fail";
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            // 异步保存调用日志
            saveCallLog(apiName, callerName, personType, personLevel, department, duration, status);
        }
    }

    private void saveCallLog(String apiName, String callerName, String personType,
                             String personLevel, String department, long durationMs, String status) {
        CallLog log = new CallLog();
        log.setApiName(apiName);
        log.setCallerName(callerName != null ? callerName : "anonymous");
        log.setPersonType(personType != null ? personType : "unknown");
        log.setPersonLevel(personLevel != null ? personLevel : "unknown");
        log.setDepartment(department != null ? department : "unknown");
        log.setCallTime(LocalDateTime.now());
        log.setDurationMs(durationMs);
        log.setStatus(status);
        callLogAsyncSaver.save(log);
    }
}