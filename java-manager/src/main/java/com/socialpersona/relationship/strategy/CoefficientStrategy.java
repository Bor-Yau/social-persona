package com.socialpersona.relationship.strategy;

import com.socialpersona.persona.entity.Persona;
import com.socialpersona.relationship.engine.DerivedCoefficients;

/**
 * 系数策略接口 —— 策略模式抽象层
 *
 * ★ 策略模式的价值：
 *   同一份 Persona 数据，不同策略推导出完全不同的关系引擎参数。
 *   不必在每个引擎方法里写 if (anxiety>0.5) ... else if (avoidance>0.5) ...
 *
 * ★ 三个实现：
 *   - SecureStrategy   （焦虑 < 0.35 且回避 < 0.35）
 *   - AnxiousStrategy  （焦虑 ≥ 0.5）
 *   - AvoidantStrategy （回避 ≥ 0.5）
 *
 * ★ 为什么 derive 而非 compute/calculate：
 *   derive 强调"从...(Persona)推导出...(DerivedCoefficients)"，
 *   而非凭空计算。
 */
public interface CoefficientStrategy {

    /**
     * 从人格配置推导关系引擎计算参数
     *
     * @param persona 人格实体（含 attachment_anxiety、attachment_avoidance、initiative_tendency）
     * @return 派生系数（增长率、衰减率、阈值等）
     */
    DerivedCoefficients derive(Persona persona);
}
