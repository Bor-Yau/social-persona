package com.socialpersona.event.service;

import com.socialpersona.event.entity.DailyEvent;
import com.socialpersona.event.repository.EventMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventServiceTest {

    @Mock
    private EventMapper eventMapper;

    @InjectMocks
    private EventService eventService;

    @Test
    public void testHasTodayEventsTrue() {
        when(eventMapper.findAllToday(anyString(), anyString()))
                .thenReturn(List.of(new DailyEvent()));

        assertTrue(eventService.hasTodayEvents("persona-1"));
    }

    @Test
    public void testHasTodayEventsFalse() {
        when(eventMapper.findAllToday(anyString(), anyString()))
                .thenReturn(List.of());

        assertFalse(eventService.hasTodayEvents("persona-1"));
    }
}
