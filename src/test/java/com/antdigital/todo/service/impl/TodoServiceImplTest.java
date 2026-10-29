package com.antdigital.todo.service.impl;

import com.antdigital.todo.common.enums.TodoErrorCodeEnum;
import com.antdigital.todo.common.exception.TodoBizException;
import com.antdigital.todo.common.exception.TodoDuplicateKeyException;
import com.antdigital.todo.dao.entity.TodoItemDO;
import com.antdigital.todo.dao.manager.TodoItemManager;
import com.antdigital.todo.dao.mapper.TodoItemMapper;
import com.antdigital.todo.integration.AuditIntegration;
import com.antdigital.todo.model.dto.OperatorContext;
import com.antdigital.todo.model.dto.TodoCreateRequest;
import com.antdigital.todo.model.dto.TodoCreateResult;
import com.antdigital.todo.service.component.TodoNoGenerator;
import com.antdigital.todo.service.component.TodoRateLimiter;
import com.antdigital.todo.service.component.TodoValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link TodoServiceImpl} 创建链路单元测试。
 *
 * <p>覆盖正常创建、幂等命中、并发唯一键冲突、编号冲突重试与耗尽、存储异常、权限与登录态、
 * 限流以及审计降级场景。依赖均为手写替身，不引入额外 Mock 框架，保持与项目现有测试技术栈一致。
 *
 * @author AiWork
 * @date 2026/10/29
 */
class TodoServiceImplTest {

    private static final String TENANT_ID = "0";

    private static final String USER_ID = "U10086";

    private static final String USER_NAME = "张三";

    private static final String REQUEST_ID = "8f2b1c34-5d6e-4a7b-9c8d-0e1f2a3b4c5d";

    private static final String TODO_NO = "TD20261029000001";

    private FakeTodoItemMapper todoItemMapper;

    private FakeTodoNoGenerator todoNoGenerator;

    private FakeAuditIntegration auditIntegration;

    private TodoRateLimiter todoRateLimiter;

    private TodoServiceImpl todoService;

    @BeforeEach
    void setUp() {
        todoItemMapper = new FakeTodoItemMapper();
        todoNoGenerator = new FakeTodoNoGenerator();
        auditIntegration = new FakeAuditIntegration();
        todoRateLimiter = new TodoRateLimiter(100, () -> 1_000L);
        todoService = new TodoServiceImpl(new TodoItemManager(todoItemMapper), new TodoValidator(),
                todoNoGenerator, todoRateLimiter, auditIntegration);
    }

    // ==================== createTodo 测试 ====================

    @Test
    @DisplayName("正常创建：落库一条待办并返回回执")
    void should_createTodo_when_validRequest() {
        // Arrange
        TodoCreateRequest request = buildRequest(REQUEST_ID, "整理季度经营分析材料", "汇总 Q3 数据");
        OperatorContext operator = buildOperator(Collections.singletonList("TODO_CREATE"));
        todoNoGenerator.enqueue(TODO_NO);

        // Act
        TodoCreateResult result = todoService.createTodo(request, operator);

        // Assert
        assertThat(result.getId()).isNotNull();
        assertThat(result.getTodoNo()).isEqualTo(TODO_NO);
        assertThat(result.getItemName()).isEqualTo("整理季度经营分析材料");
        assertThat(result.getStatus()).isEqualTo(0);
        assertThat(result.isIdempotent()).isFalse();
        assertThat(result.getCreatorId()).isEqualTo(USER_ID);
        assertThat(todoItemMapper.insertCount).isEqualTo(1);
        assertThat(todoItemMapper.size()).isEqualTo(1);
        assertThat(auditIntegration.records).containsExactly(REQUEST_ID + "|" + TODO_NO + "|" + USER_ID + "|true");
    }

