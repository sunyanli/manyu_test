package com.antdigital.todo.service.component;

/**
 * 待办编号当日序号提供者（取号 SPI）。
 *
 * <p>抽象取号能力，便于按环境替换实现：生产环境可接入 Redis INCR（键按天过期），
 * 无中间件或 Redis 不可用时使用本地雪花序号实现
 * {@link SnowflakeSequenceProvider}。任一实现的返回值都会再做 6 位取模，
 * 唯一性最终由 {@code uk_todo_item_no} 兜底。
 *
 * @author AiWork
 * @date 2026/10/29
 */
public interface TodoSequenceProvider {

    /**
     * 获取指定日期的下一个序号。
     *
     * @param dateKey 日期键，格式 yyyyMMdd
     * @return 正整数序号，重复或回绕不视为错误
     */
    long nextSequence(String dateKey);
}
