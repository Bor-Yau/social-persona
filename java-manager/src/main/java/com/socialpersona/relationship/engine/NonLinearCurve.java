package com.socialpersona.relationship.engine;

/**
 * 非线性关系曲线计算器 —— S形曲线 + 信任易碎 + 能量衰减
 *
 * ★ 为什么不是线性加减：
 *   真人感情不按固定速率变化。刚认识时升温快（新鲜感），
 *   关系稳定后变慢（老夫老妻），突破需要重大事件（质变）。
 *
 * ★ 三条曲线各自独立：
 *   1. applyClosenessCurve —— 亲密感 S 形曲线
 *   2. applyTrustFragility —— 信任易碎效应（跌快涨慢）
 *   3. applyEnergyDecay   —— 情绪能量指数衰减
 */
public class NonLinearCurve {

    // ==================== 亲密感 S 形曲线 ====================

    /**
     * 计算亲密的实际变化量（S形曲线）
     *
     * ★ 四个区间：
     *   刚认识 0~20  → delta 全量　　（聊几句就熟了）
     *   敏感期 20~60 → delta 全量　　（热恋期，升温飞快）
     *   减速期 60~85 → delta×(85-current)/25（越来越难推进）
     *   平台期 85+   → delta×0.1　　（需要质变事件才能突破）
     *
     * @param current          当前亲密值
     * @param delta            LLM 返回的原始变化量
     * @param isQualitativeLeap 是否为质变事件（如表白、大吵）
     * @return 实际生效的变化量
     */
    public static double applyClosenessCurve(double current, double delta,
                                              boolean isQualitativeLeap) {
        // 刚认识：全量变化
        if (current < 20) return delta;

        // 敏感期：全量变化
        if (current < 60) return delta;

        // 减速期：线性递减
        if (current < 85) {
            // (85 - current) / 25 → current=60 时为 1.0，current=85 时为 0
            return delta * (85 - current) / 25.0;
        }

        // 平台期 (85+)：质变事件可突破天花板
        if (isQualitativeLeap) return delta;

        // 无质变：极慢增长（10%）
        return delta * 0.1;
    }

    // ==================== 信任易碎效应 ====================

    /**
     * 信任的"易碎效应"：涨按原值，跌 × 惩罚系数
     *
     * ★ 为什么这样设计：
     *   人类的信任——建立需要很多次真诚互动，毁掉只需要一次背叛。
     *   一次撒谎抵十次诚实。但上涨不设减速（信任不需要"酝酿"）。
     *
     * ★ fragilityMultiplier 的来源：
     *   由系数策略（CoefficientStrategy）从人格维度推导。
     *   焦虑型 > 1.0（极脆），回避型 < 1.0（不那么在乎就不容易碎）。
     *
     * @param delta              原始 trust 变化量（正=涨，负=跌）
     * @param fragilityMultiplier 信任易碎放大系数
     * @return 实际 trust 变化量
     */
    public static double applyTrustFragility(double delta, double fragilityMultiplier) {
        // 上涨：原值不变 ÷
        if (delta >= 0) return delta;

        // 下跌：× (1.0 + fragilityMultiplier × 0.5) 放大惩罚
        // fragilityMultiplier=1.0 → 跌1.5×
        // fragilityMultiplier=1.8 → 跌1.9×
        return delta * (1.0 + fragilityMultiplier * 0.5);
    }

    // ==================== 情绪能量衰减 ====================

    /**
     * 情绪能量指数衰减
     *
     * ★ 公式：current × decayRate^hours
     *
     * ★ 为什么是指数衰减而非线性：
     *   时间越久，能量的"热乎劲"流失越快。第一个小时流失最多，
     *   后面慢慢趋于平稳。指数曲线完美模拟这种"先快后慢"。
     *
     * ★ decayRate 的来源：
     *   由系数策略从人格维度推导。
     *   焦虑型~0.95（黏着），回避型~0.75（来去匆匆）。
     *
     * @param current   当前情绪能量
     * @param hours     距上次心跳的小时数
     * @param decayRate 衰减率（0~1，每小时保留比例）
     * @return 衰减后的能量
     */
    public static double applyEnergyDecay(double current, double hours, double decayRate) {
        return current * Math.pow(decayRate, hours);
    }
}
