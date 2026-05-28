package com.socialpersona.relationship.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

/**
 * 关系状态实体 —— 映射 relationship_state 表
 *
 * ★ 与 Persona 的关系：一对一（persona_id 同时是 PK 和 FK）
 *
 * ★ 核心字段说明：
 *   trust（信任）0~100   —— 初始 50，陌生人距离。跌快涨慢（信任易碎效应）
 *   closeness（亲密）0~100—— 初始 20，刚认识。20-60 全量生效、60-85 减速、85+ 需质变
 *   tension（张力）0~100  —— 未解决摩擦，自然衰减（×0.95^hours）
 *   emotional_energy（情绪能量）0~100 —— 初始 30 而非 0，刚认识应有新鲜感
 *   tension_pressure（张力压强）—— 冲动积攒器，达到阈值时 LLM 判断是否主动联系
 *   contact_urge（联系冲动）—— 表示 AI 当前有多想找人说话
 *   last_heartbeat_at —— 惰性衰减的时间锚点
 *
 * ★ 为什么不用枚举常量类：
 *   0~100 范围由 SQL CHECK 约束保证，代码层不需要重复校验。
 */
@TableName("relationship_state")
public class RelationshipState implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Persona ID（主键 + 外键） */
    @TableId
    private String personaId;

    /** 信任度 0~100 */
    private Double trust;

    /** 亲密感 0~100 */
    private Double closeness;

    /** 张力（未解决摩擦） */
    private Double tension;

    /** 情绪能量 0~100（初始 30，不是 0 —— 刚认识有新鲜感） */
    private Double emotionalEnergy;

    /** 张力压强 —— 联系冲动的积攒速度 */
    private Double tensionPressure;

    /** 联系冲动 —— 当前多想找人说话 */
    private Double contactUrge;

    /** 上次心跳时间（惰性衰减的基准时间戳） */
    private String lastHeartbeatAt;

    /** 最后更新时间 */
    private String updatedAt;

    // ==================== Getter / Setter ====================

    public String getPersonaId() { return personaId; }
    public void setPersonaId(String personaId) { this.personaId = personaId; }

    public Double getTrust() { return trust; }
    public void setTrust(Double trust) { this.trust = trust; }

    public Double getCloseness() { return closeness; }
    public void setCloseness(Double closeness) { this.closeness = closeness; }

    public Double getTension() { return tension; }
    public void setTension(Double tension) { this.tension = tension; }

    public Double getEmotionalEnergy() { return emotionalEnergy; }
    public void setEmotionalEnergy(Double emotionalEnergy) { this.emotionalEnergy = emotionalEnergy; }

    public Double getTensionPressure() { return tensionPressure; }
    public void setTensionPressure(Double tensionPressure) { this.tensionPressure = tensionPressure; }

    public Double getContactUrge() { return contactUrge; }
    public void setContactUrge(Double contactUrge) { this.contactUrge = contactUrge; }

    public String getLastHeartbeatAt() { return lastHeartbeatAt; }
    public void setLastHeartbeatAt(String lastHeartbeatAt) { this.lastHeartbeatAt = lastHeartbeatAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
