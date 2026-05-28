package com.socialpersona.sim;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialpersona.event.entity.DailyEvent;
import com.socialpersona.event.entity.EventLog;
import com.socialpersona.event.repository.EventLogMapper;
import com.socialpersona.event.repository.EventMapper;
import com.socialpersona.event.scheduler.EventTriggerScheduler;
import com.socialpersona.event.service.EventService;
import com.socialpersona.gateway.PythonClient;
import com.socialpersona.gateway.dto.EventTriggerRequest;
import com.socialpersona.gateway.dto.EventTriggerResponse;
import com.socialpersona.gateway.dto.ImageGenerateRequest;
import com.socialpersona.gateway.dto.ImageGenerateResponse;
import com.socialpersona.message.service.MessageService;
import com.socialpersona.persona.dto.ApiConfigDTO;
import com.socialpersona.persona.entity.CharacterLifeArchive;
import com.socialpersona.persona.entity.Persona;
import com.socialpersona.persona.repository.CharacterLifeArchiveMapper;
import com.socialpersona.persona.repository.PersonaMapper;
import com.socialpersona.persona.service.PersonaService;
import com.socialpersona.relationship.entity.RelationshipState;
import com.socialpersona.relationship.repository.RelationshipStateMapper;
import com.socialpersona.relationship.engine.RelationshipEngine;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/sim")
public class SimController {