    @Test
    @DisplayName("创建后状态固定待处理，来源缺省为 web")
    void should_forcePendingStatusAndDefaultSource_when_create() {
        // Arrange
        TodoCreateRequest request = buildRequest(REQUEST_ID, "整理周报", null);
        OperatorContext operator = buildOperator(Collections.singletonList("TODO_CREATE"));
        todoNoGenerator.enqueue(TODO_NO);

        // Act
        todoService.createTodo(request, operator);

        // Assert
        TodoItemDO saved = todoItemMapper.first();
        assertThat(saved.getStatus()).isEqualTo(0);
        assertThat(saved.getDeleted()).isEqualTo(0);
        assertThat(saved.getSource()).isEqualTo("web");
        assertThat(saved.getItemDesc()).isEmpty();
        assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    @DisplayName("幂等命中：同一 requestId 重复提交返回首次结果")
    void should_returnFirstResult_when_sameRequestIdSubmittedTwice() {
        // Arrange
        OperatorContext operator = buildOperator(Collections.singletonList("TODO_CREATE"));
        todoNoGenerator.enqueue(TODO_NO);
        TodoCreateResult first = todoService.createTodo(buildRequest(REQUEST_ID, "整理周报", null), operator);

        // Act
        TodoCreateResult second = todoService.createTodo(buildRequest(REQUEST_ID, "整理周报", null), operator);

        // Assert
        assertThat(second.getTodoNo()).isEqualTo(first.getTodoNo());
        assertThat(second.isIdempotent()).isTrue();
        assertThat(todoItemMapper.insertCount).isEqualTo(1);
        assertThat(todoItemMapper.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("并发重复提交：唯一键冲突后重查并返回首次结果")
    void should_returnFirstResult_when_duplicateRequestIdRace() {
        // Arrange
        OperatorContext operator = buildOperator(Collections.singletonList("TODO_CREATE"));
        TodoItemDO existing = buildExisting(TODO_NO, REQUEST_ID);
        todoItemMapper.seed(existing);
        // 模拟并发竞态：首次幂等查询未命中，插入时唯一键冲突
        todoItemMapper.missSelectOnce = true;
        todoNoGenerator.enqueue("TD20261029000099");

        // Act
        TodoCreateResult result = todoService.createTodo(buildRequest(REQUEST_ID, "整理周报", null), operator);

        // Assert
        assertThat(result.isIdempotent()).isTrue();
        assertThat(result.getTodoNo()).isEqualTo(TODO_NO);
        assertThat(todoItemMapper.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("编号冲突：重试一次取新号后创建成功")
    void should_retryAndSucceed_when_todoNoConflictOnce() {
        // Arrange
        OperatorContext operator = buildOperator(Collections.singletonList("TODO_CREATE"));
        todoItemMapper.seed(buildExisting(TODO_NO, "another-request"));
        todoNoGenerator.enqueue(TODO_NO);
        todoNoGenerator.enqueue("TD20261029000002");

        // Act
        TodoCreateResult result = todoService.createTodo(buildRequest(REQUEST_ID, "整理周报", null), operator);

        // Assert
        assertThat(result.getTodoNo()).isEqualTo("TD20261029000002");
        assertThat(result.isIdempotent()).isFalse();
        assertThat(todoItemMapper.insertCount).isEqualTo(2);
        assertThat(todoItemMapper.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("编号冲突：重试一次仍冲突则返回 TODO_0007")
    void should_throwTodoNoConflict_when_todoNoAlwaysConflicts() {
        // Arrange
        OperatorContext operator = buildOperator(Collections.singletonList("TODO_CREATE"));
        todoItemMapper.seed(buildExisting(TODO_NO, "another-request"));
        todoNoGenerator.enqueue(TODO_NO);
        todoNoGenerator.enqueue(TODO_NO);
        TodoCreateRequest request = buildRequest(REQUEST_ID, "整理周报", null);

        // Act & Assert
        assertThatThrownBy(() -> todoService.createTodo(request, operator))
                .isInstanceOfSatisfying(TodoBizException.class,
                        e -> assertThat(e.getCode()).isEqualTo(TodoErrorCodeEnum.TODO_NO_CONFLICT.getCode()));
        assertThat(todoItemMapper.insertCount).isEqualTo(2);
    }

    @Test
    @DisplayName("存储异常：落库抛非唯一键异常时返回 TODO_0008")
    void should_throwStorageError_when_insertFails() {
        // Arrange
        OperatorContext operator = buildOperator(Collections.singletonList("TODO_CREATE"));
        todoNoGenerator.enqueue(TODO_NO);
        todoItemMapper.enqueueFailure(new IllegalStateException("connection reset"));
        TodoCreateRequest request = buildRequest(REQUEST_ID, "整理周报", null);

        // Act & Assert
        assertThatThrownBy(() -> todoService.createTodo(request, operator))
                .isInstanceOfSatisfying(TodoBizException.class,
                        e -> assertThat(e.getCode()).isEqualTo(TodoErrorCodeEnum.STORAGE_ERROR.getCode()));
    }

    @Test
    @DisplayName("权限校验：无「待办-创建」权限点时返回 TODO_0006")
    void should_throwNoPermission_when_rolesLackPermission() {
        // Arrange
        OperatorContext operator = buildOperator(Collections.singletonList("ORDER_VIEW"));
        TodoCreateRequest request = buildRequest(REQUEST_ID, "整理周报", null);

        // Act & Assert
        assertThatThrownBy(() -> todoService.createTodo(request, operator))
                .isInstanceOfSatisfying(TodoBizException.class,
                        e -> assertThat(e.getCode()).isEqualTo(TodoErrorCodeEnum.NO_PERMISSION.getCode()));
        assertThat(todoItemMapper.insertCount).isZero();
    }

    @Test
    @DisplayName("权限校验：全量权限标识 * 可创建")
    void should_allowCreate_when_roleIsWildcard() {
        // Arrange
        OperatorContext operator = buildOperator(Collections.singletonList("*"));
        todoNoGenerator.enqueue(TODO_NO);

        // Act
        TodoCreateResult result = todoService.createTodo(buildRequest(REQUEST_ID, "整理周报", null), operator);

        // Assert
        assertThat(result.getTodoNo()).isEqualTo(TODO_NO);
    }

    @Test
    @DisplayName("登录态校验：操作人上下文缺失时返回 TODO_0005")
    void should_throwNotLogin_when_operatorIsNull() {
        // Arrange
        TodoCreateRequest request = buildRequest(REQUEST_ID, "整理周报", null);

        // Act & Assert
        assertThatThrownBy(() -> todoService.createTodo(request, null))
                .isInstanceOfSatisfying(TodoBizException.class,
                        e -> assertThat(e.getCode()).isEqualTo(TodoErrorCodeEnum.NOT_LOGIN.getCode()));
    }

    @Test
    @DisplayName("登录态校验：用户 ID 为空白时返回 TODO_0005")
    void should_throwNotLogin_when_userIdBlank() {
        // Arrange
        OperatorContext operator = new OperatorContext("  ", USER_NAME, TENANT_ID,
                Collections.singletonList("TODO_CREATE"));
        TodoCreateRequest request = buildRequest(REQUEST_ID, "整理周报", null);

        // Act & Assert
        assertThatThrownBy(() -> todoService.createTodo(request, operator))
                .isInstanceOfSatisfying(TodoBizException.class,
                        e -> assertThat(e.getCode()).isEqualTo(TodoErrorCodeEnum.NOT_LOGIN.getCode()));
    }

    @Test
    @DisplayName("入参校验：名称仅含空白字符时返回 TODO_0002")
    void should_throwItemNameRequired_when_nameIsBlank() {
        // Arrange
        OperatorContext operator = buildOperator(Collections.singletonList("TODO_CREATE"));
        TodoCreateRequest request = buildRequest(REQUEST_ID, "　  ", null);

        // Act & Assert
        assertThatThrownBy(() -> todoService.createTodo(request, operator))
                .isInstanceOfSatisfying(TodoBizException.class,
                        e -> assertThat(e.getCode()).isEqualTo(TodoErrorCodeEnum.ITEM_NAME_REQUIRED.getCode()));
    }

    @Test
    @DisplayName("限流：超出单用户每分钟上限返回 TODO_0009")
    void should_throwRateLimited_when_exceedsLimit() {
        // Arrange
        todoRateLimiter = new TodoRateLimiter(1, () -> 1_000L);
        todoService = new TodoServiceImpl(new TodoItemManager(todoItemMapper), new TodoValidator(),
                todoNoGenerator, todoRateLimiter, auditIntegration);
        OperatorContext operator = buildOperator(Collections.singletonList("TODO_CREATE"));
        todoNoGenerator.enqueue(TODO_NO);
        todoNoGenerator.enqueue("TD20261029000002");
        todoService.createTodo(buildRequest(REQUEST_ID, "整理周报", null), operator);
        TodoCreateRequest secondRequest = buildRequest("another-request", "整理周报", null);

        // Act & Assert
        assertThatThrownBy(() -> todoService.createTodo(secondRequest, operator))
                .isInstanceOfSatisfying(TodoBizException.class,
                        e -> assertThat(e.getCode()).isEqualTo(TodoErrorCodeEnum.RATE_LIMITED.getCode()));
    }

    @Test
    @DisplayName("审计降级：审计投递抛异常不影响创建结果")
    void should_stillSucceed_when_auditThrows() {
        // Arrange
        OperatorContext operator = buildOperator(Collections.singletonList("TODO_CREATE"));
        todoNoGenerator.enqueue(TODO_NO);
        auditIntegration.failure = new IllegalStateException("audit down");

        // Act
        TodoCreateResult result = todoService.createTodo(buildRequest(REQUEST_ID, "整理周报", null), operator);

        // Assert
        assertThat(result.getTodoNo()).isEqualTo(TODO_NO);
        assertThat(todoItemMapper.size()).isEqualTo(1);
        assertThat(auditIntegration.invokeCount).isEqualTo(1);
    }

    // ==================== queryByRequestId 测试 ====================

    @Test
    @DisplayName("幂等查询：命中返回首次结果并标记 idempotent")
    void should_returnExistingResult_when_requestIdExists() {
        // Arrange
        OperatorContext operator = buildOperator(Collections.singletonList("TODO_CREATE"));
        todoItemMapper.seed(buildExisting(TODO_NO, REQUEST_ID));

        // Act
        TodoCreateResult result = todoService.queryByRequestId(REQUEST_ID, operator);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTodoNo()).isEqualTo(TODO_NO);
        assertThat(result.isIdempotent()).isTrue();
    }

    @Test
    @DisplayName("幂等查询：未命中返回 null")
    void should_returnNull_when_requestIdNotExists() {
        // Arrange
        OperatorContext operator = buildOperator(Collections.singletonList("TODO_CREATE"));

        // Act
        TodoCreateResult result = todoService.queryByRequestId(REQUEST_ID, operator);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("幂等查询：请求标识为空时返回 TODO_0001")
    void should_throwParamInvalid_when_requestIdIsBlank() {
        // Arrange
        OperatorContext operator = buildOperator(Collections.singletonList("TODO_CREATE"));

        // Act & Assert
        assertThatThrownBy(() -> todoService.queryByRequestId(" ", operator))
                .isInstanceOfSatisfying(TodoBizException.class,
                        e -> assertThat(e.getCode()).isEqualTo(TodoErrorCodeEnum.PARAM_INVALID.getCode()));
    }

    // ==================== 测试数据构造 ====================

    private TodoCreateRequest buildRequest(String requestId, String itemName, String itemDesc) {
        TodoCreateRequest request = new TodoCreateRequest();
        request.setRequestId(requestId);
        request.setItemName(itemName);
        request.setItemDesc(itemDesc);
        return request;
    }

    private OperatorContext buildOperator(List<String> roles) {
        return new OperatorContext(USER_ID, USER_NAME, TENANT_ID, roles);
    }

    private TodoItemDO buildExisting(String todoNo, String requestId) {
        TodoItemDO entity = new TodoItemDO();
        entity.setId(1000L);
        entity.setTodoNo(todoNo);
        entity.setRequestId(requestId);
        entity.setTenantId(TENANT_ID);
        entity.setItemName("既有待办");
        entity.setItemDesc("");
        entity.setStatus(0);
        entity.setCreatorId("U10000");
        entity.setCreatorName("李四");
        entity.setSource("web");
        entity.setDeleted(0);
        entity.setGmtCreate(java.time.LocalDateTime.of(2026, 10, 29, 9, 0, 0));
        entity.setGmtModified(java.time.LocalDateTime.of(2026, 10, 29, 9, 0, 0));
        return entity;
    }

    /**
     * 内存版待办 Mapper 替身：支持幂等键唯一约束、编号唯一约束、查询未命中与异常注入。
     */
    private static final class FakeTodoItemMapper implements TodoItemMapper {

        private final Map<String, TodoItemDO> store = new LinkedHashMap<>();

        private final Deque<RuntimeException> failures = new ArrayDeque<>();

        private long idSequence = 1000L;

        private int insertCount;

        private boolean missSelectOnce;

        @Override
        public TodoItemDO selectByRequestId(TodoItemDO query) {
            if (missSelectOnce) {
                missSelectOnce = false;
                return null;
            }
            return store.get(query.getTenantId() + "|" + query.getRequestId());
        }

        @Override
        public int insert(TodoItemDO todoItem) {
            insertCount++;
            if (!failures.isEmpty()) {
                throw failures.poll();
            }
            String requestKey = todoItem.getTenantId() + "|" + todoItem.getRequestId();
            if (store.containsKey(requestKey)) {
                throw new DuplicateKeyException("Duplicate entry for key '"
                        + TodoDuplicateKeyException.UK_REQUEST_ID + "'");
            }
            for (TodoItemDO item : store.values()) {
                if (item.getTodoNo().equals(todoItem.getTodoNo())) {
                    throw new DuplicateKeyException("Duplicate entry for key '"
                            + TodoDuplicateKeyException.UK_TODO_NO + "'");
                }
            }
            todoItem.setId(++idSequence);
            store.put(requestKey, todoItem);
            return 1;
        }

        private void seed(TodoItemDO entity) {
            store.put(entity.getTenantId() + "|" + entity.getRequestId(), entity);
        }

        private void enqueueFailure(RuntimeException failure) {
            failures.add(failure);
        }

        private TodoItemDO first() {
            return store.values().iterator().next();
        }

        private int size() {
            return store.size();
        }
    }

    /**
     * 待办编号生成器替身：按预设队列返回编号。
     */
    private static final class FakeTodoNoGenerator implements TodoNoGenerator {

        private final Deque<String> numbers = new ArrayDeque<>();

        @Override
        public String nextTodoNo() {
            String number = numbers.poll();
            return number == null ? TODO_NO : number;
        }

        private void enqueue(String todoNo) {
            numbers.addLast(todoNo);
        }
    }

    /**
     * 审计投递替身：记录投递内容，支持注入失败。
     */
    private static final class FakeAuditIntegration implements AuditIntegration {

        private final List<String> records = new ArrayList<>();

        private RuntimeException failure;

        private int invokeCount;

        @Override
        public void recordCreate(String requestId, String todoNo, String operatorId, boolean success) {
            invokeCount++;
            if (failure != null) {
                throw failure;
            }
            records.add(requestId + "|" + todoNo + "|" + operatorId + "|" + success);
        }
    }
}
