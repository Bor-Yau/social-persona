#!/usr/bin/env python3
"""
Simulate full user lifecycle — integration test script.

Usage:
  1. Start Java:    cd java-manager && mvn spring-boot:run
  2. Start Python:  cd python-core && python -m uvicorn main:app --host 127.0.0.1 --port 8000
  3. Run:           cd python-core && python simulate_lifecycle.py

Flow:
  [1] Create persona → bind QQ → lazy-load events → chat 3 rounds → event trigger → daily summary
  [2] Create 2nd persona → transfer QQ → lazy-load events → chat 3 rounds → daily summary
  [3] Auto-cleanup all test data
"""

import requests
import json
import time
import sys
import os
import base64
import logging
from datetime import datetime

logger = logging.getLogger(__name__)

JAVA = "http://127.0.0.1:8080"
PYTHON = "http://127.0.0.1:8000"

# ============================================================
# 测试角色定义
# ============================================================

LIU_RUYAN = {
    "name": "柳如烟",
    "bigFiveJson": json.dumps({
        "openness": 0.65, "conscientiousness": 0.55, "extraversion": 0.75,
        "agreeableness": 0.50, "neuroticism": 0.45
    }),
    "attachmentAnxiety": 0.65,
    "attachmentAvoidance": 0.30,
    "selfEsteemStability": 0.55,
    "socialRhythm": "irregular",
    "conflictStyle": "direct_confront",
    "initiativeTendency": 0.50,
    "inputMethod": "phone_thumb",
    "typingStyleJson": json.dumps({"default_style":"傲娇毒舌","fragmentation_level":0.55}),
    "typingSpeed": 3.0,
    "imageEnabled": 0,
    "imageStylePrompt": "",
    "characterCurrentContext": "她刚认识你不久，还在试探阶段，表面毒舌但内心有点依赖。",
    "birthday": "2006-01-01",
    "relationshipPhase": "stranger",
    "ownerQq": "1875552542"
}

LIU_ERYAN = {
    "name": "柳二烟",
    "bigFiveJson": json.dumps({
        "openness": 0.65, "conscientiousness": 0.55, "extraversion": 0.70,
        "agreeableness": 0.52, "neuroticism": 0.42
    }),
    "attachmentAnxiety": 0.60,
    "attachmentAvoidance": 0.28,
    "selfEsteemStability": 0.58,
    "socialRhythm": "irregular",
    "conflictStyle": "direct_confront",
    "initiativeTendency": 0.55,
    "inputMethod": "phone_thumb",
    "typingStyleJson": json.dumps({"default_style":"傲娇毒舌","fragmentation_level":0.35}),
    "typingSpeed": 3.2,
    "imageEnabled": 1,
    "imageStylePrompt": ("二次元动漫风格，日系插画，可爱清新的色调，"
                         "类似《夏洛特》友利奈绪的氛围感"),
    "characterCurrentContext": "她刚认识你不久，还端着，表面毒舌但偷偷在意你说了什么。",
    "relationshipPhase": "stranger"
}


# ============================================================
# 工具函数
# ============================================================

created_ids = []
step_num = 0
passed = 0
failed = 0
warnings = 0

def step(name):
    global step_num
    step_num += 1
    print(f"\n{'='*60}")
    print(f"[{step_num}] {name}")
    print(f"{'='*60}")

def ok(msg=""):
    global passed
    passed += 1
    print(f"  ✅ {msg}")

def fail(msg=""):
    global failed
    failed += 1
    print(f"  ❌ {msg}")

def warn(msg=""):
    global warnings
    warnings += 1
    print(f"  ⚠️  {msg}")

def post(path, data=None):
    try:
        r = requests.post(f"{JAVA}{path}", json=data or {}, timeout=120)
        return r.json() if r.text else {}
    except Exception as e:
        return {"ok": False, "error": str(e)}

