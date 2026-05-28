package com.socialpersona.relationship.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AIStateMachine TDD 测试 —— 覆盖全部状态转移路径
 *
 * ★ 核心原则（来自规格书）：
 *   状态机只看 event_type，不看 description。
 *   sleep 事件 → SLEEPING，起床事件 → ACTIVE，拉黑 → ARCHIVED。
 */
public class AIStateMachineTest {

    private AIStateMachine stateMachine;

    @BeforeEach
    public void setUp() {
        stateMachine = new AIStateMachine();
    }

    // ==================== 正常生命周期 ====================

    /**
     * 初始状态：无记录 → ACTIVE（默认）
     */
    @Test
    public void testDefaultStateIsActive() {
        assertEquals(AIStatus.ACTIVE, stateMachine.getState("persona1"));
    }

    /**
     * sleep 事件 → ACTIVE → SLEEPING
     * 核心规则：到点就睡，不调 LLM
     */
    @Test
    public void testSleepEventTriggersSleeping() {
        stateMachine.transitionTo("persona1", AIStatus.ACTIVE);
        stateMachine.onSleepEvent("persona1");
        assertEquals(AIStatus.SLEEPING, stateMachine.getState("persona1"));
    }

    /**
     * 起床事件 → SLEEPING → ACTIVE
     */
    @Test
    public void testWakeUpEventTriggersActive() {
        stateMachine.transitionTo("persona1", AIStatus.SLEEPING);
        stateMachine.onWakeUpEvent("persona1");
        assertEquals(AIStatus.ACTIVE, stateMachine.getState("persona1"));
    }

    /**
     * 用户拉黑 → ACTIVE → ARCHIVED
     */
    @Test
    public void testUserBlockTriggersArchived() {
        stateMachine.transitionTo("persona1", AIStatus.ACTIVE);
        stateMachine.onUserBlock("persona1");
        assertEquals(AIStatus.ARCHIVED, stateMachine.getState("persona1"));
    }

    /**
     * 拉黑 → 请求重新联系 → REQUEST_PENDING
     */
    @Test
    public void testReconnectRequestFromArchived() {
        stateMachine.transitionTo("persona1", AIStatus.ARCHIVED);
        stateMachine.onReconnectRequest("persona1");
        assertEquals(AIStatus.REQUEST_PENDING, stateMachine.getState("persona1"));
    }

    /**
     * AI 同意重新联系 → REQUEST_PENDING → ACTIVE
     */
    @Test
    public void testReconnectAccepted() {
        stateMachine.transitionTo("persona1", AIStatus.REQUEST_PENDING);
        stateMachine.onReconnectAccepted("persona1");
        assertEquals(AIStatus.ACTIVE, stateMachine.getState("persona1"));
    }

    /**
     * AI 拒绝重新联系 → REQUEST_PENDING → ARCHIVED（回到）
     */
    @Test
    public void testReconnectRejected() {
        stateMachine.transitionTo("persona1", AIStatus.REQUEST_PENDING);
        stateMachine.onReconnectRejected("persona1");
        assertEquals(AIStatus.ARCHIVED, stateMachine.getState("persona1"));
    }

    // ==================== 错误路径防护 ====================

    /**
     * sleep 事件 → 已 SLEEPING → 不应切换状态（不能睡两次）
     */
    @Test
    public void testSleepEventWhenAlreadySleeping() {
        stateMachine.transitionTo("persona1", AIStatus.SLEEPING);
        stateMachine.onSleepEvent("persona1");
        assertEquals(AIStatus.SLEEPING, stateMachine.getState("persona1"),
                "已经睡着时不应再次触发 sleep");
    }

    /**
     * sleep 事件 → ARCHIVED → 不切换（已归档的人格不会睡觉）
     */
    @Test
    public void testSleepEventWhenArchived() {
        stateMachine.transitionTo("persona1", AIStatus.ARCHIVED);
        stateMachine.onSleepEvent("persona1");
        assertEquals(AIStatus.ARCHIVED, stateMachine.getState("persona1"),
                "归档人格不会触发 sleep 状态切换");
    }

    /**
     * 用户消息 → SLEEPING → 不应唤醒（需要起床事件）
     * 用户发消息不应该把 AI 吵醒——只有起床事件能唤醒。
     */
    @Test
    public void testUserMessageDoesNotWakeUp() {
        stateMachine.transitionTo("persona1", AIStatus.SLEEPING);
        stateMachine.onUserMessage("persona1");
        assertEquals(AIStatus.SLEEPING, stateMachine.getState("persona1"),
                "用户发消息不应唤醒 AI，只有起床事件才行");
    }
}
