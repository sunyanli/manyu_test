package com.manyu.todo.service.impl;

import com.manyu.todo.common.exception.BizException;
import com.manyu.todo.dao.mapper.TodoMapper;
import com.manyu.todo.model.dto.TodoCreateRequest;
import com.manyu.todo.model.entity.TodoDO;
import com.manyu.todo.model.vo.TodoVO;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * TodoServiceImpl 单元测试。
 *
 * @author AiWork
 */
@ExtendWith(MockitoExtension.class)
class TodoServiceImplTest {

    @Mock
    private TodoMapper todoMapper;

    @InjectMocks
    private TodoServiceImpl todoService;

    // ==================== createTodo 测试 ====================

    @Test
    void should_returnTodoVO_when_requestIsValid() {
        // Arrange (Given)
        TodoCreateRequest request = new TodoCreateRequest();
        request.setName("  买牛奶  ");
        request.setDescription("每周两箱");
        Mockito.when(todoMapper.insert(Mockito.any(TodoDO.class))).thenAnswer(invocation -> {
            TodoDO todo = invocation.getArgument(0);
            todo.setId(100L);
            return 1;
        });

        // Act (When)
        TodoVO vo = todoService.createTodo(request);

        // Assert (Then)
        Assertions.assertThat(vo.getId()).isEqualTo(100L);
        Assertions.assertThat(vo.getName()).isEqualTo("买牛奶");
        Assertions.assertThat(vo.getDescription()).isEqualTo("每周两箱");

        ArgumentCaptor<TodoDO> captor = ArgumentCaptor.forClass(TodoDO.class);
        Mockito.verify(todoMapper, Mockito.times(1)).insert(captor.capture());
        Assertions.assertThat(captor.getValue().getName()).isEqualTo("买牛奶");
        Assertions.assertThat(captor.getValue().getDescription()).isEqualTo("每周两箱");
    }

    @Test
    void should_throwException_when_requestIsNull() {
        // Act & Assert (When/Then)
        Assertions.assertThatThrownBy(() -> todoService.createTodo(null))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("请求参数不能为空");
        Mockito.verifyNoInteractions(todoMapper);
    }

    @Test
    void should_throwException_when_nameIsBlank() {
        // Arrange (Given)
        TodoCreateRequest request = new TodoCreateRequest();
        request.setName("   ");

        // Act & Assert (When/Then)
        Assertions.assertThatThrownBy(() -> todoService.createTodo(request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("事项名称不能为空");
        Mockito.verifyNoInteractions(todoMapper);
    }

    @Test
    void should_throwException_when_nameExceedsMaxLength() {
        // Arrange (Given)
        TodoCreateRequest request = new TodoCreateRequest();
        request.setName(repeat("名", 65));

        // Act & Assert (When/Then)
        Assertions.assertThatThrownBy(() -> todoService.createTodo(request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("事项名称不能超过64个字符");
        Mockito.verifyNoInteractions(todoMapper);
    }

    @Test
    void should_throwException_when_descriptionExceedsMaxLength() {
        // Arrange (Given)
        TodoCreateRequest request = new TodoCreateRequest();
        request.setName("正常名称");
        request.setDescription(repeat("描", 513));

        // Act & Assert (When/Then)
        Assertions.assertThatThrownBy(() -> todoService.createTodo(request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("事项描述不能超过512个字符");
        Mockito.verifyNoInteractions(todoMapper);
    }

    @Test
    void should_throwException_when_mapperInsertFails() {
        // Arrange (Given)
        TodoCreateRequest request = new TodoCreateRequest();
        request.setName("买牛奶");
        Mockito.when(todoMapper.insert(Mockito.any(TodoDO.class))).thenReturn(0);

        // Act & Assert (When/Then)
        Assertions.assertThatThrownBy(() -> todoService.createTodo(request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("待办事项保存失败");
    }

    private String repeat(String s, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(s);
        }
        return sb.toString();
    }
}
