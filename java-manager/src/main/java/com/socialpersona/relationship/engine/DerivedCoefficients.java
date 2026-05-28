package com.socialpersona.relationship.engine;

/**
 * 派生系数 —— 从人格维度和策略推导出的关系引擎计算参数
 *
 * ★ 为什么不是 Persona 实体的一部分：
 *   这些参数是 Persona × 策略 = 计算结果，不是存储数据。
 *   同一 Persona 的系数始终一致（Persona 不改，系数不变），但理论上每次 getState() 重新 derive。
 *
 * ★ 各字段含义：
 *   tensionGrowthMultiplier   — 张力压强积攒速度的倍率（>1=焦虑型更快，<1=回避型更慢）
 *   energyDecayRate           — 情绪能量衰减率（0~1，每小时的保留比例）
 *   trustFragilityMultiplier  — 信任易碎度（>1=跌得快，焦虑型的信任像玻璃）
 *   closenessThreshold        — 亲密突破阈值（>70 表示更难进入"老夫老妻"阶段）
 *   tensionPressureBase       — 张力压强每小时积攒基数
 *   initiativeAcceleration    — 主动倾向加速度
 */
public class DerivedCoefficients {

    private double tensionGrowthMultiplier;
    private double energyDecayRate;
    private double trustFragilityMultiplier;
    private double closenessThreshold;
    private double tensionPressureBase;
    private double initiativeAcceleration;

    public DerivedCoefficients() {}

    // ==================== Setter ====================

    public void setTensionGrowthMultiplier(double tensionGrowthMultiplier) {
        this.tensionGrowthMultiplier = tensionGrowthMultiplier;
    }

    public void setEnergyDecayRate(double energyDecayRate) {
        this.energyDecayRate = energyDecayRate;
    }

    public void setTrustFragilityMultiplier(double trustFragilityMultiplier) {
        this.trustFragilityMultiplier = trustFragilityMultiplier;
    }

    public void setClosenessThreshold(double closenessThreshold) {
        this.closenessThreshold = closenessThreshold;
    }

    public void setTensionPressureBase(double tensionPressureBase) {
        this.tensionPressureBase = tensionPressureBase;
    }

    public void setInitiativeAcceleration(double initiativeAcceleration) {
        this.initiativeAcceleration = initiativeAcceleration;
    }

    // ==================== Getter ====================

    public double getTensionGrowthMultiplier() { return tensionGrowthMultiplier; }
    public double getEnergyDecayRate() { return energyDecayRate; }
    public double getTrustFragilityMultiplier() { return trustFragilityMultiplier; }
    public double getClosenessThreshold() { return closenessThreshold; }
    public double getTensionPressureBase() { return tensionPressureBase; }
    public double getInitiativeAcceleration() { return initiativeAcceleration; }
}
