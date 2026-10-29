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
 * <p>关注点：索引名解析仅用于日志与诊断，解析失败必须原样传递空串，由业务层按「未知冲突」
 * 处理（先幂等回查、再重试），避免把幂等冲突误判为编号冲突。
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
        TodoItemManager manager = new TodoItemManager(new StubTodoItemMapper(null, false));
        TodoItemDO entity = new TodoItemDO();

        // Act
        manager.insert(entity);

        // Assert
        assertThat(entity.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("按幂等键加锁查询：透传租户与幂等键")
    void should_queryForUpdate_when_selectByRequestIdForUpdate() {
        // Arrange
        StubTodoItemMapper mapper = new StubTodoItemMapper(null, false);
        TodoItemManager manager = new TodoItemManager(mapper);
        TodoItemDO query = new TodoItemDO();
        query.setTenantId("7");
        query.setRequestId("req-1");

        // Act
        manager.selectByRequestIdForUpdate(query);

        // Assert
        assertThat(mapper.lastQuery.getTenantId()).isEqualTo("7");
        assertThat(mapper.lastQuery.getRequestId()).isEqualTo("req-1");
    }

    private TodoItemMapper buildMapper(String duplicateMessage) {
        return new StubTodoItemMapper(duplicateMessage, true);
    }

    /**
     * Mapper 替身：可选择在插入时抛出唯一键冲突。
     */
    private static final class StubTodoItemMapper implements TodoItemMapper {

        private final String duplicateMessage;

        private final boolean duplicateOnInsert;

        private TodoItemDO lastQuery;

        private StubTodoItemMapper(String duplicateMessage, boolean duplicateOnInsert) {
            this.duplicateMessage = duplicateMessage;
            this.duplicateOnInsert = duplicateOnInsert;
        }

        @Override
        public TodoItemDO selectByRequestId(TodoItemDO query) {
            this.lastQuery = query;
            return null;
        }

        @Override
        public TodoItemDO selectByRequestIdForUpdate(TodoItemDO query) {
            this.lastQuery = query;
            return null;
        }

        @Override
        public int insert(TodoItemDO todoItem) {
            if (duplicateOnInsert) {
                throw new DuplicateKeyException(duplicateMessage);
            }
            todoItem.setId(1L);
            return 1;
        }
    }
}
