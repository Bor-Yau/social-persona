package com.socialpersona.message.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialpersona.persona.entity.Persona;
import com.socialpersona.persona.service.PersonaService;
import com.socialpersona.event.scheduler.EventTriggerScheduler;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ServerEndpoint(value = "/ws/qq", configurator = QQWebSocketConfigurator.class)
public class QQWebSocketHandler implements ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(QQWebSocketHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static ApplicationContext applicationContext;
    private final PersonaService personaService;

    private static final ConcurrentHashMap<String, Session> napCatSessions = new ConcurrentHashMap<>();

    public QQWebSocketHandler(PersonaService personaService) {
        this.personaService = personaService;
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        applicationContext = ctx;
    }

    @OnOpen
    public void onOpen(Session session) {
        String sessionId = session.getId();
        log.info("QQ WebSocket 连接: NapCat 已连入，session={}", sessionId);
    }

    @OnClose
    public void onClose(Session session) {
        String sessionId = session.getId();
        napCatSessions.entrySet().removeIf(e -> e.getValue().equals(session));
        log.info("QQ WebSocket 断开: session={}", sessionId);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("QQ WebSocket 异常: {}", error.getMessage());
    }

    @OnMessage
    public void onMessage(String rawMessage, Session session) {
        try {
            JsonNode json = objectMapper.readTree(rawMessage);
            String postType = json.path("post_type").asText();

            if ("meta_event".equals(postType)) {
                handleMetaEvent(json, session);
                return;
            }
            if (!"message".equals(postType)) return;

            String messageType = json.path("message_type").asText();
            String rawMsg = json.path("raw_message").asText(null);
            if (rawMsg == null || rawMsg.isEmpty()) return;

            String userId = json.path("user_id").asText();
            if (userId.isEmpty()) userId = String.valueOf(json.path("user_id").asLong());

            String selfId = json.path("self_id").asText();
            if (selfId.isEmpty()) selfId = String.valueOf(json.path("self_id").asLong());

            if (!selfId.isEmpty()) {
                napCatSessions.put(selfId, session);
            }

            if ("group".equals(messageType)) {
                log.debug("群消息暂不处理: group_id={}, user_id={}", json.path("group_id").asText(), userId);
                return;
            }
            if (!"private".equals(messageType)) return;

            Persona persona = personaService.findByAiQQ(selfId);
            if (persona == null) {
                log.debug("未找到匹配AI: self_id={}", selfId);
                return;
            }

            if (persona.getOwnerQq() == null || persona.getOwnerQq().isEmpty()) {
                log.debug("AI 未配置 owner_qq: persona={}", persona.getId());
                return;
            }
            if (!userId.equals(persona.getOwnerQq())) {
                log.debug("非主人消息忽略: user_id={}, owner_qq={}", userId, persona.getOwnerQq());
                return;
            }

            if (applicationContext != null) {
                QQAsyncMessageHandler asyncHandler =
                        applicationContext.getBean(QQAsyncMessageHandler.class);
                asyncHandler.handleMessage(persona.getId(), rawMsg);
            }

        } catch (Exception e) {
            log.error("QQ消息处理失败: {}", e.getMessage(), e);
        }
    }

    private void handleMetaEvent(JsonNode json, Session session) {
        String metaType = json.path("meta_event_type").asText();
        if ("heartbeat".equals(metaType)) {
            log.debug("OneBot 心跳: {}", session.getId());
            String selfId = json.path("status").path("self_id").asText();
            if (!selfId.isEmpty()) {
                napCatSessions.put(selfId, session);
                triggerLazyEventGeneration(selfId);
            }
        }
    }

    /**
     * 心跳时触发今日事件懒加载 —— 异步执行，不阻塞 WebSocket 线程
     */
    private void triggerLazyEventGeneration(String selfId) {
        try {
            Persona persona = personaService.findByAiQQ(selfId);
            if (persona == null) return;
            if (applicationContext != null) {
                EventTriggerScheduler scheduler =
                        applicationContext.getBean(EventTriggerScheduler.class);
                scheduler.lazyGenerateTodayEventsIfNeeded(persona.getId());
            }
        } catch (Exception e) {
            log.debug("懒加载触发异常(正常降级): {}", e.getMessage());
        }
    }

    public void sendReply(String userId, String message) {
        sendWithRetry(userId, message, 0);
    }

    public void sendImage(String userId, String imagePath, String caption) {
        String absPath = Path.of(imagePath).toAbsolutePath().toString().replace("\\", "/");
        String cqCode = "[CQ:image,file=" + absPath + "]";
        String message = (caption != null && !caption.isEmpty()) ? caption + " " + cqCode : cqCode;
        sendReply(userId, message);
    }

    private void sendWithRetry(String userId, String message, int attempt) {
        Session targetSession = null;
        for (Map.Entry<String, Session> entry : napCatSessions.entrySet()) {
            if (entry.getValue().isOpen()) {
                targetSession = entry.getValue();
                break;
            }
        }

        if (targetSession == null) {
            log.warn("NapCat 未连接，无法发送: user={}", userId);
            return;
        }

        try {
            Map<String, Object> action = Map.of(
                    "action", "send_private_msg",
                    "params", Map.of("user_id", userId, "message", message)
            );
            String json = objectMapper.writeValueAsString(action);
            synchronized (targetSession) {
                targetSession.getBasicRemote().sendText(json);
            }
        } catch (IOException e) {
            if (attempt < 2) {
                log.warn("QQ发送失败(重试 {}/2): user={}, error={}", attempt + 1, userId, e.getMessage());
                try { Thread.sleep(500 * (attempt + 1)); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                sendWithRetry(userId, message, attempt + 1);
            } else {
                log.error("QQ发送失败(已重试2次): user={}, error={}", userId, e.getMessage());
            }
        }
    }
}