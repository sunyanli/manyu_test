package com.manyu.algodemo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 三示例接口演示应用启动类。
 *
 * <p>包含 demo（helloworld/哈希/冒泡排序）、export（页面导出）、tracking（埋点统计报表）三个模块。</p>
 */
@SpringBootApplication
@MapperScan("com.manyu.algodemo.tracking.dao")
public class AlgoDemoApplication {

    /**
     * 应用入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AlgoDemoApplication.class, args);
    }
}
