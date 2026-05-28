package com.socialpersona.error;

import com.socialpersona.persona.entity.Persona;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 系统异常处理器 —— Python/LLM 不可达时的降级回复生成
 *
 * ★ 核心原则：
 *   API 调用失败 → Java 不调 Python，按人设模板库 + 失败次数级别生成文本回复。
 *   让用户感觉"对面那人今天网不好/手机坏了"，而非"系统故障"。
 *
 * ★ 失败次数分级：
 *   1-2 次 → 轻度借口（"信号不太好"）
 *   3-4 次 → 中度借口（"这破手机又卡了"）
 *   5+  次 → 重度借口（"最近这软件是不是出bug了"）
 */
@Component
public class SystemErrorHandler {

    private final ConcurrentHashMap<String, AtomicInteger> failCounters = new ConcurrentHashMap<>();

    /**
     * 获取当前失败次数并自增
     */
    public int incrementFailCount(String personaId) {
        return failCounters.computeIfAbsent(personaId, k -> new AtomicInteger(0))
                .incrementAndGet();
    }

    /**
     * 成功后清零失败次数
     */
    public void resetFailCount(String personaId) {
        failCounters.remove(personaId);
    }

    /**
     * LLM 调用失败时的降级回复
     *
     * @param persona   人格实体（用于人设化回复，当前简单模式不依赖）
     * @param failCount 连续失败次数
     * @return 人设化降级文本
     */
    public String generateLlmFailReply(Persona persona, int failCount) {
        if (failCount <= 2) {
            return "信号不太好，我这边消息好像发不过去，等一下哈。";
        } else if (failCount <= 4) {
            return "这破手机又卡了，消息一直转圈圈发不出去……";
        } else {
            return "最近这软件是不是出bug了，老连不上。你先忙，我搞好了找你。";
        }
    }

    /**
     * 图片生成失败时的降级回复
     */
    public String generateImageFailReply(Persona persona, int imageFailCount) {
        if (imageFailCount <= 2) {
            return "图片好像没加载出来，你脑补一下那个画面吧。";
        } else {
            return "这破手机又抽风了，拍不了照片。";
        }
    }
}