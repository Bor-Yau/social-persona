package com.socialpersona.event.service;

import com.socialpersona.event.entity.DailyEvent;
import com.socialpersona.event.repository.EventMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 事件服务 —— 事件线 CRUD + 作废/新增/延后逻辑
 *
 * ★ 职责边界：
 *   EventService → 数据库操作（查/增/删/改事件）
 *   EventTriggerScheduler → 定时扫描 + 调 Python + 状态机切换
 *   两个类通过 EventMapper 共享数据，不直接互相调用
 */
@Service
public class EventService {

    /** 时间格式：HH:mm:ss（统一 6 位） */
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** 日期格式：yyyy-MM-dd */
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** sleep 延后每次增加分钟数 */
    private static final int POSTPONE_MINUTES = 30;

    /** sleep 硬上限（角色时间），超过此值不再延后 */
    private static final LocalTime SLEEP_HARD_CAP = LocalTime.of(2, 0);

    @Autowired
    private EventMapper eventMapper;

    // ==================== 查询方法 ====================

    /**
     * 查找今天到期的活跃事件
     *
     * ★ 由 EventTriggerScheduler 每分钟调用
     * ★ 使用索引：idx_daily_events_scan
     */
    public List<DailyEvent> findDue(String personaId) {
        String today = LocalDate.now().format(DATE_FMT);
        String nowTime = LocalTime.now().format(TIME_FMT);
        return eventMapper.findDue(personaId, today, nowTime);
    }

    /**
     * 查指定时间之后的下一个活跃事件
     */
    public DailyEvent findNextActive(String personaId, String currentTime) {
        String today = LocalDate.now().format(DATE_FMT);
        return eventMapper.findNextActive(personaId, today, currentTime);
    }

    /**
     * 查今天的全部活跃事件
     */
    public List<DailyEvent> findTodayEvents(String personaId) {
        String today = LocalDate.now().format(DATE_FMT);
        return eventMapper.findAllToday(personaId, today);
    }

    /**
     * 今天是否有活跃事件 —— 懒加载判断用
     */
    public boolean hasTodayEvents(String personaId) {
        return !findTodayEvents(personaId).isEmpty();
    }

    public DailyEvent findNextActiveAfter(String personaId, String today, String nowTime) {
        return eventMapper.findNextActiveAfter(personaId, today, nowTime);
    }

    public int countBetween(String personaId, String today, String from, String to) {
        return eventMapper.countBetween(personaId, today, from, to);
    }

    // ==================== 写入方法 ====================

    /**
     * 批量插入事件线（每天凌晨事件线生成结果入库）
     *
     * @param personaId Persona UUID
     * @param events   新事件列表
     */
    @Transactional
    public void insertEvents(String personaId, List<DailyEvent> events) {
        String today = LocalDate.now().format(DATE_FMT);
        for (DailyEvent event : events) {
            if (event.getId() == null || event.getId().isEmpty()) {
                event.setId(UUID.randomUUID().toString());
            }
            event.setPersonaId(personaId);
            event.setEventDate(today);
            if (event.getIsActive() == null) {
                event.setIsActive(1);
            }
            eventMapper.insert(event);
        }
    }

    // ==================== 修改方法 ====================

    /**
     * 作废指定时间之后的所有活跃事件
     *
     * ★ 用途：
     *   1. sleep 延后：作废旧 sleep + 插入新 sleep
     *   2. AI 取消原计划：作废后续 routine/moment
     *
     * @param personaId Persona UUID
     * @param afterTime 在此之后的事件全部作废（HH:mm:ss）
     */
    @Transactional
    public void invalidateAfter(String personaId, String afterTime) {
        String today = LocalDate.now().format(DATE_FMT);
        eventMapper.invalidateAfter(personaId, today, afterTime);
    }

    /**
     * 作废单个事件
     */
    public void invalidateEvent(String eventId) {
        DailyEvent event = eventMapper.selectById(eventId);
        if (event != null) {
            event.setIsActive(0);
            eventMapper.updateById(event);
        }
    }

    /**
     * 新增单个事件（如 sleep 延后时插入新 sleep）
     */
    public void insertEvent(DailyEvent event) {
        if (event.getId() == null || event.getId().isEmpty()) {
            event.setId(UUID.randomUUID().toString());
        }
        if (event.getEventDate() == null) {
            event.setEventDate(LocalDate.now().format(DATE_FMT));
        }
        if (event.getIsActive() == null) {
            event.setIsActive(1);
        }
        eventMapper.insert(event);
    }

    /**
     * 按 ID 查询事件
     */
    public DailyEvent getById(String eventId) {
        return eventMapper.selectById(eventId);
    }

    /**
     * sleep 延后 —— 用户发消息时自动推迟 AI 的入睡时间
     *
     * ★ 逻辑：
     *   1. 找到最近的未过期 sleep 事件
     *   2. 如果当前时间已超过硬上限（02:00），不延后
     *   3. 作废旧 sleep → 插入新 sleep（+30 分钟）
     *
     * @param personaId Persona UUID
     * @return 是否成功延后
     */
    @Transactional
    public boolean postponeSleep(String personaId) {
        String today = LocalDate.now().format(DATE_FMT);
        String nowTime = LocalTime.now().format(TIME_FMT);

        // 硬上限检查：当前时间超过 02:00 不延后
        if (LocalTime.now().isAfter(SLEEP_HARD_CAP)) {
            return false;
        }

        // 找到最近的未过期 sleep 事件
        DailyEvent nextSleep = eventMapper.findNextActive(personaId, today, nowTime);
        if (nextSleep == null || !"sleep".equals(nextSleep.getEventType())) {
            return false;
        }

        LocalTime sleepTime = LocalTime.parse(nextSleep.getEventTime(), TIME_FMT);
        LocalTime postponed = sleepTime.plusMinutes(POSTPONE_MINUTES);

        // 如果延后后超过硬上限，直接设到硬上限
        if (postponed.isAfter(SLEEP_HARD_CAP)) {
            postponed = SLEEP_HARD_CAP;
        }

        // 作废旧 sleep
        eventMapper.invalidateAfter(personaId, today, nextSleep.getEventTime());

        // 插入新 sleep
        DailyEvent newSleep = new DailyEvent();
        newSleep.setId(UUID.randomUUID().toString());
        newSleep.setPersonaId(personaId);
        newSleep.setEventDate(today);
        newSleep.setEventTime(postponed.format(TIME_FMT));
        newSleep.setEventType("sleep");
        newSleep.setDescription("推迟入睡（原 " + nextSleep.getEventTime() + "→" + postponed.format(TIME_FMT) + "）");
        newSleep.setIsActive(1);
        eventMapper.insert(newSleep);

        return true;
    }
}
