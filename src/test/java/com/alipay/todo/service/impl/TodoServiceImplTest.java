package com.alipay.todo.service.impl;

import com.alipay.todo.common.constant.SecurityContextHolder;
import com.alipay.todo.common.constant.TodoConstants;
import com.alipay.todo.common.enums.TodoStatusEnum;
import com.alipay.todo.common.exception.TodoException;
import com.alipay.todo.dao.mapper.TodoItemMapper;
import com.alipay.todo.model.dto.TodoCreateRequest;
import com.alipay.todo.model.dto.TodoItemDTO;
import com.alipay.todo.model.entity.TodoItemDO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("TodoServiceImpl 单元测试")
@ExtendWith(MockitoExtension.class)
class TodoServiceImplTest {

    @Mock
    private TodoItemMapper todoItemMapper;

    @InjectMocks
    private TodoServiceImpl todoService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setCreator("testUser001");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    // ==================== create 测试 ====================

    @Nested
    @DisplayName("正常路径")
    class CreateSuccess {

        @Test
        @DisplayName("should_returnTodoItemDTO_when_validRequest")
        void should_returnTodoItemDTO_when_validRequest() {
            // Arrange
            TodoCreateRequest request = new TodoCreateRequest();
            request.setName("准备周报");
            request.setDescription("整理本周项目进展与风险点");

            TodoItemDO savedEntity = new TodoItemDO();
            savedEntity.setId(1001L);
            savedEntity.setTenantId(TodoConstants.DEFAULT_TENANT_ID);
            savedEntity.setName("准备周报");
            savedEntity.setDescription("整理本周项目进展与风险点");
            savedEntity.setStatus(TodoStatusEnum.INIT.getCode());
            savedEntity.setCreator("testUser001");
            savedEntity.setIsDeleted(0);
            savedEntity.setGmtCreate(LocalDateTime.now());
            savedEntity.setGmtModified(LocalDateTime.now());

            when(todoItemMapper.insert(any(TodoItemDO.class))).thenReturn(1);
            when(todoItemMapper.selectById(1001L)).thenReturn(savedEntity);

            // Act
            TodoItemDTO result = todoService.create(request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1001L);
            assertThat(result.getName()).isEqualTo("准备周报");
            assertThat(result.getDescription()).isEqualTo("整理本周项目进展与风险点");
            assertThat(result.getStatus()).isEqualTo(TodoStatusEnum.INIT.getCode());
            assertThat(result.getCreator()).isEqualTo("testUser001");
            assertThat(result.getGmtCreate()).isNotNull();

            verify(todoItemMapper, times(1)).insert(any(TodoItemDO.class));
        }

        @Test
        @DisplayName("should_setInitStatus_when_createSuccess")
        void should_setInitStatus_when_createSuccess() {
            // Arrange
            TodoCreateRequest request = new TodoCreateRequest();
            request.setName("测试事项");

            TodoItemDO savedEntity = new TodoItemDO();
            savedEntity.setId(1L);
            savedEntity.setName("测试事项");
            savedEntity.setStatus(TodoStatusEnum.INIT.getCode());
            savedEntity.setCreator("testUser001");
            savedEntity.setIsDeleted(0);
            savedEntity.setGmtCreate(LocalDateTime.now());
            savedEntity.setGmtModified(LocalDateTime.now());

            when(todoItemMapper.insert(any(TodoItemDO.class))).thenReturn(1);
            when(todoItemMapper.selectById(1L)).thenReturn(savedEntity);

            // Act
            TodoItemDTO result = todoService.create(request);

            // Assert
            assertThat(result.getStatus()).isEqualTo(TodoStatusEnum.INIT.getCode());
        }

        @Test
        @DisplayName("should_trimName_when_nameHasLeadingTrailingSpaces")
        void should_trimName_when_nameHasLeadingTrailingSpaces() {
            // Arrange
            TodoCreateRequest request = new TodoCreateRequest();
            request.setName("  测试事项  ");

            TodoItemDO savedEntity = new TodoItemDO();
            savedEntity.setId(1L);
            savedEntity.setName("测试事项");
            savedEntity.setStatus(TodoStatusEnum.INIT.getCode());
            savedEntity.setCreator("testUser001");
            savedEntity.setIsDeleted(0);
            savedEntity.setGmtCreate(LocalDateTime.now());
            savedEntity.setGmtModified(LocalDateTime.now());

            when(todoItemMapper.insert(any(TodoItemDO.class))).thenReturn(1);
            when(todoItemMapper.selectById(1L)).thenReturn(savedEntity);

            // Act
            TodoItemDTO result = todoService.create(request);

            // Assert
            assertThat(result.getName()).isEqualTo("测试事项");
        }

