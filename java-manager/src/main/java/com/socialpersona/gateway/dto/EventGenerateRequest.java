package com.socialpersona.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class EventGenerateRequest {

    @JsonProperty("api_config")
    private Object apiConfig;

    @JsonProperty("persona_config")
    private Object personaConfig;

    @JsonProperty("relationship_state")
    private Object relationshipState;

    @JsonProperty("today_date")
    private String todayDate;

    @JsonProperty("day_of_week")
    private String dayOfWeek;

    @JsonProperty("yesterday_events")
    private List<Object> yesterdayEvents;

    @JsonProperty("today_inner_thoughts")
    private List<Object> todayInnerThoughts;

    public Object getApiConfig() { return apiConfig; }
    public void setApiConfig(Object apiConfig) { this.apiConfig = apiConfig; }
    public Object getPersonaConfig() { return personaConfig; }
    public void setPersonaConfig(Object personaConfig) { this.personaConfig = personaConfig; }
    public Object getRelationshipState() { return relationshipState; }
    public void setRelationshipState(Object relationshipState) { this.relationshipState = relationshipState; }
    public String getTodayDate() { return todayDate; }
    public void setTodayDate(String todayDate) { this.todayDate = todayDate; }
    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public List<Object> getYesterdayEvents() { return yesterdayEvents; }
    public void setYesterdayEvents(List<Object> yesterdayEvents) { this.yesterdayEvents = yesterdayEvents; }
    public List<Object> getTodayInnerThoughts() { return todayInnerThoughts; }
    public void setTodayInnerThoughts(List<Object> todayInnerThoughts) { this.todayInnerThoughts = todayInnerThoughts; }
}
