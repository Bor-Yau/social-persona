package com.socialpersona.middleware;

/**
 * 消息拆分器 —— 按 [SPLIT] 标记切割 LLM 的 raw_text
 *
 * ★ 为什么不按标点或字符数切：
 *   LLM 按人设 + 情境自主决定拆分位置。日常碎片化的人，分手时也可能发 500 字小作文不拆分。
 *   机械按标点切 = 剥夺了 LLM 的表现力。
 *
 * ★ [SPLIT] 标记的设计理念（来自 CONTEXT.md）：
 *   LLM 在 raw_text 中自由嵌入 [SPLIT]，表示"这里要分开发"。
 *   typing_style.default_style 和 fragmentation_level 只是默认倾向，[SPLIT] 才是最终判决。
 *   没有 [SPLIT] 标记时，整段作为一条消息发送。
 */
public class MessageSplitter {

    /**
     * 按 [SPLIT] 标记拆分文本
     *
     * 示例：
     *   "早啊[SPLIT]困死了，今天又迟到了五分钟" → ["早啊", "困死了，今天又迟到了五分钟"]
     *   "我今天没什么想说的" → ["我今天没什么想说的"]
     *
     * @param rawText LLM 返回的完整文本（含可选的 [SPLIT] 标记）
     * @return 拆分后的片段数组，至少 1 条
     */
    public static String[] split(String rawText) {
        if (rawText == null || rawText.isEmpty()) {
            return new String[]{""};
        }
        return rawText.split("\\[SPLIT\\]", -1);
    }
}
