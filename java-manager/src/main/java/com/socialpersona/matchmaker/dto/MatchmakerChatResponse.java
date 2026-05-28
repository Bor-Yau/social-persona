package com.socialpersona.matchmaker.dto;

import java.util.Map;

public class MatchmakerChatResponse {

    private String sessionId;
    private String reply;
    private String currentStage;
    private Map<String, Object> extractedData;
    private boolean isComplete;
    private String personaId;

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }

    public String getCurrentStage() { return currentStage; }
    public void setCurrentStage(String currentStage) { this.currentStage = currentStage; }

    public Map<String, Object> getExtractedData() { return extractedData; }
    public void setExtractedData(Map<String, Object> extractedData) { this.extractedData = extractedData; }

    public boolean isComplete() { return isComplete; }
    public void setComplete(boolean complete) { isComplete = complete; }

    public String getPersonaId() { return personaId; }
    public void setPersonaId(String personaId) { this.personaId = personaId; }
}
