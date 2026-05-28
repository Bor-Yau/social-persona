package com.socialpersona.middleware;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * MessageSplitter 单元测试 —— 验证 [SPLIT] 标记拆分逻辑
 *
 * 覆盖场景：
 *   1. 正常拆分（含 [SPLIT] 标记）
 *   2. 无标记（整段作为一条）
 *   3. 连续多个 [SPLIT]
 *   4. 边界：null、空字符串
 *   5. 含特殊字符的文本
 */
public class MessageSplitterTest {

    /**
     * 标准拆分：一个 [SPLIT] → 两段
     */
    @Test
    public void testSingleSplit() {
        String[] result = MessageSplitter.split("早啊[SPLIT]困死了，今天又迟到了五分钟");
        assertEquals(2, result.length);
        assertEquals("早啊", result[0]);
        assertEquals("困死了，今天又迟到了五分钟", result[1]);
    }

    /**
     * 多个 [SPLIT] → 多段
     */
    @Test
    public void testMultipleSplits() {
        String[] result = MessageSplitter.split("你好[SPLIT]我今天[SPLIT]有点累");
        assertEquals(3, result.length);
        assertEquals("你好", result[0]);
        assertEquals("我今天", result[1]);
        assertEquals("有点累", result[2]);
    }

    /**
     * 无 [SPLIT] 标记 → 整段作为一条
     */
    @Test
    public void testNoSplitMarker() {
        String[] result = MessageSplitter.split("我今天没什么想说的，就单纯想发条消息");
        assertEquals(1, result.length);
        assertEquals("我今天没什么想说的，就单纯想发条消息", result[0]);
    }

    /**
     * 连续两个 [SPLIT] 紧挨着 → 中间产生空字符串
     */
    @Test
    public void testConsecutiveSplits() {
        String[] result = MessageSplitter.split("嗨[SPLIT][SPLIT]好吧");
        assertEquals(3, result.length);
        assertEquals("嗨", result[0]);
        assertEquals("", result[1]);
        assertEquals("好吧", result[2]);
    }

    /**
     * 以 [SPLIT] 开头 → 第一段为空字符串
     */
    @Test
    public void testSplitAtStart() {
        String[] result = MessageSplitter.split("[SPLIT]你好");
        assertEquals(2, result.length);
        assertEquals("", result[0]);
        assertEquals("你好", result[1]);
    }

    /**
     * 以 [SPLIT] 结尾 → 最后一段为空字符串
     */
    @Test
    public void testSplitAtEnd() {
        String[] result = MessageSplitter.split("再见[SPLIT]");
        assertEquals(2, result.length);
        assertEquals("再见", result[0]);
        assertEquals("", result[1]);
    }

    /**
     * null 输入 → 返回含空字符串的数组
     */
    @Test
    public void testNullInput() {
        String[] result = MessageSplitter.split(null);
        assertEquals(1, result.length);
        assertEquals("", result[0]);
    }

    /**
     * 空字符串输入 → 返回含空字符串的数组
     */
    @Test
    public void testEmptyInput() {
        String[] result = MessageSplitter.split("");
        assertEquals(1, result.length);
        assertEquals("", result[0]);
    }

    /**
     * 只有 [SPLIT] 的文本
     */
    @Test
    public void testOnlySplitMarker() {
        String[] result = MessageSplitter.split("[SPLIT]");
        assertEquals(2, result.length);
        assertEquals("", result[0]);
        assertEquals("", result[1]);
    }

    /**
     * 长文本、500字不拆分 —— 验证 LLM 自主决定不拆分的场景
     */
    @Test
    public void testLongTextWithoutSplit() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            sb.append("字");
        }
        String longText = sb.toString();
        String[] result = MessageSplitter.split(longText);
        assertEquals(1, result.length);
        assertEquals(longText, result[0]);
    }
}