package com.antdigital.todo.service.impl;

import com.antdigital.todo.common.constant.TodoConstants;
import com.antdigital.todo.common.enums.DeleteFlagEnum;
import com.antdigital.todo.common.enums.TodoErrorCodeEnum;
import com.antdigital.todo.common.enums.TodoStatusEnum;
import com.antdigital.todo.common.exception.TodoBizException;
import com.antdigital.todo.common.exception.TodoDuplicateKeyException;
import com.antdigital.todo.common.util.TodoTextUtil;
import com.antdigital.todo.dao.entity.TodoItemDO;
import com.antdigital.todo.dao.manager.TodoItemManager;
import com.antdigital.todo.integration.AuditIntegration;
import com.antdigital.todo.model.dto.OperatorContext;
import com.antdigital.todo.model.dto.TodoCreateRequest;
import com.antdigital.todo.model.dto.TodoCreateResult;
import com.antdigital.todo.model.dto.ValidatedTodo;
import com.antdigital.todo.service.TodoService;
import com.antdigital.todo.service.component.TodoCreateGate;
import com.antdigital.todo.service.component.TodoMetrics;
import com.antdigital.todo.service.component.TodoNoGenerator;
import com.antdigital.todo.service.component.TodoRateLimiter;
import com.antdigital.todo.service.component.TodoValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

/**
 * 待办业务服务实现：创建链路（F01~F05）的编排者。
 *
 * <p>处理顺序遵循系分 5.1.3.1 时序图：身份校验 → 功能开关/灰度 → 权限校验 → 限流 →
 * 入参校验规范化 → 幂等查询 → 取号 → 落库 → 事务提交后审计留痕 → 回执组装。
 * 全链路埋点（{@code todo_create_total} 等）由 {@link TodoMetrics} 统一上报。
 *
 * @author AiWork
 * @date 2026/10/29
 */
@Service
public class TodoServiceImpl implements TodoService {

    /**
     * 日志对象
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TodoServiceImpl.class);

    /**
     * 待办数据访问编排
     */
    private final TodoItemManager todoItemManager;

    /**
     * 入参校验与规范化
     */
    private final TodoValidator todoValidator;

    /**
     * 待办编号生成
     */
    private final TodoNoGenerator todoNoGenerator;

    /**
     * 单用户限流
     */
    private final TodoRateLimiter todoRateLimiter;

    /**
     * 审计日志投递
     */
    private final AuditIntegration auditIntegration;

    /**
     * 创建功能开关与灰度
     */
    private final TodoCreateGate todoCreateGate;

    /**
     * 指标埋点
     */
    private final TodoMetrics todoMetrics;

    /**
     * 构造注入依赖。
     *
     * @param todoItemManager  待办数据访问编排
     * @param todoValidator    入参校验与规范化
     * @param todoNoGenerator  待办编号生成
     * @param todoRateLimiter  单用户限流
     * @param auditIntegration 审计日志投递
     * @param todoCreateGate   创建功能开关与灰度
     * @param todoMetrics      指标埋点
     */
    public TodoServiceImpl(TodoItemManager todoItemManager,
                           TodoValidator todoValidator,
                           TodoNoGenerator todoNoGenerator,
                           TodoRateLimiter todoRateLimiter,
                           AuditIntegration auditIntegration,
                           TodoCreateGate todoCreateGate,
                           TodoMetrics todoMetrics) {
        this.todoItemManager = todoItemManager;
        this.todoValidator = todoValidator;
        this.todoNoGenerator = todoNoGenerator;
        this.todoRateLimiter = todoRateLimiter;
        this.auditIntegration = auditIntegration;
        this.todoCreateGate = todoCreateGate;
        this.todoMetrics = todoMetrics;
    }

