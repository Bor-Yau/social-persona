package com.socialpersona.relationship.strategy;

import com.socialpersona.persona.entity.Persona;
import com.socialpersona.relationship.engine.CoefficientDeriver;
import com.socialpersona.relationship.engine.DerivedCoefficients;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 系数策略 TDD 测试 —— 先写测试，再写实现
 *
 * 测试覆盖三个维度：
 *   1. 策略选择逻辑（CoefficientDeriver.select）
 *   2. 各策略的系数推导值（是否符合人格维度预期）
 *   3. 边界情况（双低/双高/边界值）
 */
public class CoefficientStrategyTest {

    /**
     * 焦虑度 ≥ 0.5 → 应选 AnxiousStrategy
     * 验证：高焦虑人格推导出的张力增长率应该显著高于基准（1.0）
     */
    @Test
    public void testAnxiousStrategyIncreasesGrowth() {
        Persona persona = new Persona();
        persona.setAttachmentAnxiety(0.8);
        persona.setAttachmentAvoidance(0.1);
        persona.setInitiativeTendency(0.5);

        CoefficientStrategy strategy = new AnxiousStrategy();
        DerivedCoefficients coeff = strategy.derive(persona);

        // 高焦虑 → 张力增长率 > 1.3× 基准
        assertTrue(coeff.getTensionGrowthMultiplier() > 1.3,
                "焦虑度 0.8 时，张力增长率应 > 1.3，实际 = " + coeff.getTensionGrowthMultiplier());

        // 高焦虑 → 信任更易碎
        assertTrue(coeff.getTrustFragilityMultiplier() > 1.0,
                "焦虑型人格的信任易碎度应 > 1.0，实际 = " + coeff.getTrustFragilityMultiplier());

        // 高焦虑 → 衰减慢（憋着不放）
        assertTrue(coeff.getEnergyDecayRate() > 0.9,
                "焦虑型人格能量衰减应慢（> 0.9），因为情绪不容易自己消散");
    }

    /**
     * 回避度 ≥ 0.5 → 应选 AvoidantStrategy
     * 验证：高回避人格亲密阈值高（难熟）、张力增长率低（几乎不主动）
     */
    @Test
    public void testAvoidantStrategySlowsCloseness() {
        Persona persona = new Persona();
        persona.setAttachmentAnxiety(0.1);
        persona.setAttachmentAvoidance(0.8);
        persona.setInitiativeTendency(0.2);

        CoefficientStrategy strategy = new AvoidantStrategy();
        DerivedCoefficients coeff = strategy.derive(persona);

        // 高回避 → 亲密阈值更高（难以进入深层关系）
        assertTrue(coeff.getClosenessThreshold() > 70,
                "回避度 0.8 时，亲密阈值应 > 70，实际 = " + coeff.getClosenessThreshold());

        // 高回避 → 几乎不主动
        assertTrue(coeff.getTensionGrowthMultiplier() < 0.5,
                "回避型人格张力增长率应 < 0.5（几乎不主动），实际 = " + coeff.getTensionGrowthMultiplier());

        // 高回避 → 衰减快（情绪收得快，不纠缠）
        assertTrue(coeff.getEnergyDecayRate() < 0.85,
                "回避型人格能量衰减应快（< 0.85），情绪收放自如");
    }

    /**
     * 策略选择逻辑：CoefficientDeriver.select()
     * 焦虑优先于回避——如果同时高焦虑+高回避，选焦虑（怕被抛弃比怕亲密更底层）
     */
    @Test
    public void testStrategySelection() {
        // 高焦虑 → AnxiousStrategy
        Persona anxious = new Persona();
        anxious.setAttachmentAnxiety(0.7);
        anxious.setAttachmentAvoidance(0.2);
        assertTrue(CoefficientDeriver.select(anxious) instanceof AnxiousStrategy,
                "焦虑 0.7 应选 AnxiousStrategy");

        // 高回避 → AvoidantStrategy
        Persona avoidant = new Persona();
        avoidant.setAttachmentAnxiety(0.2);
        avoidant.setAttachmentAvoidance(0.7);
        assertTrue(CoefficientDeriver.select(avoidant) instanceof AvoidantStrategy,
                "回避 0.7 应选 AvoidantStrategy");

        // 双低 → SecureStrategy
        Persona secure = new Persona();
        secure.setAttachmentAnxiety(0.2);
        secure.setAttachmentAvoidance(0.2);
        assertTrue(CoefficientDeriver.select(secure) instanceof SecureStrategy,
                "焦虑 0.2 + 回避 0.2 应选 SecureStrategy");
    }

    /**
     * 边界测试：刚好在阈值 0.5 上
     */
    @Test
    public void testBoundaryValues() {
        Persona borderline = new Persona();
        borderline.setAttachmentAnxiety(0.5);
        borderline.setAttachmentAvoidance(0.3);

        // 焦虑恰好 0.5 → 应选 AnxiousStrategy（≥ 0.5 触发）
        assertTrue(CoefficientDeriver.select(borderline) instanceof AnxiousStrategy,
                "焦虑恰好 0.5 应触发 AnxiousStrategy");
    }

    /**
     * 安全型人格的系数应该在合理的中性范围
     */
    @Test
    public void testSecureStrategyGivesNeutralCoefficients() {
        Persona secure = new Persona();
        secure.setAttachmentAnxiety(0.2);
        secure.setAttachmentAvoidance(0.2);
        secure.setInitiativeTendency(0.5);

        CoefficientStrategy strategy = new SecureStrategy();
        DerivedCoefficients coeff = strategy.derive(secure);

        // 安全型 → 张力增长率接近基准 1.0
        assertTrue(coeff.getTensionGrowthMultiplier() >= 0.8
                && coeff.getTensionGrowthMultiplier() <= 1.2,
                "安全型张力增长率应在 0.8~1.2，实际 = " + coeff.getTensionGrowthMultiplier());

        // 安全型 → 信任易碎度接近 1.0
        assertTrue(coeff.getTrustFragilityMultiplier() >= 0.8
                && coeff.getTrustFragilityMultiplier() <= 1.2,
                "安全型信任易碎度应在 0.8~1.2");

        // 安全型 → 亲密阈值适中
        assertTrue(coeff.getClosenessThreshold() >= 70
                && coeff.getClosenessThreshold() <= 85,
                "安全型亲密阈值应在 70~85 之间");
    }
}
