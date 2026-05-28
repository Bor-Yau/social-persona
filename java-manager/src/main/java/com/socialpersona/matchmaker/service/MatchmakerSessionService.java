package com.socialpersona.matchmaker.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialpersona.matchmaker.entity.MatchmakerSession;
import com.socialpersona.matchmaker.repository.MatchmakerSessionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MatchmakerSessionService {

    private static final Logger log = LoggerFactory.getLogger(MatchmakerSessionService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MatchmakerSessionMapper sessionMapper;

    /**
     * 创建新会话 —— 首次进入牵线人时调用
     */
    public MatchmakerSession createSession() {
        String id = UUID.randomUUID().toString();
        MatchmakerSession session = new MatchmakerSession();
        session.setSessionId(id);
        session.setCurrentStage("basic_profile");
        session.setCollectedDataJson("{}");
        session.setHistoryJson("[]");
        session.setStatus("in_progress");
        sessionMapper.insert(session);
        log.info("Matchmaker 会话创建: sessionId={}", id);
        return session;
    }

    @Cacheable(value = "session", key = "#sessionId")
    public MatchmakerSession getSession(String sessionId) {
        return sessionMapper.selectById(sessionId);
    }

    /**
     * 推进阶段（返回后进入下一阶段时调用）
     */
    @CacheEvict(value = "session", key = "#sessionId")
    public void advanceStage(String sessionId, String nextStage, Map<String, Object> newData) {
        MatchmakerSession session = getSession(sessionId);
        if (session == null) return;

        session.setCurrentStage(nextStage);

        // 累积新提取的数据到 collected_data
        if (newData != null && !newData.isEmpty()) {
            try {
                Map<String, Object> existing = readCollectedData(session);
                existing.putAll(newData);
                session.setCollectedDataJson(objectMapper.writeValueAsString(existing));
            } catch (JsonProcessingException e) {
                log.warn("collectedData 序列化失败: {}", e.getMessage());
            }
        }
        sessionMapper.updateById(session);
    }

    /**
     * 追加对话历史
     */
    @CacheEvict(value = "session", key = "#sessionId")
    public void appendHistory(String sessionId, String role, String content) {
        MatchmakerSession session = getSession(sessionId);
        if (session == null) return;

        try {
            List<Map<String, String>> history = readHistory(session);
            Map<String, String> entry = new HashMap<>();
            entry.put("role", role);
            entry.put("content", content);
            history.add(entry);
            session.setHistoryJson(objectMapper.writeValueAsString(history));
            sessionMapper.updateById(session);
        } catch (JsonProcessingException e) {
            log.warn("history 序列化失败: {}", e.getMessage());
        }
    }

    /**
     * 会话完成 —— 关联创建的 Persona
     */
    @CacheEvict(value = "session", key = "#sessionId")
    public void markCompleted(String sessionId, String personaId) {
        MatchmakerSession session = getSession(sessionId);
        if (session == null) return;
        session.setStatus("completed");
        session.setPersonaId(personaId);
        sessionMapper.updateById(session);
    }

    // ==================== JSON 反序列化辅助 ====================

    @SuppressWarnings("unchecked")
    private Map<String, Object> readCollectedData(MatchmakerSession session) {
        try {
            return objectMapper.readValue(session.getCollectedDataJson(), Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> readHistory(MatchmakerSession session) {
        try {
            return objectMapper.readValue(session.getHistoryJson(), List.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
