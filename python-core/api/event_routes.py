"""
事件端点 —— POST /api/event/trigger + POST /api/event/generate

Day 6 新增：Mem0 记忆检索注入 + 今日反思 → 关键记忆持久化
"""
import logging
import time as _time  # 用于事件生成重试计时
from fastapi import APIRouter
from api.models import (
    EventTriggerRequest, EventTriggerResponse,
    EventGenerateRequest, EventGenerateResponse,
    Reply, InnerThought, RelationshipDeltas
)
from engine.llm.factory import create_provider
from engine.prompt_templates import build_event_trigger_prompt, build_event_generate_prompt
from engine.memory import memory_service

logger = logging.getLogger(__name__)
router = APIRouter()


def _safe_dict(raw) -> dict | None:
    if isinstance(raw, dict):
        return raw
    return None


def _safe_list(raw) -> list:
    if isinstance(raw, list):
        return raw
    return []


def _safe_bool(raw) -> bool:
    if isinstance(raw, bool):
        return raw
    if isinstance(raw, str):
        return raw.lower() in ("true", "1", "yes")
    return bool(raw)


def _safe_inner_thought(raw: dict) -> dict | None:
    if not isinstance(raw, dict):
        return None
    return {
        "raw_thought": str(raw.get("raw_thought", "")),
        "attitude": str(raw.get("attitude", "neutral")),
        "should_remember": bool(raw.get("should_remember", False)),
        "memorable_point": raw.get("memorable_point"),
    }


def _safe_reply(raw) -> dict | None:
    if not isinstance(raw, dict):
        return None
    items_raw = raw.get("items", [])
    safe_items = []
    if isinstance(items_raw, list):
        for item in items_raw:
            if isinstance(item, dict):
                safe_items.append({
                    "type": str(item.get("type", "text")),
                    "content": item.get("content"),
                    "mood_hint": item.get("mood_hint"),
                    "content_prompt": item.get("content_prompt"),
                    "generation_mode": item.get("generation_mode"),
                    "delay_ms": item.get("delay_ms"),
                })
    return {
        "raw_text": str(raw.get("raw_text", "")),
        "items": safe_items,
        "mood": str(raw.get("mood", "")),
    }


def _safe_deltas(raw) -> dict | None:
    if not isinstance(raw, dict):
        return None
    return {
        "trust_delta": float(raw.get("trust_delta", 0)),
        "closeness_delta": float(raw.get("closeness_delta", 0)),
        "tension_delta": float(raw.get("tension_delta", 0)),
        "emotional_energy_delta": float(raw.get("emotional_energy_delta", 0)),
        "contact_urge_delta": float(raw.get("contact_urge_delta", 0)),
        "is_qualitative_leap": bool(raw.get("is_qualitative_leap", False)),
    }


