package com.antdigital.todo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 待办服务启动类。
 *
 * <p>与既有排序模块（com.antdigital.sort）同仓库共存、互不依赖，仅新增 todo 包。
 *
 * @author AiWork
 * @date 2026/10/29
 */
@SpringBootApplication
@MapperScan("com.antdigital.todo.dao.mapper")
public class TodoApplication {

    /**
     * 应用入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(TodoApplication.class, args);
    }
}
