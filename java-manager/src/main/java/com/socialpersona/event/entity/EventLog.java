package com.socialpersona.event.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 事件日志实体 —— 映射 event_log 表
 *
 * ★ 三种 log_type：
 *   inner_thought  → AI 的内心独白（每条消息回复附带）
 *   relationship_event → 关系里程碑（如 "亲密突破 60"）
 *   daily_reflection → 每日反思（凌晨事件线生成后写入）
 *
 * ★ 为什么存 detail_json 而非拆字段：
 *   不同 log_type 的数据结构完全不同——inner_thought 有 attitude/should_remember，
 *   relationship_event 有 old_value/new_value，daily_reflection 有 key_memories。
 *   JSON 列允许每种类型存自己的结构，无需 ALTER TABLE。
 */
@TableName("event_log")
public class EventLog {

    @TableId
    private String id;

    /** 所属 Persona */
    private String personaId;

    /** 日志类型：inner_thought | relationship_event | daily_reflection */
    private String logType;

    /** 详细数据 JSON */
    private String detailJson;

    /** 发生时间 */
    private String occurredAt;

    private String createdAt;

    // ==================== Getter / Setter ====================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPersonaId() { return personaId; }
    public void setPersonaId(String personaId) { this.personaId = personaId; }

    public String getLogType() { return logType; }
    public void setLogType(String logType) { this.logType = logType; }

    public String getDetailJson() { return detailJson; }
    public void setDetailJson(String detailJson) { this.detailJson = detailJson; }

    public String getOccurredAt() { return occurredAt; }
    public void setOccurredAt(String occurredAt) { this.occurredAt = occurredAt; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
