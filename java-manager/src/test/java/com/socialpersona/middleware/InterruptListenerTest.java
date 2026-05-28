package com.socialpersona.middleware;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * InterruptListener 单元测试 —— 验证发送序列中断机制
 *
 * 覆盖场景：
 *   1. 注册后未中断 → isInterrupted=false
 *   2. 手动中断 → isInterrupted=true
 *   3. 清除后 → isInterrupted 移除
 *   4. 未注册的 persona → isInterrupted=false
 *   5. 多个 persona 独立中断
 *   6. 重复中断不影响结果
 */
public class InterruptListenerTest {

    private InterruptListener listener;

    @BeforeEach
    public void setUp() {
        listener = new InterruptListener();
    }

    /**
     * 注册后默认未被中断
     */
    @Test
    public void testDefaultNotInterrupted() {
        listener.register("persona1");
        assertFalse(listener.isInterrupted("persona1"),
                "刚注册的 persona 不应被中断");
    }

    /**
     * 手动中断后 → isInterrupted 返回 true
     */
    @Test
    public void testInterruptSetsFlag() {
        listener.register("persona1");
        listener.interrupt("persona1");
        assertTrue(listener.isInterrupted("persona1"),
                "interrupt() 后应标记为中断");
    }

    /**
     * 中断后清除 → isInterrupted 变为 false
     */
    @Test
    public void testClearRemovesFlag() {
        listener.register("persona1");
        listener.interrupt("persona1");
        listener.clear("persona1");
        assertFalse(listener.isInterrupted("persona1"),
                "clear() 后应不再标记为中断");
    }

    /**
     * 未注册的 persona → isInterrupted 返回 false
     */
    @Test
    public void testUnregisteredPersonaNotInterrupted() {
        assertFalse(listener.isInterrupted("nonexistent"),
                "未注册的 persona 应返回 false");
    }

    /**
     * 多个 persona 独立中断，互不影响
     */
    @Test
    public void testMultiplePersonasIndependently() {
        listener.register("persona1");
        listener.register("persona2");
        listener.register("persona3");

        listener.interrupt("persona2");

        assertFalse(listener.isInterrupted("persona1"), "persona1 不应被中断");
        assertTrue(listener.isInterrupted("persona2"), "persona2 应被中断");
        assertFalse(listener.isInterrupted("persona3"), "persona3 不应被中断");
    }

    /**
     * 重复中断同一个 persona → 保持 true
     */
    @Test
    public void testDoubleInterruptKeepsTrue() {
        listener.register("persona1");
        listener.interrupt("persona1");
        listener.interrupt("persona1");
        assertTrue(listener.isInterrupted("persona1"),
                "重复中断应保持 true");
    }

    /**
     * 未注册就 clear → 不应抛出异常
     */
    @Test
    public void testClearUnregisteredDoesNotThrow() {
        listener.clear("persona1");
        assertFalse(listener.isInterrupted("persona1"),
                "清除未注册的 persona 不应抛异常");
    }

    /**
     * 注册后 clear（无中断）→ 状态正常
     */
    @Test
    public void testClearWithoutInterrupt() {
        listener.register("persona1");
        listener.clear("persona1");
        assertFalse(listener.isInterrupted("persona1"),
                "注册后直接 clear 应正常");
    }
}