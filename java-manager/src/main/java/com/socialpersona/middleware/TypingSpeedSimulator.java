package com.socialpersona.middleware;

import java.util.Map;

/**
 * 打字速度模拟器 —— 按人设输入方式计算消息延迟
 *
 * ★ 为什么需要模拟延迟：
 *   OpenAI API 返回一条消息只要 2 秒，没人打字这么快。
 *   按人设现实打字速度逐条延迟发送，让用户感知"对面是一个真人在打字"。
 *
 * ★ 速度取值（经讨论调整，不同于规格书原值）：
 *   - 具体速度由 typingSpeed 字段决定（牵线人按人设设定，非枚举）
 *   - 本类同时保留 input_method 的兜底速度（兼容未设 typingSpeed 的场景）
 *
 * ★ 思考停顿公式：
 *   基础 1~3 秒随机 − attachmentAnxiety×0.8 秒 + attachmentAvoidance×1.0 秒，最少 500ms
 *   为什么焦虑型快、回避型慢：
 *     焦虑型 — 时刻关注对方，想到就说，不停顿
 *     回避型 — 发消息前要想半天"这样说好不好"
 */
public class TypingSpeedSimulator {

    /** 输入方式兜底速度（字/秒）—— 仅在 typingSpeed 未设时使用 */
    private static final Map<String, Double> FALLBACK_SPEED = Map.of(
            "keyboard",      3.0,
            "phone_thumb",   1.5,
            "phone_swipe",   2.5,
            "voice_input",   999.0
    );

    /** 默认兜底速度 */
    private static final double DEFAULT_SPEED = 2.0;

    /**
     * 获取打字速度（字/秒）
     *
     * ★ 优先级：typingSpeed（牵线人设定）> input_method 兜底 > 默认 2.0
     *
     * @param typingSpeed 牵线人设定的具体速度（可为 null）
     * @param inputMethod 输入方式（可为 null）
     * @return 打字速度（字/秒）
     */
    public static double getSpeed(Double typingSpeed, String inputMethod) {
        if (typingSpeed != null && typingSpeed > 0) {
            return typingSpeed;
        }
        if (inputMethod != null) {
            return FALLBACK_SPEED.getOrDefault(inputMethod, DEFAULT_SPEED);
        }
        return DEFAULT_SPEED;
    }

    /**
     * 计算打字耗时（毫秒）
     *
     * @param text     文本内容
     * @param inputMethod 输入方式
     * @param typingSpeed 牵线人设定的打字速度（可覆盖 inputMethod）
     * @return 打字耗时（毫秒），语音输入返回 0
     */
    public static long typingDelay(String text, String inputMethod, Double typingSpeed) {
        double charsPerSec = getSpeed(typingSpeed, inputMethod);
        if (charsPerSec >= 999) return 0;      // 意念交流 / 语音 —— 不限速
        return (long) (text.length() / charsPerSec * 1000);
    }

    /**
     * 思考停顿（毫秒）
     *
     * ★ 为什么焦虑型偏短、回避型偏长：
     *   焦虑型 — 时刻关注对方，想到就说
     *   回避型 — 发消息前要想"这样说好不好"，犹豫更久
     *
     * @param attachmentAnxiety   依恋焦虑 0~1
     * @param attachmentAvoidance 依恋回避 0~1
     * @return 停顿时间（毫秒），最低 500ms
     */
    public static long thinkingPause(double attachmentAnxiety, double attachmentAvoidance) {
        double base = 1000 + Math.random() * 2000;          // 1~3 秒基础随机
        base -= attachmentAnxiety * 800;                     // 焦虑型 → 更短停顿
        base += attachmentAvoidance * 1000;                   // 回避型 → 更长停顿
        return (long) Math.max(500, base);                   // 至少 500ms
    }
}
