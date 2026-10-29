package com.antdigital.todo.item.service.impl;

import com.antdigital.todo.common.api.ResultCode;
import com.antdigital.todo.common.context.OperatorContext;
import com.antdigital.todo.common.exception.BizException;
import com.antdigital.todo.item.mapper.TodoItemMapper;
import com.antdigital.todo.item.model.CreateTodoRequest;
import com.antdigital.todo.item.model.CreateTodoResponse;
import com.antdigital.todo.item.model.TodoItemDO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link TodoItemServiceImpl} 单元测试。
 *
 * @author AiWork
 */
@ExtendWith(MockitoExtension.class)
class TodoItemServiceImplTest {

    private static final String USER_ID = "U1001";

    private static final String TENANT_ID = "T01";

    private static final String USER_NAME = "张三";

    private static final long GENERATED_ID = 102401L;

    @Mock
    private TodoItemMapper todoItemMapper;

    @InjectMocks
    private TodoItemServiceImpl todoItemService;

    // ==================== createTodo 测试 ====================

    @Test
    @DisplayName("正常路径：合法入参落库成功并返回新建记录")
    void should_returnCreatedTodo_when_requestIsValid() {
        // Arrange
        CreateTodoRequest request = buildRequest("  整理季度汇报材料  ", " 汇总三条业务线数据 ");
        when(todoItemMapper.insert(any(TodoItemDO.class))).thenAnswer(invocation -> {
            TodoItemDO record = invocation.getArgument(0);
            record.setId(GENERATED_ID);
            return 1;
        });

        // Act
        CreateTodoResponse response = todoItemService.createTodo(request, buildOperator());

        // Assert
        assertThat(response.getId()).isEqualTo(GENERATED_ID);
        assertThat(response.getTitle()).isEqualTo("整理季度汇报材料");
        assertThat(response.getStatus()).isEqualTo(0);
        assertThat(response.getGmtCreate()).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");

        ArgumentCaptor<TodoItemDO> captor = ArgumentCaptor.forClass(TodoItemDO.class);
        verify(todoItemMapper, times(1)).insert(captor.capture());
        TodoItemDO saved = captor.getValue();
        assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(saved.getCreatorId()).isEqualTo(USER_ID);
        assertThat(saved.getCreatorName()).isEqualTo(USER_NAME);
        assertThat(saved.getTitle()).isEqualTo("整理季度汇报材料");
        assertThat(saved.getDescription()).isEqualTo("汇总三条业务线数据");
        assertThat(saved.getStatus()).isEqualTo(0);
        assertThat(saved.getGmtCreate()).isNotNull();
        assertThat(saved.getGmtModified()).isNotNull();
    }

    @Test
    @DisplayName("描述缺省：按空串落库，不落 NULL")
    void should_storeEmptyDescription_when_descriptionIsNull() {
        // Arrange
        CreateTodoRequest request = buildRequest("买咖啡豆", null);
        when(todoItemMapper.insert(any(TodoItemDO.class))).thenAnswer(invocation -> {
            TodoItemDO record = invocation.getArgument(0);
            record.setId(GENERATED_ID);
            return 1;
        });

        // Act
        todoItemService.createTodo(request, buildOperator());

        // Assert
        ArgumentCaptor<TodoItemDO> captor = ArgumentCaptor.forClass(TodoItemDO.class);
        verify(todoItemMapper).insert(captor.capture());
        assertThat(captor.getValue().getDescription()).isEmpty();
    }

    @Test
    @DisplayName("名称全空白：判定为空并抛 TODO_001")
    void should_throwTodo001_when_titleIsBlank() {
        // Arrange
        CreateTodoRequest request = buildRequest("　 　", "描述");

        // Act & Assert
        assertThatThrownBy(() -> todoItemService.createTodo(request, buildOperator()))
                .isInstanceOf(BizException.class)
                .hasMessage("事项名称不能为空")
                .extracting(ex -> ((BizException) ex).getResultCode())
                .isEqualTo(ResultCode.TODO_001);
        verify(todoItemMapper, never()).insert(any(TodoItemDO.class));
    }