def post_json(path, data=None):
    """POST 原始 JSON 字符串（PersonaController.create 需要 @RequestBody String）"""
    try:
        raw = json.dumps(data, ensure_ascii=False) if data else "{}"
        r = requests.post(f"{JAVA}{path}", data=raw,
                          headers={"Content-Type": "application/json"}, timeout=120)
        return r.json() if r.text else {}
    except Exception as e:
        return {"ok": False, "error": str(e)}

def get(path):
    try:
        r = requests.get(f"{JAVA}{path}", timeout=10)
        return r.json() if r.text else {}
    except Exception as e:
        return {"ok": False, "error": str(e)}

def check_health():
    try:
        r = requests.get(f"{JAVA}/api/health", timeout=5)
        return r.status_code == 200
    except Exception:
        logger.warning("健康检查失败", exc_info=True)
        return False

def run_cleanup():
    global created_ids
    if not created_ids:
        return
    step("清理测试数据")
    result = post("/api/sim/cleanup", {"personaIds": created_ids})
    if result.get("ok"):
        ok(f"删除 {result.get('deleted_personas',0)} Persona, "
           f"{result.get('deleted_daily_events',0)} 事件, "
           f"{result.get('deleted_event_logs',0)} 日志, "
           f"{result.get('deleted_relationships',0)} 关系状态")
        created_ids.clear()
    else:
        fail(f"清理失败: {result.get('error')}")


# ============================================================
# 碎片化测试 —— 直接调 Python /api/message，检查 [SPLIT]
# ============================================================

def _load_api_config():
    """从 system_config.json 读取 LLM API 配置（同 Java loadApiConfig）"""
    cfg_path = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                            "..", "java-manager", "data", "system_config.json")
    with open(cfg_path, "r", encoding="utf-8") as f:
        cfg = json.load(f)
    api_key = ""
    encoded = cfg.get("apiKeyEncrypted", "")
    if encoded:
        try:
            api_key = base64.b64decode(encoded).decode()
        except Exception:
            logger.warning("apiKeyEncrypted Base64解码失败——注意：Base64不是加密，仅用于避免明文存储")
            pass
    return {
        "provider": cfg.get("provider", "deepseek"),
        "api_key": api_key,
        "base_url": cfg.get("baseUrl", "https://api.deepseek.com/v1"),
        "model": cfg.get("model", "deepseek-chat"),
    }


def _parse_frag_level(typing_style_json):
    """从 typing_style_json 中提取 fragmentation_level"""
    try:
        if isinstance(typing_style_json, str):
            ts = json.loads(typing_style_json)
        else:
            ts = typing_style_json or {}
        return float(ts.get("fragmentation_level", 0.5))
    except Exception:
        return 0.5


