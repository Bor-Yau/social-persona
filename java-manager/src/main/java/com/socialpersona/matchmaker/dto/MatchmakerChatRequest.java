package com.socialpersona.matchmaker.dto;

public class MatchmakerChatRequest {

    private String sessionId;
    private String userMessage;
    private String languageHint;

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getUserMessage() { return userMessage; }
    public void setUserMessage(String userMessage) { this.userMessage = userMessage; }

    public String getLanguageHint() { return languageHint; }
    public void setLanguageHint(String languageHint) { this.languageHint = languageHint; }
}
