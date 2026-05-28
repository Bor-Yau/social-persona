package com.socialpersona.matchmaker.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

@TableName("matchmaker_sessions")
public class MatchmakerSession implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private String sessionId;

    private String currentStage;

    private String collectedDataJson;

    private String historyJson;

    private String personaId;

    private String status;

    private String createdAt;

    private String updatedAt;

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getCurrentStage() { return currentStage; }
    public void setCurrentStage(String currentStage) { this.currentStage = currentStage; }

    public String getCollectedDataJson() { return collectedDataJson; }
    public void setCollectedDataJson(String collectedDataJson) { this.collectedDataJson = collectedDataJson; }

    public String getHistoryJson() { return historyJson; }
    public void setHistoryJson(String historyJson) { this.historyJson = historyJson; }

    public String getPersonaId() { return personaId; }
    public void setPersonaId(String personaId) { this.personaId = personaId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