def _redistribute_event_times(events: list) -> list:
    """LLM 常把所有事件时间设成同一个点，后处理均匀分布"""
    if not events:
        return events

    times = [e.get("time", "") for e in events if e.get("time")]
    unique_times = set(times)

    if len(unique_times) > 1:
        return events

    # 所有事件都是同一时间 → 均匀分布
    START_HOUR = 8
    END_HOUR = 22
    result = []
    sleep_count = 0
    for e in events:
        if e.get("type") == "sleep":
            e["time"] = "23:00:00"
            sleep_count += 1
        elif e.get("type") == "moment":
            e["time"] = "14:00:00" if "afternoon" not in e.get("description", "").lower() else "16:00:00"
        result.append(e)

    # 非 sleep 事件均匀分布
    non_sleep = [e for e in result if e.get("type") != "sleep"]
    if len(non_sleep) <= 1:
        return result

    slots = len(non_sleep)
    step = max(1, (END_HOUR - START_HOUR) // (slots + 1))
    for i, e in enumerate(non_sleep):
        hour = START_HOUR + step * (i + 1)
        minute = (i * 17) % 60
        e["time"] = f"{hour:02d}:{minute:02d}:00"

    return result


@router.post("/api/event/trigger")
async def trigger_event(request: EventTriggerRequest):
    """..."""
    try:
        provider = create_provider(request.api_config)

        persona_id = request.persona_config.get("id", "")
        current_context = request.persona_config.get("character_current_context", "")

        event_desc = request.current_event.get("description", "")
        mem_results = memory_service.search(
            f"事件: {event_desc} | 处境: {current_context}", persona_id, limit=3,
            api_config=request.api_config
        )
        if mem_results:
            logger.info(f"事件触发记忆检索: persona={persona_id}, 命中{len(mem_results)}条")
        else:
            logger.debug(f"事件触发记忆检索: persona={persona_id}, 无匹配记忆")

        next_event = {"time": request.next_event_time,
                      "description": request.next_event_type}

        system_prompt = build_event_trigger_prompt(
            request.persona_config,
            request.relationship_state,
            request.current_event,
            next_event,
            request.today_events_so_far,
            current_context
        )

        user_prompt = f"现在时间是 {request.now}。刚才发生的事件：{event_desc}"
        all_memories = list(request.recent_memories or [])
        for m in mem_results:
            all_memories.append({"content": m})
        if all_memories:
            memory_text = "\n".join(
                f"- {m.get('content', m)}" for m in all_memories[:3]
            )
            user_prompt += f"\n\n相关记忆：\n{memory_text}"

        result = await provider.chat(
            system_prompt, user_prompt,
            response_format={"type": "json_object"}
        )

        safe_thought = _safe_inner_thought(result.get("inner_thought")) or {}
        if safe_thought.get("should_remember"):
            memorable = safe_thought.get("memorable_point", "")
            if memorable:
                memory_service.add(memorable, persona_id, api_config=request.api_config)
                logger.info(f"记忆写入: persona={persona_id}, content={memorable[:60]}...")

        # 手动构造响应，绕过 Pydantic response_model 二次校验
        # DeepSeek 偶尔返回空字符串 "" 替代 null → Pydantic dict/list 类型拒绝
        return {
            "should_contact_user": _safe_bool(result.get("should_contact_user")),
            "reply": _safe_reply(result.get("reply")),
            "inner_thought": _safe_inner_thought(result.get("inner_thought")),
            "relationship_deltas": _safe_deltas(result.get("relationship_deltas")),
            "conversation_ended": _safe_bool(result.get("conversation_ended", True)),
            "interval_pre_scheduling": _safe_dict(result.get("interval_pre_scheduling")),
            "event_changed": _safe_bool(result.get("event_changed")),
            "cancelled_scheduled_messages": _safe_list(result.get("cancelled_scheduled_messages")),
            "invalidated_events": _safe_list(result.get("invalidated_events")),
            "new_events": _safe_list(result.get("new_events")),
        }

    except Exception as e:
        return EventTriggerResponse(
            should_contact_user=False,
            inner_thought=InnerThought(raw_thought=f"[系统错误] {e}", attitude="neutral")
        )


@router.post("/api/event/generate", response_model=EventGenerateResponse)
async def generate_events(request: EventGenerateRequest):
    """
    ★ 每日凌晨 → 生成今日反思 + 明天事件线

    调用频率：每天 1 次
    必须返回至少 1 个 type="sleep" 的事件

    Day 6 新增：today_reflection.key_memories 中 importance≥7 的永久存入 ChromaDB
    """
    try:
        provider = create_provider(request.api_config)

        persona_id = request.persona_config.get("id", "")
        current_context = request.persona_config.get("character_current_context", "")

        system_prompt = build_event_generate_prompt(
            request.persona_config,
            request.relationship_state,
            request.today_date,
            request.day_of_week,
            request.yesterday_events,
            request.today_inner_thoughts,
            current_context
        )

        user_prompt = "请生成你今天的事件线和今日反思。注意：先判断今天身份/地点是否变化，再填写 life_stage_transition。"

        _t0 = _time.monotonic()
        result = await provider.chat(
            system_prompt, user_prompt,
            response_format={"type": "json_object"}
        )
        _elapsed = _time.monotonic() - _t0

        # ★ Day 6 新增：今日反思 → 关键记忆批量持久化
        reflection = result.get("today_reflection", {})
        key_memories = reflection.get("key_memories", [])
        if key_memories:
            memory_service.add_batch(key_memories, persona_id, api_config=request.api_config)

        # ★ 安全网：检测 LLM 是否照抄了 null 占位符
        transition = reflection.get("life_stage_transition", {})
        if transition and transition.get("should_transition") is None:
            logger.warning(f"life_stage_transition: LLM 未替换 null 占位符, persona={persona_id}")

        # 后处理：LLM 常把所有事件时间设成一样 → 均匀分布
        events = _redistribute_event_times(result.get("events", []))

        # ★ 重试：首次 LLM 调用时间 > 5s 说明确实调了 LLM 但结果为空 → 重试一次
        if not events and _elapsed > 5.0:
            logger.warning(
                f"事件线生成: 首次返回空事件(耗时{_elapsed:.1f}s), persona={persona_id}, "
                f"raw_result_keys={list(result.keys()) if result else 'None'}, 准备重试"
            )
            _retry_prompt = user_prompt + " 请务必生成至少8个事件，覆盖从早到晚的生活，包括至少一个sleep事件。"
            _t1 = _time.monotonic()
            result = await provider.chat(
                system_prompt, _retry_prompt,
                response_format={"type": "json_object"}
            )
            _retry_elapsed = _time.monotonic() - _t1
            events = _redistribute_event_times(result.get("events", []))
            if events:
                logger.info(f"事件线生成: 重试成功, persona={persona_id}, {len(events)}条(耗时{_retry_elapsed:.1f}s)")
                # 重试成功后也写入 key_memories
                refl = result.get("today_reflection", {})
                km = refl.get("key_memories", [])
                if km:
                    memory_service.add_batch(km, persona_id, api_config=request.api_config)
                return EventGenerateResponse(
                    today_reflection=refl,
                    events=events,
                )
            else:
                logger.warning(f"事件线生成: 重试仍为空, persona={persona_id}")
        elif not events:
            logger.warning(f"事件线生成: LLM 返回空事件列表, persona={persona_id}, "
                           f"raw_result_keys={list(result.keys()) if result else 'None'}")

        return EventGenerateResponse(
            today_reflection=result.get("today_reflection"),
            events=events,
        )

    except Exception as e:
        return EventGenerateResponse(
            events=[{"time": "23:00", "type": "sleep", "description": "睡着"}]
        )
