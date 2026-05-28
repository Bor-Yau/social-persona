package com.socialpersona.event.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 事件实体 —— 映射 daily_events 表
 *
 * ★ AI 的一天时间轴：每天凌晨由 LLM 生成，Java 定时扫描触发
 *
 * ★ 为什么要存 event_date + event_time 分开：
 *   event_date → 天级别筛选（今天的事件），event_time → 精确到秒的触发时刻
 *   如果合并为一个时间戳，查"今天的事件"需要 BETWEEN 两次时间戳计算
 *
 * ★ 三种事件类型的区别：
 *   routine  → 日常事件（起床/上班/吃饭），LLM 可能会也可能不会主动联系
 *   moment   → 特殊时刻（八卦/优惠/发现有趣的事），LLM 更倾向于联系用户
 *   sleep    → ★ 不调 LLM，直接触发状态机切换到 SLEEPING
 *
 * ★ is_active 的妙用：
 *   AI 可以自行作废事件：is_active=0 = 软删除
 *   真实入眠延后 = 作废旧 sleep + 插入新 sleep，全部通过 UPDATE is_active 实现
 */
@TableName("daily_events")
public class DailyEvent {

    /** 事件 UUID */
    @TableId
    private String id;

    /** 所属 Persona ID */
    private String personaId;

    /** 事件日期（yyyy-MM-dd），与 event_time 组合定位唯一时刻 */
    private String eventDate;

    /** 事件时间（HH:mm:ss），统一 6 位格式 */
    private String eventTime;

    /** 事件类型：routine | moment | sleep */
    private String eventType;

    /** 事件描述（自然语言，传给 LLM 做上下文） */
    private String description;

    /** 是否活跃：1=活跃，0=已作废 */
    private Integer isActive;

    /** 创建时间 */
    private String createdAt;

    /** 更新时间 */
    private String updatedAt;

    // ==================== Getter / Setter ====================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPersonaId() { return personaId; }
    public void setPersonaId(String personaId) { this.personaId = personaId; }

    public String getEventDate() { return eventDate; }
    public void setEventDate(String eventDate) { this.eventDate = eventDate; }

    public String getEventTime() { return eventTime; }
    public void setEventTime(String eventTime) { this.eventTime = eventTime; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getIsActive() { return isActive; }
    public void setIsActive(Integer isActive) { this.isActive = isActive; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
