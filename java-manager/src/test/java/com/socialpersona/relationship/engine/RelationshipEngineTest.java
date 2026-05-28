package com.socialpersona.relationship.engine;

import com.socialpersona.persona.entity.Persona;
import com.socialpersona.persona.repository.PersonaMapper;
import com.socialpersona.relationship.entity.RelationshipState;
import com.socialpersona.relationship.repository.RelationshipStateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RelationshipEngineTest {

    @Mock private RelationshipStateMapper stateMapper;
    @Mock private PersonaMapper personaMapper;

    @InjectMocks
    private RelationshipEngine engine;

    private Persona makePersona(String phase) {
        Persona p = new Persona();
        p.setId("test-persona-1");
        p.setName("小奈");
        p.setRelationshipPhase(phase);
        p.setAttachmentAnxiety(0.5);
        p.setAttachmentAvoidance(0.3);
        return p;
    }

    @Test
    public void testCreateDefaultStranger() {
        Persona persona = makePersona("stranger");
        when(personaMapper.selectById("test-persona-1")).thenReturn(persona);
        when(stateMapper.selectById("test-persona-1")).thenReturn(null, (RelationshipState) null);

        engine.getState("test-persona-1");

        ArgumentCaptor<RelationshipState> captor = ArgumentCaptor.forClass(RelationshipState.class);
        verify(stateMapper).insert(captor.capture());
        RelationshipState state = captor.getValue();
        assertEquals(10.0, state.getTrust(), 0.001);
        assertEquals(2.0, state.getCloseness(), 0.001);
    }

    @Test
    public void testCreateDefaultAcquaintance() {
        Persona persona = makePersona("acquaintance");
        when(personaMapper.selectById("test-persona-1")).thenReturn(persona);
        when(stateMapper.selectById("test-persona-1")).thenReturn(null, (RelationshipState) null);

        engine.getState("test-persona-1");

        ArgumentCaptor<RelationshipState> captor = ArgumentCaptor.forClass(RelationshipState.class);
        verify(stateMapper).insert(captor.capture());
        RelationshipState state = captor.getValue();
        assertEquals(40.0, state.getTrust(), 0.001);
        assertEquals(15.0, state.getCloseness(), 0.001);
    }

    @Test
    public void testCreateDefaultFriend() {
        Persona persona = makePersona("friend");
        when(personaMapper.selectById("test-persona-1")).thenReturn(persona);
        when(stateMapper.selectById("test-persona-1")).thenReturn(null, (RelationshipState) null);

        engine.getState("test-persona-1");

        ArgumentCaptor<RelationshipState> captor = ArgumentCaptor.forClass(RelationshipState.class);
        verify(stateMapper).insert(captor.capture());
        RelationshipState state = captor.getValue();
        assertEquals(60.0, state.getTrust(), 0.001);
        assertEquals(35.0, state.getCloseness(), 0.001);
    }

    @Test
    public void testCreateDefaultCloseFriend() {
        Persona persona = makePersona("close_friend");
        when(personaMapper.selectById("test-persona-1")).thenReturn(persona);
        when(stateMapper.selectById("test-persona-1")).thenReturn(null, (RelationshipState) null);

        engine.getState("test-persona-1");

        ArgumentCaptor<RelationshipState> captor = ArgumentCaptor.forClass(RelationshipState.class);
        verify(stateMapper).insert(captor.capture());
        RelationshipState state = captor.getValue();
        assertEquals(80.0, state.getTrust(), 0.001);
        assertEquals(60.0, state.getCloseness(), 0.001);
    }

    @Test
    public void testCreateDefaultNullPhase() {
        Persona persona = makePersona(null);
        when(personaMapper.selectById("test-persona-1")).thenReturn(persona);
        when(stateMapper.selectById("test-persona-1")).thenReturn(null, (RelationshipState) null);

        engine.getState("test-persona-1");

        ArgumentCaptor<RelationshipState> captor = ArgumentCaptor.forClass(RelationshipState.class);
        verify(stateMapper).insert(captor.capture());
        RelationshipState state = captor.getValue();
        assertEquals(50.0, state.getTrust(), 0.001);
        assertEquals(20.0, state.getCloseness(), 0.001);
    }

    @Test
    public void testCreateDefaultUnknownPhase() {
        Persona persona = makePersona("bestie");
        when(personaMapper.selectById("test-persona-1")).thenReturn(persona);
        when(stateMapper.selectById("test-persona-1")).thenReturn(null, (RelationshipState) null);

        engine.getState("test-persona-1");

        ArgumentCaptor<RelationshipState> captor = ArgumentCaptor.forClass(RelationshipState.class);
        verify(stateMapper).insert(captor.capture());
        RelationshipState state = captor.getValue();
        assertEquals(50.0, state.getTrust(), 0.001);
        assertEquals(20.0, state.getCloseness(), 0.001);
    }

    @Test
    public void testCreateDefaultNullPersona() {
        when(personaMapper.selectById("test-persona-1")).thenReturn(null);
        when(stateMapper.selectById("test-persona-1")).thenReturn(null, (RelationshipState) null);

        engine.getState("test-persona-1");

        ArgumentCaptor<RelationshipState> captor = ArgumentCaptor.forClass(RelationshipState.class);
        verify(stateMapper).insert(captor.capture());
        RelationshipState state = captor.getValue();
        assertEquals(50.0, state.getTrust(), 0.001);
        assertEquals(20.0, state.getCloseness(), 0.001);
    }
}
