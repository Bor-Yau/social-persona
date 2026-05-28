package com.socialpersona.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MatchmakerRequest {

    @JsonProperty("api_config")
    private Object apiConfig;

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("current_stage")
    private String currentStage;

    @JsonProperty("history")
    private Object history;

    @JsonProperty("user_message")
    private String userMessage;

    @JsonProperty("collected_data")
    private Object collectedData;

    @JsonProperty("language_hint")
    private String languageHint;

    @JsonProperty("persona_config")
    private Object personaConfig;

    public Object getApiConfig() { return apiConfig; }
    public void setApiConfig(Object apiConfig) { this.apiConfig = apiConfig; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getCurrentStage() { return currentStage; }
    public void setCurrentStage(String currentStage) { this.currentStage = currentStage; }
    public Object getHistory() { return history; }
    public void setHistory(Object history) { this.history = history; }
    public String getUserMessage() { return userMessage; }
    public void setUserMessage(String userMessage) { this.userMessage = userMessage; }
    public Object getCollectedData() { return collectedData; }
    public void setCollectedData(Object collectedData) { this.collectedData = collectedData; }
    public String getLanguageHint() { return languageHint; }
    public void setLanguageHint(String languageHint) { this.languageHint = languageHint; }
    public Object getPersonaConfig() { return personaConfig; }
    public void setPersonaConfig(Object personaConfig) { this.personaConfig = personaConfig; }
}
