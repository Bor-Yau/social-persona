package com.socialpersona;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI网友模拟器 Spring Boot 启动入口
 *
 * 三个核心注解：
 * @SpringBootApplication  — 自动配置 + 组件扫描 + 配置类
 * @EnableFeignClients     — 扫描 @FeignClient 接口，声明式调用 Python FastAPI
 * @EnableScheduling       — 启用 @Scheduled 定时任务（事件触发扫描、消息扫描调度）
 */
@SpringBootApplication
@EnableFeignClients
@EnableScheduling
@EnableAsync
public class SocialPersonaApplication {

    public static void main(String[] args) {
        SpringApplication.run(SocialPersonaApplication.class, args);
    }
}
