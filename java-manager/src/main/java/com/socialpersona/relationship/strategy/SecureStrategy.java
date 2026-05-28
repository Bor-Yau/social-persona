package com.socialpersona.relationship.strategy;

import com.socialpersona.persona.entity.Persona;
import com.socialpersona.relationship.engine.DerivedCoefficients;

/**
 * 安全型策略 —— 低焦虑 + 低回避
 *
 * ★ 适用条件：anxiety < 0.5 && avoidance < 0.5
 *
 * ★ 设计原理（公式为什么这样写）：
 *   安全型人格是"基准线"，所有公式以 1.0 为基准，用连续维度微调。
 *   这样 AnxiousStrategy 和 AvoidantStrategy 只需在安全型的基础上放大/缩小即可。
 */
public class SecureStrategy implements CoefficientStrategy {

    @Override
    public DerivedCoefficients derive(Persona p) {
        DerivedCoefficients c = new DerivedCoefficients();

        // 张力增长率：0.8 ~ 1.3（焦虑越高越焦虑）
        // 基准 0.8 → anxiety=0.0 时 0.8, anxiety=0.5 时 1.05
        c.setTensionGrowthMultiplier(0.8 + p.getAttachmentAnxiety() * 0.5);

        // 能量衰减率：0.85 ~ 0.95（回避越高衰减越慢——憋着不放）
        c.setEnergyDecayRate(0.85 + p.getAttachmentAvoidance() * 0.1);

        // 信任易碎度：0.8 ~ 1.2（焦虑越高越易碎）
        c.setTrustFragilityMultiplier(0.8 + p.getAttachmentAnxiety() * 0.4);

        // 亲密阈值：65 ~ 80（回避越高阈值越高——更难深入）
        c.setClosenessThreshold(80.0 - p.getAttachmentAvoidance() * 15);

        // 张力压强基数：0.3 ~ 0.8（initiative_tendency 越高积攒越快）
        c.setTensionPressureBase(0.3 + p.getInitiativeTendency() * 0.5);

        // 主动倾向加速度 = initiative_tendency 本身
        c.setInitiativeAcceleration(p.getInitiativeTendency());

        return c;
    }
}
