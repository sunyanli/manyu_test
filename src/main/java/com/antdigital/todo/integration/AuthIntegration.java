package com.antdigital.todo.integration;

import com.antdigital.todo.model.dto.OperatorContext;

import java.util.Map;

/**
 * 登录态与身份解析接口（I01）。
 *
 * <p>默认信任统一网关已完成校验并透传的身份头；开启二次票据校验时要求票据头存在。
 * 登录校验为强依赖，失败即快速失败，不做降级放行（系分 6.1）。
 *
 * @author AiWork
 * @date 2026/10/29
 */
public interface AuthIntegration {

    /**
     * 从请求头解析可信操作人上下文。
     *
     * @param headers 请求头键值（大小写不敏感由调用方保证）
     * @return 可信操作人上下文
     * @throws com.antdigital.todo.common.exception.TodoBizException 身份缺失或票据校验失败时抛出 TODO_0005
     */
    OperatorContext resolveOperator(Map<String, String> headers);
}
