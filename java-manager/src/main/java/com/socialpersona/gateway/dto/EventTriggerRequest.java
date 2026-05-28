package com.socialpersona.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EventTriggerRequest {

    @JsonProperty("api_config")
    private Object apiConfig;

    @JsonProperty("persona_config")
    private Object personaConfig;

    @JsonProperty("relationship_state")
    private Object relationshipState;

    @JsonProperty("recent_memories")
    private Object recentMemories;

    @JsonProperty("today_events_so_far")
    private Object todayEventsSoFar;

    @JsonProperty("current_event")
    private Object currentEvent;

    @JsonProperty("next_event_type")
    private String nextEventType;

    @JsonProperty("next_event_time")
    private String nextEventTime;

    @JsonProperty("now")
    private Long now;

    @JsonProperty("image_config")
    private Object imageConfig;

    public Object getApiConfig() { return apiConfig; }
    public void setApiConfig(Object apiConfig) { this.apiConfig = apiConfig; }
    public Object getPersonaConfig() { return personaConfig; }
    public void setPersonaConfig(Object personaConfig) { this.personaConfig = personaConfig; }
    public Object getRelationshipState() { return relationshipState; }
    public void setRelationshipState(Object relationshipState) { this.relationshipState = relationshipState; }
    public Object getRecentMemories() { return recentMemories; }
    public void setRecentMemories(Object recentMemories) { this.recentMemories = recentMemories; }
    public Object getTodayEventsSoFar() { return todayEventsSoFar; }
    public void setTodayEventsSoFar(Object todayEventsSoFar) { this.todayEventsSoFar = todayEventsSoFar; }
    public Object getCurrentEvent() { return currentEvent; }
    public void setCurrentEvent(Object currentEvent) { this.currentEvent = currentEvent; }
    public String getNextEventType() { return nextEventType; }
    public void setNextEventType(String nextEventType) { this.nextEventType = nextEventType; }
    public String getNextEventTime() { return nextEventTime; }
    public void setNextEventTime(String nextEventTime) { this.nextEventTime = nextEventTime; }
    public Long getNow() { return now; }
    public void setNow(Long now) { this.now = now; }
    public Object getImageConfig() { return imageConfig; }
    public void setImageConfig(Object imageConfig) { this.imageConfig = imageConfig; }
}
