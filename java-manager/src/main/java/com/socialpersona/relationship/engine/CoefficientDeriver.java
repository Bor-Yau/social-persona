package com.socialpersona.relationship.engine;

import com.socialpersona.persona.entity.Persona;
import com.socialpersona.relationship.strategy.*;

/**
 * 策略选择器 —— 从 Persona 的连续依恋维度自动选择策略
 *
 * ★ 选择逻辑（优先级递减）：
 *   1. attachment_anxiety ≥ 0.5 → AnxiousStrategy
 *   2. attachment_avoidance ≥ 0.5 → AvoidantStrategy
 *   3. 其余 → SecureStrategy
 *
 * ★ 为什么焦虑优先于回避：
 *   如果一个人同时高焦虑+高回避（恐惧型），行为上更接近焦虑型。
 *   因为"怕被抛弃"比"怕亲密"更本能——恐惧型会一边回避亲密一边疯狂担心被抛弃。
 *
 * ★ 为什么是静态方法而非实例方法：
 *   select() 无状态，不依赖外部数据。纯函数不需要对象实例化。
 */
public class CoefficientDeriver {

    /**
     * 从 Persona 的连续依恋维度自动选择策略
     *
     * @param persona 人格实体
     * @return 对应的系数策略实例
     */
    public static CoefficientStrategy select(Persona persona) {
        double anxiety = persona.getAttachmentAnxiety() != null
                ? persona.getAttachmentAnxiety() : 0.5;
        double avoidance = persona.getAttachmentAvoidance() != null
                ? persona.getAttachmentAvoidance() : 0.3;

        // 焦虑优先——怕被抛弃是最底层的心理驱动
        if (anxiety >= 0.5) {
            return new AnxiousStrategy();
        }
        // 回避次之——抗拒亲密但底层的被抛弃恐惧不强
        if (avoidance >= 0.5) {
            return new AvoidantStrategy();
        }
        // 双低 → 安全型
        return new SecureStrategy();
    }
}
