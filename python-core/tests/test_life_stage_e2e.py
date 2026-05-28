"""
角色生命阶段全流程端到端测试（含 LLM 自动切换验证）

运行方式:
  python tests/test_life_stage_e2e.py
"""

import requests
import json
import sys
import time

JAVA_BASE = "http://localhost:8080"
HEADERS = {"Content-Type": "application/json"}
SEP = "=" * 70
SUBSEP = "-" * 50


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
        value = json.dumps(value, ensure_ascii=False, indent=4)
    print(f"  {key}: {value}")


def print_event_table(events, title=""):
    if not events:
        print("  (无事件)")
        return
    if title:
        print(f"\n  ┌─ {title} ───────────────────────────")
    print(f"  │ {'时间':<10} {'类型':<12} {'描述'}")
    print(f"  │ {'─'*8}   {'─'*10}   {'─'*50}")
    for e in events:
        t = e.get("eventTime", e.get("time", "?"))
        tp = e.get("eventType", e.get("type", "?"))
        desc = e.get("description", e.get("desc", ""))
        icon = {"routine": "📋", "moment": "💡", "sleep": "🌙"}.get(tp, "❓")
        print(f"  │ {t:<10} {icon} {tp:<10} {desc}")
    print(f"  └{'─'*70}")


def api_post(path, body):
    return requests.post(f"{JAVA_BASE}{path}", json=body, headers=HEADERS)


def api_get(path):
    return requests.get(f"{JAVA_BASE}{path}", headers=HEADERS)


def api_put(path, body):
    return requests.put(f"{JAVA_BASE}{path}", json=body, headers=HEADERS)


def check_persona_fields(persona_id, expected, label):
    """检查 persona 字段是否匹配预期，打印对比结果"""
    r = api_get(f"/api/personas/{persona_id}")
    p = r.json()
    all_ok = True
    for field, expected_val in expected.items():
        actual = p.get(field)
        ok = actual == expected_val
        mark = "✓" if ok else "✗"
        if not ok:
            all_ok = False
        print(f"  {mark} {field}: 预期={expected_val}, 实际={actual}")
    return all_ok


