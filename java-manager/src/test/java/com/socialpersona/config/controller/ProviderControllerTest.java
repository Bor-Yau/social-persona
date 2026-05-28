package com.socialpersona.config.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialpersona.persona.service.PersonaService;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ProviderController 单元测试 —— Type 过滤 + refreshModels fallback
 */
public class ProviderControllerTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    private ProviderController controller;

    @BeforeEach
    public void setUp() {
        controller = new ProviderController();
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void testGetAllProviders() throws Exception {
        mockMvc.perform(get("/api/config/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[0].type").value("chat"))
                .andExpect(jsonPath("$[4].type").value("image"));
    }

    @Test
    public void testGetChatProviders() throws Exception {
        mockMvc.perform(get("/api/config/providers?type=chat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].id").value("deepseek"))
                .andExpect(jsonPath("$[1].id").value("openai"))
                .andExpect(jsonPath("$[2].id").value("anthropic"))
                .andExpect(jsonPath("$[3].id").value("custom"));
    }

    @Test
    public void testGetImageProviders() throws Exception {
        mockMvc.perform(get("/api/config/providers?type=image"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value("openai_image"))
                .andExpect(jsonPath("$[1].id").value("volcengine_image"))
                .andExpect(jsonPath("$[2].id").value("custom_image"));
    }

    @Test
    public void testChatListHasNoImageProviders() throws Exception {
        mockMvc.perform(get("/api/config/providers?type=chat"))
                .andExpect(jsonPath("$[?(@.id == 'openai_image')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.id == 'volcengine_image')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.id == 'custom_image')]").doesNotExist());
    }

    @Test
    public void testImageListHasNoChatProviders() throws Exception {
        mockMvc.perform(get("/api/config/providers?type=image"))
                .andExpect(jsonPath("$[?(@.id == 'deepseek')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.id == 'openai')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.id == 'anthropic')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.id == 'custom')]").doesNotExist());
    }

    @Test
    public void testRefreshModelsOnCustomFallbackToBaseUrl() throws Exception {
        // custom_image 的 models_url=null，但传 baseUrl → 应 fallback 到 {baseUrl}/models
        // 不实际发起 HTTP 请求（会连不通），只验证不抛异常且返回合理的错误信息
        Map<String, String> body = Map.of(
                "apiKey", "sk-test",
                "baseUrl", "https://custom-image-service.com/v1"
        );

        // 应该尝试连接（会失败），但不应返回 "该 Provider 不支持在线获取模型列表"
        String content = mockMvc.perform(post("/api/config/providers/custom_image/refresh-models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // 不应是 models_url=null 的早期返回
        org.junit.jupiter.api.Assertions.assertFalse(
                content.contains("不支持在线获取模型列表"),
                "custom_image 有 baseUrl 时应尝试连接而非直接拒绝"
        );
    }
}
