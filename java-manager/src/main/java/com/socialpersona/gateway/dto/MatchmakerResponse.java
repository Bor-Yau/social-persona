package com.socialpersona.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MatchmakerResponse {

    @JsonProperty("reply")
    private String reply;

    @JsonProperty("next_stage")
    private String nextStage;

    @JsonProperty("extracted_data")
    private Object extractedData;

    @JsonProperty("is_complete")
    private Boolean isComplete;

    @JsonProperty("persona_config")
    private Object personaConfig;

    @JsonProperty("sample_chats")
    private Object sampleChats;

    @JsonProperty("life_archive_json")
    private Object lifeArchiveJson;

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
    public String getNextStage() { return nextStage; }
    public void setNextStage(String nextStage) { this.nextStage = nextStage; }
    public Object getExtractedData() { return extractedData; }
    public void setExtractedData(Object extractedData) { this.extractedData = extractedData; }
    public Boolean getIsComplete() { return isComplete; }
    public void setIsComplete(Boolean isComplete) { this.isComplete = isComplete; }
    public Object getPersonaConfig() { return personaConfig; }
    public void setPersonaConfig(Object personaConfig) { this.personaConfig = personaConfig; }
    public Object getSampleChats() { return sampleChats; }
    public void setSampleChats(Object sampleChats) { this.sampleChats = sampleChats; }
    public Object getLifeArchiveJson() { return lifeArchiveJson; }
    public void setLifeArchiveJson(Object lifeArchiveJson) { this.lifeArchiveJson = lifeArchiveJson; }
}
