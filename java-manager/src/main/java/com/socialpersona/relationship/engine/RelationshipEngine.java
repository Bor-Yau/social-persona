package com.socialpersona.relationship.engine;

import com.socialpersona.persona.entity.Persona;
import com.socialpersona.persona.repository.PersonaMapper;
import com.socialpersona.relationship.dto.RelationshipDeltasDTO;
import com.socialpersona.relationship.entity.RelationshipState;
import com.socialpersona.relationship.repository.RelationshipStateMapper;
import com.socialpersona.relationship.strategy.CoefficientStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * 关系引擎核心 —— 惰性心跳衰减 + 应用 LLM 返回的关系变化增量
 *
 * ★ 两个核心方法：
 *   1. getState(personaId)  —— 惰性计算衰减后返回当前关系状态
 *   2. applyDeltas(personaId, deltas) —— 将 LLM 返回的增量应用非线性曲线后写入 DB
 *
 * ★ 惰性心跳衰减（Heartbeat Decay）：
 *   不在定时器中计算。只在"被查询时"计算从 last_heartbeat_at 到现在的衰减。
 *   零调度开销。对 SQLite 特别友好——减少不必要的写入。
 *
 * ★ Redis 缓存策略：
 *   @CachePut("relation") → 每次查询都更新缓存，避免首次衰减后被旧值命中
 *   @CacheEvict → applyDeltas 后立即清缓存
 */
@Service
public class RelationshipEngine {

    @Autowired
    private RelationshipStateMapper stateMapper;

    @Autowired
    private PersonaMapper personaMapper;

    /**
     * 获取当前关系状态 —— 惰性计算心跳衰减
     *
     * ★ 流程：
     *   1. 从 DB 取出 RelationshipState
     *   2. 如果距上次心跳 > 0 小时 → 计算这段时间的衰减
     *   3. 衰减后的值写回 DB + 更新 last_heartbeat_at
     *   4. 返回最新值
     *
     * ★ 为什么用 @Cacheable：
     *   同一分钟内多次查询（如事件触发后又收到用户消息）→ 只算一次衰减
     *
     * @param personaId Persona UUID
     * @return 当前关系状态（已应用衰减）
     */
    @CachePut(value = "relation", key = "#personaId")
    public RelationshipState getState(String personaId) {
        RelationshipState state = stateMapper.selectById(personaId);

        Persona persona = personaMapper.selectById(personaId);

        if (state == null) {
            synchronized ((personaId != null ? personaId : "").intern()) {
                state = stateMapper.selectById(personaId);
                if (state == null) {
                    state = createDefault(personaId, persona);
                    stateMapper.insert(state);
                }
            }
        }

        if (persona == null) return state;

        // 计算距上次心跳的小时数
        long hoursSinceLastBeat = 0;
        if (state.getLastHeartbeatAt() != null) {
            Instant lastBeat = Instant.parse(state.getLastHeartbeatAt());
            hoursSinceLastBeat = Duration.between(lastBeat, Instant.now()).toHours();
        }

        // 超过 0 小时 → 应用惰性衰减
        if (hoursSinceLastBeat > 0) {
            CoefficientStrategy strategy = CoefficientDeriver.select(persona);
            DerivedCoefficients coeff = strategy.derive(persona);

            // ① 张力自然消散（固定 0.95^hours）
            if (state.getTension() != null) {
                double newTension = state.getTension() * Math.pow(0.95, hoursSinceLastBeat);
                // 下限钳制到 0（不会变负）
                state.setTension(Math.max(0, newTension));
            }

            // ② 情绪能量衰减（衰减率由人格策略决定）
            if (state.getEmotionalEnergy() != null) {
                double newEnergy = NonLinearCurve.applyEnergyDecay(
                        state.getEmotionalEnergy(), hoursSinceLastBeat, coeff.getEnergyDecayRate());
                state.setEmotionalEnergy(Math.max(0, newEnergy));
            }

            // ③ 张力压强自增：每小时积攒 tensionPressureBase
            if (state.getTensionPressure() != null) {
                double increment = coeff.getTensionPressureBase() * hoursSinceLastBeat;
                state.setTensionPressure(state.getTensionPressure() + increment);
            }

            // 更新心跳时间 → 下次查询从此刻开始算
            state.setLastHeartbeatAt(Instant.now().toString());
            stateMapper.updateById(state);
        }

        return state;
    }

