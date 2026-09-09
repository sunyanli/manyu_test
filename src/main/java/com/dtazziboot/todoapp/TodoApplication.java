package com.dtazziboot.todoapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 待办事项应用启动类
 *
 * @author AiWork
 * @date 2026/09/09
 */
@SpringBootApplication
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
