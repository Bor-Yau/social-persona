package com.socialpersona.config.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProviderController 请求体格式测试 —— 验证不同 api_format 的正确请求格式
 */
public class ProviderControllerFormatTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    // ==================== testPath ====================

    @Test
    public void testChatApiPath() {
        assertEquals("/chat/completions", ProviderController.buildTestPath("openai"));
        assertEquals("/chat/completions", ProviderController.buildTestPath("anthropic"));
        assertEquals("/chat/completions", ProviderController.buildTestPath(null));
    }

    @Test
    public void testImageApiPath() {
        assertEquals("/images/generations", ProviderController.buildTestPath("openai_image"));
    }

    // ==================== testRequestBody ====================

    @Test
    public void testChatRequestBodyFormat() throws Exception {
        String json = ProviderController.buildTestRequestBody("openai", "deepseek-chat");
        @SuppressWarnings("unchecked")
        Map<String, Object> body = mapper.readValue(json, Map.class);

        assertEquals("deepseek-chat", body.get("model"));
        assertEquals(10, body.get("max_tokens"));
        assertEquals(0, body.get("temperature"));
        assertTrue(body.containsKey("messages"), "chat 请求体应含 messages");
        assertFalse(body.containsKey("prompt"), "chat 请求体不应含 prompt");
        assertFalse(body.containsKey("n"), "chat 请求体不应含 n");
        assertFalse(body.containsKey("size"), "chat 请求体不应含 size");
    }

    @Test
    public void testOpenAiImageRequestBodyFormat() throws Exception {
        String json = ProviderController.buildTestRequestBody("openai_image", "dall-e-3");
        @SuppressWarnings("unchecked")
        Map<String, Object> body = mapper.readValue(json, Map.class);

        assertEquals("dall-e-3", body.get("model"));
        assertTrue(body.containsKey("prompt"), "图片请求体应含 prompt");
        assertFalse(body.containsKey("messages"), "图片请求体不应含 messages");
        assertFalse(body.containsKey("max_tokens"), "图片请求体不应含 max_tokens");
        assertFalse(body.containsKey("n"), "图片请求体不应含 n——Seedream 不认");
        assertFalse(body.containsKey("size"), "图片请求体不应含 size——不同 Provider 格式冲突");
    }

    @Test
    public void testVolcengineImageRequestBodyCompatible() throws Exception {
        // 火山引擎 Seedream 4.5 认可的最小请求体
        String json = ProviderController.buildTestRequestBody("openai_image", "doubao-seedream-4-5-251128");
        @SuppressWarnings("unchecked")
        Map<String, Object> body = mapper.readValue(json, Map.class);

        assertEquals("doubao-seedream-4-5-251128", body.get("model"));
        assertEquals("a cute cat on windowsill, digital art", body.get("prompt"));
        // 以下是 Seedream 4.5 不支持的参数——必须不存在
        assertFalse(body.containsKey("n"), "Seedream 4.5 不支持 n 参数");
        assertFalse(body.containsKey("size"), "Seedream 4.5 的 size 格式与 OpenAI 不兼容");

        // 验证只有 model + prompt 两个字段（最小请求体）
        assertEquals(2, body.size(), "Seedream 请求体应只有 model + prompt");
    }

    // ==================== parseTestResponse ====================

    @Test
    public void testParseChatResponse() {
        Map<String, Object> resp = Map.of("choices", List.of(
                Map.of("message", Map.of("content", "Hello!"))
        ));
        String result = ProviderController.parseTestResponse("openai", resp);
        assertEquals("Hello!", result);
    }

    @Test
    public void testParseImageResponse() {
        Map<String, Object> resp = Map.of("data", List.of(
                Map.of("url", "https://cdn.example.com/img/abc123.png")
        ));
        String result = ProviderController.parseTestResponse("openai_image", resp);
        assertTrue(result.contains("图片已生成"), "应返回图片生成成功标记");
    }

    @Test
    public void testParseImageResponseEmptyData() {
        Map<String, Object> resp = Map.of("data", List.of());
        String result = ProviderController.parseTestResponse("openai_image", resp);
        assertEquals("无图片返回", result);
    }

    @Test
    public void testParseImageResponseNoUrl() {
        Map<String, Object> resp = Map.of("data", List.of(Map.of("error", "fail")));
        String result = ProviderController.parseTestResponse("openai_image", resp);
        assertEquals("无图片返回", result);
    }

    @Test
    public void testParseChatResponseEmptyChoices() {
        Map<String, Object> resp = Map.of("choices", List.of());
        String result = ProviderController.parseTestResponse("openai", resp);
        assertEquals("无响应", result);
    }
}
