package com.socialpersona.message.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 消息实体 —— 映射 scheduled_messages 表
 *
 * ★ 消息的两种生命路径：
 *
 *   now 消息（LLM返回 scheduled_time="now"）：
 *     Java 将时间替换为 Instant.now()+1s → WebSocket 立即推送 → 直接标记 is_sent=1 → 写入 DB
 *
 *   定时消息（LLM返回 scheduled_time="14:30:00"，间隔预判产出）：
 *     直接写 DB（is_sent=0） → MessageScanScheduler 到点扫描 → 推送 → 标记 is_sent=1
 *
 * ★ now 消息为什么也入库：
 *   前端刷新页面时需要加载历史消息。纯 WebSocket 推送无法回看历史。
 *
 * ★ burst_group_id 连发组机制：
 *   同一 burst_group_id 的多条消息按 burst_order 排序。
 *   第 1 条 WebSocket 立即推，其余按打字速度逐条延迟推送。
 *   模拟真人一口气发多条消息的延迟——每打完一条会喘口气再打第二条。
 */
@TableName("scheduled_messages")
public class ScheduledMessage {

    /** 消息 UUID */
    @TableId
    private String id;

    /** 所属 Persona */
    private String personaId;

    /** 发送时间：HH:mm:ss（入库前 "now" 已被替换为实际时间戳） */
    private String scheduledTime;

    /** 实际发送时间戳（调试用，before/after 对比） */
    private String actualSendTime;

    /** 连发组 UUID（同组消息共享，null=独立消息） */
    private String burstGroupId;

    /** 组内序号（0, 1, 2...） */
    private Integer burstOrder;

    /** 发送序列 JSON：[{type, content, delay_ms, ...}] */
    private String itemsJson;

    /** 内心独白 JSON（不发给用户，存入 event_log） */
    private String innerThoughtJson;

    /** 心情标签 */
    private String mood;

    /** 已发送标记：0=待发，1=已发 */
    private Integer isSent;

    private String createdAt;

    private String updatedAt;

    // ==================== Getter / Setter ====================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPersonaId() { return personaId; }
    public void setPersonaId(String personaId) { this.personaId = personaId; }

    public String getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(String scheduledTime) { this.scheduledTime = scheduledTime; }

    public String getActualSendTime() { return actualSendTime; }
    public void setActualSendTime(String actualSendTime) { this.actualSendTime = actualSendTime; }

    public String getBurstGroupId() { return burstGroupId; }
    public void setBurstGroupId(String burstGroupId) { this.burstGroupId = burstGroupId; }

    public Integer getBurstOrder() { return burstOrder; }
    public void setBurstOrder(Integer burstOrder) { this.burstOrder = burstOrder; }

    public String getItemsJson() { return itemsJson; }
    public void setItemsJson(String itemsJson) { this.itemsJson = itemsJson; }

    public String getInnerThoughtJson() { return innerThoughtJson; }
    public void setInnerThoughtJson(String innerThoughtJson) { this.innerThoughtJson = innerThoughtJson; }

    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }

    public Integer getIsSent() { return isSent; }
    public void setIsSent(Integer isSent) { this.isSent = isSent; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
