package com.antdigital.todo.model.dto;

/**
 * 校验与规范化后的待办数据（S03 出参）。
 *
 * <p>校验层不做静默截断，超长直接拒绝（R03）；本对象仅承载已通过校验的最终值。
 *
 * @author AiWork
 * @date 2026/10/29
 */
public class ValidatedTodo {

    /**
     * 规范化后的幂等键
     */
    private final String requestId;

    /**
     * 规范化后的事项名称
     */
    private final String itemName;

    /**
     * 规范化后的事项描述，缺省为空串
     */
    private final String itemDesc;

    /**
     * 规范化后的创建来源编码
     */
    private final String source;

    /**
     * 构造校验后的待办数据。
     *
     * @param requestId 规范化后的幂等键
     * @param itemName  规范化后的事项名称
     * @param itemDesc  规范化后的事项描述
     * @param source    规范化后的创建来源编码
     */
    public ValidatedTodo(String requestId, String itemName, String itemDesc, String source) {
        this.requestId = requestId;
        this.itemName = itemName;
        this.itemDesc = itemDesc;
        this.source = source;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getItemName() {
        return itemName;
    }

    public String getItemDesc() {
        return itemDesc;
    }

    public String getSource() {
        return source;
    }

    /**
     * 输出字段快照，名称与描述不打印原文。
     *
     * @return 字段快照
     */
    @Override
    public String toString() {
        return "ValidatedTodo{requestId='" + requestId + '\''
                + ", itemName='(hidden)'"
                + ", itemDesc='(hidden)'"
                + ", source='" + source + '\''
                + '}';
    }
}
