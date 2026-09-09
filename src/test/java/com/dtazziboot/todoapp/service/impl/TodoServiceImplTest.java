package com.dtazziboot.todoapp.service.impl;

import com.dtazziboot.todoapp.common.context.LoginContext;
import com.dtazziboot.todoapp.common.enums.ErrorCodeEnum;
import com.dtazziboot.todoapp.common.exception.BusinessException;
import com.dtazziboot.todoapp.dao.mapper.TodoItemMapper;
import com.dtazziboot.todoapp.model.dto.TodoCreateRequest;
import com.dtazziboot.todoapp.model.dto.TodoCreateResult;
import com.dtazziboot.todoapp.model.entity.TodoItemDO;
import com.dtazziboot.todoapp.service.TodoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TodoServiceImpl 单元测试
 *
 * @author AiWork
 * @date 2026/09/09
 */
@DisplayName("TodoServiceImpl 单元测试")
@ExtendWith(MockitoExtension.class)
class TodoServiceImplTest {

    @Mock
    private TodoItemMapper todoItemMapper;

    @InjectMocks
    private TodoServiceImpl todoService;

    @Captor
    private ArgumentCaptor<TodoItemDO> todoItemCaptor;

    @BeforeEach
    void setUp() {
        // 设置登录上下文
        LoginContext.set("user_001", "tenant_a");
    }

    @AfterEach
    void tearDown() {
        // 清理线程变量，防止内存泄漏
        LoginContext.clear();
    }

    @Nested
    @DisplayName("正常路径")
    class NormalPath {

        @Test
        @DisplayName("should_returnCreateResult_when_validRequest")
        void should_returnCreateResult_when_validRequest() {
            // given
            TodoCreateRequest request = new TodoCreateRequest();
            request.setTitle("完成日报");
            request.setDescription("编写并提交每日工作日报");

            when(todoItemMapper.insert(any(TodoItemDO.class))).thenReturn(1);

            // when
            TodoCreateResult result = todoService.createTodo(request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("完成日报");
            assertThat(result.getDescription()).isEqualTo("编写并提交每日工作日报");
            assertThat(result.getCreatorId()).isEqualTo("user_001");
            assertThat(result.getTenantId()).isEqualTo("tenant_a");
            assertThat(result.getGmtCreate()).isNotNull();
            assertThat(result.getGmtModified()).isNotNull();

            verify(todoItemMapper).insert(todoItemCaptor.capture());
            TodoItemDO captured = todoItemCaptor.getValue();
            assertThat(captured.getTitle()).isEqualTo("完成日报");
            assertThat(captured.getDescription()).isEqualTo("编写并提交每日工作日报");
            assertThat(captured.getCreatorId()).isEqualTo("user_001");
            assertThat(captured.getTenantId()).isEqualTo("tenant_a");
            assertThat(captured.getGmtCreate()).isNotNull();
            assertThat(captured.getGmtModified()).isNotNull();
        }

        @Test
        @DisplayName("should_createTodo_without_description")
        void should_createTodo_without_description() {
            // given
            TodoCreateRequest request = new TodoCreateRequest();
            request.setTitle("简单任务");

            when(todoItemMapper.insert(any(TodoItemDO.class))).thenReturn(1);

            // when
            TodoCreateResult result = todoService.createTodo(request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("简单任务");
            assertThat(result.getDescription()).isNull();
        }
    }

    @Nested
    @DisplayName("参数校验异常")
    class ValidationException {

        @Test
        @DisplayName("should_throwBusinessException_when_creatorIdIsNull")
        void should_throwBusinessException_when_creatorIdIsNull() {
            // given
            LoginContext.clear();
            TodoCreateRequest request = new TodoCreateRequest();
            request.setTitle("任务");

            // when / then
            assertThatThrownBy(() -> todoService.createTodo(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCodeEnum.TODO_004));
        }

        @Test
        @DisplayName("should_throwBusinessException_when_tenantIdIsNull")
        void should_throwBusinessException_when_tenantIdIsNull() {
            // given
            LoginContext.set("user_001", null);
            TodoCreateRequest request = new TodoCreateRequest();
            request.setTitle("任务");

            // when / then
            assertThatThrownBy(() -> todoService.createTodo(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCodeEnum.TODO_004));
        }
    }

    @Nested
    @DisplayName("数据库异常")
    class DatabaseException {

        @Test
        @DisplayName("should_throwBusinessException_when_insertFails")
        void should_throwBusinessException_when_insertFails() {
            // given
            TodoCreateRequest request = new TodoCreateRequest();
            request.setTitle("任务");

            when(todoItemMapper.insert(any(TodoItemDO.class))).thenReturn(0);

            // when / then
            assertThatThrownBy(() -> todoService.createTodo(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCodeEnum.TODO_005));
        }
    }
}