"""测试 fragmentation_level → [SPLIT] 行为 —— 绕过 Java，直接调 LLM"""
import json
import sys
import os
import asyncio
import base64

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
from engine.prompt_templates import build_message_system_prompt
from engine.llm.factory import create_provider

CONFIG_PATH = os.path.join(
    os.path.dirname(__file__), "..", "..", "java-manager", "data", "system_config.json"
)


async def test_fragmentation(level: float, description: str):
    """用指定的 fragmentation_level 构造 persona_config 并测试 LLM 输出"""
    persona_config = {
        "name": "小奈",
        "character_current_context": "你是小奈，一个性格傲娇的大学生网友，和用户正在闲聊中",
        "attachment_anxiety": 0.6,
        "attachment_avoidance": 0.3,
        "conflict_style": "playful_tease",
        "social_rhythm": "fast_warm",
        "typing_style_json": json.dumps(
            {
                "default_style": "简洁利落",
                "override_triggers": None,
                "fragmentation_level": level,
            },
            ensure_ascii=False,
        ),
        "relationship_phase": "acquaintance",
        "image_enabled": 0,
    }

    relationship_state = {"trust": 50, "closeness": 20, "tension": 10}
    today_events = []
    current_context = persona_config["character_current_context"]

    system_prompt = build_message_system_prompt(
        persona_config, relationship_state, today_events, current_context
    )
    user_prompt = "用户说：'哼，既然你这么爱听，那我就再讲一个～昨天在操场，有个人跑步时鞋带散了...'"

    with open(CONFIG_PATH, "r", encoding="utf-8") as f:
        cfg = json.load(f)

    api_config = {
        "provider": cfg.get("provider", "deepseek"),
        "base_url": cfg.get("baseUrl", "https://api.deepseek.com/v1"),
        "model": cfg.get("model", "deepseek-chat"),
    }
    # 注意：Base64 不是加密，仅用于测试中避免明文存储 API Key
    encoded = cfg.get("apiKeyEncrypted", "")
    if encoded:
        api_config["api_key"] = base64.b64decode(encoded).decode()
    else:
        api_config["api_key"] = ""

    provider = create_provider(api_config)
    result = await provider.chat(
        system_prompt, user_prompt, response_format={"type": "json_object"}
    )

    reply = result.get("reply", {})
    raw_text = (
        reply.get("raw_text", "") if isinstance(reply, dict) else str(reply)
    )

    print(f"\n{'='*60}")
    print(f"测试: fragmentation_level={level} ({description})")
    print(f"{'='*60}")
    print(f"raw_text: {raw_text}")
    print(f"含 [SPLIT]: {'[SPLIT]' in raw_text}")
    print(f"[SPLIT] 个数: {raw_text.count('[SPLIT]')}")
    return raw_text


if __name__ == "__main__":
    asyncio.run(test_fragmentation(0.0, "沉稳型 — 不应拆分"))
    asyncio.run(test_fragmentation(0.55, "碎片化 — 应拆分（小奈当前值）"))
    asyncio.run(test_fragmentation(0.8, "重度碎片化 — 应密集拆分"))