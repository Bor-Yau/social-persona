package com.socialpersona.middleware;

import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 用户中断监听器 —— 用户发新消息时中断当前正在发送的序列
 *
 * ★ 典型场景（图片 async 模式）：
 *   AI 决定发一张 async 图片 → 先发 stall_text → 后台生成图片 →
 *   如果用户在图片生成期间发了新消息 → 中断发送序列 → 丢弃未发 item（包括那张图）
 *
 * ★ 单用户单 Persona 场景假设：
 *   同一 persona_id 同时只存在一个活跃的发送序列。
 *   interrupt(personaId) → 该 Persona 的所有延迟消息立即作废。
 */
@Component
public class InterruptListener {

    /** personaId → 该 Persona 当前是否被中断 */
    private final ConcurrentHashMap<String, AtomicBoolean> interruptedMap = new ConcurrentHashMap<>();

    /**
     * 注册一个可中断的序列
     *
     * @param personaId Persona UUID
     */
    public void register(String personaId) {
        interruptedMap.put(personaId, new AtomicBoolean(false));
    }

    /**
     * 用户发新消息 → 中断当前序列
     *
     * @param personaId Persona UUID
     */
    public void interrupt(String personaId) {
        AtomicBoolean flag = interruptedMap.get(personaId);
        if (flag != null) {
            flag.set(true);
        }
    }

    /**
     * 检查是否已被中断
     *
     * ★ BurstGroupManager 在发送每条消息前调用，如果 true → 跳过剩余消息
     */
    public boolean isInterrupted(String personaId) {
        AtomicBoolean flag = interruptedMap.get(personaId);
        return flag != null && flag.get();
    }

    /**
     * 序列发送完毕 → 清除中断标记
     */
    public void clear(String personaId) {
        interruptedMap.remove(personaId);
    }
}