        @Test
        @DisplayName("should_setEmptyDescription_when_descriptionIsNull")
        void should_setEmptyDescription_when_descriptionIsNull() {
            // Arrange
            TodoCreateRequest request = new TodoCreateRequest();
            request.setName("测试事项");
            request.setDescription(null);

            TodoItemDO savedEntity = new TodoItemDO();
            savedEntity.setId(1L);
            savedEntity.setName("测试事项");
            savedEntity.setDescription("");
            savedEntity.setStatus(TodoStatusEnum.INIT.getCode());
            savedEntity.setCreator("testUser001");
            savedEntity.setIsDeleted(0);
            savedEntity.setGmtCreate(LocalDateTime.now());
            savedEntity.setGmtModified(LocalDateTime.now());

            when(todoItemMapper.insert(any(TodoItemDO.class))).thenReturn(1);
            when(todoItemMapper.selectById(1L)).thenReturn(savedEntity);

            // Act
            TodoItemDTO result = todoService.create(request);

            // Assert
            assertThat(result.getDescription()).isEqualTo("");
        }
    }

    // ==================== 参数校验异常测试 ====================

    @Nested
    @DisplayName("参数校验异常")
    class CreateValidationFailure {

        @Test
        @DisplayName("should_throwTodoException_when_nameIsBlank")
        void should_throwTodoException_when_nameIsBlank() {
            // Arrange
            TodoCreateRequest request = new TodoCreateRequest();
            request.setName("   ");

            // Act & Assert
            assertThatThrownBy(() -> todoService.create(request))
                    .isInstanceOf(TodoException.class)
                    .satisfies(e -> {
                        assertThat(((TodoException) e).getErrorCode()).isEqualTo("TODO_001");
                        assertThat(e.getMessage()).contains("事项名称必填");
                    });

            verify(todoItemMapper, never()).insert(any());
        }

        @Test
        @DisplayName("should_throwTodoException_when_nameIsNull")
        void should_throwTodoException_when_nameIsNull() {
            // Arrange
            TodoCreateRequest request = new TodoCreateRequest();
            request.setName(null);

            // Act & Assert
            assertThatThrownBy(() -> todoService.create(request))
                    .isInstanceOf(TodoException.class)
                    .satisfies(e -> {
                        assertThat(((TodoException) e).getErrorCode()).isEqualTo("TODO_001");
                        assertThat(e.getMessage()).contains("事项名称必填");
                    });

            verify(todoItemMapper, never()).insert(any());
        }

        @Test
        @DisplayName("should_throwTodoException_when_nameExceedsMaxLength")
        void should_throwTodoException_when_nameExceedsMaxLength() {
            // Arrange
            TodoCreateRequest request = new TodoCreateRequest();
            request.setName("a".repeat(65));

            // Act & Assert
            assertThatThrownBy(() -> todoService.create(request))
                    .isInstanceOf(TodoException.class)
                    .satisfies(e -> {
                        assertThat(((TodoException) e).getErrorCode()).isEqualTo("TODO_001");
                        assertThat(e.getMessage()).contains("不超过64字");
                    });

            verify(todoItemMapper, never()).insert(any());
        }

        @Test
        @DisplayName("should_throwTodoException_when_descriptionExceedsMaxLength")
        void should_throwTodoException_when_descriptionExceedsMaxLength() {
            // Arrange
            TodoCreateRequest request = new TodoCreateRequest();
            request.setName("测试事项");
            request.setDescription("b".repeat(513));

            // Act & Assert
            assertThatThrownBy(() -> todoService.create(request))
                    .isInstanceOf(TodoException.class)
                    .satisfies(e -> {
                        assertThat(((TodoException) e).getErrorCode()).isEqualTo("TODO_001");
                        assertThat(e.getMessage()).contains("不超过512字");
                    });

            verify(todoItemMapper, never()).insert(any());
        }
    }

    // ==================== 登录态校验异常测试 ====================

    @Nested
    @DisplayName("登录态校验异常")
    class CreateAuthFailure {

        @Test
        @DisplayName("should_throwTodoException_when_notLoggedIn")
        void should_throwTodoException_when_notLoggedIn() {
            // Arrange
            SecurityContextHolder.clear();
            TodoCreateRequest request = new TodoCreateRequest();
            request.setName("测试事项");

            // Act & Assert
            assertThatThrownBy(() -> todoService.create(request))
                    .isInstanceOf(TodoException.class)
                    .satisfies(e -> {
                        assertThat(((TodoException) e).getErrorCode()).isEqualTo("TODO_002");
                        assertThat(e.getMessage()).contains("请先登录");
                    });

            verify(todoItemMapper, never()).insert(any());
        }
    }

    // ==================== 系统异常测试 ====================

    @Nested
    @DisplayName("系统异常")
    class CreateSystemError {

        @Test
        @DisplayName("should_throwTodoException_when_insertFails")
        void should_throwTodoException_when_insertFails() {
            // Arrange
            TodoCreateRequest request = new TodoCreateRequest();
            request.setName("测试事项");

            when(todoItemMapper.insert(any(TodoItemDO.class))).thenReturn(0);

            // Act & Assert
            assertThatThrownBy(() -> todoService.create(request))
                    .isInstanceOf(TodoException.class)
                    .satisfies(e -> {
                        assertThat(((TodoException) e).getErrorCode()).isEqualTo("TODO_003");
                        assertThat(e.getMessage()).contains("系统异常");
                    });
        }
    }
}