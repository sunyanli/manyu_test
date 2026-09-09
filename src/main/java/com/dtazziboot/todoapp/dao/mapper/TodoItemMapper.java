package com.dtazziboot.todoapp.dao.mapper;

import com.dtazziboot.todoapp.model.entity.TodoItemDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

/**
 * 待办事项数据访问接口
 *
 * @author AiWork
 * @date 2026/09/09
 */
@Mapper
public interface TodoItemMapper {

    /**
     * 插入一条待办事项记录
     *
     * @param record 待办事项数据对象
     * @return 影响行数
     */
    @Insert("INSERT INTO todo_item (title, description, creator_id, tenant_id, gmt_create, gmt_modified) " +
            "VALUES (#{title}, #{description}, #{creatorId}, #{tenantId}, #{gmtCreate}, #{gmtModified})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TodoItemDO record);
}