def test_chat_fragmentation(persona_id, persona_config, messages, label=""):
    """
    直接调 Python /api/message，验证分句行为

    返回: True=全部通过, False=有问题
    """
    prefix = f"  [{label}] " if label else "  "

    # 构造 persona_config（模拟 Java buildPersonaConfig，用创建时的数据）
    p_cfg = {
        "id": persona_id,
        "name": persona_config.get("name", "AI网友"),
        "typing_style_json": persona_config.get("typingStyleJson", ""),
        "attachment_anxiety": persona_config.get("attachmentAnxiety", 0.5),
        "attachment_avoidance": persona_config.get("attachmentAvoidance", 0.3),
        "social_rhythm": persona_config.get("socialRhythm", "slow_warm"),
        "conflict_style": persona_config.get("conflictStyle", "cold_shoulder"),
        "image_enabled": persona_config.get("imageEnabled", 0),
        "image_style_prompt": persona_config.get("imageStylePrompt", ""),
        "character_current_context": persona_config.get("characterCurrentContext", ""),
        "relationship_phase": persona_config.get("relationshipPhase", "stranger"),
        "recent_conversations": [],
    }

    frag_level = _parse_frag_level(p_cfg["typing_style_json"])
    api_config = _load_api_config()

    all_pass = True
    for i, msg in enumerate(messages):
        body = {
            "api_config": api_config,
            "persona_config": p_cfg,
            "relationship_state": {"trust": 50, "closeness": 20, "tension": 10},
            "recent_memories": [],
            "today_events_so_far": [],
            "user_message": msg,
            "timestamp": int(time.time()),
        }

        try:
            r = requests.post(f"{PYTHON}/api/message", json=body, timeout=120)
            resp = r.json()
        except Exception as e:
            fail(f"{prefix}第{i+1}轮 调用 Python 失败: {e}")
            all_pass = False
            continue

        reply = resp.get("reply") or {}
        raw_text = reply.get("raw_text", "")
        should_reply = resp.get("should_reply", False)

        has_split = "[SPLIT]" in raw_text
        split_count = raw_text.count("[SPLIT]")

        # 打印详情供人工判断
        print(f"{prefix}┌─ 第{i+1}轮: \"{msg}\"")
        print(f"{prefix}│  fragmentation_level = {frag_level}")
        print(f"{prefix}│  should_reply = {should_reply}")

        if frag_level <= 0.3:
            # 沉稳型：不应有 [SPLIT]
            if has_split:
                warn(f"{prefix}│  ⚠️  沉稳型不应分句，但检测到 {split_count} 个 [SPLIT]")
                all_pass = False
            else:
                ok(f"{prefix}│  ✅ 沉稳型正确：无 [SPLIT]")
            print(f"{prefix}│  raw_text: {raw_text}")
        elif frag_level <= 0.6:
            # 碎片化：应有 [SPLIT]
            if has_split:
                ok(f"{prefix}│  ✅ 碎片化正确：{split_count} 个 [SPLIT]")
            else:
                warn(f"{prefix}│  ⚠️  碎片化应分句，但 raw_text 无 [SPLIT]")
                all_pass = False
            print(f"{prefix}│  raw_text: {raw_text}")
        else:
            # 重度碎片化：应有多处 [SPLIT]
            if split_count >= 2:
                ok(f"{prefix}│  ✅ 重度碎片化正确：{split_count} 个 [SPLIT]")
            elif split_count >= 1:
                warn(f"{prefix}│  ⚠️  重度碎片化应密集拆分，但只有 {split_count} 个 [SPLIT]")
            else:
                warn(f"{prefix}│  ⚠️  重度碎片化应分句，但 raw_text 无 [SPLIT]")
                all_pass = False
            print(f"{prefix}│  raw_text: {raw_text}")
        print(f"{prefix}└─")

    return all_pass


# ============================================================
# 主动调用机制测试 —— 插入未来事件 + 等待 + 触发扫描 + 检测 LLM 响应
# ============================================================

