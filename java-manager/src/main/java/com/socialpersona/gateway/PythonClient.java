package com.socialpersona.gateway;

import com.socialpersona.gateway.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Python FastAPI 声明式 HTTP 客户端（Feign 接口桩）
 *
 * 设计依据：规格书 二、IPC契约 — 6个端点
 *
 * ★ 什么是 Feign 接口桩：
 *   你只定义接口 + 注解，不写任何 HTTP 连接代码。
 *   Spring Cloud OpenFeign 在运行时自动生成实现类：序列化 Java 对象→JSON→HTTP POST→
 *   反序列化 JSON→Java 对象。相当于你声明了"我要调这个 URL"，框架帮你执行。
 *
 * ★ 为什么叫"桩"：
 *   Day 1 Python 端点还不存在，但 Java 编译需要这个接口的方法签名存在。
 *   桩 = 只有签名没有网络流量的占位符，Day 5 Python 就绪后自动接通。
 *
 * ★ Day 5 升级计划：
 *   当前手动写的接口签名，Day 5 用 openapi-generator 插件从 openapi.json 自动生成覆盖。
 *
 * ★ name 和 url 的含义：
 *   name="python-core"   → Feign 客户端标识名（日志/监控用）
 *   url="${python.core.url}" → 从 application.yml 读取 python.core.url = http://127.0.0.1:8000
 */
@FeignClient(name = "python-core", url = "${python.core.url:http://127.0.0.1:8000}")
public interface PythonClient {

    /**
     * ★ 端点①：处理用户消息
     * 调用频率：用户每发一条 → 高频率
     *
     * @param request 包含 apiConfig、personaConfig、relationshipState、
     *        recentMemories、todayEventsSoFar、userMessage、timestamp
     * @return MessageResponse 三层判断模型（要不要回 → 回什么 → 对话结束了吗）
     */
    @PostMapping("/api/message")
    MessageResponse handleMessage(@RequestBody MessageRequest request);

    /**
     * ★ 端点②：事件触发处理
     * 调用频率：每天 5~15 次（routine/moment 事件触发时）
     *
     * 与 handleMessage 的区别：
     *   返回 EventTriggerResponse 含 shouldContactUser（替代 shouldReply）
     *   因为事件触发时 AI 是主动方，不是被动回复方
     *
     * @param request 包含 apiConfig、personaConfig、relationshipState、
     *        以及 currentEvent（当前事件对象）、nextEventType、nextEventTime
     * @return EventTriggerResponse 含 shouldContactUser + 间隔预判
     */
    @PostMapping("/api/event/trigger")
    EventTriggerResponse triggerEvent(@RequestBody EventTriggerRequest request);

    /**
     * ★ 端点③：事件线生成（含今日反思）
     * 调用频率：每天 1 次（凌晨 / 首次启动时）
     *
     * @param request 包含 apiConfig、personaConfig、relationshipState、
     *        todayDate、dayOfWeek、yesterdayEvents、todayInnerThoughts
     * @return EventGenerateResponse 含 todayReflection（今日反思）+
     *         events（明天事件线，必须含至少1个 sleep）
     */
    @PostMapping("/api/event/generate")
    EventGenerateResponse generateEvents(@RequestBody EventGenerateRequest request);

    /**
     * ★ 端点④：牵线人访谈
     * 调用频率：创建新 AI 网友时，多轮对话（7 阶段）
     *
     * @param request 包含 apiConfig、sessionId、currentStage、
     *        history、userMessage、collectedData
     * @return MatchmakerResponse 含 reply、nextStage、extractedData、
     *         isComplete=true 时含完整 personaConfig + sampleChats
     */
    @PostMapping("/api/matchmaker")
    MatchmakerResponse matchmakerChat(@RequestBody MatchmakerRequest request);

    /**
     * ★ 图片生成
     * 由 dispatchReply 处理 type=image item 时调用
     */
    @PostMapping("/api/image/generate")
    ImageGenerateResponse generateImage(@RequestBody ImageGenerateRequest request);

    /**
     * ★ 端点⑥：健康检查
     * 调用频率：Java 启动时 + 定期检测
     *
     * 为什么用 GET 而非 POST：健康检查不传数据，是纯查询操作，
     *   RESTful 规范中 GET 用于查询，POST 用于改变状态
     *
     * @return HealthResponse 含 status("ok"/"degraded")、llmConnected、
     *         memoryConnected、uptimeSeconds
     */
    @GetMapping("/api/health")
    HealthResponse health();
}
