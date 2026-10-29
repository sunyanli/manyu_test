package com.antdigital.todo.service;

import com.antdigital.todo.model.dto.OperatorContext;
import com.antdigital.todo.model.dto.TodoCreateRequest;
import com.antdigital.todo.model.dto.TodoCreateResult;

/**
 * 待办业务服务（S01/S02）：创建链路的唯一业务入口，后续批量/导入场景复用。
 *
 * @author AiWork
 * @date 2026/10/29
 */
public interface TodoService {

    /**
     * 创建待办事项。
     *
     * <p>同一 {@code tenantId + requestId} 只允许创建一条待办，重复提交返回首次结果且
     * {@code idempotent=true}（R07）。
     *
     * @param request  原始入参
     * @param operator 可信操作人上下文，不能为 null
     * @return 创建结果
     * @throws com.antdigital.todo.common.exception.TodoBizException 参数非法、无权限、限流、
     *         编号冲突或存储异常时抛出
     */
    TodoCreateResult createTodo(TodoCreateRequest request, OperatorContext operator);

    /**
     * 按幂等键查询已创建的待办（S02）。
     *
     * @param requestId 幂等键
     * @param operator  可信操作人上下文，用于租户隔离
     * @return 命中时返回待办快照并置 {@code idempotent=true}；未命中返回 null
     */
    TodoCreateResult queryByRequestId(String requestId, OperatorContext operator);
}