    /**
     * 应用 LLM 返回的关系变化增量 —— 走非线性曲线后写 DB
     *
     * ★ 流程：
     *   1. getState() → 当前值
     *   2. CoefficientDeriver.select() → 得策略
     *   3. 每个变量走各自的非线性曲线
     *   4. 写 DB + 清 Redis 缓存
     *
     * ★ 为什么每条增量走各自的曲线：
     *   信任有易碎效应，亲密有 S 形曲线，张力和情绪能量是线性加减。
     *   不同类型的变化需要不同的数学处理。
     *
     * @param personaId Persona UUID
     * @param deltas    LLM 返回的增量
     */
    @CacheEvict(value = "relation", key = "#personaId")
    public void applyDeltas(String personaId, RelationshipDeltasDTO deltas) {
        // 1. 获取当前值（已含衰减）
        RelationshipState state = getState(personaId);
        if (state == null) return;

        Persona persona = personaMapper.selectById(personaId);
        if (persona == null) return;

        CoefficientStrategy strategy = CoefficientDeriver.select(persona);
        DerivedCoefficients coeff = strategy.derive(persona);

        // 2. 亲密感 → S 形曲线
        if (deltas.getClosenessDelta() != null) {
            double newCloseness = state.getCloseness() != null ? state.getCloseness() : 20;
            double actualDelta = NonLinearCurve.applyClosenessCurve(
                    newCloseness,
                    deltas.getClosenessDelta(),
                    deltas.getIsQualitativeLeap() != null && deltas.getIsQualitativeLeap()
            );
            newCloseness += actualDelta;
            // 钳制到 0~100
            state.setCloseness(Math.min(100, Math.max(0, newCloseness)));
        }

        // 3. 信任 → 易碎效应
        if (deltas.getTrustDelta() != null) {
            double newTrust = state.getTrust() != null ? state.getTrust() : 50;
            double actualDelta = NonLinearCurve.applyTrustFragility(
                    deltas.getTrustDelta(), coeff.getTrustFragilityMultiplier());
            newTrust += actualDelta;
            state.setTrust(Math.min(100, Math.max(0, newTrust)));
        }

        // 4. 张力 → 线性加减（没有非线性曲线，但由 LLM 判断变化幅度）
        if (deltas.getTensionDelta() != null) {
            double newTension = state.getTension() != null ? state.getTension() : 0;
            newTension += deltas.getTensionDelta();
            state.setTension(Math.max(0, newTension));
        }

        // 5. 情绪能量 → 线性加减
        if (deltas.getEmotionalEnergyDelta() != null) {
            double newEnergy = state.getEmotionalEnergy() != null ? state.getEmotionalEnergy() : 30;
            newEnergy += deltas.getEmotionalEnergyDelta();
            state.setEmotionalEnergy(Math.min(100, Math.max(0, newEnergy)));
        }

        // 6. 联系冲动 → 线性加减
        if (deltas.getContactUrgeDelta() != null) {
            double newUrge = state.getContactUrge() != null ? state.getContactUrge() : 0;
            newUrge += deltas.getContactUrgeDelta();
            state.setContactUrge(Math.max(0, newUrge));
        }

        // 7. 更新时间戳 → 写 DB
        state.setLastHeartbeatAt(Instant.now().toString());
        stateMapper.updateById(state);
    }

    /**
     * 创建默认关系状态 —— 按 relationshipPhase 设初值
     *
     * ★ 各阶段默认值：
     *   stranger:     trust=30, closeness=5
     *   acquaintance: trust=45, closeness=15
     *   friend:       trust=60, closeness=35
     *   close_friend: trust=75, closeness=55
     *   默认:         trust=50, closeness=20
     */
    private RelationshipState createDefault(String personaId, Persona persona) {
        RelationshipState state = new RelationshipState();
        state.setPersonaId(personaId);

        String phase = persona != null ? persona.getRelationshipPhase() : null;
        double trust, closeness;
        switch (phase != null ? phase : "") {
            case "stranger":      trust = 10; closeness = 2;  break;
            case "acquaintance":  trust = 40; closeness = 15; break;
            case "friend":        trust = 60; closeness = 35; break;
            case "close_friend":  trust = 80; closeness = 60; break;
            default:              trust = 50; closeness = 20; break;
        }

        state.setTrust(trust);
        state.setCloseness(closeness);
        state.setTension(10.0);
        state.setEmotionalEnergy(30.0);
        state.setTensionPressure(0.0);
        state.setContactUrge(0.0);
        state.setLastHeartbeatAt(Instant.now().toString());
        return state;
    }
}
