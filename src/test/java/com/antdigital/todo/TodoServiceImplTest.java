package com.antdigital.todo;

import com.antdigital.todo.common.exception.BizException;
import com.antdigital.todo.common.result.Result;
import com.antdigital.todo.controller.TodoController;
import com.antdigital.todo.model.dto.CreateTodoRequest;
import com.antdigital.todo.model.dto.TodoVO;
import com.antdigital.todo.service.TodoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.ArgumentCaptor;

/**
 * TodoServiceImpl 单元测试
 *
 * @author AiWork
 * @since 2026-09-09
 */
@ExtendWith(MockitoExtension.class)
class TodoServiceImplTest {

    @Mock
    private TodoItemMapper todoItemMapper;

    @InjectMocks
    private TodoServiceImpl todoService;

    private CreateTodoRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new CreateTodoRequest();
        validRequest.setTitle("完成日报");
        validRequest.setDescription("编写并提交每日工作日报");
    }

    @Test
    void should_returnTodoVo_when_validRequest() {
        // Arrange
        TodoItemDO savedItem = new TodoItemDO();
        savedItem.setId(1L);
        savedItem.setTitle("完成日报");
        when(todoItemMapper.insert(any(TodoItemDO.class))).thenReturn(1);

        // Act
        TodoVO result = todoService.createTodo(validRequest, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getTodoId());
        verify(todoItemMapper, times(1)).insert(any(TodoItemDO.class));
    }

    @Test
    void should_throwBizException_when_titleIsBlank() {
        // Arrange
        CreateTodoRequest request = new CreateTodoRequest();
        request.setTitle("   ");

        // Act & Assert
        BizException exception = assertThrows(BizException.class, () -> todoService.createTodo(request, 1L));
        assertEquals("TODO_001", exception.getErrorCode());
        assertEquals("事项名称不能为空", exception.getMessage());
        verify(todoItemMapper, never()).insert(any());
    }

    @Test
    void should_throwBizException_when_titleIsNull() {
        // Arrange
        CreateTodoRequest request = new CreateTodoRequest();
        request.setTitle(null);

        // Act & Assert
        BizException exception = assertThrows(BizException.class, () -> todoService.createTodo(request, 1L));
        assertEquals("TODO_001", exception.getErrorCode());
        verify(todoItemMapper, never()).insert(any());
    }

    @Test
    void should_throwBizException_when_titleTooLong() {
        // Arrange
        CreateTodoRequest request = new CreateTodoRequest();
        request.setTitle("a".repeat(201));

        // Act & Assert
        BizException exception = assertThrows(BizException.class, () -> todoService.createTodo(request, 1L));
        assertEquals("TODO_002", exception.getErrorCode());
        assertEquals("事项名称长度不能超过200字符", exception.getMessage());
        verify(todoItemMapper, never()).insert(any());
    }

    @Test
    void should_throwBizException_when_descriptionTooLong() {
        // Arrange
        CreateTodoRequest request = new CreateTodoRequest();
        request.setTitle("正常标题");
        request.setDescription("b".repeat(2001));

        // Act & Assert
        BizException exception = assertThrows(BizException.class, () -> todoService.createTodo(request, 1L));
        assertEquals("TODO_003", exception.getErrorCode());
        assertEquals("事项描述长度不能超过2000字符", exception.getMessage());
        verify(todoItemMapper, never()).insert(any());
    }

    @Test
    void should_allowDescriptionNull() {
        // Arrange
        CreateTodoRequest request = new CreateTodoRequest();
        request.setTitle("无描述事项");
        request.setDescription(null);
        TodoItemDO savedItem = new TodoItemDO();
        savedItem.setId(2L);
        when(todoItemMapper.insert(any(TodoItemDO.class))).thenReturn(1);

        // Act
        TodoVO result = todoService.createTodo(request, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(2L, result.getTodoId());
        verify(todoItemMapper, times(1)).insert(any(TodoItemDO.class));
    }

    @Test
    void should_setGmtCreateAndGmtModified_onInsert() {
        // Arrange
        when(todoItemMapper.insert(any(TodoItemDO.class))).thenAnswer(invocation -> {
            TodoItemDO item = invocation.getArgument(0);
            item.setId(3L);
            return 1;
        });

        // Act
        TodoVO result = todoService.createTodo(validRequest, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(3L, result.getTodoId());
        ArgumentCaptor<TodoItemDO> captor = ArgumentCaptor.forClass(TodoItemDO.class);
        verify(todoItemMapper, times(1)).insert(captor.capture());
        TodoItemDO captured = captor.getValue();
        assertNotNull(captured.getGmtCreate());
        assertNotNull(captured.getGmtModified());
    }
}
