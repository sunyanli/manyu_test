package com.antdigital.todo.common.enums;

import com.antdigital.todo.common.exception.TodoBizException;

/**
 * 待办创建来源枚举，对应 todo_item.source。
 *
 * @author AiWork
 * @date 2026/10/29
 */
public enum TodoSourceEnum {

    /**
     * Web 控制台创建（默认）
     */
    WEB("web"),

    /**
     * 移动端创建（预留）
     */
    APP("app"),

    /**
     * 接口调用创建（预留）
     */
    API("api");

    /**
     * 来源编码
     */
    private final String code;

    TodoSourceEnum(String code) {
        this.code = code;
    }

    /**
     * 获取来源编码。
     *
     * @return 来源编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 将来源编码转换为枚举，编码非法时抛出参数错误。
     *
     * @param code 来源编码，忽略大小写
     * @return 对应的来源枚举
     * @throws TodoBizException 编码非法时抛出 TODO_0001
     */
    public static TodoSourceEnum of(String code) {
        for (TodoSourceEnum source : values()) {
            if (source.code.equalsIgnoreCase(code)) {
                return source;
            }
        }
        throw new TodoBizException(TodoErrorCodeEnum.PARAM_INVALID);
    }
}