def main():
    print(f"\n{'#' * 70}")
    print(f"  角色生命阶段全流程端到端测试（含自动切换验证）")
    print(f"{'#' * 70}")
    print(f"  Java: {JAVA_BASE}")
    print(f"  开始: {time.strftime('%Y-%m-%d %H:%M:%S')}")

    persona_id = None

    # =====================================================
    # 步骤 1：创建柳如烟（大四学生，临近毕业）
    # =====================================================
    print_step(1, "创建柳如烟（大四学生 / 杭州，临近毕业）")

    create_body = {
        "name": "柳如烟",
        "bigFiveJson": json.dumps({
            "openness": 0.65, "conscientiousness": 0.70,
            "extraversion": 0.55, "agreeableness": 0.75, "neuroticism": 0.40
        }),
        "attachmentAnxiety": 0.45,
        "attachmentAvoidance": 0.30,
        "selfEsteemStability": 0.65,
        "socialRhythm": "steady",
        "conflictStyle": "direct_confront",
        "initiativeTendency": 0.40,
        "inputMethod": "phone_thumb",
        "typingStyleJson": json.dumps({"fragmentation_level": 0.3}),
        "typingSpeed": 3.0,
        "imageStylePrompt": "二次元动漫风格，柔和色调，日系画风",
        "characterAppearance": "黑色长发，戴着眼镜，经常穿白色衬衫和格子裙，身高165cm，体型纤细",
        "imageEnabled": 1,
        "characterInitialWorldTime": "2025-06-15",
        "birthday": "2002-03-28",
        "characterCurrentContext": "柳如烟是浙江大学计算机系大四学生，即将毕业。论文答辩刚刚通过，已经拿到了上海一家互联网公司的offer。最近在忙着收拾行李、参加毕业典礼、和同学们道别。心情复杂：既有对校园生活的不舍，也有对未来职场的期待和紧张。",
        "lifeStage": "student",
        "lifeStageDetail": "大四计算机系学生（即将毕业）",
        "currentLocation": "杭州",
        "relationshipPhase": "stranger"
    }

    r = requests.post(f"{JAVA_BASE}/api/personas",
                      data=json.dumps(create_body), headers=HEADERS)
    resp = r.json()
    if resp.get("status") == "ok":
        persona_id = resp["id"]
        print_ok(f"创建成功 → ID: {persona_id}")
    else:
        print_fail(f"创建失败: {resp}")
        sys.exit(1)

    r = api_get(f"/api/personas/{persona_id}")
    p = r.json()
    print_data("name", p.get("name"))
    print_data("lifeStage", p.get("lifeStage"))
    print_data("lifeStageDetail", p.get("lifeStageDetail"))
    print_data("currentLocation", p.get("currentLocation"))
    print_data("characterInitialWorldTime", p.get("characterInitialWorldTime"))

    # =====================================================
    # 步骤 2：生成事件线（学生身份）
    # =====================================================
    print_step(2, "生成事件线 —— 预期：上课、论文、毕业季等学生特征")

    print_info("调 LLM 生成中...")
    start = time.time()
    r = api_post("/api/sim/generate-events", {"personaId": persona_id})
    elapsed = time.time() - start
    resp = r.json()

    if resp.get("ok"):
        print_ok(f"生成成功，{resp.get('count', 0)} 条（{elapsed:.1f}s）")
        print_event_table(resp.get("events", []), "🎓 大四学生事件线")
    else:
        print_fail(f"失败: {resp.get('error', resp)}")

    # =====================================================
    # 步骤 3：LLM 自动切换 —— 注入毕业日 context → 调 new-day
    # =====================================================
    print_step(3, "LLM 自动切换 —— 注入'今天是毕业日'，观察 LLM 是否自动声明 life_stage_transition")

    print_info("先更新 context：告诉 LLM 今天是毕业日 + 明天入职...")
    grad_context = (
        "柳如烟是浙江大学计算机系大四学生。今天是2025年6月20日，毕业日。"
        "上午参加了毕业典礼，拿到了学位证书。下午和室友们拍了很多照片，晚上全班聚餐道别。"
        "明天她就要坐高铁去上海，后天正式入职某互联网公司做前端开发工程师。"
        "她已经租好了上海浦东的一个小单间。心情激动又不舍，对未来充满期待。"
    )
    api_put(f"/api/personas/{persona_id}", {
        "characterCurrentContext": grad_context
    })
    print_ok("context 已更新为毕业日场景")

    print_info("调 new-day（LLM 生成事件 + 反思 + 可能声明切换）...")
    start = time.time()
    r = api_post("/api/sim/new-day", {"personaId": persona_id})
    elapsed = time.time() - start
    resp = r.json()

    if resp.get("ok"):
        cleared = resp.get("cleared", 0)
        count = resp.get("new_count", 0)
        print_ok(f"new-day 完成：清除 {cleared} 条，生成 {count} 条（{elapsed:.1f}s）")
        print_event_table(resp.get("events", []), "🎉 毕业日事件线")
    else:
        print_fail(f"new-day 失败: {resp.get('error', resp)}")

    # 检查 LLM 是否自动切换了 life_stage
    print_info("检查 LLM 是否自动更新了 persona 的 life_stage...")
    r = api_get(f"/api/personas/{persona_id}")
    p = r.json()
    current_ls = p.get("lifeStage")
    current_lsd = p.get("lifeStageDetail")
    current_loc = p.get("currentLocation")
    current_ctx = p.get("characterCurrentContext", "")

    print_data("lifeStage", current_ls)
    print_data("lifeStageDetail", current_lsd)
    print_data("currentLocation", current_loc)

    # 判断切换结果
    auto_transitioned = (current_ls != "student")
    if auto_transitioned:
        print_ok(f"🎯 LLM 自动切换成功！lifeStage: student → {current_ls}")
        if "上海" in str(current_loc):
            print_ok(f"🎯 地点已自动更新为上海: {current_loc}")
    else:
        print_warn("LLM 未自动切换 life_stage（可能需要在反思中包含 life_stage_transition）")
        print_info("检查 context 中是否被追加了切换信息：")
        if "【当前身份】" in current_ctx or "【切换原因】" in current_ctx:
            print_ok("context 包含追加信息（processLifeStageTransition 已执行），但 lifeStage 字段可能未更新")
        else:
            print_info("context 无追加信息，说明 LLM 没有声明 should_transition=true")

    # =====================================================
    # 步骤 4：次日 new-day —— 验证职场事件线
    # =====================================================
    if auto_transitioned:
        print_step(4, f"次日 new-day —— 预期：通勤、开会、加班等职场特征（当前身份: {current_lsd}）")
    else:
        print_step(4, "次日 new-day —— 手动切换后用职场 context 生成")

    # 如果 LLM 没自动切换，手动设置职场身份
    if not auto_transitioned:
        print_info("手动切换到职场身份...")
        api_put(f"/api/personas/{persona_id}", {
            "lifeStage": "working",
            "lifeStageDetail": "初级前端开发工程师",
            "currentLocation": "上海",
            "characterCurrentContext": "柳如烟刚从浙江大学毕业，已经在上海浦东的一家科技公司入职做初级前端开发工程师。她租了一个小单间，每天坐地铁通勤。今天是入职第二天，还在熟悉代码库和开发流程。"
        })
        r = api_get(f"/api/personas/{persona_id}")
        p = r.json()
        print_data("lifeStage", p.get("lifeStage"))
        print_data("lifeStageDetail", p.get("lifeStageDetail"))
        print_data("currentLocation", p.get("currentLocation"))

    print_info("调 new-day 生成次日事件...")
    start = time.time()
    r = api_post("/api/sim/new-day", {"personaId": persona_id})
    elapsed = time.time() - start
    resp = r.json()

    if resp.get("ok"):
        cleared = resp.get("cleared", 0)
        count = resp.get("new_count", 0)
        print_ok(f"清除旧 {cleared} 条，生成新 {count} 条（{elapsed:.1f}s）")
        print_event_table(resp.get("events", []), "💼 职场身份事件线")
    else:
        print_fail(f"失败: {resp.get('error', resp)}")

    # =====================================================
    # 步骤 5：图片生成
    # =====================================================
    print_step(5, "图片生成 —— 含角色 + 无角色 + 验证风格注入")

    image_tests = [
        ("柳如烟在图书馆看书", True),
        ("一只橘猫和一只柴犬在公园草地上追逐嬉戏，阳光明媚", False),
    ]

    for i, (prompt, has_character) in enumerate(image_tests, 1):
        label = "含角色" if has_character else "无角色"
        print_sub(f"图片 {i}（{label}）: {prompt[:40]}...")
        start = time.time()
        r = api_post("/api/sim/test-image", {"personaId": persona_id, "prompt": prompt})
        elapsed = time.time() - start
        resp = r.json()
        if resp.get("ok"):
            path = resp.get("local_path", "")
            print_ok(f"成功（{elapsed:.1f}s）→ {path}")
            print_info("请打开查看：风格是否为二次元动漫风")
        else:
            print_fail(f"失败: {resp.get('error', '')[:100]}")

    # =====================================================
    # 步骤 6：记忆系统
    # =====================================================
    print_step(6, "记忆系统验证")

    # 6a
    print_sub("a) 触发 LLM 决策 → 内心独白 + 可能写入记忆")
    print_info("调 LLM...")
    r = api_post("/api/sim/force-trigger-event", {
        "personaId": persona_id,
        "eventType": "moment",
        "description": "下班路上遇到一只可爱的橘猫"
    })
    resp = r.json()
    if resp.get("ok"):
        thought = resp.get("inner_thought", "")
        if isinstance(thought, dict):
            thought = thought.get("raw_thought", str(thought))
        print_ok("LLM 决策成功")
        print_data("内心独白", thought)
        print_data("态度", resp.get("attitude", "?"))
        print_data("是否联系用户", resp.get("will_contact_user", "?"))
        should_rem = resp.get("should_remember", False)
        memorable = resp.get("memorable_point", "")
        if should_rem and memorable:
            print_ok(f"记忆已写入 ChromaDB: {memorable[:60]}...")
        elif should_rem:
            print_info("should_remember=true 但 memorable_point 为空（未写入）")
        else:
            print_info("should_remember=false（未写入记忆）")
    else:
        print_fail(str(resp.get("error", resp))[:150])

    # 6b
    print_sub("b) new-day → 每日反思写入 ChromaDB")
    print_info("调 LLM...")
    start = time.time()
    r = api_post("/api/sim/new-day", {"personaId": persona_id})
    elapsed = time.time() - start
    resp = r.json()
    if resp.get("ok"):
        print_ok(f"生成 {resp.get('new_count', 0)} 条（{elapsed:.1f}s）")
        print_info("今日反思的 key_memories 已由 Python 端写入 ChromaDB")
    else:
        print_fail(str(resp.get("error", resp))[:150])

    # 6c
    print_sub("c) 再次触发 LLM → 检索之前记忆")
    print_info("调 LLM（预期回忆到之前的橘猫）...")
    r = api_post("/api/sim/force-trigger-event", {
        "personaId": persona_id,
        "eventType": "routine",
        "description": "午饭时间，坐在公司食堂吃饭"
    })
    resp = r.json()
    if resp.get("ok"):
        thought = resp.get("inner_thought", "")
        if isinstance(thought, dict):
            thought = thought.get("raw_thought", str(thought))
        print_ok("第二次 LLM 决策成功")
        print_data("内心独白", thought)

        if any(kw in str(thought).lower() for kw in ['猫', '橘', '小猫', '动物']):
            print_ok("🎯 记忆有效！LLM 记得之前遇到的橘猫")
        else:
            print_info("→ 未发现对之前事件的回忆，请检查 ChromaDB 是否正常工作")
    else:
        print_fail(str(resp.get("error", resp))[:150])

    # =====================================================
    # 步骤 7：清理
    # =====================================================
    print(f"\n{SEP}")
    print(f"  [步骤 7] 清理测试数据")
    print(f"{SEP}")
    print_info(f"Persona: {persona_id}")

    user_input = input("\n  >>> 按 Enter 清理，输入其他跳过: ").strip()
    if user_input == "":
        r = api_post("/api/sim/cleanup", {"personaIds": [persona_id]})
        resp = r.json()
        if resp.get("ok"):
            print_ok("已清理！")
            for k, v in resp.items():
                if k != "ok":
                    print(f"  {k}: {v}")
        else:
            print_fail(str(resp))
    else:
        print_info("数据已保留")

    print(f"\n{'#' * 70}")
    print(f"  测试完成")
    print(f"{'#' * 70}")


def print_sub(title):
    print(f"\n  {SUBSEP}")
    print(f"  [步骤 {title}]")
    print(f"  {SUBSEP}")


if __name__ == "__main__":
    main()