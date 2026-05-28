package com.socialpersona.persona.dto;

/**
 * API 配置 DTO —— 解密后传给 Python 的 LLM 连接信息
 *
 * ★ 为什么存在：
 *   PersonaService.decryptApiConfig() 解密 api_key_encrypted 后，
 *   组装成这个对象传给调用方（MessageService → 塞进 MessageRequest.apiConfig）。
 *
 * ★ 安全约定：
 *   这个对象只在 Java 内存中短暂存在，不入库、不入日志、不序列化到前端。
 */
public class ApiConfigDTO {

    /** LLM 提供商：openai | deepseek | anthropic */
    private String provider;

    /** 解密后的明文 API Key（Python 用完即弃，Java 用完也尽快释放） */
    private String apiKey;

    /** API 基础 URL（可选，如 https://api.deepseek.com/v1） */
    private String baseUrl;

    /** 模型名：如 deepseek-chat / gpt-4o / claude-3.5-sonnet */
    private String model;

    public ApiConfigDTO() {}

    public ApiConfigDTO(String provider, String apiKey, String baseUrl, String model) {
        this.provider = provider;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    // ==================== Getter / Setter ====================

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}
