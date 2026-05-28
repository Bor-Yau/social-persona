package com.socialpersona.relationship.state;

/**
 * AI 状态枚举 —— 状态机的状态集合
 *
 * ★ 为什么是枚举而非字符串：
 *   状态值是有限的（4 种），枚举提供类型安全——不会写出 state="SLEPING" 这样的拼写错误。
 *
 * ★ 三种状态的业务语义：
 *   ACTIVE          — 正常在线，接收消息，响应事件
 *   SLEEPING        — 真实入眠，不响应消息和事件，等待起床事件唤醒
 *   ARCHIVED        — 被用户拉黑，心跳停止，数据完整保留（可发起重新联系申请）
 *   REQUEST_PENDING — 拉黑后发起重新联系，等待 AI 的 LLM 判断是否同意
 */
public enum AIStatus {
    ACTIVE,
    SLEEPING,
    ARCHIVED,
    REQUEST_PENDING
}
