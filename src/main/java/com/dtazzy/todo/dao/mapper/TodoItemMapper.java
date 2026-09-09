package com.dtazzy.todo.dao.mapper;

import com.dtazzy.todo.dao.entity.TodoItemDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 待办事项数据访问接口。
 */
@Mapper
public interface TodoItemMapper {

    /**
     * 插入待办事项记录。
     *
     * @param todoItem 待办事项数据对象
     * @return 影响行数
     */
    int insert(TodoItemDO todoItem);
}