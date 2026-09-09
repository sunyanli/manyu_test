package com.dtazzy.todo.service;

import com.dtazzy.todo.api.request.CreateTodoItemRequest;
import com.dtazzy.todo.common.exception.BusinessException;
import com.dtazzy.todo.dao.entity.TodoItemDO;
import com.dtazzy.todo.dao.mapper.TodoItemMapper;
import com.dtazzy.todo.service.impl.TodoItemServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TodoItemServiceImpl 单元测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TodoItemServiceImpl 单元测试")
class TodoItemServiceImplTest {

    @Mock
    private TodoItemMapper todoItemMapper;

    @InjectMocks
    private TodoItemServiceImpl todoItemService;

    private static final String TEST_TENANT_ID = "tenant-001";

    private CreateTodoItemRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new CreateTodoItemRequest();
        validRequest.setName("完成周报");
        validRequest.setDescription("整理本周工作内容并提交周报");
    }

    /**
     * 正常路径：合法请求应成功创建待办事项。
     */
    @Test
    @DisplayName("should create todo item when request is valid")
    void shouldCreateTodoItem_whenRequestIsValid() {
        when(todoItemMapper.insert(any(TodoItemDO.class))).thenReturn(1);

        TodoItemDO result = todoItemService.createTodoItem(TEST_TENANT_ID, validRequest);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("完成周报");
        assertThat(result.getDescription()).isEqualTo("整理本周工作内容并提交周报");
        assertThat(result.getTenantId()).isEqualTo(TEST_TENANT_ID);
        verify(todoItemMapper).insert(any(TodoItemDO.class));
    }

    /**
     * 参数校验：事项名称为 null 时抛出异常。
     */
    @Test
    @DisplayName("should throw exception when name is null")
    void shouldThrowException_whenNameIsNull() {
        validRequest.setName(null);

        assertThatThrownBy(() -> todoItemService.createTodoItem(TEST_TENANT_ID, validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("TODO_001");

        verify(todoItemMapper, never()).insert(any());
    }

    /**
     * 参数校验：事项名称为空字符串时抛出异常。
     */
    @Test
    @DisplayName("should throw exception when name is empty")
    void shouldThrowException_whenNameIsEmpty() {
        validRequest.setName("");

        assertThatThrownBy(() -> todoItemService.createTodoItem(TEST_TENANT_ID, validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("TODO_001");

        verify(todoItemMapper, never()).insert(any());
    }

    /**
     * 参数校验：事项名称为全空格时抛出异常。
     */
    @Test
    @DisplayName("should throw exception when name is blank")
    void shouldThrowException_whenNameIsBlank() {
        validRequest.setName("   ");

        assertThatThrownBy(() -> todoItemService.createTodoItem(TEST_TENANT_ID, validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("TODO_001");

        verify(todoItemMapper, never()).insert(any());
    }

    /**
     * 参数校验：事项名称超过 200 字符时抛出异常。
     */
    @Test
    @DisplayName("should throw exception when name exceeds max length")
    void shouldThrowException_whenNameExceedsMaxLength() {
        validRequest.setName("A".repeat(201));

        assertThatThrownBy(() -> todoItemService.createTodoItem(TEST_TENANT_ID, validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("TODO_002");

        verify(todoItemMapper, never()).insert(any());
    }

    /**
     * 边界值：事项名称恰好 200 字符时应成功。
     */
    @Test
    @DisplayName("should succeed when name is exactly max length")
    void shouldSucceed_whenNameIsExactlyMaxLength() {
        validRequest.setName("A".repeat(200));
        when(todoItemMapper.insert(any(TodoItemDO.class))).thenReturn(1);

        TodoItemDO result = todoItemService.createTodoItem(TEST_TENANT_ID, validRequest);

        assertThat(result).isNotNull();
        assertThat(result.getName()).hasSize(200);
        verify(todoItemMapper).insert(any(TodoItemDO.class));
    }

    /**
     * 参数校验：事项描述超过 2000 字符时抛出异常。
     */
    @Test
    @DisplayName("should throw exception when description exceeds max length")
    void shouldThrowException_whenDescriptionExceedsMaxLength() {
        validRequest.setDescription("B".repeat(2001));

        assertThatThrownBy(() -> todoItemService.createTodoItem(TEST_TENANT_ID, validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("TODO_003");

        verify(todoItemMapper, never()).insert(any());
    }

    /**
     * 边界值：事项描述恰好 2000 字符时应成功。
     */
    @Test
    @DisplayName("should succeed when description is exactly max length")
    void shouldSucceed_whenDescriptionIsExactlyMaxLength() {
        validRequest.setDescription("B".repeat(2000));
        when(todoItemMapper.insert(any(TodoItemDO.class))).thenReturn(1);

        TodoItemDO result = todoItemService.createTodoItem(TEST_TENANT_ID, validRequest);

        assertThat(result).isNotNull();
        assertThat(result.getDescription()).hasSize(2000);
        verify(todoItemMapper).insert(any(TodoItemDO.class));
    }

    /**
     * 边界值：描述为 null 时应成功（允许空描述）。
     */
    @Test
    @DisplayName("should succeed when description is null")
    void shouldSucceed_whenDescriptionIsNull() {
        validRequest.setDescription(null);
        when(todoItemMapper.insert(any(TodoItemDO.class))).thenReturn(1);

        TodoItemDO result = todoItemService.createTodoItem(TEST_TENANT_ID, validRequest);

        assertThat(result).isNotNull();
        assertThat(result.getDescription()).isNull();
        verify(todoItemMapper).insert(any(TodoItemDO.class));
    }

    /**
     * 异常场景：数据库插入失败时抛出系统异常。
     */
    @Test
    @DisplayName("should throw system exception when database insert fails")
    void shouldThrowSystemException_whenDatabaseInsertFails() {
        when(todoItemMapper.insert(any(TodoItemDO.class)))
                .thenThrow(new RuntimeException("DB connection error"));

        assertThatThrownBy(() -> todoItemService.createTodoItem(TEST_TENANT_ID, validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("TODO_004");
    }
}