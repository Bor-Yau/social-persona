"""
LLM Provider 抽象基类 — 所有 LLM 提供商的统一接口

为什么需要抽象层：
  用户可能用 DeepSeek / OpenAI / Anthropic，但 Python 端不关心具体是谁。
  抽象层让端点代码与具体提供商解耦，换提供商只需改一行配置。
"""
from abc import ABC, abstractmethod
from typing import Dict, Any, Optional


class LLMProvider(ABC):
    """LLM Provider 抽象基类"""

    @abstractmethod
    async def chat(
        self,
        system_prompt: str,
        user_prompt: str,
        response_format: Optional[Dict[str, Any]] = None,
    ) -> Dict[str, Any]:
        """
        发送聊天请求，返回结构化 JSON

        Args:
            system_prompt: 系统提示词（人设 + 规则）
            user_prompt:   用户提示词（当前对话上下文）
            response_format: 结构化输出约束（如 {"type":"json_object"}）

        Returns:
            LLM 返回的 JSON 字典
        """
        pass
