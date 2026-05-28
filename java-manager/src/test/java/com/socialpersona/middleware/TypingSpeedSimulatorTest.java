package com.socialpersona.middleware;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TypingSpeedSimulator 单元测试 —— 验证打字速度计算和思考停顿逻辑
 *
 * 覆盖场景：
 *   1. typingSpeed 优先于 inputMethod
 *   2. inputMethod 兜底速度
 *   3. 默认速度
 *   4. 语音/意念输入不限速
 *   5. 打字延迟计算
 *   6. 思考停顿公式验证
 */
public class TypingSpeedSimulatorTest {

    // ==================== getSpeed ====================

    /**
     * typingSpeed 明确设定时，忽略 inputMethod
     */
    @Test
    public void testTypingSpeedOverridesInputMethod() {
        double speed = TypingSpeedSimulator.getSpeed(5.0, "phone_thumb");
        assertEquals(5.0, speed, 0.01,
                "设定 typingSpeed=5.0 时应忽略 phone_thumb 的 1.5");
    }

    /**
     * typingSpeed 为 null → 使用 inputMethod 兜底速度
     */
    @Test
    public void testFallbackToInputMethod() {
        double speed = TypingSpeedSimulator.getSpeed(null, "keyboard");
        assertEquals(3.0, speed, 0.01, "keyboard 兜底速度应为 3.0 字/秒");
    }

    /**
     * voice_input → 999 字/秒（不限速）
     */
    @Test
    public void testVoiceInputSpeed() {
        double speed = TypingSpeedSimulator.getSpeed(null, "voice_input");
        assertEquals(999.0, speed, 0.01, "voice_input 应为 999 字/秒");
    }

    /**
     * phone_thumb → 1.5 字/秒
     */
    @Test
    public void testPhoneThumbSpeed() {
        double speed = TypingSpeedSimulator.getSpeed(null, "phone_thumb");
        assertEquals(1.5, speed, 0.01, "phone_thumb 应为 1.5 字/秒");
    }

    /**
     * phone_swipe → 2.5 字/秒
     */
    @Test
    public void testPhoneSwipeSpeed() {
        double speed = TypingSpeedSimulator.getSpeed(null, "phone_swipe");
        assertEquals(2.5, speed, 0.01, "phone_swipe 应为 2.5 字/秒");
    }

    /**
     * 都未设定 → 默认 2.0 字/秒
     */
    @Test
    public void testDefaultSpeed() {
        double speed = TypingSpeedSimulator.getSpeed(null, null);
        assertEquals(2.0, speed, 0.01, "无设定时应为默认 2.0 字/秒");
    }

    /**
     * typingSpeed 为 0 或负数 → 走 inputMethod 或默认值
     */
    @Test
    public void testZeroTypingSpeedFallsBack() {
        double speed = TypingSpeedSimulator.getSpeed(0.0, "keyboard");
        assertEquals(3.0, speed, 0.01, "typingSpeed=0 时应走 inputMethod 兜底");
    }

    // ==================== typingDelay ====================

    /**
     * 10 字、3 字/秒 → 约 3333ms
     */
    @Test
    public void testTypingDelayCalculation() {
        long delay = TypingSpeedSimulator.typingDelay("你好世界，今天天气真不错", null, 3.0);
        // 12 字 / 3 字/秒 = 4 秒 = 4000ms
        assertEquals(4000, delay, 50, "12 字以 3 字/秒应约 4000ms");
    }

    /**
     * 语音输入 → 延迟 0
     */
    @Test
    public void testVoiceInputNoDelay() {
        long delay = TypingSpeedSimulator.typingDelay("好长好长的一段话", "voice_input", null);
        assertEquals(0, delay, "语音输入不应有打字延迟");
    }

    /**
     * 空文本 → 延迟 0
     */
    @Test
    public void testEmptyTextNoDelay() {
        long delay = TypingSpeedSimulator.typingDelay("", null, null);
        assertEquals(0, delay, "空文本延迟应为 0");
    }

    // ==================== thinkingPause ====================

    /**
     * 思考停顿在 500~3000ms 范围内
     */
    @Test
    public void testThinkingPauseRange() {
        for (int i = 0; i < 20; i++) {
            long pause = TypingSpeedSimulator.thinkingPause(0.5, 0.3);
            assertTrue(pause >= 500, "思考停顿最低 500ms，实际 " + pause);
            assertTrue(pause <= 3000, "思考停顿最高约 3000ms，实际 " + pause);
        }
    }

    /**
     * 高焦虑型（anxiety=0.9）→ 思考停顿应偏短
     */
    @Test
    public void testHighAnxietyShorterPause() {
        // 多次采样取平均
        double sum = 0;
        int samples = 50;
        for (int i = 0; i < samples; i++) {
            sum += TypingSpeedSimulator.thinkingPause(0.9, 0.1);
        }
        double avgHighAnxiety = sum / samples;

        sum = 0;
        for (int i = 0; i < samples; i++) {
            sum += TypingSpeedSimulator.thinkingPause(0.1, 0.1);
        }
        double avgLowAnxiety = sum / samples;

        assertTrue(avgHighAnxiety < avgLowAnxiety,
                "高焦虑平均思考停顿应低于低焦虑：" + avgHighAnxiety + " vs " + avgLowAnxiety);
    }

    /**
     * 高回避型（avoidance=0.9）→ 思考停顿应偏长
     */
    @Test
    public void testHighAvoidanceLongerPause() {
        double sum = 0;
        int samples = 50;
        for (int i = 0; i < samples; i++) {
            sum += TypingSpeedSimulator.thinkingPause(0.1, 0.9);
        }
        double avgHighAvoidance = sum / samples;

        sum = 0;
        for (int i = 0; i < samples; i++) {
            sum += TypingSpeedSimulator.thinkingPause(0.1, 0.1);
        }
        double avgLowAvoidance = sum / samples;

        assertTrue(avgHighAvoidance > avgLowAvoidance,
                "高回避平均思考停顿应高于低回避：" + avgHighAvoidance + " vs " + avgLowAvoidance);
    }

    /**
     * 思考停顿最低不低于 500ms
     */
    @Test
    public void testThinkingPauseMinimum() {
        for (int i = 0; i < 30; i++) {
            long pause = TypingSpeedSimulator.thinkingPause(1.0, 0.0);
            assertTrue(pause >= 500,
                    "即使极端焦虑，思考停顿也不应低于 500ms，实际 " + pause);
        }
    }
}