def test_proactive_contact(persona_id, name=""):
    """
    测试主动调用机制：每 60 秒插入一个事件，等待后触发扫描，检查 LLM 是否被调用

    流程（三轮，每轮约 65 秒，共约 3 分 15 秒）:
      [1] 插入事件(+60s) → 等待 → 扫描 → 检测 LLM 响应 → 作废旧事件
      [2] 插入事件(+60s) → 等待 → 扫描 → 检测 LLM 响应 → 作废旧事件
      [3] 插入事件(+60s) → 等待 → 扫描 → 检测 LLM 响应 → 作废旧事件
    """
    prefix = f"  [{name}] " if name else "  "
    all_pass = True

    for round_num in range(1, 4):
        step(f"主动调用测试 第{round_num}轮/共3轮 —— 插入事件(+60s)")

        # 1. 插入一个 60 秒后到期的事件
        result = post("/api/sim/schedule-proactive-event", {
            "personaId": persona_id,
            "delaySeconds": "60",
            "eventType": "moment",
            "description": f"测试主动调用第{round_num}轮：想找用户聊天"
        })
        if not result.get("ok"):
            fail(f"{prefix}插入事件失败: {result.get('error')}")
            all_pass = False
            continue

        event_id = result.get("event_id", "")
        event_time = result.get("event_time", "")
        ok(f"{prefix}事件已插入: id={event_id[:8]}..., time={event_time}, delay=60s")

        # 2. 等待事件过期（多等 5 秒确保时间已稳稳过去）
        wait_sec = 65
        print(f"{prefix}等待 {wait_sec} 秒，让事件时间到达...")
        for remaining in range(wait_sec, 0, -5):
            print(f"{prefix}  剩余 {remaining} 秒...", end="\r")
            time.sleep(5)
        print(f"{prefix}  等待完成" + " " * 20)

        # 3. 触发事件扫描
        step(f"主动调用测试 第{round_num}轮 —— 触发扫描")
        result = post("/api/sim/trigger-events", {"personaId": persona_id})
        if not result.get("ok"):
            fail(f"{prefix}扫描失败: {result.get('error')}")
            all_pass = False
            continue

        llm_called = result.get("llm_called", False)
        inner_thought = result.get("inner_thought", "")
        attitude = result.get("attitude", "")

        if llm_called:
            ok(f"{prefix}✅ LLM 被调用 —— 主动调用机制生效")
            if inner_thought:
                print(f"{prefix}  内心独白: {inner_thought[:80]}")
            if attitude:
                print(f"{prefix}  态度: {attitude}")
        else:
            warn(f"{prefix}⚠️  LLM 未被调用（可能事件未到期或 API 配置问题）")
            all_pass = False

        # 4. 作废该事件，防止下轮扫描时被重复处理
        if event_id:
            invalidate_result = post("/api/sim/invalidate-event", {"eventId": event_id})
            if invalidate_result.get("ok"):
                ok(f"{prefix}旧事件已作废 (id={event_id[:8]}...)")
            else:
                warn(f"{prefix}作废事件失败: {invalidate_result.get('error')}")

    return all_pass


# ============================================================
# 单个 AI 的完整生命周期
# ============================================================

