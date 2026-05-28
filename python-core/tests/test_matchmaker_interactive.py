"""
牵线人交互式对话 —— 人工逐轮回复，直到 confirm 入库
运行: python tests/test_matchmaker_interactive.py
"""
import requests
import json
import time
import os
import sys

JAVA_BASE = "http://localhost:8080"
HEADERS = {"Content-Type": "application/json"}
CONFIG_PATH = os.path.normpath(os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "..", "java-manager", "data", "system_config.json"
))


def api_post(path, body=None):
    return requests.post(f"{JAVA_BASE}{path}", json=body or {}, headers=HEADERS)


def backup_hash():
    if os.path.exists(CONFIG_PATH):
        with open(CONFIG_PATH, "r", encoding="utf-8") as f:
            return json.load(f).get("masterKeyHash", "")
    return ""


def restore_hash(h):
    if not h or not os.path.exists(CONFIG_PATH):
        return
    with open(CONFIG_PATH, "r", encoding="utf-8") as f:
        data = json.load(f)
    data["masterKeyHash"] = h
    with open(CONFIG_PATH, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)


original_hash = backup_hash()
master_key = "test-mk-interactive-2026"
persona_id = None

try:
    # start
    r = api_post("/api/matchmaker/start")
    sid = r.json().get("sessionId")
    if not sid:
        print(f"启动失败: {r.json()}")
        sys.exit(1)
    print(f"会话: {sid}")

    round_count = 0
    complete = False

    while not complete:
        round_count += 1
        print(f"\n{'─'*50}")
        msg = input(f"[第 {round_count} 轮] 你: ").strip()
        if not msg:
            continue

        start = time.time()
        r = api_post("/api/matchmaker/chat", {"sessionId": sid, "userMessage": msg})
        elapsed = time.time() - start
        resp = r.json()

        reply = resp.get("reply", "(无回复)")
        stage = resp.get("currentStage", "?")
        complete = resp.get("complete", False)
        extracted = resp.get("extractedData")

        print(f"\n  [{stage}] 牵线人 ({elapsed:.1f}s):")
        print(f"  {reply}")
        if extracted:
            print(f"\n  提取字段: {list(extracted.keys())}")

        if complete:
            print(f"\n{'='*50}")
            print("访谈完成！确认创建...")
            r2 = api_post(
                f"/api/matchmaker/confirm?sessionId={sid}&masterKey={master_key}"
            )
            resp2 = r2.json()
            persona_id = resp2.get("personaId")
            if persona_id:
                print(f"✓ Persona 创建成功: {persona_id}")
                # 查询数据库
                r3 = requests.get(f"{JAVA_BASE}/api/personas/{persona_id}", headers=HEADERS)
                p = r3.json()
                print(f"\n=== 数据库中的 Persona ===")
                fields = ["name", "lifeStage", "lifeStageDetail", "currentLocation",
                          "relationshipPhase", "characterCurrentContext",
                          "characterInitialWorldTime", "imageStylePrompt",
                          "characterAppearance", "conflictStyle", "socialRhythm",
                          "typingSpeed", "attachmentAnxiety", "attachmentAvoidance",
                          "initiativeTendency", "sampleChatsJson", "matchmakerRawData"]
                for f in fields:
                    v = p.get(f)
                    if f in ("sampleChatsJson", "matchmakerRawData"):
                        v = str(v)[:80] + "..." if v and len(str(v)) > 80 else v
                    print(f"  {f}: {v}")
            else:
                print(f"✗ 创建失败: {resp2}")
            break

finally:
    restore_hash(original_hash)

if persona_id:
    print(f"\n保留 Persona ID: {persona_id}（未自动清理）")
