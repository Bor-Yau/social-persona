package com.socialpersona.config.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialpersona.event.scheduler.EventTriggerScheduler;
import com.socialpersona.persona.service.PersonaService;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SystemConfigController 单元测试 —— 图片生成提供商配置端点
 *
 * ★ 使用真实 ./data/system_config.json，在 @BeforeAll/@AfterAll 中备份/恢复
 */
public class SystemConfigControllerTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Path CONFIG_PATH = Path.of("./data/system_config.json");
    private static byte[] originalBackup;

    private MockMvc mockMvc;
    private SystemConfigController controller;
    private PersonaService personaService;

    @BeforeAll
    public static void backupConfig() throws Exception {
        Files.createDirectories(Path.of("./data"));
        if (Files.exists(CONFIG_PATH)) {
            originalBackup = Files.readAllBytes(CONFIG_PATH);
        } else {
            originalBackup = null;
        }
    }

    @AfterAll
    public static void restoreConfig() throws Exception {
        if (originalBackup != null) {
            Files.write(CONFIG_PATH, originalBackup);
        } else {
            Files.deleteIfExists(CONFIG_PATH);
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        // 重置配置文件为空配置（无图片提供商）
        Map<String, Object> empty = new java.util.LinkedHashMap<>();
        Files.write(CONFIG_PATH, objectMapper.writeValueAsBytes(empty));

        personaService = org.mockito.Mockito.mock(PersonaService.class);
        EventTriggerScheduler eventScheduler = org.mockito.Mockito.mock(EventTriggerScheduler.class);
        controller = new SystemConfigController(personaService, eventScheduler);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @AfterEach
    public void resetConfig() throws Exception {
        // @BeforeAll/@AfterAll 处理全局恢复，这里不需要额外操作
    }

    @Test
    public void testGetImageProviderNotConfigured() throws Exception {
        mockMvc.perform(get("/api/config/image-provider"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasImageKey").value(false))
                .andExpect(jsonPath("$.imageProvider").value(""));
    }

    @Test
    public void testSaveAndGetImageProvider() throws Exception {
        Map<String, String> body = Map.of(
                "imageProvider", "openai",
                "imageKey", "sk-test-key-12345",
                "imageBaseUrl", "https://api.openai.com/v1",
                "imageModel", "dall-e-3"
        );

        mockMvc.perform(post("/api/config/image-provider")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.imageProvider").value("openai"));

        mockMvc.perform(get("/api/config/image-provider"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasImageKey").value(true))
                .andExpect(jsonPath("$.imageProvider").value("openai"))
                .andExpect(jsonPath("$.imageModel").value("dall-e-3"))
                .andExpect(jsonPath("$.imageBaseUrl").value("https://api.openai.com/v1"));
    }

    @Test
    public void testImageKeyNotStoredInPlaintext() throws Exception {
        Map<String, String> body = Map.of(
                "imageProvider", "openai",
                "imageKey", "my-secret-key-123"
        );

        mockMvc.perform(post("/api/config/image-provider")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        String fileContent = Files.readString(CONFIG_PATH);
        org.junit.jupiter.api.Assertions.assertFalse(
                fileContent.contains("my-secret-key-123"),
                "明文 Key 不应出现在配置文件中"
        );
    }

    @Test
    public void testSaveImageProviderEmptyKey() throws Exception {
        Map<String, String> body = Map.of(
                "imageProvider", "openai",
                "imageKey", "",
                "imageBaseUrl", "",
                "imageModel", ""
        );

        mockMvc.perform(post("/api/config/image-provider")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/config/image-provider"))
                .andExpect(jsonPath("$.hasImageKey").value(false))
                .andExpect(jsonPath("$.imageProvider").value("openai"));
    }

    @Test
    public void testSaveImageProviderWithStandardFields() throws Exception {
        // ProviderSelector 发送的是标准字段名 (provider, apiKey, baseUrl, model)
        Map<String, String> body = Map.of(
                "provider", "openai",
                "apiKey", "sk-test-key",
                "baseUrl", "https://api.openai.com/v1",
                "model", "dall-e-3"
        );

        mockMvc.perform(post("/api/config/image-provider")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/config/image-provider"))
                .andExpect(jsonPath("$.hasImageKey").value(true))
                .andExpect(jsonPath("$.imageProvider").value("openai"));
    }
}
