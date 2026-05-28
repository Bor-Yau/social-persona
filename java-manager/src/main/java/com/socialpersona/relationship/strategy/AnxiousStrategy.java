package com.socialpersona.relationship.strategy;

import com.socialpersona.persona.entity.Persona;
import com.socialpersona.relationship.engine.DerivedCoefficients;

/**
 * 焦虑型策略 —— 焦虑 ≥ 0.5
 *
 * ★ 核心特征（从依恋理论推导）：
 *   1. 张力增长率显著放大 —— 对关系的波动极度敏感，一点点小事张力就飙升
 *   2. 信任极脆 —— 跌快涨慢效应被放大（一次失约需要十次真诚弥补）
 *   3. 能量衰减慢 —— 情绪黏着，不会自己消散（焦虑型的人"翻来覆去地想"）
 *   4. 亲密阈值低 —— 焦虑型的人容易进入深层关系（甚至太快）
 *
 * ★ 为什么焦虑优先于回避：
 *   如果 anxiety≥0.5 且 avoidance≥0.5（恐惧型依恋），选 AnxiousStrategy。
 *   因为"怕被抛弃"比"怕亲密"更底层——恐惧型在行为上常常表现为焦虑型的混合。
 */
public class AnxiousStrategy implements CoefficientStrategy {

    @Override
    public DerivedCoefficients derive(Persona p) {
        DerivedCoefficients c = new DerivedCoefficients();

        // ★ 张力增长率：基准 0.8 + 焦虑加成
        // anxiety=0.5 → 1.15, anxiety=0.8 → 1.36, anxiety=1.0 → 1.50
        // 为什么 1.0~1.5 而非更高：太高的增长率会让每次互动都像"过山车"
        c.setTensionGrowthMultiplier(0.8 + p.getAttachmentAnxiety() * 0.7);

        // ★ 能量衰减率：焦虑型的人情绪黏着，衰减更慢
        // anxiety=0.5 → 0.95, anxiety=0.8 → 0.98
        // 为什么不是 1.0：完全不衰减意味着情绪会无限积攒，这不合理
        c.setEnergyDecayRate(0.92 + p.getAttachmentAnxiety() * 0.06);

        // ★ 信任易碎度：焦虑型的信任如玻璃 —— anxiety=0.5→1.3, anxiety=1.0→1.8
        // 为什么放大这么多：信任易碎是焦虑型的定义性特征
        c.setTrustFragilityMultiplier(0.8 + p.getAttachmentAnxiety() * 1.0);

        // ★ 亲密阈值：焦虑型更快深入关系（有时太快了）
        // anxiety=0.5 → 70, anxiety=1.0 → 60
        c.setClosenessThreshold(80.0 - p.getAttachmentAnxiety() * 20);

        // ★ 张力压强：焦虑型积攒得更快（时刻处于"TA不回我"的边缘状态）
        c.setTensionPressureBase(0.5 + p.getInitiativeTendency() * 0.6);

        c.setInitiativeAcceleration(p.getInitiativeTendency());

        return c;
    }
}
