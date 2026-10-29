package com.antdigital.todo.dao.manager;

import com.antdigital.todo.common.exception.TodoDuplicateKeyException;
import com.antdigital.todo.dao.entity.TodoItemDO;
import com.antdigital.todo.dao.mapper.TodoItemMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link TodoItemManager} 唯一键冲突转换单元测试。
 *
 * @author AiWork
 * @date 2026/10/29
 */
class TodoItemManagerTest {

    @Test
    @DisplayName("幂等键冲突：转换为业务异常并识别索引名")
    void should_translateDuplicateKey_when_requestIdConflict() {
        // Arrange
        TodoItemManager manager = new TodoItemManager(
                buildMapper("Duplicate entry 'req-1' for key 'uk_todo_item_req'"));
        TodoItemDO entity = new TodoItemDO();
        entity.setTodoNo("TD20261029000001");

        // Act & Assert
        assertThatThrownBy(() -> manager.insert(entity))
                .isInstanceOfSatisfying(TodoDuplicateKeyException.class,
                        e -> assertThat(e.getConstraintName())
                                .isEqualTo(TodoDuplicateKeyException.UK_REQUEST_ID));
    }

    @Test
    @DisplayName("编号冲突：转换为业务异常并识别索引名")
    void should_translateDuplicateKey_when_todoNoConflict() {
        // Arrange
        TodoItemManager manager = new TodoItemManager(
                buildMapper("Duplicate entry 'TD20261029000001' for key 'uk_todo_item_no'"));

        // Act & Assert
        assertThatThrownBy(() -> manager.insert(new TodoItemDO()))
                .isInstanceOfSatisfying(TodoDuplicateKeyException.class,
                        e -> assertThat(e.getConstraintName())
                                .isEqualTo(TodoDuplicateKeyException.UK_TODO_NO));
    }

    @Test
    @DisplayName("无法识别索引名的冲突：索引名为空串，由业务层按未知冲突处理")
    void should_returnEmptyConstraint_when_indexUnknown() {
        // Arrange
        TodoItemManager manager = new TodoItemManager(buildMapper("Duplicate entry for key 'other_uk'"));

        // Act & Assert
        assertThatThrownBy(() -> manager.insert(new TodoItemDO()))
                .isInstanceOfSatisfying(TodoDuplicateKeyException.class,
                        e -> assertThat(e.getConstraintName()).isEmpty());
    }

    @Test
    @DisplayName("无冲突时新增成功，主键由 Mapper 回填")
    void should_insertSuccessfully_when_noConflict() {
        // Arrange
        TodoItemMapper mapper = new TodoItemMapper() {
            @Override
            public TodoItemDO selectByRequestId(TodoItemDO query) {
                return null;
            }

            @Override
            public int insert(TodoItemDO todoItem) {
                todoItem.setId(1L);
                return 1;
            }
        };
        TodoItemManager manager = new TodoItemManager(mapper);
        TodoItemDO entity = new TodoItemDO();

        // Act
        manager.insert(entity);

        // Assert
        assertThat(entity.getId()).isEqualTo(1L);
    }

    private TodoItemMapper buildMapper(String duplicateMessage) {
        return new TodoItemMapper() {
            @Override
            public TodoItemDO selectByRequestId(TodoItemDO query) {
                return null;
            }

            @Override
            public int insert(TodoItemDO todoItem) {
                throw new DuplicateKeyException(duplicateMessage);
            }
        };
    }
}
