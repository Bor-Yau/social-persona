package com.socialpersona.relationship.dto;

/**
 * 关系变化增量 DTO —— Python LLM 返回的 relationship_deltas
 *
 * ★ 来源：Python POST /api/message 或 /api/event/trigger 的响应
 *
 * ★ 为什么 LLM 输出增量而非绝对值：
 *   Java 侧维护关系状态的真实值（DB 中的 source of truth）。
 *   LLM 只判断"这次互动让关系往哪个方向变了多少"——它不需要知道绝对值。
 */
public class RelationshipDeltasDTO {

    /** 信任变化量（正=涨，负=跌） */
    private Double trustDelta;

    /** 亲密变化量 */
    private Double closenessDelta;

    /** 张力变化量 */
    private Double tensionDelta;

    /** 情绪能量变化量 */
    private Double emotionalEnergyDelta;

    /** 是否为质变事件（表白/大吵/拉黑）——决定是否突破 closeness 85 天花板 */
    private Boolean isQualitativeLeap;

    /** 联系冲动变化量 */
    private Double contactUrgeDelta;

    // ==================== Getter / Setter ====================

    public Double getTrustDelta() { return trustDelta; }
    public void setTrustDelta(Double trustDelta) { this.trustDelta = trustDelta; }

    public Double getClosenessDelta() { return closenessDelta; }
    public void setClosenessDelta(Double closenessDelta) { this.closenessDelta = closenessDelta; }

    public Double getTensionDelta() { return tensionDelta; }
    public void setTensionDelta(Double tensionDelta) { this.tensionDelta = tensionDelta; }

    public Double getEmotionalEnergyDelta() { return emotionalEnergyDelta; }
    public void setEmotionalEnergyDelta(Double emotionalEnergyDelta) { this.emotionalEnergyDelta = emotionalEnergyDelta; }

    public Boolean getIsQualitativeLeap() { return isQualitativeLeap; }
    public void setIsQualitativeLeap(Boolean isQualitativeLeap) { this.isQualitativeLeap = isQualitativeLeap; }

    public Double getContactUrgeDelta() { return contactUrgeDelta; }
    public void setContactUrgeDelta(Double contactUrgeDelta) { this.contactUrgeDelta = contactUrgeDelta; }
}