    private static final Logger log = LoggerFactory.getLogger(SimController.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String ERR_INTERNAL = "操作失败，请稍后重试";
    private static final String ERR_PERSONA_NOT_FOUND = "角色不存在";
    private static final String ERR_API_UNCONFIGURED = "API 未配置，请在设置中检查";
    private static final String DEFAULT_EVENT_DESCRIPTION = "在吗？想找你聊聊天";
    private static final String DEFAULT_PROACTIVE_DESCRIPTION = "忽然想找你说说话";
    private static final String DEFAULT_PERSONA_NAME = "AI网友";

    @Autowired private MessageService messageService;
    @Autowired private EventTriggerScheduler eventTriggerScheduler;
    @Autowired private PersonaService personaService;
    @Autowired private EventService eventService;
    @Autowired private EventMapper eventMapper;
    @Autowired private EventLogMapper eventLogMapper;
    @Autowired private PersonaMapper personaMapper;
    @Autowired private RelationshipStateMapper relationshipStateMapper;
    @Autowired private CharacterLifeArchiveMapper lifeArchiveMapper;
    @Autowired private PythonClient pythonClient;
    @Autowired private RelationshipEngine relationshipEngine;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 模拟用户发消息 —— 调完整 handleUserMessage 链路
     */
    @PostMapping("/message")
    public Map<String, Object> simMessage(@RequestBody Map<String, String> body) {
        String personaId = body.get("personaId");
        String message = body.get("message");
        if (personaId == null || message == null) {
            return Map.of("ok", false, "error", "personaId and message are required");
        }
        long start = System.currentTimeMillis();
        try {
            messageService.handleUserMessage(personaId, message);
            long elapsed = System.currentTimeMillis() - start;
            return Map.of("ok", true, "elapsed_ms", elapsed);
        } catch (Exception e) {
            log.warn("sim/message 失败: persona={}, error={}", personaId, e.getMessage());
            long elapsed = System.currentTimeMillis() - start;
            return Map.of("ok", false, "error", ERR_INTERNAL, "elapsed_ms", elapsed);
        }
    }

    /**
     * 模拟事件懒加载 —— 调 LLM 生成今日事件线 + 今日反思
     */
    @PostMapping("/generate-events")
    public Map<String, Object> simGenerateEvents(@RequestBody Map<String, String> body) {
        String personaId = body.get("personaId");
        if (personaId == null) {
            return Map.of("ok", false, "error", "personaId is required");
        }
        try {
            eventTriggerScheduler.lazyGenerateTodayEventsIfNeeded(personaId);
            List<DailyEvent> events = eventService.findTodayEvents(personaId);
            List<Map<String, Object>> eventList = new ArrayList<>();
            for (DailyEvent e : events) {
                eventList.add(Map.of(
                    "time", e.getEventTime() != null ? e.getEventTime() : "",
                    "type", e.getEventType() != null ? e.getEventType() : "",
                    "description", e.getDescription() != null ? e.getDescription() : ""
                ));
            }
            return Map.of("ok", true, "count", eventList.size(), "events", eventList);
        } catch (Exception e) {
            log.warn("sim/generate-events 失败: persona={}, error={}", personaId, e.getMessage());
            return Map.of("ok", false, "error", ERR_INTERNAL);
        }
    }

    /**
     * 手动触发事件扫描 —— 模拟时间到达某事件点
     * 返回 LLM 是否决定主动联系用户
     */
    @PostMapping("/trigger-events")
    public Map<String, Object> simTriggerEvents(@RequestBody Map<String, String> body) {
        String personaId = body.get("personaId");
        if (personaId == null) {
            return Map.of("ok", false, "error", "personaId is required");
        }
        try {
            Persona persona = personaService.getById(personaId);
            if (persona == null) return Map.of("ok", false, "error", "persona not found");

            // 记录扫描前 inner_thought 数量
            long beforeThoughts = eventLogMapper.selectCount(
                new LambdaQueryWrapper<EventLog>()
                    .eq(EventLog::getPersonaId, personaId)
                    .eq(EventLog::getLogType, "inner_thought"));

            eventTriggerScheduler.scanForPersona(persona);

            // 扫描后检查是否有新的 inner_thought（LLM 被调用过）
            long afterThoughts = eventLogMapper.selectCount(
                new LambdaQueryWrapper<EventLog>()
                    .eq(EventLog::getPersonaId, personaId)
                    .eq(EventLog::getLogType, "inner_thought"));

            boolean llmCalled = afterThoughts > beforeThoughts;

            // 查最新一条 inner_thought 看 AI 态度
            String innerThoughtText = "";
            String attitude = "";
            if (llmCalled) {
                EventLog latest = eventLogMapper.selectOne(
                    new LambdaQueryWrapper<EventLog>()
                        .eq(EventLog::getPersonaId, personaId)
                        .eq(EventLog::getLogType, "inner_thought")
                        .orderByDesc(EventLog::getOccurredAt)
                        .last("LIMIT 1"));
                if (latest != null && latest.getDetailJson() != null) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> detail = objectMapper.readValue(latest.getDetailJson(), Map.class);
                        innerThoughtText = (String) detail.getOrDefault("raw_thought", "");
                        attitude = (String) detail.getOrDefault("attitude", "");
                    } catch (Exception e) { log.debug("innerThought JSON解析失败", e); }
                }
            }

            String now = LocalTime.now().format(TIME_FMT);
            return Map.of(
                "ok", true, "now", now,
                "llm_called", llmCalled,
                "inner_thought", innerThoughtText,
                "attitude", attitude
            );
        } catch (Exception e) {
            log.warn("sim/trigger-events 失败: persona={}, error={}", personaId, e.getMessage());
            return Map.of("ok", false, "error", ERR_INTERNAL);
        }
    }

    /**
     * 模拟跨天 —— 清空今日事件 → 重新懒加载（LLM 今日反思 + 生成明天事件）
     */
    @PostMapping("/new-day")
    public Map<String, Object> simNewDay(@RequestBody Map<String, String> body) {
        String personaId = body.get("personaId");
        if (personaId == null) {
            return Map.of("ok", false, "error", "personaId is required");
        }
        try {
            String today = java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            int cleared = eventMapper.invalidateAfter(personaId, today, "00:00:00");
            eventTriggerScheduler.lazyGenerateTodayEventsIfNeeded(personaId);
            List<DailyEvent> newEvents = eventService.findTodayEvents(personaId);
            List<Map<String, Object>> eventList = new ArrayList<>();
            for (DailyEvent e : newEvents) {
                eventList.add(Map.of(
                    "time", e.getEventTime() != null ? e.getEventTime() : "",
                    "type", e.getEventType() != null ? e.getEventType() : "",
                    "description", e.getDescription() != null ? e.getDescription() : ""
                ));
            }
            return Map.of("ok", true, "cleared", cleared, "new_count", eventList.size(), "events", eventList);
        } catch (Exception e) {
            log.warn("sim/new-day 失败: persona={}, error={}", personaId, e.getMessage());
            return Map.of("ok", false, "error", ERR_INTERNAL);
        }
    }

    /**
     * 清理模拟产生的所有数据
     */
    @PostMapping("/cleanup")
    public Map<String, Object> simCleanup(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> personaIds = (List<String>) body.get("personaIds");
        if (personaIds == null || personaIds.isEmpty()) {
            return Map.of("ok", false, "error", "personaIds is required");
        }
        int deletedPersons = 0, deletedEvents = 0, deletedLogs = 0;
        int deletedRelationships = 0, deletedArchives = 0;

        for (String personaId : personaIds) {
            try {
                deletedEvents += eventMapper.delete(
                    new LambdaQueryWrapper<DailyEvent>().eq(DailyEvent::getPersonaId, personaId));
                deletedLogs += eventLogMapper.delete(
                    new LambdaQueryWrapper<EventLog>().eq(EventLog::getPersonaId, personaId));
                deletedRelationships += relationshipStateMapper.delete(
                    new LambdaQueryWrapper<RelationshipState>().eq(RelationshipState::getPersonaId, personaId));
                deletedArchives += lifeArchiveMapper.delete(
                    new LambdaQueryWrapper<CharacterLifeArchive>().eq(CharacterLifeArchive::getPersonaId, personaId));
                personaMapper.deleteById(personaId);
                deletedPersons++;
                log.info("模拟清理完成: persona={}", personaId);
            } catch (Exception e) {
                log.warn("模拟清理失败: id={}, error={}", personaId, e.getMessage());
            }
        }

        return Map.of(
            "ok", true,
            "deleted_personas", deletedPersons,
            "deleted_daily_events", deletedEvents,
            "deleted_event_logs", deletedLogs,
            "deleted_relationships", deletedRelationships,
            "deleted_archives", deletedArchives
        );
    }

    /**
     * 测试图片生成管道 —— 直接调 Python 图片 API，不依赖 LLM 决策
     * 图片 API 需要在 system_config.json 中配置 imageProvider / imageApiKeyEncrypted / imageBaseUrl / imageModel
     */
    @PostMapping("/test-image")
    public Map<String, Object> simTestImage(@RequestBody Map<String, Object> body) {
        String personaId = (String) body.get("personaId");
        String prompt = (String) body.get("prompt");
        if (personaId == null || prompt == null) {
            return Map.of("ok", false, "error", "personaId and prompt are required");
        }
        try {
            Persona persona = personaService.getById(personaId);
            if (persona == null) return Map.of("ok", false, "error", "persona not found");

            Map<String, Object> imageConfig = personaService.loadImageApiConfig();
            if (imageConfig == null) {
                return Map.of("ok", false, "error",
                    "图片 API 未配置。请在 system_config.json 中设置 imageProvider / imageApiKeyEncrypted / imageBaseUrl / imageModel");
            }

            String dirName = PersonaService.buildImageDirName(persona);

            String finalPrompt = augmentPromptWithStyle(persona, prompt);

            ImageGenerateRequest req = new ImageGenerateRequest();
            req.setImageConfig(imageConfig);
            req.setPrompt(finalPrompt);
            req.setPersonaId(personaId);
            req.setPersonaDirName(dirName);
            req.setContext("simulation_test");

            ImageGenerateResponse resp = pythonClient.generateImage(req);
            if (resp != null && resp.isSuccess()) {
                return Map.of("ok", true, "local_path", resp.getLocalPath());
            }
            String errMsg = resp != null ? resp.getError() : "null";
            log.warn("图片生成失败: persona={}, error={}", personaId, errMsg);
            return Map.of("ok", false, "error", errMsg);
        } catch (Exception e) {
            log.warn("sim/test-image 异常: persona={}, error={}", personaId, e.getMessage());
            return Map.of("ok", false, "error", ERR_INTERNAL);
        }
    }

    /**
     * 强制事件触发 —— 插入一个立即到期的事件并触发 LLM 扫描
     * 不依赖真实时间，常用于测试 AI 的事件主动联系响应
     */
    @PostMapping("/force-trigger-event")
    public Map<String, Object> simForceTrigger(@RequestBody Map<String, String> body) {
        String personaId = body.get("personaId");
        String eventType = body.getOrDefault("eventType", "routine");
        String description = body.getOrDefault("description", DEFAULT_EVENT_DESCRIPTION);

        if (personaId == null) {
            return Map.of("ok", false, "error", "personaId is required");
        }
        try {
            Persona persona = personaService.getById(personaId);
            if (persona == null) return Map.of("ok", false, "error", "persona not found");

            String today = java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String nowTime = LocalTime.now().format(TIME_FMT);

            // 直接构造事件触发请求，不依赖 scanForPersona 的 inner_thought 写入
            ApiConfigDTO apiConfig = personaService.decryptApiConfig(personaId);
            if (apiConfig == null) {
                return Map.of("ok", false, "error", "无法获取 API 配置（system_config 无可用 Key）");
            }

            EventTriggerRequest request = new EventTriggerRequest();
            request.setApiConfig(Map.of(
                    "provider", apiConfig.getProvider(),
                    "api_key", apiConfig.getApiKey(),
                    "base_url", apiConfig.getBaseUrl(),
                    "model", apiConfig.getModel()
            ));
            request.setPersonaConfig(new LinkedHashMap<>(Map.of(
                    "id", personaId,
                    "name", persona.getName() != null ? persona.getName() : DEFAULT_PERSONA_NAME
            )));
            @SuppressWarnings("unchecked")
            Map<String, Object> pCfg = (Map<String, Object>) request.getPersonaConfig();
            String ctx = persona.getCharacterCurrentContext();
            if (ctx != null) pCfg.put("character_current_context", ctx);
            String typing = persona.getTypingStyleJson();
            if (typing != null) pCfg.put("typing_style_json", typing);

            com.socialpersona.relationship.entity.RelationshipState state =
                relationshipEngine.getState(personaId);
            request.setRelationshipState(Map.of(
                "trust", state.getTrust(),
                "closeness", state.getCloseness(),
                "tension", state.getTension(),
                "emotional_energy", state.getEmotionalEnergy(),
                "tension_pressure", state.getTensionPressure(),
                "contact_urge", state.getContactUrge()
            ));
            request.setCurrentEvent(Map.of("type", eventType, "description", description));
            request.setNextEventType("");
            request.setNextEventTime("");
            request.setNow(Instant.now().getEpochSecond());
            request.setRecentMemories(List.of());
            request.setTodayEventsSoFar(List.of());

            Map<String, Object> imageCfg = personaService.loadImageApiConfig();
            if (imageCfg != null) request.setImageConfig(imageCfg);

            EventTriggerResponse resp = pythonClient.triggerEvent(request);

            if (resp != null) {
                boolean willContact = Boolean.TRUE.equals(resp.getShouldContactUser());
                if (willContact && resp.getReply() != null) {
                    messageService.handleEventReply(personaId, resp.getReply(), resp.getInnerThought());
                }

                String thought = "";
                String attitude = "";
                boolean shouldRemember = false;
                String memorablePoint = "";
                Object rawThought = resp.getInnerThought();
                if (rawThought instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> it = (Map<String, Object>) rawThought;
                    thought = (String) it.getOrDefault("raw_thought", "");
                    attitude = (String) it.getOrDefault("attitude", "");
                    shouldRemember = Boolean.TRUE.equals(it.get("should_remember"));
                    Object mp = it.get("memorable_point");
                    memorablePoint = mp != null ? String.valueOf(mp) : "";
                }

                if (shouldRemember && !memorablePoint.isEmpty()) {
                    log.info("force-trigger-event: 记忆已由 Python 写入 ChromaDB, persona={}, memorable={}",
                            personaId, memorablePoint.substring(0, Math.min(80, memorablePoint.length())));
                }

                return Map.of(
                    "ok", true,
                    "llm_called", true,
                    "inner_thought", thought,
                    "attitude", attitude,
                    "should_remember", shouldRemember,
                    "memorable_point", memorablePoint,
                    "will_contact_user", willContact,
                    "reply_dispatched", willContact,
                    "trigger_event_type", eventType,
                    "trigger_time", nowTime
                );
            }
            return Map.of("ok", false, "error", "Python 返回空响应");
        } catch (Exception e) {
            log.warn("sim/force-trigger-event 失败: persona={}, error={}", personaId, e.getMessage());
            return Map.of("ok", false, "llm_called", false, "error", ERR_INTERNAL);
        }
    }

    /**
     * 插入一个 N 秒后到期的测试事件（用于测试主动调用机制）
     * 调用 EventTriggerScheduler.insertTestEvent 写入 DB，后续由 trigger-events 端点扫描处理
     */
    @PostMapping("/schedule-proactive-event")
    public Map<String, Object> simScheduleProactiveEvent(@RequestBody Map<String, String> body) {
        String personaId = body.get("personaId");
        if (personaId == null) {
            return Map.of("ok", false, "error", "personaId is required");
        }
        int delaySeconds;
        try {
            delaySeconds = Integer.parseInt(body.getOrDefault("delaySeconds", "60"));
        } catch (NumberFormatException e) {
            return Map.of("ok", false, "error", "delaySeconds must be an integer");
        }
        String eventType = body.getOrDefault("eventType", "moment");
        String description = body.getOrDefault("description", DEFAULT_PROACTIVE_DESCRIPTION);

        try {
            Persona persona = personaService.getById(personaId);
            if (persona == null) return Map.of("ok", false, "error", "persona not found");

            DailyEvent event = eventTriggerScheduler.insertTestEvent(personaId, delaySeconds,
                    eventType, description);
            return Map.of(
                "ok", true,
                "event_id", event.getId(),
                "event_time", event.getEventTime(),
                "event_type", event.getEventType(),
                "description", event.getDescription(),
                "delay_seconds", delaySeconds
            );
        } catch (Exception e) {
            log.warn("sim/schedule-proactive-event 失败: persona={}, error={}", personaId, e.getMessage());
            return Map.of("ok", false, "error", ERR_INTERNAL);
        }
    }

    /**
     * Invalidate a single event — prevents scanForPersona from re-processing it.
     */
    @PostMapping("/invalidate-event")
    public Map<String, Object> simInvalidateEvent(@RequestBody Map<String, String> body) {
        String eventId = body.get("eventId");
        if (eventId == null) {
            return Map.of("ok", false, "error", "eventId is required");
        }
        try {
            eventService.invalidateEvent(eventId);
            return Map.of("ok", true);
        } catch (Exception e) {
            log.warn("sim/invalidate-event 失败: event={}, error={}", eventId, e.getMessage());
            return Map.of("ok", false, "error", ERR_INTERNAL);
        }
    }

    // ==================== helper ====================

    /** 将 Persona 的图片风格偏好和外貌描述注入到 prompt 前缀中
     *  只有 prompt 明确提到角色名时才注入外貌（避免无角色图片被注入外貌） */
    private String augmentPromptWithStyle(Persona persona, String rawPrompt) {
        String style = persona.getImageStylePrompt();
        String appearance = persona.getCharacterAppearance();
        String name = persona.getName();
        StringBuilder sb = new StringBuilder();
        if (style != null && !style.isEmpty()) {
            sb.append("风格：").append(style).append("。");
        }
        if (appearance != null && !appearance.isEmpty()
                && name != null && !name.isEmpty()
                && rawPrompt.contains(name)) {
            sb.append("人物外貌：").append(appearance).append("。");
        }
        if (sb.length() > 0) {
            sb.append(" ").append(rawPrompt);
            return sb.toString();
        }
        return rawPrompt;
    }
}
