package com.socialpersona.gateway.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * MessageResponse DTO 字段映射测试
 *
 * ★ 核心验证：Python 返回的 reply.raw_text 能被 Java 正确反序列化。
 *   这是修复 extractReplyText Bug 的验收测试。
 *
 * 覆盖场景：
 *   1. 完整 JSON 反序列化（含 raw_text）
 *   2. should_reply=false 时不含 reply
 *   3. inner_thought 反序列化
 *   4. 空 reply
 */
public class MessageResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * ★ 关键测试：Python 返回的 reply 对象含 raw_text 字段，
     *   Jackson 应正确反序列化为 Map，可通过 "raw_text" 键取值。
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testReplyRawTextDeserialization() throws Exception {
        String json = """
            {
                "should_reply": true,
                "reply": {
                    "raw_text": "你好呀[SPLIT]今天过得怎么样？",
                    "items": [
                        {"type": "text", "content": "你好呀"},
                        {"type": "text", "content": "今天过得怎么样？"}
                    ],
                    "mood": "happy"
                },
                "inner_thought": {
                    "raw_thought": "这人还挺热情的",
                    "attitude": "positive",
                    "should_remember": true,
                    "memorable_point": "第一次打招呼"
                },
                "relationship_deltas": {
                    "trust_delta": 2.0,
                    "closeness_delta": 1.5
                },
                "conversation_ended": false
            }""";

        MessageResponse response = objectMapper.readValue(json, MessageResponse.class);

        assertTrue(response.getShouldReply(), "should_reply 应为 true");

        // ★ 验证 reply 反序列化为 Map，且含 raw_text 键
        Object replyObj = response.getReply();
        assertNotNull(replyObj, "reply 不应为 null");
        assertTrue(replyObj instanceof Map, "reply 应反序列化为 Map");

        Map<String, Object> replyMap = (Map<String, Object>) replyObj;
        assertEquals("你好呀[SPLIT]今天过得怎么样？", replyMap.get("raw_text"),
                "raw_text 字段应正确映射");
        assertEquals("happy", replyMap.get("mood"));

        // ★ 验证 inner_thought 反序列化
        Object thoughtObj = response.getInnerThought();
        assertNotNull(thoughtObj);
        assertTrue(thoughtObj instanceof Map);

        Map<String, Object> thoughtMap = (Map<String, Object>) thoughtObj;
        assertEquals("positive", thoughtMap.get("attitude"));
        assertEquals(true, thoughtMap.get("should_remember"));
    }

    /**
     * should_reply=false 时，reply 应为 null
     */
    @Test
    public void testShouldNotReply() throws Exception {
        String json = """
            {
                "should_reply": false,
                "reply": null,
                "inner_thought": {
                    "raw_thought": "不想理这个人",
                    "attitude": "negative"
                },
                "conversation_ended": true
            }""";

        MessageResponse response = objectMapper.readValue(json, MessageResponse.class);

        assertFalse(response.getShouldReply());
        assertNull(response.getReply(), "should_reply=false 时 reply 应为 null");
    }

    /**
     * conversation_ended=true + interval_pre_scheduling
     */
    @Test
    public void testConversationEndedWithScheduling() throws Exception {
        String json = """
            {
                "should_reply": true,
                "reply": {
                    "raw_text": "晚安[SPLIT]好梦",
                    "items": [],
                    "mood": "sleepy"
                },
                "conversation_ended": true,
                "interval_pre_scheduling": {
                    "scheduled_messages": [
                        {"scheduled_time": "08:00:00", "items": []}
                    ],
                    "next_event_time": "08:30:00"
                }
            }""";

        MessageResponse response = objectMapper.readValue(json, MessageResponse.class);

        assertTrue(response.getConversationEnded());
        assertNotNull(response.getIntervalPreScheduling());
    }

    /**
     * 最小响应：should_reply=false，其他字段取默认值
     */
    @Test
    public void testMinimalResponse() throws Exception {
        String json = "{\"should_reply\": false}";

        MessageResponse response = objectMapper.readValue(json, MessageResponse.class);

        assertFalse(response.getShouldReply());
        assertNull(response.getReply());
        assertNull(response.getInnerThought());
        assertNull(response.getConversationEnded());
    }

    /**
     * reply 中无 raw_text 但有 items 的场景
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testReplyWithOnlyItems() throws Exception {
        String json = """
            {
                "should_reply": true,
                "reply": {
                    "items": [
                        {"type": "text", "content": "hello"}
                    ],
                    "mood": "cheerful"
                }
            }""";

        MessageResponse response = objectMapper.readValue(json, MessageResponse.class);

        Object replyObj = response.getReply();
        assertTrue(replyObj instanceof Map);
        Map<String, Object> replyMap = (Map<String, Object>) replyObj;
        // raw_text 不存在时应为 null
        assertNull(replyMap.get("raw_text"));
    }
}