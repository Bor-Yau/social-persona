package com.socialpersona.error;

import com.socialpersona.persona.entity.Persona;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * SystemErrorHandler 单元测试 —— 验证降级回复模板 + 失败计数器
 */
public class SystemErrorHandlerTest {

    private SystemErrorHandler handler;
    private Persona testPersona;

    @BeforeEach
    public void setUp() {
        handler = new SystemErrorHandler();
        testPersona = new Persona();
        testPersona.setId("persona1");
        testPersona.setName("小奈");
    }

    // ==================== 失败计数器 ====================

    @Test
    public void testIncrementFailCountStartsAt1() {
        assertEquals(1, handler.incrementFailCount("persona1"));
    }

    @Test
    public void testIncrementFailCountIncreases() {
        handler.incrementFailCount("persona1");
        assertEquals(2, handler.incrementFailCount("persona1"));
    }

    @Test
    public void testResetFailCount() {
        handler.incrementFailCount("persona1");
        handler.incrementFailCount("persona1");
        handler.resetFailCount("persona1");
        assertEquals(1, handler.incrementFailCount("persona1"),
                "重置后首次计数应从 1 开始");
    }

    @Test
    public void testMultiplePersonasHaveSeparateCounters() {
        handler.incrementFailCount("persona1");
        handler.incrementFailCount("persona1");
        handler.incrementFailCount("persona2");

        assertEquals(3, handler.incrementFailCount("persona1"));
        assertEquals(2, handler.incrementFailCount("persona2"));
    }

    // ==================== LLM 降级回复 ====================

    @Test
    public void testLlmFailReplyLevel1() {
        String reply = handler.generateLlmFailReply(testPersona, 1);
        assertTrue(reply.contains("信号不太好"), "1 次失败应提示信号不好");
    }

    @Test
    public void testLlmFailReplyLevel2() {
        String reply = handler.generateLlmFailReply(testPersona, 3);
        assertTrue(reply.contains("破手机"), "3 次失败应提示手机卡");
    }

    @Test
    public void testLlmFailReplyLevel3() {
        String reply = handler.generateLlmFailReply(testPersona, 5);
        assertTrue(reply.contains("bug"), "5 次失败应提示软件bug");
    }

    // ==================== 图片降级回复 ====================

    @Test
    public void testImageFailReplyLevel1() {
        String reply = handler.generateImageFailReply(testPersona, 1);
        assertTrue(reply.contains("脑补"), "1 次图片失败应提示脑补");
    }

    @Test
    public void testImageFailReplyLevel2() {
        String reply = handler.generateImageFailReply(testPersona, 5);
        assertTrue(reply.contains("抽风"), "5 次图片失败应提示手机抽风");
    }

    // ==================== null/边界 ====================

    @Test
    public void testGenerateLlmFailReplyWithNullPersona() {
        // 应正常工作，不依赖 persona
        String reply = handler.generateLlmFailReply(null, 1);
        assertNotNull(reply);
        assertFalse(reply.isEmpty());
    }

    @Test
    public void testFailCountLowerBound() {
        assertEquals(1, handler.incrementFailCount("new-persona"),
                "新 persona 首次自增应为 1");
    }
}