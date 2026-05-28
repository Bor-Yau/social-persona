package com.socialpersona.relationship.engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * NonLinearCurve TDD 测试
 *
 * 覆盖三条曲线：
 *   1. 亲密 S形曲线（applyClosenessCurve）
 *   2. 信任易碎效应（applyTrustFragility）
 *   3. 情绪能量衰减（applyEnergyDecay）
 */
public class NonLinearCurveTest {

    // ==================== 亲密感 S 形曲线 ====================

    /**
     * 减速期：current=70, delta=5 → 实际生效 = 5 × (85-70)/25 = 3
     */
    @Test
    public void testClosenessDecelerationPhase() {
        double result = NonLinearCurve.applyClosenessCurve(70, 5, false);
        // 70 在减速期 (60~85)，delta 被压缩
        // 公式：delta × (85 - current) / 25 = 5 × 15/25 = 3
        assertEquals(3.0, result, 0.01,
                "closeness=70, delta=5 在减速期应被压缩为 3");
    }

    /**
     * 敏感期：current=40, delta=5 → 全量 5
     */
    @Test
    public void testClosenessSensitivePhase() {
        double result = NonLinearCurve.applyClosenessCurve(40, 5, false);
        assertEquals(5.0, result, 0.01,
                "closeness=40 在敏感期 (20~60)，delta 应全量生效");
    }

    /**
     * 刚认识：current=10, delta=5 → 全量 5
     */
    @Test
    public void testClosenessEarlyPhase() {
        double result = NonLinearCurve.applyClosenessCurve(10, 5, false);
        assertEquals(5.0, result, 0.01,
                "closeness=10 在刚认识期 (0~20)，delta 应全量生效");
    }

    /**
     * 平台期无质变：current=87, delta=5, isQualitativeLeap=false → 仅 10%
     */
    @Test
    public void testClosenessPlateauWithoutLeap() {
        double result = NonLinearCurve.applyClosenessCurve(87, 5, false);
        // 5 × 0.1 = 0.5
        assertEquals(0.5, result, 0.01,
                "closeness=87 在平台期且无质变，delta 应仅 10%");
    }

    /**
     * 平台期有质变：current=87, delta=5, isQualitativeLeap=true → 全量突破
     */
    @Test
    public void testClosenessPlateauWithLeap() {
        double result = NonLinearCurve.applyClosenessCurve(87, 5, true);
        assertEquals(5.0, result, 0.01,
                "closeness=87 在平台期但有质变事件，delta 应全量突破天花板");
    }

    /**
     * 减速期边界：current=60（减速期起点），delta=5
     * 公式：5 × (85-60)/25 = 5 × 1.0 = 5（刚好全量，边界值）
     */
    @Test
    public void testClosenessDecelerationBoundary() {
        double result = NonLinearCurve.applyClosenessCurve(60, 5, false);
        assertEquals(5.0, result, 0.01,
                "closeness=60 是减速期起点，25/25=1 → 接近全量");
    }

    /**
     * 减速期深部：current=80, delta=10
     * 公式：10 × (85-80)/25 = 10 × 0.2 = 2
     */
    @Test
    public void testClosenessDecelerationDeep() {
        double result = NonLinearCurve.applyClosenessCurve(80, 10, false);
        assertEquals(2.0, result, 0.01,
                "closeness=80, delta=10 → 10 × 5/25 = 2");
    }

    // ==================== 信任易碎效应 ====================

    /**
     * 信任上涨：delta=5 → 全量 5（不上涨惩罚）
     */
    @Test
    public void testTrustPositiveDeltaFull() {
        double result = NonLinearCurve.applyTrustFragility(5, 1.0);
        assertEquals(5.0, result, 0.01,
                "信任上涨时全量生效，不受 fragilityMultiplier 影响");
    }

    /**
     * 信任下跌，基准系数：delta=-5, fragilityMultiplier=1.0
     * 公式：-5 × (1.0 + 1.0×0.5) = -5 × 1.5 = -7.5
     */
    @Test
    public void testTrustNegativeDeltaFragile() {
        double result = NonLinearCurve.applyTrustFragility(-5, 1.0);
        assertEquals(-7.5, result, 0.01,
                "trust delta=-5, fragility=1.0 → 被放大到 -7.5");
    }

    /**
     * 信任下跌，高焦虑系数：delta=-5, fragilityMultiplier=1.5
     * 公式：-5 × (1.0 + 1.5×0.5) = -5 × 1.75 = -8.75
     */
    @Test
    public void testTrustNegativeDeltaHighFragility() {
        double result = NonLinearCurve.applyTrustFragility(-5, 1.5);
        assertEquals(-8.75, result, 0.01,
                "焦虑型人格 fragility=1.5 时 trust 下跌被进一步放大");
    }

    /**
     * 信任下跌，低回避系数：delta=-5, fragilityMultiplier=0.5
     * 公式：-5 × (1.0 + 0.5×0.5) = -5 × 1.25 = -6.25
     */
    @Test
    public void testTrustNegativeDeltaLowFragility() {
        double result = NonLinearCurve.applyTrustFragility(-5, 0.5);
        assertEquals(-6.25, result, 0.01,
                "回避型人格 fragility=0.5 时信任下跌不那么脆");
    }

    // ==================== 情绪能量衰减 ====================

    /**
     * 能量衰减：current=100, hours=2, decayRate=0.9
     * 公式：100 × 0.9² = 100 × 0.81 = 81
     */
    @Test
    public void testEnergyDecay() {
        double result = NonLinearCurve.applyEnergyDecay(100, 2, 0.9);
        assertEquals(81.0, result, 0.01,
                "100 能量过 2 小时以 0.9 衰减率 → 81");
    }

    /**
     * 能量零衰减：hours=0 → 不变
     */
    @Test
    public void testEnergyDecayZeroHours() {
        double result = NonLinearCurve.applyEnergyDecay(50, 0, 0.9);
        assertEquals(50.0, result, 0.01,
                "0 小时后能量应不变");
    }

    /**
     * 高衰减率：100 能量，10 小时，decayRate=0.85
     * 100 × 0.85¹⁰ ≈ 19.69
     */
    @Test
    public void testEnergyDecayLongDuration() {
        double result = NonLinearCurve.applyEnergyDecay(100, 10, 0.85);
        assertTrue(result < 25, "10 小时后高衰减应大幅降低能量");
        assertTrue(result > 15, "但不应到 0");
    }
}
