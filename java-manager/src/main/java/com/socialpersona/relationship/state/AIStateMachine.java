package com.socialpersona.relationship.state;

import com.socialpersona.persona.entity.Persona;
import com.socialpersona.persona.repository.PersonaMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 状态机 —— 只看 event_type，不看 description
 *
 * ★ 核心原则：
 *   状态切换由事件类型决定，不由自然语言描述决定。
 *   "sleep" 事件 → SLEEPING，无论 description 是"躺下刷新间"还是"熬夜到凌晨"。
 *   这样 LLM 可以自由发挥 description，Java 侧不需要解析它。
 *
 * ★ 为什么用 ConcurrentHashMap 而非数据库：
 *   状态是运行时概念。服务重启后，状态机可以重建——
 *   所有 ACTIVE/SLEEPING 从 daily_events 推断，ARCHIVED 从 personas.status 推断。
 *   不需要持久化状态机本身。
 *
 * ★ 状态转移图：
 *
 *         ┌──────────────┐
 *         │    ACTIVE    │
 *         └──┬──┬────┬───┘
 *    sleep  │  │拉黑 │起床
 *           │  │    │
 *    ┌──────┘  │    └──────┐
 *    ▼         ▼           │
 * ┌──────┐  ┌──────┐       │
 * │SLEEP-│  │ARCHI-│       │
 * │ING   │  │VED   │       │
 * └──┬───┘  └─┬──┬─┘       │
 *    │起床     │  │重新联系   │
 *    └─────────┘  │         │
 *                 ▼         │
 *          ┌──────────────┐ │
 *          │REQUEST_      │ │
 *          │PENDING       │─┘
 *          └──┬──┬────────┘
 *      AI同意  │  │AI拒绝
 *             ▼  ▼
 *        ACTIVE  ARCHIVED
 */
@Component
public class AIStateMachine {

    private static final Logger log = LoggerFactory.getLogger(AIStateMachine.class);

    @Autowired
    private PersonaMapper personaMapper;

    /** 内存状态表：personaId → AIStatus */
    private final Map<String, AIStatus> states = new ConcurrentHashMap<>();

    // ==================== 状态转移方法 ====================

    /**
     * 直接设状态（初始化/恢复时使用），写入后持久化到数据库
     */
    public void transitionTo(String personaId, AIStatus status) {
        states.put(personaId, status);

        // 持久化到数据库
        try {
            Persona persona = personaMapper.selectById(personaId);
            if (persona != null) {
                persona.setStatus(status.name().toLowerCase());
                personaMapper.updateById(persona);
            }
        } catch (Exception e) {
            log.warn("状态持久化失败 (非关键): {}", e.getMessage());
        }
    }

    /**
     * 从数据库恢复状态。在应用启动时调用。
     */
    @PostConstruct
    public void recoverStates() {
        try {
            List<Persona> personas = personaMapper.selectList(null);
            for (Persona p : personas) {
                if (p.getStatus() != null) {
                    try {
                        AIStatus status = AIStatus.valueOf(p.getStatus().toUpperCase());
                        states.put(p.getId(), status);
                        log.info("恢复状态: {} -> {}", p.getId(), status);
                    } catch (IllegalArgumentException e) {
                        states.put(p.getId(), AIStatus.ACTIVE);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("状态恢复跳过（可能DB尚未就绪）: {}", e.getMessage());
        }
    }

    /**
     * 查询当前状态（无记录默认 ACTIVE）
     */
    public AIStatus getState(String personaId) {
        return states.getOrDefault(personaId, AIStatus.ACTIVE);
    }

    // ==================== 事件处理方法 ====================

    /**
     * sleep 事件触发 —— 由 EventTriggerScheduler 调用
     *
     * ★ 前提条件：当前状态为 ACTIVE
     *   已 SLEEPING → 不重复切换（防止多个 sleep 事件）
     *   已 ARCHIVED → 不切换
     */
    public void onSleepEvent(String personaId) {
        if (getState(personaId) == AIStatus.ACTIVE) {
            transitionTo(personaId, AIStatus.SLEEPING);
        }
    }

    /**
     * 起床事件触发 —— 凌晨生成新事件线时调用
     */
    public void onWakeUpEvent(String personaId) {
        if (getState(personaId) == AIStatus.SLEEPING) {
            transitionTo(personaId, AIStatus.ACTIVE);
        }
    }

    /**
     * 用户消息到达 —— 不改变状态
     *
     * ★ 为什么不改状态：
     *   用户发消息不唤醒 AI——只有起床事件才唤醒。
     *   实现中此方法可能仅用于记录/日志。
     */
    public void onUserMessage(String personaId) {
        // 不改变状态——用户消息不唤醒 AI
    }

    /**
     * 用户拉黑 → ARCHIVED
     */
    public void onUserBlock(String personaId) {
        transitionTo(personaId, AIStatus.ARCHIVED);
    }

    /**
     * 发起重新联系申请 → REQUEST_PENDING（仅从 ARCHIVED 可触发）
     */
    public void onReconnectRequest(String personaId) {
        if (getState(personaId) == AIStatus.ARCHIVED) {
            transitionTo(personaId, AIStatus.REQUEST_PENDING);
        }
    }

    /**
     * AI 同意重新联系 → ACTIVE
     */
    public void onReconnectAccepted(String personaId) {
        if (getState(personaId) == AIStatus.REQUEST_PENDING) {
            transitionTo(personaId, AIStatus.ACTIVE);
        }
    }

    /**
     * AI 拒绝重新联系 → 回到 ARCHIVED
     */
    public void onReconnectRejected(String personaId) {
        if (getState(personaId) == AIStatus.REQUEST_PENDING) {
            transitionTo(personaId, AIStatus.ARCHIVED);
        }
    }

    // ==================== 查询辅助 ====================

    /** 是否为可交互状态（ACTIVE 或 REQUEST_PENDING） */
    public boolean isInteractable(String personaId) {
        AIStatus status = getState(personaId);
        return status == AIStatus.ACTIVE || status == AIStatus.REQUEST_PENDING;
    }
}
