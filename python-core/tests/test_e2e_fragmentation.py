"""端到端测试：模拟 Java → Python 完整链路，验证 fragmentation_level → [SPLIT]

路径：
  SQLite DB → persona_config (同 Java buildPersonaConfig) 
  → build_message_system_prompt → DeepSeek → 检查 raw_text 是否含 [SPLIT]


与之前 test_fragmentation_prompt.py 的区别：
  前者: 手工构造 persona_config（可能和 DB 真实值不同）
  本测试: 直接从 DB 读小奈真实数据，完全模拟 Java 传过来的 persona_config
"""
import json
import sys
import os
import asyncio
import base64
import sqlite3

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
from engine.prompt_templates import build_message_system_prompt
from engine.llm.factory import create_provider

DB_PATH = os.path.join(
    os.path.dirname(__file__), "..", "..", "java-manager", "data", "social_persona.db"
)
CONFIG_PATH = os.path.join(
    os.path.dirname(__file__), "..", "..", "java-manager", "data", "system_config.json"
)


def load_persona_from_db():
    """直接从 SQLite DB 读取小奈数据，模拟 Java PersonaService.getById() 查库"""
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    row = conn.execute(
        "SELECT * FROM personas WHERE name LIKE '%小奈%'"
    ).fetchone()
    conn.close()

    if not row:
        raise RuntimeError("DB 中未找到小奈！")

    persona = dict(row)
    print(f"[DB] 读取到小奈: id={persona['id']}, name={persona['name']}")
    print(f"[DB] typing_style_json = {persona['typing_style_json']}")
    return persona


def build_persona_config(persona: dict):
    """完全模拟 Java MessageService.buildPersonaConfig()"""
    cfg = {
        "id": persona["id"],
        "name": persona["name"],
        "big_five_json": persona.get("big_five_json"),
        "attachment_anxiety": persona.get("attachment_anxiety"),
        "attachment_avoidance": persona.get("attachment_avoidance"),
        "self_esteem_stability": persona.get("self_esteem_stability"),
        "social_rhythm": persona.get("social_rhythm"),
        "conflict_style": persona.get("conflict_style"),
        "initiative_tendency": persona.get("initiative_tendency"),
        "input_method": persona.get("input_method"),
        "typing_style_json": persona.get("typing_style_json"),  # ★ 关键字段
        "typing_speed": persona.get("typing_speed"),
        "image_style_prompt": persona.get("image_style_prompt"),
        "image_enabled": persona.get("image_enabled", 1),
        "sample_chats_json": persona.get("sample_chats_json"),
        "character_current_context": persona.get("character_current_context"),
        "relationship_phase": persona.get("relationship_phase"),
    }
    return cfg


async def run_e2e_test():
    print("=" * 60)
    print("端到端测试：SQLite → Java buildPersonaConfig → Python → DeepSeek")
    print("=" * 60)

    # Step 1: 从 DB 读数据
    persona = load_persona_from_db()

    # Step 2: 构造 persona_config（同 Java）
    persona_config = build_persona_config(persona)
    print(f"\n[Config] typing_style_json = {persona_config['typing_style_json']}")

    # Step 3: 解析 fragmentation_level
    from engine.prompt_templates import _parse_fragmentation_level, _build_fragmentation_instruction
    frag_level = _parse_fragmentation_level(persona_config["typing_style_json"])
    frag_instruction = _build_fragmentation_instruction(frag_level)
    print(f"[Config] 解析出 fragmentation_level = {frag_level}")
    print(f"[Config] 生成的指令: {frag_instruction}")

    # Step 4: 构建 System Prompt（同 message_routes.py）
    relationship_state = {"trust": 50, "closeness": 20, "tension": 10}
    today_events = []
    current_context = persona_config.get("character_current_context", "")

    system_prompt = build_message_system_prompt(
        persona_config, relationship_state, today_events, current_context
    )

    # 打印 System Prompt 中碎片化相关部分
    lines = system_prompt.split("\n")
    for i, line in enumerate(lines):
        if "碎片化" in line or "fragmentation" in line or "SPLIT" in line:
            print(f"[Prompt L{i}] {line}")

    # Step 5: 调用 LLM
    with open(CONFIG_PATH, "r", encoding="utf-8") as f:
        cfg = json.load(f)

    api_config = {
        "provider": cfg.get("provider", "deepseek"),
        "base_url": cfg.get("baseUrl", "https://api.deepseek.com/v1"),
        "model": cfg.get("model", "deepseek-chat"),
    }
    # 注意：Base64 不是加密，仅用于测试中避免明文存储 API Key
    encoded = cfg.get("apiKeyEncrypted", "")
    api_config["api_key"] = base64.b64decode(encoded).decode() if encoded else ""

    provider = create_provider(api_config)

    user_prompt = (
        "用户刚才说：'哼，既然你这么爱听，那我就再讲一个～昨天在操场，有个人跑步时鞋带散了，"
        "自己踩到鞋带摔了个狗啃泥，爬起来还若无其事地继续跑，假装什么都没发生，笑死我了～"
        "不过你彩票到底验证了没有啊，别光顾着笑！'"
    )

    print(f"\n[LLM] 正在调用 DeepSeek...")
    result = await provider.chat(
        system_prompt, user_prompt, response_format={"type": "json_object"}
    )

    reply = result.get("reply", {})
    raw_text = (
        reply.get("raw_text", "") if isinstance(reply, dict) else str(reply)
    )

    print(f"\n{'='*60}")
    print(f"端到端测试结果")
    print(f"{'='*60}")
    print(f"fragmentation_level: {frag_level}")
    print(f"raw_text: {raw_text}")
    print(f"含 [SPLIT]: {'[SPLIT]' in raw_text}")
    print(f"[SPLIT] 个数: {raw_text.count('[SPLIT]')}")

    if "[SPLIT]" in raw_text:
        print(f"\n✅ 端到端链路正常！fragmentation_level={frag_level} → LLM 生成了 [SPLIT]")
        print(f"   如果 QQ 端仍然不分句，请检查：")
        print(f"   1. Java 服务是否已重启（清除 @Cacheable 缓存）")
        print(f"   2. Python 服务是否已重启（加载新版 prompt_templates.py）")
    else:
        print(f"\n❌ 端到端链路异常！fragmentation_level={frag_level} 但 LLM 未生成 [SPLIT]")


if __name__ == "__main__":
    asyncio.run(run_e2e_test())