    /**
     * 创建待办事项。
     *
     * <p>事务说明：创建为单表单条 INSERT，使用本地事务；MySQL 唯一键冲突不会中断事务，
     * 因此编号冲突可在同一事务内重新取号重试。限流在事务外语义上执行；审计在事务**提交后**
     * 才投递，避免事务回滚仍留下「创建成功」审计。
     *
     * @param request  原始入参
     * @param operator 可信操作人上下文
     * @return 创建结果
     * @throws TodoBizException 违反业务规则时抛出对应错误码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TodoCreateResult createTodo(TodoCreateRequest request, OperatorContext operator) {
        long startMillis = System.currentTimeMillis();
        try {
            TodoCreateResult result = doCreate(request, operator);
            todoMetrics.recordCreateSuccess(System.currentTimeMillis() - startMillis);
            return result;
        } catch (TodoBizException e) {
            todoMetrics.recordCreateFailure(e.getCode(), System.currentTimeMillis() - startMillis);
            throw e;
        } catch (RuntimeException e) {
            todoMetrics.recordCreateFailure(TodoErrorCodeEnum.STORAGE_ERROR.getCode(),
                    System.currentTimeMillis() - startMillis);
            throw e;
        }
    }

    /**
     * 创建主流程（在 {@link #createTodo} 的事务内执行）。
     *
     * @param request  原始入参
     * @param operator 可信操作人上下文
     * @return 创建结果
     */
    private TodoCreateResult doCreate(TodoCreateRequest request, OperatorContext operator) {
        validateOperator(operator);
        todoCreateGate.check(operator.getTenantId());
        checkPermission(operator);
        todoRateLimiter.acquire(operator.getUserId());

        ValidatedTodo validated = todoValidator.validateAndNormalize(request);

        TodoCreateResult existed = queryByRequestId(validated.getRequestId(), operator);
        if (existed != null) {
            todoMetrics.recordIdempotentHit();
            LOGGER.info("todo create idempotent hit, requestId={}, todoNo={}",
                    validated.getRequestId(), existed.getTodoNo());
            return existed;
        }

        long insertStartMillis = System.currentTimeMillis();
        for (int attempt = 0; attempt <= TodoConstants.TODO_NO_CONFLICT_RETRY_TIMES; attempt++) {
            TodoItemDO entity = buildEntity(validated, operator);
            try {
                todoItemManager.insert(entity);
            } catch (TodoDuplicateKeyException e) {
                // 索引名可能因驱动/版本差异无法解析，先无条件做幂等回查，避免误把幂等冲突当编号冲突
                TodoItemDO concurrent = selectByRequestIdForUpdate(validated.getRequestId(), operator);
                if (concurrent != null) {
                    todoMetrics.recordIdempotentHit();
                    LOGGER.info("todo create idempotent hit by duplicate key, requestId={}, todoNo={}, constraint={}",
                            validated.getRequestId(), concurrent.getTodoNo(), e.getConstraintName());
                    return toResult(concurrent, true);
                }
                LOGGER.warn("todo item unique key conflict, retry with new no. todoNo={}, attempt={}, constraint={}",
                        entity.getTodoNo(), attempt, e.getConstraintName());
                continue;
            } catch (RuntimeException e) {
                LOGGER.error("todo create storage error, requestId={}", validated.getRequestId(), e);
                throw new TodoBizException(TodoErrorCodeEnum.STORAGE_ERROR);
            }

            auditAfterCommit(validated.getRequestId(), entity.getTodoNo(), operator.getUserId());
            LOGGER.info("todo create success, requestId={}, todoNo={}, creatorId={}, cost={}ms",
                    validated.getRequestId(), entity.getTodoNo(), operator.getUserId(),
                    System.currentTimeMillis() - insertStartMillis);
            return toResult(entity, false);
        }
        throw new TodoBizException(TodoErrorCodeEnum.TODO_NO_CONFLICT);
    }

    /**
     * 按幂等键查询已创建的待办。
     *
     * @param requestId 幂等键
     * @param operator  可信操作人上下文，用于租户隔离
     * @return 命中时返回待办快照并置 {@code idempotent=true}；未命中返回 null
     * @throws TodoBizException 身份缺失或幂等键非法时抛出
     */
    @Override
    public TodoCreateResult queryByRequestId(String requestId, OperatorContext operator) {
        validateOperator(operator);
        String normalized = todoValidator.normalizeRequestId(requestId);
        TodoItemDO existed = selectByRequestId(normalized, operator);
        return existed == null ? null : toResult(existed, true);
    }

    /**
     * 按租户 + 幂等键查询，查询条件强制携带租户，禁止跨租户查询（S02）。
     *
     * @param requestId 已规范化的幂等键
     * @param operator  可信操作人上下文
     * @return 命中的待办，未命中返回 null
     */
    private TodoItemDO selectByRequestId(String requestId, OperatorContext operator) {
        return todoItemManager.selectByRequestId(buildQuery(requestId, operator));
    }

    /**
     * 按租户 + 幂等键加锁查询，用于唯一键冲突后的幂等回查。
     *
     * @param requestId 已规范化的幂等键
     * @param operator  可信操作人上下文
     * @return 命中的待办，未命中返回 null
     */
    private TodoItemDO selectByRequestIdForUpdate(String requestId, OperatorContext operator) {
        return todoItemManager.selectByRequestIdForUpdate(buildQuery(requestId, operator));
    }

