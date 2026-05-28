package com.socialpersona.message.websocket;

import com.socialpersona.message.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * QQ 消息异步处理器 —— 将 WebSocket 线程中的消息处理放到异步线程池
 *
 * ★ 为什么需要异步：
 *   QQWebSocketHandler.onMessage() 运行在 Tomcat WebSocket 线程中。
 *   如果同步调 handleUserMessage()，后者同步调 Python LLM（可能 10-15 秒），
 *   这期间 NapCat 的心跳包无法被处理，触发 WebSocket 超时断开。
 *
 * ★ @Async 机制：
 *   Spring 自动在异步线程池中执行，主线程立即返回。
 *   @EnableAsync 在 SocialPersonaApplication 中开启。
 */
@Component
public class QQAsyncMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(QQAsyncMessageHandler.class);

    @Autowired
    private MessageService messageService;

    @Async
    public void handleMessage(String personaId, String rawMsg) {
        log.debug("异步处理 QQ 消息: persona={}", personaId);
        messageService.handleUserMessage(personaId, rawMsg);
    }
}