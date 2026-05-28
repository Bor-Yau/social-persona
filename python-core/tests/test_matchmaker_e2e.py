"""
牵线人系统端到端测试 —— Part B（直接 confirm）+ Part A（完整 7 阶段访谈）

运行方式:
  python tests/test_matchmaker_e2e.py
"""

import requests
import json
import sys
import time
import os

JAVA_BASE = "http://localhost:8080"
HEADERS = {"Content-Type": "application/json"}
SEP = "=" * 70
SUBSEP = "-" * 50
CONFIG_PATH = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "..", "java-manager", "data", "system_config.json"
)
CONFIG_PATH = os.path.normpath(CONFIG_PATH)


def print_step(num, title):
    print(f"\n{SEP}")
    print(f"  [步骤 {num}] {title}")
    print(f"{SEP}")


def print_ok(msg):
    print(f"  ✓ {msg}")


def print_fail(msg):
    print(f"  ✗ {msg}")


def print_info(msg):
    print(f"  → {msg}")


def print_warn(msg):
    print(f"  ⚠ {msg}")


def print_data(key, value):
    if isinstance(value, (dict, list)):
        value = json.dumps(value, ensure_ascii=False, indent=2)
    print(f"  {key}: {value}")


def api_post(path, body=None):
    if body is None:
        body = {}
    return requests.post(f"{JAVA_BASE}{path}", json=body, headers=HEADERS)


def api_get(path):
    return requests.get(f"{JAVA_BASE}{path}", headers=HEADERS)


def backup_master_key_hash():
    """备份 system_config.json 中的 masterKeyHash"""
    if os.path.exists(CONFIG_PATH):
        with open(CONFIG_PATH, "r", encoding="utf-8") as f:
            data = json.load(f)
        return data.get("masterKeyHash", "")
    return ""