    @Test
    @DisplayName("名称超长（101 字符）：抛 TODO_001 且提示具体字段")
    void should_throwTodo001_when_titleTooLong() {
        // Arrange
        CreateTodoRequest request = buildRequest(repeat('a', 101), null);

        // Act & Assert
        assertThatThrownBy(() -> todoItemService.createTodo(request, buildOperator()))
                .isInstanceOf(BizException.class)
                .hasMessage("事项名称不能超过100个字符");
        verify(todoItemMapper, never()).insert(any(TodoItemDO.class));
    }

    @Test
    @DisplayName("名称边界（100 字符）：创建成功")
    void should_createTodo_when_titleLengthIsExactly100() {
        // Arrange
        CreateTodoRequest request = buildRequest(repeat('a', 100), null);
        when(todoItemMapper.insert(any(TodoItemDO.class))).thenAnswer(invocation -> {
            TodoItemDO record = invocation.getArgument(0);
            record.setId(GENERATED_ID);
            return 1;
        });

        // Act
        CreateTodoResponse response = todoItemService.createTodo(request, buildOperator());

        // Assert
        assertThat(response.getTitle()).hasSize(100);
    }

    @Test
    @DisplayName("描述超长（501 字符）：抛 TODO_001")
    void should_throwTodo001_when_descriptionTooLong() {
        // Arrange
        CreateTodoRequest request = buildRequest("正常名称", repeat('b', 501));

        // Act & Assert
        assertThatThrownBy(() -> todoItemService.createTodo(request, buildOperator()))
                .isInstanceOf(BizException.class)
                .hasMessage("事项描述不能超过500个字符");
        verify(todoItemMapper, never()).insert(any(TodoItemDO.class));
    }

    @Test
    @DisplayName("文本含控制字符：抛 TODO_005")
    void should_throwTodo005_when_textContainsControlChar() {
        // Arrange
        CreateTodoRequest request = buildRequest("正常名称", "非法\u0000字符");

        // Act & Assert
        assertThatThrownBy(() -> todoItemService.createTodo(request, buildOperator()))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getResultCode())
                .isEqualTo(ResultCode.TODO_005);
    }

    @Test
    @DisplayName("创建人展示名缺失：以用户 ID 兜底")
    void should_useUserIdAsCreatorName_when_userNameIsBlank() {
        // Arrange
        CreateTodoRequest request = buildRequest("待办A", null);
        OperatorContext operator = new OperatorContext(USER_ID, "  ", TENANT_ID);
        when(todoItemMapper.insert(any(TodoItemDO.class))).thenAnswer(invocation -> {
            TodoItemDO record = invocation.getArgument(0);
            record.setId(GENERATED_ID);
            return 1;
        });

        // Act
        todoItemService.createTodo(request, operator);

        // Assert
        ArgumentCaptor<TodoItemDO> captor = ArgumentCaptor.forClass(TodoItemDO.class);
        verify(todoItemMapper).insert(captor.capture());
        assertThat(captor.getValue().getCreatorName()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("落库影响行数不为 1：抛 TODO_004")
    void should_throwTodo004_when_affectedRowsNotOne() {
        // Arrange
        CreateTodoRequest request = buildRequest("待办B", null);
        when(todoItemMapper.insert(any(TodoItemDO.class))).thenReturn(0);

        // Act & Assert
        assertThatThrownBy(() -> todoItemService.createTodo(request, buildOperator()))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getResultCode())
                .isEqualTo(ResultCode.TODO_004);
    }

    @Test
    @DisplayName("操作人上下文缺失：抛 TODO_001，不落库")
    void should_throwTodo001_when_operatorIsNull() {
        // Arrange
        CreateTodoRequest request = buildRequest("待办C", null);

        // Act & Assert
        assertThatThrownBy(() -> todoItemService.createTodo(request, null))
                .isInstanceOf(BizException.class)
                .extracting(ex -> ((BizException) ex).getResultCode())
                .isEqualTo(ResultCode.TODO_001);
        verify(todoItemMapper, never()).insert(any(TodoItemDO.class));
    }

    private CreateTodoRequest buildRequest(String title, String description) {
        CreateTodoRequest request = new CreateTodoRequest();
        request.setTitle(title);
        request.setDescription(description);
        return request;
    }

    private OperatorContext buildOperator() {
        return new OperatorContext(USER_ID, USER_NAME, TENANT_ID);
    }

    private String repeat(char value, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }
}