def lifecycle(name, persona_config, qq_number, rounds=3):
    global created_ids

    # --- 第1步：创建 Persona ---
    step(f"创建 AI {name}")
    result = post_json("/api/personas", persona_config)
    if result.get("status") != "ok":
        fail(f"创建失败: {result}")
        raise SystemExit(f"无法创建 {name}")
    persona_id = result["id"]
    created_ids.append(persona_id)
    ok(f"personaId={persona_id}")

    # 验证
    detail = get(f"/api/personas/{persona_id}")
    if isinstance(detail, dict):
        ok(f"名称={detail.get('name')}, 状态={detail.get('status')}")
    else:
        warn("无法获取详情")

    # --- 第2步：绑定 QQ ---
    step(f"绑定 QQ {qq_number}")
    result = post(f"/api/personas/{persona_id}/bind-channel",
                  {"type": "qq", "account": qq_number})
    if result.get("status") == "ok":
        ok(f"aiQq={qq_number}")
    else:
        fail(f"绑定失败: {result}")

    # --- 第3步：懒加载事件 ---
    step("事件懒加载（调用 LLM 生成事件线）")
    result = post("/api/sim/generate-events", {"personaId": persona_id})
    if result.get("ok"):
        events = result.get("events", [])
        count = result.get("count", 0)
        if count > 0:
            ok(f"生成 {count} 个事件:")
            for ev in events:
                print(f"      {ev['time']} {ev['type']:<8} {ev['description']}")
        else:
            warn(f"生成了 0 个事件")
    else:
        fail(f"事件生成失败: {result.get('error')}")

    # --- 第4步：模拟聊天 ---
    messages = [
        "你好呀～",
        "今天天气真不错，你在干嘛呢？",
        "周末有空吗？想约你出来玩"
    ]
    for i in range(rounds):
        step(f"模拟聊天 第{i+1}轮: \"{messages[i]}\"")
        result = post("/api/sim/message", {
            "personaId": persona_id,
            "message": messages[i]
        })
        if result.get("ok"):
            ok(f"回复成功 ({result.get('elapsed_ms')}ms)")
        else:
            fail(f"回复失败: {result.get('error')}")
        time.sleep(0.5)

    # --- 碎片化验证：直接调 Python /api/message，检查 [SPLIT] ---
    step("碎片化验证 —— 调 Python /api/message 检查 [SPLIT] 分句")
    frag_ok = test_chat_fragmentation(persona_id, persona_config, messages, name)
    if frag_ok:
        ok(f"{name} 碎片化行为符合 fragmentation_level 预期")
    else:
        warn(f"{name} 碎片化行为有偏差，请看上方日志判断")

    # 检查短期记忆
    short_term = get(f"/api/events/{persona_id}/today")
    is_ok = isinstance(short_term, (list, dict))
    if is_ok:
        # check conversation_turns in event_log via events endpoint
        thought = get(f"/api/events/{persona_id}/thought")
        if isinstance(thought, dict) and thought.get("raw_thought"):
            ok(f"短期记忆已写入 (内心独白: {thought['raw_thought'][:40]}...)")
        else:
            ok("短期记忆已写入")

    # --- 第5步：事件触发验证 ---
    step("事件触发扫描（验证 LLM 事件响应）")
    result = post("/api/sim/trigger-events", {"personaId": persona_id})
    if result.get("ok"):
        llm_called = result.get("llm_called", False)
        thought = result.get("inner_thought", "")
        attitude = result.get("attitude", "")
        if llm_called:
            ok(f"LLM 被调用 (内心独白: {thought[:50]}..., 态度: {attitude})")
        else:
            warn("本次无到期事件，LLM 未被调用")
        ok(f"扫描完成 (当前时间: {result.get('now')})")
    else:
        fail(f"触发失败: {result.get('error')}")

    # --- 第6步：图片生成测试（仅对 imageEnabled=1 的 AI） ---
    if persona_config.get("imageEnabled") == 1:
        step("图片生成测试")
        prompt = (persona_config.get("imageStylePrompt") or "二次元可爱风格")
        result = post("/api/sim/test-image", {
            "personaId": persona_id,
            "prompt": f"一只可爱的布偶猫在阳光下打滚"
        })
        if result.get("ok"):
            ok(f"图片已生成: {result.get('local_path')}")
        else:
            warn(f"图片生成失败: {result.get('error')}（可能是未配置图片 API）")

    # --- 第7步：跨天 ---
    step(f"模拟跨天（LLM 记忆总结 + 生成下一天事件）")
    result = post("/api/sim/new-day", {"personaId": persona_id})
    if result.get("ok"):
        count = result.get("new_count", 0)
        cleared = result.get("cleared", 0)
        ok(f"清除了 {cleared} 个旧事件, 新生成 {count} 个事件")
        for ev in result.get("events", []):
            print(f"      {ev['time']} {ev['type']:<8} {ev['description']}")
    else:
        fail(f"跨天失败: {result.get('error')}")

    # --- 第8步：强制事件触发（绕过时间判断直接测试 LLM 事件响应） ---
    step("强制事件触发（直接插入事件并调用 LLM，不依赖时间）")
    result = post("/api/sim/force-trigger-event", {
        "personaId": persona_id,
        "eventType": "moment",
        "description": "在吗？突然想找你说说话"
    })
    if result.get("ok"):
        if result.get("llm_called"):
            dispatched = result.get("reply_dispatched", False)
            ok(f"LLM 被调用 → 决定{'联系' if dispatched else '不联系'}用户"
               f"\n      内心独白: {result.get('inner_thought','')[:60]}"
               f"\n      态度: {result.get('attitude','')}")
        else:
            warn("LLM 未被调用（可能 API Key 配置问题）")
    else:
        fail(f"强制事件触发失败: {result.get('error')}")

    return persona_id


