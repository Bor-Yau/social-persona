package com.socialpersona.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class EventTriggerResponse {

    @JsonProperty("should_contact_user")
    private Boolean shouldContactUser;

    @JsonProperty("reply")
    private Object reply;

    @JsonProperty("inner_thought")
    private Object innerThought;

    @JsonProperty("relationship_deltas")
    private Object relationshipDeltas;

    @JsonProperty("conversation_ended")
    private Boolean conversationEnded;

    @JsonProperty("interval_pre_scheduling")
    private Object intervalPreScheduling;

    @JsonProperty("event_changed")
    private Boolean eventChanged;

    @JsonProperty("cancelled_scheduled_messages")
    private List<String> cancelledScheduledMessages;

    @JsonProperty("invalidated_events")
    private List<String> invalidatedEvents;

    @JsonProperty("new_events")
    private List<Object> newEvents;

    public Boolean getShouldContactUser() { return shouldContactUser; }
    public void setShouldContactUser(Boolean shouldContactUser) { this.shouldContactUser = shouldContactUser; }
    public Object getReply() { return reply; }
    public void setReply(Object reply) { this.reply = reply; }
    public Object getInnerThought() { return innerThought; }
    public void setInnerThought(Object innerThought) { this.innerThought = innerThought; }
    public Object getRelationshipDeltas() { return relationshipDeltas; }
    public void setRelationshipDeltas(Object relationshipDeltas) { this.relationshipDeltas = relationshipDeltas; }
    public Boolean getConversationEnded() { return conversationEnded; }
    public void setConversationEnded(Boolean conversationEnded) { this.conversationEnded = conversationEnded; }
    public Object getIntervalPreScheduling() { return intervalPreScheduling; }
    public void setIntervalPreScheduling(Object intervalPreScheduling) { this.intervalPreScheduling = intervalPreScheduling; }
    public Boolean getEventChanged() { return eventChanged; }
    public void setEventChanged(Boolean eventChanged) { this.eventChanged = eventChanged; }
    public List<String> getCancelledScheduledMessages() { return cancelledScheduledMessages; }
    public void setCancelledScheduledMessages(List<String> cancelledScheduledMessages) { this.cancelledScheduledMessages = cancelledScheduledMessages; }
    public List<String> getInvalidatedEvents() { return invalidatedEvents; }
    public void setInvalidatedEvents(List<String> invalidatedEvents) { this.invalidatedEvents = invalidatedEvents; }
    public List<Object> getNewEvents() { return newEvents; }
    public void setNewEvents(List<Object> newEvents) { this.newEvents = newEvents; }
}
