package com.socialpersona.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class EventGenerateResponse {

    @JsonProperty("today_reflection")
    private Object todayReflection;

    @JsonProperty("events")
    private List<Object> events;

    public Object getTodayReflection() { return todayReflection; }
    public void setTodayReflection(Object todayReflection) { this.todayReflection = todayReflection; }
    public List<Object> getEvents() { return events; }
    public void setEvents(List<Object> events) { this.events = events; }
}
