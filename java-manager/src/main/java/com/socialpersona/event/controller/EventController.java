package com.socialpersona.event.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.socialpersona.event.entity.DailyEvent;
import com.socialpersona.event.entity.EventLog;
import com.socialpersona.event.repository.EventLogMapper;
import com.socialpersona.event.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/events")
public class EventController {

    @Autowired
    private EventService eventService;

    @Autowired
    private EventLogMapper eventLogMapper;

    @GetMapping("/{personaId}/today")
    public List<DailyEvent> todayEvents(@PathVariable String personaId) {
        return eventService.findTodayEvents(personaId);
    }

    @GetMapping("/{personaId}/thought")
    public Map<String, Object> latestThought(@PathVariable String personaId) {
        LambdaQueryWrapper<EventLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventLog::getPersonaId, personaId)
               .eq(EventLog::getLogType, "inner_thought")
               .orderByDesc(EventLog::getOccurredAt)
               .last("LIMIT 1");
        EventLog log = eventLogMapper.selectOne(wrapper);
        if (log == null) return Map.of();
        return Map.of(
                "raw_thought", log.getDetailJson(),
                "occurred_at", log.getOccurredAt()
        );
    }
}
