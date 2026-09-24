package com.manyu.todo.dao.mapper;

import com.manyu.todo.model.entity.TodoDO;

/**
 * 待办事项数据访问接口。
 *
 * @author AiWork
 */
public interface TodoMapper {

    /**
     * 插入待办事项，成功后回填自增主键 id。
     *
     * @param todo 待办事项数据对象
     * @return 影响行数
     */
    int insert(TodoDO todo);
}
