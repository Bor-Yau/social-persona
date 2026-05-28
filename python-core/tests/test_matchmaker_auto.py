"""牵线人自动对话测试——一次性跑完整段 7 阶段访谈"""
import requests
import json
import time
import os
import sys

JAVA_BASE = "http://localhost:8080"
HEADERS = {"Content-Type": "application/json"}
CONFIG_PATH = os.path.normpath(os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "..", "..", "java-manager", "data", "system_config.json"
))


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

try:
    r = requests.post(f"{JAVA_BASE}/api/matchmaker/start", json={}, headers=HEADERS)
    sid = r.json()["sessionId"]
    print(f"会话: {sid}\n")

    # 预定义对话策略——针对每个阶段可能的问题准备好回复
    # 每轮：先发送消息 → 读取牵线人回复 → 根据回复内容决定下一条
    messages = [
        # basic_profile 阶段问题：名字、性别、年龄、性格、关系定位、世界时间
        "我想创建一个女网友，叫柳如烟。23岁，大四计算机系学生。性格温和安静，有点社恐，喜欢编程和看书。我们以陌生人身份开始吧。她的世界从2025年6月15日开始。",

# 牵线人会追问"关系定位"（纯聊天or更深连接）
        "就是朋友关系吧，能互相分享日常、吐槽、偶尔倾诉的那种。不是恋爱向的。",

        # style_anchor - 回答说话风格参考
        "她的说话风格参考《葬送的芙莉莲》里的芙莉莲——温柔、轻声细语、偶尔有点天然呆。用词偏书面但不会太生硬。如果用一个词形容，就是「温柔」。",

        # 牵线人追问一个词 → 已在上轮回答，可能追问冲突风格
        "冲突时她会先沉默一会儿，然后温和地表达自己的感受，不会直接怼人，不会冷战。她属于那种表面平静但内心会默默消化情绪的类型。",

        # boundary_probe
        "如果三天没联系她，她会发一条消息问一下'最近还好吗'，不会狂轰滥炸。如果我对她说'你管得着吗'，她可能愣一下然后说'我只是有点担心你'。希望她每天主动找我一次左右吧。",

        # attachment_explore
        "如果我拉黑她，她可能会难过但不会纠缠，会默默等。过一个月重新加回来，她会先问'为什么？'，然后慢慢重新建立信任，大概半个月能回到之前的状态。她是那种表面平静但心里在意的人。",

        # system_detail
        "回复节奏是steady——不会秒回但也不会拖很久。主动倾向打0.4分。手机打字，喜欢一口气把话说完（碎片化程度大概0.3）。打字速度3分吧。开启图片功能！图片风格：二次元动漫风格，柔和色调，日系画风。外貌：黑色长发，戴着眼镜，经常穿白色衬衫和格子裙，身高165cm，体型纤细。认识阶段就是stranger。年龄23。她目前是大四计算机系学生，在杭州。",
    ]

    complete = False
    round_num = 0
    msg_idx = 0

    while not complete and round_num < 15:
        round_num += 1
        print(f"{'─'*60}")
        print(f"第 {round_num} 轮")

        # 确定本轮消息
        if msg_idx < len(messages):
            user_msg = messages[msg_idx]
            msg_idx += 1
        else:
            # 如果预定义消息用完了但还在 sample_confirm，说"确认创建"
            user_msg = "确认创建"

        print(f"我: {user_msg[:60]}...")
        start = time.time()
        r = requests.post(f"{JAVA_BASE}/api/matchmaker/chat",
                          json={"sessionId": sid, "userMessage": user_msg},
                          headers=HEADERS)
        elapsed = time.time() - start
        resp = r.json()

        reply = resp.get("reply", "(无回复)")
        stage = resp.get("currentStage", "?")
        complete = resp.get("complete", False)
        extracted = resp.get("extractedData", {})

        print(f"[{stage}] 牵线人 ({elapsed:.1f}s):")
        print(f"{reply[:500]}")
        if extracted:
            print(f"提取: {list(extracted.keys())}")
        print()

        if complete:
            print(f"{'='*60}")
            print("访谈完成！confirm 入库...")
            r2 = requests.post(
                f"{JAVA_BASE}/api/matchmaker/confirm?sessionId={sid}&masterKey={master_key}",
                headers=HEADERS
            )
            resp2 = r2.json()
            pid = resp2.get("personaId")
            if pid:
                print(f"✓ Persona 创建成功: {pid}")
                r3 = requests.get(f"{JAVA_BASE}/api/personas/{pid}", headers=HEADERS)
                p = r3.json()
                print(f"\n===== 数据库实际内容 =====")
                fields = [
                    ("name", None),
                    ("lifeStage", None),
                    ("lifeStageDetail", None),
                    ("currentLocation", None),
                    ("relationshipPhase", None),
                    ("characterCurrentContext", None),
                    ("characterInitialWorldTime", None),
                    ("birthday", None),
                    ("imageStylePrompt", None),
                    ("characterAppearance", None),
                    ("imageEnabled", None),
                    ("conflictStyle", None),
                    ("socialRhythm", None),
                    ("typingSpeed", None),
                    ("attachmentAnxiety", None),
                    ("attachmentAvoidance", None),
                    ("selfEsteemStability", None),
                    ("initiativeTendency", None),
                    ("inputMethod", None),
                    ("typingStyleJson", None),
                    ("bigFiveJson", None),
                    ("sampleChatsJson", "long"),
                    ("matchmakerRawData", "long"),
                ]
                for fname, ftype in fields:
                    v = p.get(fname)
                    if ftype == "long" and v:
                        v = str(v)[:100] + "..."
                    print(f"  {fname}: {v}")

                r4 = requests.get(f"{JAVA_BASE}/api/personas/{pid}/export", headers=HEADERS)
                export = r4.json()
                archive = export.get("life_archive") or export.get("character_life_outline")
                if archive:
                    print(f"\n  人生档案: {list(archive.keys()) if isinstance(archive, dict) else '有内容'}")
            else:
                print(f"✗ confirm 失败: {resp2}")
            break

    if not complete:
        print(f"⚠ 未在 {round_num} 轮内完成")

finally:
    restore_hash(original_hash)
