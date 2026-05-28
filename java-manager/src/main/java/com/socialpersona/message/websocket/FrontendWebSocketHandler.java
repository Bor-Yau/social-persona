package com.socialpersona.message.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 前端 WebSocket 端点 —— 管理面板实时消息推送
 *
 * ★ 连接生命周期：
 *   浏览器打开管理面板 → new WebSocket("ws://localhost:8080/ws/frontend")
 *   → @OnOpen → 注册到 sessions
 *   → Java 产出一条 AI 回复 → sendToPersona() → session.sendText()
 *   → 浏览器关闭 → @OnClose → 从 sessions 移除
 *
 * ★ 为什么用 ConcurrentHashMap：
 *   多个 Persona 可能同时发消息（多线程），需要线程安全的 Map。
 *
 * ★ 消息格式（JSON）：
 *   {"type":"message","personaId":"xxx","content":"早啊","mood":"困","timestamp":"..."}
 */
@Component
@ServerEndpoint("/ws/frontend")
public class FrontendWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(FrontendWebSocketHandler.class);

    /** 单例实例（@ServerEndpoint 不由 Spring 管理，需要用静态字段持有 Bean 引用） */
    private static FrontendWebSocketHandler instance;

    /** personaId → Session 映射（每个 Persona 的浏览器连接） */
    private final Map<String, Session> personaSessions = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @OnOpen
    public void onOpen(Session session) {
        String personaId = extractPersonaId(session);
        personaSessions.put(personaId, session);
        log.info("Frontend WebSocket 连接: persona={}, session={}", personaId, session.getId());
    }

    @OnClose
    public void onClose(Session session) {
        personaSessions.values().remove(session);
        personaSessions.entrySet().removeIf(e -> e.getValue().getId().equals(session.getId()));
        log.info("Frontend WebSocket 断开: session={}", session.getId());
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("Frontend WebSocket 异常: {}", error.getMessage());
        personaSessions.entrySet().removeIf(e -> e.getValue().getId().equals(session.getId()));
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        log.debug("Frontend WebSocket 收到消息: {}", message);
    }

    /**
     * 向指定 Persona 的浏览器推送消息
     *
     * @param personaId Persona UUID
     * @param message   消息 JSON 字符串
     */
    public void sendToPersona(String personaId, String message) {
        Session session = personaSessions.get(personaId);
        if (session == null || !session.isOpen()) {
            log.debug("无活跃前端连接: persona={}", personaId);
            return;
        }
        try {
            synchronized (session) {
                session.getBasicRemote().sendText(message);
            }
        } catch (IOException e) {
            log.warn("前端推送失败: persona={}, error={}", personaId, e.getMessage());
            personaSessions.remove(personaId);
        }
    }

    /**
     * 从 Session 查询参数中提取 personaId
     */
    private String extractPersonaId(Session session) {
        String query = session.getQueryString();
        if (query != null && query.contains("personaId=")) {
            int start = query.indexOf("personaId=") + 10;
            int end = query.indexOf("&", start);
            return end > 0 ? query.substring(start, end) : query.substring(start);
        }
        return session.getId();
    }
}
