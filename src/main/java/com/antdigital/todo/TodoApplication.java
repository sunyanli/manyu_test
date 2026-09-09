package com.antdigital.todo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 待办事项应用启动类
 *
 * @author AiWork
 * @since 2026-09-09
 */
@SpringBootApplication
@MapperScan("com.antdigital.todo.dao.mapper")
public class TodoApplication {

    /**
     * 应用入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(TodoApplication.class, args);
    }
}