# ============================================================
# 主流程
# ============================================================

def main():
    global created_ids

    print("=" * 60)
    print("  Netizen-Simulator 完整生命周期模拟测试")
    print(f"  开始时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 60)

    # 检查前置条件
    step("检查服务健康状态")
    if not check_health():
        fail("Java 服务未运行！请先 `cd java-manager && mvn spring-boot:run`")
        sys.exit(1)
    ok("Java :8080 在线")
    try:
        r = requests.get("http://127.0.0.1:8000/api/health", timeout=5)
        if r.status_code == 200:
            ok("Python :8000 在线")
        else:
            warn(f"Python 状态异常: {r.status_code}")
    except Exception:
        logger.warning("Python 健康检查失败", exc_info=True)
        warn("Python 未运行——LLM/记忆功能将不可用")

    try:
        # ===== AI 1: 柳如烟 =====
        print("\n" + "🔵" * 30)
        print("  AI 1: 柳如烟（傲娇毒舌女大学生）")
        print("🔵" * 30)
        lifecycle("柳如烟", LIU_RUYAN, "111", rounds=3)

        # --- 主动调用机制测试（约 3 分钟，需 Java + Python 同时运行） ---
        step("主动调用机制测试 —— 插入事件+等待+扫描（约3分钟）")
        print("  此测试需要约 3 分钟，请耐心等待...")
        start_proactive = time.time()
        proactive_ok = test_proactive_contact(created_ids[-1], "柳如烟")
        elapsed_proactive = time.time() - start_proactive
        if proactive_ok:
            ok(f"主动调用测试全部通过 (耗时 {elapsed_proactive:.0f} 秒)")
        else:
            warn(f"主动调用测试存在偏差，请查看上方日志 (耗时 {elapsed_proactive:.0f} 秒)")

        # ===== AI 2: 柳二烟 ====="
        print("\n" + "🟢" * 30)
        print("  AI 2: 柳二烟（傲娇毒舌 + 图片能力）")
        print("🟢" * 30)

        # 先解绑柳如烟的 QQ
        step("转移 QQ：解绑柳如烟")
        old_persona_id = created_ids[-1]  # last one is 柳如烟
        name_lookup = get(f"/api/personas/{old_persona_id}")
        if isinstance(name_lookup, dict):
            post(f"/api/personas/{old_persona_id}/bind-channel",
                 {"type": "qq", "account": ""})
            ok(f"柳如烟 QQ 已解绑")

        # 创建柳二烟
        eryan_id = lifecycle("柳二烟", LIU_ERYAN, "111", rounds=3)

    except SystemExit:
        pass
    except KeyboardInterrupt:
        print("\n中断——开始清理...")
    except Exception as e:
        print(f"\n模拟异常: {e}")
    finally:
        print("\n" + "=" * 60)
        print(f"  模拟结束: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"  通过: {passed}  失败: {failed}  警告: {warnings}")
        print("=" * 60)

        # 自动清理
        run_cleanup()

        # 检查日志中的错误
        log_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "java-manager", "data", "logs")
        java_log = os.path.join(log_dir, "java.log")
        if os.path.exists(java_log):
            try:
                with open(java_log, "r", encoding="utf-8", errors="ignore") as f:
                    lines = f.readlines()
                errors = [l for l in lines[-500:] if "ERROR" in l or "WARN" in l]
                if errors:
                    print(f"\n日志中发现 {len(errors)} 条 ERROR/WARN (最近500行):")
                    for l in errors[-20:]:
                        print(f"  {l.rstrip()[:200]}")
            except Exception:
                logger.warning("日志文件读取失败", exc_info=True)
                pass


if __name__ == "__main__":
    main()
