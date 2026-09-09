package com.antdigital.todo.dao.mapper;

import com.antdigital.todo.model.entity.TodoItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 待办事项Mapper接口
 *
 * @author AiWork
 * @since 2026-09-09
 */
@Mapper
public interface TodoItemMapper {

    /**
     * 新增待办事项
     *
     * @param todoItem 待办事项数据对象
     * @return 影响行数
     */
    int insert(TodoItemDO todoItem);

    /**
     * 根据主键查询待办事项
     *
     * @param id 主键ID
     * @return 待办事项数据对象
     */
    TodoItemDO selectById(@Param("id") Long id);
}
