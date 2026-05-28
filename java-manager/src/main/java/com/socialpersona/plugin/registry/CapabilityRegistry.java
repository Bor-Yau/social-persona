package com.socialpersona.plugin.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 能力注册表 —— 能力查找 + 执行调度
 *
 * ★ 能力类型：
 *   被动能力（image_generation）：不影响消息流程，异步产出图片
 *
 * ★ 为什么用注册表而非硬编码 if-else：
 *   未来加新插件只需在注册表加一条记录，不改引擎代码。
 */
@Component
public class CapabilityRegistry {

    private static final Logger log = LoggerFactory.getLogger(CapabilityRegistry.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 能力名称 → 能力元数据 */
    private static final Map<String, Map<String, Object>> REGISTRY = Map.of(
            "image_generation", Map.of(
                    "name", "图片生成",
                    "type", "passive",
                    "description", "能按人设风格生成图片",
                    "python_endpoint", "/api/image/generate",
                    "requires_llm", false
            )
    );

    /**
     * 解析能力插件 JSON → 能力列表
     */
    @SuppressWarnings("unchecked")
    public List<String> parseEnabled(String enabledPluginsJson) {
        if (enabledPluginsJson == null || enabledPluginsJson.isEmpty()) return List.of();
        try {
            return objectMapper.readValue(enabledPluginsJson, List.class);
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 查询能力注册信息
     */
    public Map<String, Object> lookup(String pluginName) {
        return REGISTRY.get(pluginName);
    }

    /**
     * 是否启用某能力
     */
    public boolean isEnabled(String enabledPluginsJson, String pluginName) {
        return parseEnabled(enabledPluginsJson).contains(pluginName);
    }

    /**
     * 列出所有启用的能力元数据
     */
    public List<Map<String, Object>> listEnabled(String enabledPluginsJson) {
        List<String> names = parseEnabled(enabledPluginsJson);
        List<Map<String, Object>> result = new ArrayList<>();
        for (String name : names) {
            Map<String, Object> meta = lookup(name);
            if (meta != null) {
                result.add(meta);
            }
        }
        return result;
    }
}
