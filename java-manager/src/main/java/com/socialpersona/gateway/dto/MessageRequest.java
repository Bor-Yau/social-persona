package com.socialpersona.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * POST /api/message 请求体
 *
 * ★ @JsonProperty 的作用：
 *   Java 字段用 camelCase（shouldReply），但 Python/JSON 用 snake_case（should_reply）。
 *   @JsonProperty("should_reply") 告诉 Jackson 反序列化时自动映射。
 */
public class MessageRequest {

    @JsonProperty("api_config")
    private Object apiConfig;

    @JsonProperty("persona_config")
    private Object personaConfig;

    @JsonProperty("relationship_state")
    private Object relationshipState;

    @JsonProperty("recent_memories")
    private List<Object> recentMemories;

    @JsonProperty("today_events_so_far")
    private List<Object> todayEventsSoFar;

    @JsonProperty("user_message")
    private String userMessage;

    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("image_config")
    private Object imageConfig;

    public Object getApiConfig() { return apiConfig; }
    public void setApiConfig(Object apiConfig) { this.apiConfig = apiConfig; }
    public Object getPersonaConfig() { return personaConfig; }
    public void setPersonaConfig(Object personaConfig) { this.personaConfig = personaConfig; }
    public Object getRelationshipState() { return relationshipState; }
    public void setRelationshipState(Object relationshipState) { this.relationshipState = relationshipState; }
    public List<Object> getRecentMemories() { return recentMemories; }
    public void setRecentMemories(List<Object> recentMemories) { this.recentMemories = recentMemories; }
    public List<Object> getTodayEventsSoFar() { return todayEventsSoFar; }
    public void setTodayEventsSoFar(List<Object> todayEventsSoFar) { this.todayEventsSoFar = todayEventsSoFar; }
    public String getUserMessage() { return userMessage; }
    public void setUserMessage(String userMessage) { this.userMessage = userMessage; }
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    public Object getImageConfig() { return imageConfig; }
    public void setImageConfig(Object imageConfig) { this.imageConfig = imageConfig; }
}
