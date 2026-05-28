package com.socialpersona.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GET /api/health 响应体
 *
 * ★ @JsonProperty 解决 snake_case → camelCase 映射：
 *   Python: {"status":"ok", "llm_connected":true, "memory_connected":true}
 *   Java:   HealthResponse { status="ok", llmConnected=true, memoryConnected=true }
 */
public class HealthResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("llm_connected")
    private Boolean llmConnected;

    @JsonProperty("memory_connected")
    private Boolean memoryConnected;

    @JsonProperty("uptime_seconds")
    private Long uptimeSeconds;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getLlmConnected() { return llmConnected; }
    public void setLlmConnected(Boolean llmConnected) { this.llmConnected = llmConnected; }
    public Boolean getMemoryConnected() { return memoryConnected; }
    public void setMemoryConnected(Boolean memoryConnected) { this.memoryConnected = memoryConnected; }
    public Long getUptimeSeconds() { return uptimeSeconds; }
    public void setUptimeSeconds(Long uptimeSeconds) { this.uptimeSeconds = uptimeSeconds; }
}
