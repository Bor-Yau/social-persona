package com.socialpersona.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket 配置 —— 注册 @ServerEndpoint 端点
 *
 * ★ @ServerEndpoint 不由 Spring 管理实例生命周期。
 *   ServerEndpointExporter 能找到 @ServerEndpoint 注解并注册，
 *   但实例由 WebSocket 容器创建 → @Autowired 字段不生效。
 *
 * ★ 解决方案（已实现）：
 *   FrontendWebSocketHandler —— 无 @Autowired 依赖，直接用
 *   QQWebSocketHandler       —— 使用静态 ApplicationContext 查找注入
 */
@Configuration
public class WebSocketConfig {

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
