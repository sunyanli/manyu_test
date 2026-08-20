package com.manyu.algodemo.common.context;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 调用人上下文解析器。
 *
 * <p>演示环境从请求头模拟解析人员信息；正式环境可替换为统一登录体系接入
 * （系分设计 A03 / 6.4.1）。缺失时以 anonymous/SYSTEM 兜底，保证主流程可用。</p>
 */
@Component
public class CallContextResolver {

    /** 请求头：调用人ID。 */
    public static final String HEADER_CALLER_ID = "X-Caller-Id";
    /** 请求头：调用人姓名。 */
    public static final String HEADER_CALLER_NAME = "X-Caller-Name";
    /** 请求头：人员类型。 */
    public static final String HEADER_CALLER_TYPE = "X-Caller-Type";
    /** 请求头：人员层级。 */
    public static final String HEADER_CALLER_LEVEL = "X-Caller-Level";
    /** 请求头：人员部门编码。 */
    public static final String HEADER_CALLER_DEPT_CODE = "X-Caller-Dept-Code";
    /** 请求头：人员部门名称。 */
    public static final String HEADER_CALLER_DEPT_NAME = "X-Caller-Dept-Name";

    private static final String ANONYMOUS_ID = "anonymous";
    private static final String ANONYMOUS_NAME = "匿名用户";
    private static final String SYSTEM_TYPE = "SYSTEM";

    /**
     * 解析当前调用人信息；请求上下文缺失时返回兜底对象（不抛异常）。
     *
     * @return 调用人信息
     */
    public CallerInfo resolve() {
        CallerInfo info = new CallerInfo();
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return fallback(info);
        }
        info.setCallerId(firstNonBlank(request.getHeader(HEADER_CALLER_ID), ANONYMOUS_ID));
        info.setCallerName(firstNonBlank(request.getHeader(HEADER_CALLER_NAME), ANONYMOUS_NAME));
        info.setCallerType(firstNonBlank(request.getHeader(HEADER_CALLER_TYPE), SYSTEM_TYPE));
        info.setCallerLevel(firstNonBlank(request.getHeader(HEADER_CALLER_LEVEL), "N/A"));
        info.setCallerDeptCode(firstNonBlank(request.getHeader(HEADER_CALLER_DEPT_CODE), "N/A"));
        info.setCallerDeptName(firstNonBlank(request.getHeader(HEADER_CALLER_DEPT_NAME), "未知部门"));
        return info;
    }

    private CallerInfo fallback(CallerInfo info) {
        info.setCallerId(ANONYMOUS_ID);
        info.setCallerName(ANONYMOUS_NAME);
        info.setCallerType(SYSTEM_TYPE);
        info.setCallerLevel("N/A");
        info.setCallerDeptCode("N/A");
        info.setCallerDeptName("未知部门");
        return info;
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private String firstNonBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
