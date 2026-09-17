package com.antdigital.video;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 小猫喵喵叫文生视频服务启动入口。
 */
@SpringBootApplication
@MapperScan("com.antdigital.video.dao.mapper")
public class VideoApplication {

    public static void main(String[] args) {
        SpringApplication.run(VideoApplication.class, args);
    }
}