    /**
     * 构造带租户隔离的查询条件。
     *
     * @param requestId 已规范化的幂等键
     * @param operator  可信操作人上下文
     * @return 查询条件载体
     */
    private TodoItemDO buildQuery(String requestId, OperatorContext operator) {
        TodoItemDO query = new TodoItemDO();
        query.setTenantId(operator.getTenantId());
        query.setRequestId(requestId);
        return query;
    }

    /**
     * R05：校验可信操作人上下文完整，缺失时视为未登录。
     *
     * @param operator 可信操作人上下文
     */
    private void validateOperator(OperatorContext operator) {
        if (operator == null
                || TodoTextUtil.isBlank(operator.getUserId())
                || TodoTextUtil.isBlank(operator.getTenantId())) {
            LOGGER.error("operator context missing, reject anonymous create");
            throw new TodoBizException(TodoErrorCodeEnum.NOT_LOGIN);
        }
    }

    /**
     * R06：垂直权限校验，须具备「待办-创建」权限点。
     *
     * @param operator 可信操作人上下文
     */
    private void checkPermission(OperatorContext operator) {
        for (String permission : operator.getRoles()) {
            if (TodoConstants.PERMISSION_ALL.equals(permission)
                    || TodoConstants.PERMISSION_TODO_CREATE.equals(permission)) {
                return;
            }
        }
        LOGGER.warn("no permission to create todo, userId={}", operator.getUserId());
        throw new TodoBizException(TodoErrorCodeEnum.NO_PERMISSION);
    }

    /**
     * 组装待入库的待办实体：状态、删除位、来源等固定值在此收口，不接受前端指定（R08）。
     *
     * @param validated 校验后的入参
     * @param operator  可信操作人上下文
     * @return 待入库实体
     */
    private TodoItemDO buildEntity(ValidatedTodo validated, OperatorContext operator) {
        // 显式使用业务时区，避免部署时区变更导致 gmt_create 漂移
        LocalDateTime now = LocalDateTime.now(TodoConstants.BIZ_ZONE);
        TodoItemDO entity = new TodoItemDO();
        entity.setTodoNo(todoNoGenerator.nextTodoNo());
        entity.setRequestId(validated.getRequestId());
        entity.setTenantId(operator.getTenantId());
        entity.setItemName(validated.getItemName());
        entity.setItemDesc(validated.getItemDesc());
        entity.setStatus(TodoStatusEnum.PENDING.getCode());
        entity.setCreatorId(operator.getUserId());
        entity.setCreatorName(TodoTextUtil.truncate(operator.getUserName(), TodoConstants.MAX_CREATOR_NAME_LENGTH));
        entity.setSource(validated.getSource());
        entity.setDeleted(DeleteFlagEnum.NOT_DELETED.getCode());
        entity.setGmtCreate(now);
        entity.setGmtModified(now);
        return entity;
    }

    /**
     * R10 + G3.2：审计留痕在事务提交后投递，弱依赖，任何异常都不得影响创建结果。
     *
     * @param requestId  幂等键
     * @param todoNo     待办编号
     * @param operatorId 操作人用户 ID
     */
    private void auditAfterCommit(String requestId, String todoNo, String operatorId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            auditCreateQuietly(requestId, todoNo, operatorId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                auditCreateQuietly(requestId, todoNo, operatorId);
            }
        });
    }

    /**
     * 投递审计事件，失败降级为本地 ERROR 日志与指标计数。
     *
     * @param requestId  幂等键
     * @param todoNo     待办编号
     * @param operatorId 操作人用户 ID
     */
    private void auditCreateQuietly(String requestId, String todoNo, String operatorId) {
        try {
            auditIntegration.recordCreate(requestId, todoNo, operatorId, true);
        } catch (Throwable throwable) {
            // 审计为弱依赖：投递失败降级为本地 ERROR 日志并计数告警
            todoMetrics.recordAuditDegrade();
            LOGGER.error("audit record failed, degrade. requestId={}, todoNo={}", requestId, todoNo, throwable);
        }
    }

    /**
     * 实体转回执。
     *
     * @param entity     待办实体
     * @param idempotent 是否幂等命中
     * @return 创建结果
     */
    private TodoCreateResult toResult(TodoItemDO entity, boolean idempotent) {
        TodoCreateResult result = new TodoCreateResult();
        result.setId(entity.getId());
        result.setTodoNo(entity.getTodoNo());
        result.setItemName(entity.getItemName());
        result.setItemDesc(entity.getItemDesc());
        result.setStatus(entity.getStatus());
        result.setCreatorId(entity.getCreatorId());
        result.setGmtCreate(entity.getGmtCreate());
        result.setIdempotent(idempotent);
        return result;
    }
}
