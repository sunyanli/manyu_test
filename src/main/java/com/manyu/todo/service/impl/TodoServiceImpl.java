package com.manyu.todo.service.impl;

import com.manyu.todo.common.exception.BizException;
import com.manyu.todo.dao.mapper.TodoMapper;
import com.manyu.todo.model.dto.TodoCreateRequest;
import com.manyu.todo.model.entity.TodoDO;
import com.manyu.todo.model.vo.TodoVO;
import com.manyu.todo.service.TodoService;
import org.springframework.stereotype.Service;

/**
 * 待办事项业务服务实现。
 *
 * @author AiWork
 */
@Service
public class TodoServiceImpl implements TodoService {

    /** 名称最大长度 */
    private static final int NAME_MAX_LENGTH = 64;

    /** 描述最大长度 */
    private static final int DESCRIPTION_MAX_LENGTH = 512;

    private final TodoMapper todoMapper;

    public TodoServiceImpl(TodoMapper todoMapper) {
        this.todoMapper = todoMapper;
    }

    @Override
    public TodoVO createTodo(TodoCreateRequest request) {
        checkRequest(request);

        TodoDO todo = new TodoDO();
        todo.setName(request.getName().trim());
        todo.setDescription(request.getDescription());
        int affected = todoMapper.insert(todo);
        if (affected != 1) {
            throw new BizException("B0002", "待办事项保存失败");
        }
        return toVO(todo);
    }

    private void checkRequest(TodoCreateRequest request) {
        if (request == null) {
            throw new BizException("A0001", "请求参数不能为空");
        }
        String name = request.getName();
        if (name == null || name.trim().isEmpty()) {
            throw new BizException("A0001", "事项名称不能为空");
        }
        if (name.trim().length() > NAME_MAX_LENGTH) {
            throw new BizException("A0002", "事项名称不能超过64个字符");
        }
        String description = request.getDescription();
        if (description != null && description.length() > DESCRIPTION_MAX_LENGTH) {
            throw new BizException("A0003", "事项描述不能超过512个字符");
        }
    }

    private TodoVO toVO(TodoDO todo) {
        TodoVO vo = new TodoVO();
        vo.setId(todo.getId());
        vo.setName(todo.getName());
        vo.setDescription(todo.getDescription());
        vo.setGmtCreate(todo.getGmtCreate());
        return vo;
    }
}
