package com.antdigital.todo.dao.mapper;

import com.antdigital.todo.dao.entity.TodoItemDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 待办事项数据访问接口，由 resources/mapper/TodoItemMapper.xml 提供 SQL 绑定。
 *
 * <p>持久化实现需保证两点：唯一键冲突转换为
 * {@link com.antdigital.todo.common.exception.TodoDuplicateKeyException}（携带索引名）；
 * 插入时回填自增主键至 {@code id}。
 *
 * @author AiWork
 * @date 2026/10/29
 */
@Mapper
public interface TodoItemMapper {

    /**
     * 按租户 + 幂等键查询未逻辑删除的待办。
     *
     * @param query 查询条件载体，需设置 tenantId 与 requestId
     * @return 命中的待办；不存在时返回 null
     */
    TodoItemDO selectByRequestId(TodoItemDO query);

    /**
     * 新增待办。
     *
     * @param todoItem 待办数据对象，自增主键回填至 id
     * @return 影响行数
     */
    int insert(TodoItemDO todoItem);
}
