package com.antdigital.todo.common.exception;

/**
 * 唯一键冲突异常：由持久化适配层在捕获数据库唯一约束冲突后转换抛出。
 *
 * <p>业务层据此区分「幂等键冲突」（返回首次创建结果）与「编号冲突」（重新取号重试），
 * 使业务逻辑不依赖具体持久化框架的异常类型。
 *
 * @author AiWork
 * @date 2026/10/29
 */
public class TodoDuplicateKeyException extends RuntimeException {

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

    /**
     * 幂等键唯一索引名称
     */
    public static final String UK_REQUEST_ID = "uk_todo_item_req";

    /**
     * 待办编号唯一索引名称
     */
    public static final String UK_TODO_NO = "uk_todo_item_no";

    /**
     * 冲突的唯一索引名称
     */
    private final String constraintName;

    /**
     * 构造唯一键冲突异常。
     *
     * @param constraintName 冲突的唯一索引名称
     */
    public TodoDuplicateKeyException(String constraintName) {
        super("duplicate key: " + constraintName);
        this.constraintName = constraintName;
    }

    /**
     * 获取冲突的唯一索引名称。
     *
     * @return 唯一索引名称
     */
    public String getConstraintName() {
        return constraintName;
    }
}
