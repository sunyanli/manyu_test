package com.alipay.todo.dao.mapper;

import com.alipay.todo.model.entity.TodoItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 待办事项数据访问接口
 *
 * @author AiWork
 */
@Mapper
public interface TodoItemMapper {

    /**
     * 插入待办事项
     *
     * @param todoItem 待办事项实体
     * @return 影响行数
     */
    int insert(TodoItemDO todoItem);

    /**
     * 根据ID查询待办事项
     *
     * @param id 主键ID
     * @return 待办事项实体
     */
    TodoItemDO selectById(@Param("id") Long id);
}