package com.antdigital.todo.integration;

/**
 * 创建审计日志投递接口（I02）。
 *
 * <p>弱依赖：投递失败必须降级为本地 ERROR 日志，不得阻断或回滚创建主链路（R10）。
 *
 * @author AiWork
 * @date 2026/10/29
 */
public interface AuditIntegration {

    /**
     * 记录一次创建动作。
     *
     * @param requestId  幂等键
     * @param todoNo     待办编号
     * @param operatorId 操作人用户 ID
     * @param success    是否创建成功
     */
    void recordCreate(String requestId, String todoNo, String operatorId, boolean success);
}
