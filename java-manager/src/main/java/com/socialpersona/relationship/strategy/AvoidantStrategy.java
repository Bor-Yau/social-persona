package com.socialpersona.relationship.strategy;

import com.socialpersona.persona.entity.Persona;
import com.socialpersona.relationship.engine.DerivedCoefficients;

/**
 * 回避型策略 —— 回避 ≥ 0.5
 *
 * ★ 核心特征（从依恋理论推导）：
 *   1. 张力增长率极低 —— "无所谓，不关我事"的态度（回避型不会因为冷漠而焦虑）
 *   2. 能量衰减极快 —— 情绪收放自如，从不纠缠
 *   3. 亲密阈值极高 —— "生人勿近"模式，进入深层关系极难
 *   4. 张力压强积攒慢 —— 几乎不主动联系
 */
public class AvoidantStrategy implements CoefficientStrategy {

    @Override
    public DerivedCoefficients derive(Persona p) {
        DerivedCoefficients c = new DerivedCoefficients();

        // ★ 张力增长率：回避型几乎不积攒张力 —— avoidance=0.5→0.5, avoidance=0.8→0.35
        // 为什么不是 0：完全 0 意味着 AI 永远不会主动，不真实。只是非常慢。
        c.setTensionGrowthMultiplier(0.8 - p.getAttachmentAvoidance() * 0.55);

        // ★ 能量衰减率：回避型情绪收得快 —— avoidance=0.5→0.80, avoidance=0.8→0.74
        // 为什么衰减比 SecureStrategy 更快：回避型"不在乎"，关系能量来去匆匆
        c.setEnergyDecayRate(0.90 - p.getAttachmentAvoidance() * 0.2);

        // ★ 信任易碎度：回避型信任本来就不深，跌快涨也无所谓
        // 所以 fragility 接近 1.0（甚至略低——不在乎就没那么容易碎）
        c.setTrustFragilityMultiplier(1.0 - p.getAttachmentAvoidance() * 0.1);

        // ★ 亲密阈值极高：avoidance=0.5→72.5, avoidance=0.8→77.0
        // 为什么不是 90+：阈值太高 AI 永远无法进入深层关系，但真实的人即使回避也可能被突破
        c.setClosenessThreshold(65.0 + p.getAttachmentAvoidance() * 15);

        // ★ 张力压强积攒极慢：回避型几乎不主动
        c.setTensionPressureBase(0.1 + p.getInitiativeTendency() * 0.2);

        c.setInitiativeAcceleration(p.getInitiativeTendency());

        return c;
    }
}