def restore_master_key_hash(original_hash):
    """恢复 masterKeyHash"""
    if not os.path.exists(CONFIG_PATH) or not original_hash:
        return
    with open(CONFIG_PATH, "r", encoding="utf-8") as f:
        data = json.load(f)
    data["masterKeyHash"] = original_hash
    with open(CONFIG_PATH, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print_info(f"masterKeyHash 已恢复: {original_hash[:20]}...")


# ============================================================
# Part B: 直接 confirm 验证数据写入链
# ============================================================
def test_part_b():
    print(f"\n{'#' * 70}")
    print(f"  Part B: 直接 confirm 验证数据写入链")
    print(f"{'#' * 70}")

    test_master_key = "test-matchmaker-key-2026"
    original_hash = backup_master_key_hash()
    session_id = None
    persona_id = None

    try:
        # === B-1: 启动会话 ===
        print_step("B-1", "启动牵线人会话 → 获取 sessionId")
        r = api_post("/api/matchmaker/start")
        resp = r.json()
        session_id = resp.get("sessionId")
        if not session_id:
            print_fail(f"启动失败: {resp}")
            return
        print_ok(f"sessionId: {session_id}")

        # === B-2: 模拟完整 7 阶段聊天 → 直到 is_complete=true ===
        print_step("B-2", "模拟完整 7 阶段访谈（自动发送用户消息）")

        stages = [
            ("basic_profile",
             "我想创建一位叫柳如烟的女网友，23岁，大四计算机系学生，即将毕业。"
             "性格温和但有点社恐，喜欢编程和看书。"
             "我们以陌生人身份开始。世界初始时间是2025-06-15。"),
            ("style_anchor",
             "她的说话风格参考葬送的芙莉莲里的芙莉莲——温柔、轻声细语、偶尔有点天然呆。"
             "用词偏书面但不生硬，给人安静治愈的感觉。"),
            ("boundary_probe",
             "三天没联系她应该会发消息问问但不会轰炸。"
             "如果我说'你管得着吗'她可能沉默一下然后说'我只是关心你'。"
             "大概每天主动联系一次。"),
            ("attachment_explore",
             "如果我拉黑她，她可能会难过但不会纠缠。"
             "过一个月重新加回来，她会问为什么。"
             "信任重建需要一段时间，大概半个月。"),
            ("system_detail",
             "回复节奏steady。主动倾向打0.4分，不算太主动。"
             "手机打字。碎片化程度0.3——喜欢把话一口气说完。打字速度3分。"
             "开启图片功能！风格偏好：二次元动漫风格，柔和色调，日系画风。"
             "外貌：黑色长发，戴着眼镜，经常穿白色衬衫和格子裙，身高165cm，体型纤细。"
             "认识阶段是stranger。年龄23岁。"),
        ]

        complete = False
        current_stage = "basic_profile"
        user_msg_index = 0
        round_count = 0
        sample_confirm_rounds = 0

        while not complete:
            round_count += 1
            print(f"\n  {'─' * 50}")
            print(f"  [B-2 第 {round_count} 轮] 当前阶段: {current_stage}")

            if current_stage == "sample_confirm" and sample_confirm_rounds == 0:
                user_msg = "确认创建"
                sample_confirm_rounds += 1
                print(f"  → 用户消息: {user_msg}")
            elif current_stage != "sample_confirm" and user_msg_index < len(stages):
                expected_stage, user_msg = stages[user_msg_index]
                print(f"  → 用户消息 (阶段提示: {expected_stage}): {user_msg[:50]}...")
                user_msg_index += 1
            elif current_stage == "sample_confirm":
                user_msg = "确认创建"
                sample_confirm_rounds += 1
                print(f"  → 用户消息: {user_msg}")
            else:
                print_fail(f"阶段消息用尽但未完成: stage={current_stage}")
                break

            print_info("调 LLM...")
            start = time.time()
            r = api_post("/api/matchmaker/chat", {
                "sessionId": session_id,
                "userMessage": user_msg
            })
            elapsed = time.time() - start
            resp = r.json()

            reply = resp.get("reply", "")
            current_stage = resp.get("currentStage", current_stage)
            complete = resp.get("complete", False)
            extracted = resp.get("extractedData")

            print(f"  阶段: {current_stage} | complete: {complete} | {elapsed:.1f}s")
            if reply:
                print(f"  牵线人回复: {reply[:80]}...")
            if extracted:
                keys = list(extracted.keys())
                print(f"  提取字段({len(keys)}): {keys}")

            if round_count > 15:
                print_warn("超过 15 轮仍未完成，强制退出")
                break

        if not complete:
            print_fail("LLM 未在预期轮次内标记 is_complete=true")
            return

        print_ok(f"访谈完成: {round_count} 轮, 当前阶段 {current_stage}, complete={complete}")

        # === B-3: confirm 写入数据库 ===
        print_step("B-3", "confirm → 写入数据库")

        print_info(f"调 confirm: sessionId={session_id}, masterKey={test_master_key}")
        r = api_post(f"/api/matchmaker/confirm?sessionId={session_id}&masterKey={test_master_key}")
        resp = r.json()
        persona_id = resp.get("personaId")
        if persona_id:
            print_ok(f"Persona 已创建: {persona_id}")
        else:
            print_fail(f"confirm 失败: {resp.get('reply', resp)}")
            return

        # === B-4: 查询数据库 → 字段对比 ===
        print_step("B-4", "查询数据库 → 验证所有字段是否正确写入")

        r = api_get(f"/api/personas/{persona_id}")
        persona = r.json()

        checks = [
            ("name", "柳如烟", persona.get("name")),
            ("lifeStage", persona.get("lifeStage")),
            ("lifeStageDetail", persona.get("lifeStageDetail")),
            ("currentLocation", persona.get("currentLocation")),
            ("relationshipPhase", persona.get("relationshipPhase")),
            ("imageStylePrompt", persona.get("imageStylePrompt")),
            ("characterAppearance", persona.get("characterAppearance")),
            ("characterInitialWorldTime", persona.get("characterInitialWorldTime")),
            ("characterCurrentContext", persona.get("characterCurrentContext")),
            ("imageEnabled", persona.get("imageEnabled")),
            ("bigFiveJson", persona.get("bigFiveJson")),
            ("typingStyleJson", persona.get("typingStyleJson")),
            ("typingSpeed", persona.get("typingSpeed")),
            ("attachmentAnxiety", persona.get("attachmentAnxiety")),
            ("attachmentAvoidance", persona.get("attachmentAvoidance")),
            ("initiativeTendency", persona.get("initiativeTendency")),
            ("conflictStyle", persona.get("conflictStyle")),
            ("socialRhythm", persona.get("socialRhythm")),
            ("inputMethod", persona.get("inputMethod")),
            ("matchmakerRawData", persona.get("matchmakerRawData")),
            ("sampleChatsJson", persona.get("sampleChatsJson")),
        ]

        passed = 0
        failed = 0
        print(f"\n  ┌─ Persona 字段验证 ─────────────────────────────")
        print(f"  │ {'字段':<30} {'结果'}")
        print(f"  │ {'─'*28}   {'─'*40}")
        for item in checks:
            if len(item) == 2:
                # 只检查非空
                field, value = item
                ok = value is not None and str(value).strip() not in ("", "None", "null", "{}", "[]")
                mark = "✓" if ok else "✗"
                if ok:
                    passed += 1
                else:
                    failed += 1
                print(f"  │ {mark} {field:<28} {'有值' if ok else '空/None'}")
            else:
                # 精确比对
                field, expected, actual = item
                match = str(actual) == str(expected) if actual is not None else False
                mark = "✓" if match else "✗"
                if match:
                    passed += 1
                else:
                    failed += 1
                act_str = str(actual)[:40] if actual is not None else "None"
                if match:
                    print(f"  │ {mark} {field:<28} ✓ {act_str}")
                else:
                    print(f"  │ {mark} {field:<28} 预期={expected}, 实际={act_str}")
        print(f"  └{'─'*70}")
        print(f"  通过: {passed}/{passed+failed}, 失败: {failed}")

        # === B-5: export 端点查看完整导出 ===
        print_step("B-5", "GET export → 查看完整 Persona 配置导出")
        r = api_get(f"/api/personas/{persona_id}/export")
        export_text = r.text
        try:
            export_json = json.loads(export_text)
            has_archive = "life_archive" in export_json or "character_life_outline" in export_json
            keys = list(export_json.keys()) if isinstance(export_json, dict) else []
            print_ok(f"export 返回 {len(keys)} 个顶层字段: {keys[:15]}")
            if has_archive:
                archive_data = export_json.get("life_archive") or export_json.get("character_life_outline", {})
                if isinstance(archive_data, dict):
                    archive_keys = list(archive_data.keys())
                    print(f"  人生档案字段: {archive_keys}")
        except json.JSONDecodeError:
            print_warn(f"export 非 JSON: {export_text[:100]}")

        # === B-6: 展示完整 persona JSON ===
        print_step("B-6", "完整 Persona JSON（数据库实际内容）")
        print(json.dumps({
            "name": persona.get("name"),
            "lifeStage": persona.get("lifeStage"),
            "lifeStageDetail": persona.get("lifeStageDetail"),
            "currentLocation": persona.get("currentLocation"),
            "relationshipPhase": persona.get("relationshipPhase"),
            "characterCurrentContext": persona.get("characterCurrentContext"),
            "characterInitialWorldTime": persona.get("characterInitialWorldTime"),
            "bigFiveJson": persona.get("bigFiveJson"),
            "attachmentAnxiety": persona.get("attachmentAnxiety"),
            "attachmentAvoidance": persona.get("attachmentAvoidance"),
            "selfEsteemStability": persona.get("selfEsteemStability"),
            "socialRhythm": persona.get("socialRhythm"),
            "conflictStyle": persona.get("conflictStyle"),
            "initiativeTendency": persona.get("initiativeTendency"),
            "inputMethod": persona.get("inputMethod"),
            "typingStyleJson": persona.get("typingStyleJson"),
            "typingSpeed": persona.get("typingSpeed"),
            "imageStylePrompt": persona.get("imageStylePrompt"),
            "characterAppearance": persona.get("characterAppearance"),
            "imageEnabled": persona.get("imageEnabled"),
            "sampleChatsJson": persona.get("sampleChatsJson"),
            "matchmakerRawData": json.dumps(
                persona.get("matchmakerRawData")[:200] + "..."
                if persona.get("matchmakerRawData") and len(str(persona.get("matchmakerRawData"))) > 200
                else persona.get("matchmakerRawData")
            ) if persona.get("matchmakerRawData") else "None",
        }, ensure_ascii=False, indent=2))

        return persona_id

    finally:
        restore_master_key_hash(original_hash)

    return persona_id


# ============================================================
# Part A: 交互式预览（可选）
# ============================================================
def test_part_a():
    print(f"\n{'#' * 70}")
    print(f"  Part A: 交互式回顾（查看 LLM 全程回复）")
    print(f"{'#' * 70}")

    r = api_post("/api/matchmaker/start")
    session_id = r.json().get("sessionId")
    if not session_id:
        print_fail("启动会话失败")
        return

    original_hash = backup_master_key_hash()
    test_master_key = "test-matchmaker-key-2026-a"

    try:
        print_ok(f"会话已创建: {session_id}")
        print_info("我会把 LLM 的每次回复都打印出来。输入 'quit' 退出。")

        round_count = 0
        complete = False
        while not complete:
            round_count += 1
            user_msg = input(f"\n[第 {round_count} 轮] 请输入（或输入 quit 退出）: ")
            if user_msg.lower() == "quit":
                print_info("用户退出")
                break

            start = time.time()
            r = api_post("/api/matchmaker/chat", {
                "sessionId": session_id,
                "userMessage": user_msg
            })
            elapsed = time.time() - start
            resp = r.json()

            reply = resp.get("reply", "")
            stage = resp.get("currentStage", "?")
            complete = resp.get("complete", False)
            extracted = resp.get("extractedData")

            print(f"\n  [{stage}] 牵线人 ({elapsed:.1f}s):")
            print(f"  {reply}")
            if extracted:
                print(f"  提取: {json.dumps(extracted, ensure_ascii=False)[:200]}")

            if complete:
                print_ok("访谈完成！")
                # confirm
                r2 = api_post(
                    f"/api/matchmaker/confirm?sessionId={session_id}&masterKey={test_master_key}"
                )
                resp2 = r2.json()
                pid = resp2.get("personaId")
                if pid:
                    print_ok(f"Persona 创建成功: {pid}")
                else:
                    print_fail(f"创建失败: {resp2}")
                break
    finally:
        restore_master_key_hash(original_hash)


# ============================================================
# 清理
# ============================================================
def cleanup(persona_id):
    if not persona_id:
        return
    print(f"\n{SUBSEP}")
    print(f"  清理测试数据 → Persona: {persona_id}")
    print(f"  >>> 按 Enter 清理，输入其他跳过:")
    choice = input("  >>> ").strip()
    if choice != "":
        print_info("跳过清理")
        return

    r = api_post("/api/sim/cleanup", {"personaIds": [persona_id]})
    resp = r.json()
    print_ok("已清理！")
    for k, v in resp.items():
        if k != "ok":
            print(f"  {k}: {v}")


# ============================================================
def main():
    print(f"\n{'#' * 70}")
    print(f"  牵线人系统端到端测试")
    print(f"{'#' * 70}")
    print(f"  Java: {JAVA_BASE}")
    print(f"  开始: {time.strftime('%Y-%m-%d %H:%M:%S')}")

    # Part B: 自动模拟完整流程
    persona_id = test_part_b()

    if persona_id:
        cleanup(persona_id)

    # Part A: 可选交互式测试
    print(f"\n{SEP}")
    part_a = input("  是否运行 Part A（交互式访谈）？[y/N]: ").strip().lower()
    if part_a == "y":
        test_part_a()

    print(f"\n{'#' * 70}")
    print(f"  测试完成")
    print(f"{'#' * 70}")


if __name__ == "__main__":
    main()
