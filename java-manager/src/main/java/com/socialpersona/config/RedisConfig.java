package com.socialpersona.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 缓存配置类
 *
 * 核心机制：
 *   1. @EnableCaching 启动 Spring Cache 抽象层
 *   2. 配置 CacheManager：不同缓存名 → 不同 TTL
 *   3. 序列化用 JSON（GenericJackson2JsonRedisSerializer）—— 存什么类型取回来还是什么类型
 *
 * 降级策略：
 *   Redis 连不上时，Spring Cache 自动跳过缓存层，直接走原方法（如 DB 查询）。
 *   服务不会崩溃，只是缓存不生效。
 *
 * 当前 4 个缓存区：
 *   persona       — Persona 配置，30 分钟（极少改动）
 *   relation      — RelationshipState，5 分钟（每次回复后更新，短 TTL 保新鲜）
 *   session       — Matchmaker 会话，60 分钟（访谈通常 10~20 分钟内完成）
 *   default       — 其他，10 分钟
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * 缓存管理器 —— 按缓存名分别设置 TTL
     *
     * ★ 为什么不用默认的单一 TTL：
     *   Persona 配置几乎不变 → 长 TTL（30 分钟）
     *   RelationshipState 每次对话都变 → 短 TTL（5 分钟）
     *   如果不分区，要么 Persona 缓存太短（频繁失效），要么 RelationshipState 缓存太长（读到过期数据）
     */
    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        // 默认序列化：Key 用字符串，Value 用 JSON
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues();  // 与其他缓存区策略不同

        // 各缓存区自定义 TTL
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        // persona 缓存：30 分钟（Persona 配置极少变动，长 TTL 最大化命中率）
        cacheConfigs.put("persona", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // relation 缓存：5 分钟（RelationshipState 每次回复后更新，短 TTL 保证新鲜）
        cacheConfigs.put("relation", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // session 缓存：60 分钟（牵线人访谈会话，访谈完成后清除）
        cacheConfigs.put("session", defaultConfig.entryTtl(Duration.ofMinutes(60)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
