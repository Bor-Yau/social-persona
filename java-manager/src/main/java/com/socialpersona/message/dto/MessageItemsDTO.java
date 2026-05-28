package com.socialpersona.message.dto;

/**
 * 消息发送序列项 DTO —— LLM 返回的 items 数组元素
 *
 * ★ 两种类型的 item：
 *   text    → {type:"text", content:"你好", delay_ms:null}
 *   image   → {type:"image", content_prompt:"一只橘猫在窗台上晒太阳",
 *              generation_mode:"sync"|"async", delay_ms:null}
 *
 * ★ generation_mode：
 *   sync  = 主动场景（如发自拍），先生成图片再发，用户等待
 *   async = 聊天场景（如说"给你看个东西"），先发 stall_text 拖延，后台上图
 */
public class MessageItemsDTO {

    /** 类型：text | image */
    private String type;

    /** 文本内容（type=text 时必含） */
    private String content;

    /** 图片内容描述（type=image 时必含） */
    private String contentPrompt;

    /** 图片生成模式：sync | async */
    private String generationMode;

    /** 发送延迟（毫秒），null=按打字速度自动计算 */
    private Long delayMs;

    // ==================== Getter / Setter ====================

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getContentPrompt() { return contentPrompt; }
    public void setContentPrompt(String contentPrompt) { this.contentPrompt = contentPrompt; }

    public String getGenerationMode() { return generationMode; }
    public void setGenerationMode(String generationMode) { this.generationMode = generationMode; }

    public Long getDelayMs() { return delayMs; }
    public void setDelayMs(Long delayMs) { this.delayMs = delayMs; }
}
