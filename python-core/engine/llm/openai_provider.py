"""
OpenAI 兼容 LLM Provider

支持 OpenAI / DeepSeek / 任何 OpenAI 兼容 API。
换 base_url 即换提供商——同一个类，不同配置。
"""
import json
from typing import Dict, Any, Optional
from openai import AsyncOpenAI
from .provider import LLMProvider


class OpenAICompatibleProvider(LLMProvider):
    """
    OpenAI 兼容 Provider 实现

    用法：
      provider = OpenAICompatibleProvider(
          base_url="https://api.deepseek.com/v1",
          api_key="sk-xxx",
          model="deepseek-chat"
      )
      result = await provider.chat(system_prompt, user_prompt)
    """

    def __init__(self, base_url: str, api_key: str, model: str):
        self.client = AsyncOpenAI(base_url=base_url, api_key=api_key)
        self.model = model

    async def chat(
        self,
        system_prompt: str,
        user_prompt: str,
        response_format: Optional[Dict[str, Any]] = None,
    ) -> Dict[str, Any]:
        """
        发送聊天请求，返回结构化 JSON

        ★ response_format={"type":"json_object"}：
          OpenAI 兼容的 JSON Mode，确保 LLM 返回合法 JSON。
          DeepSeek 也支持这个参数。

        ★ 为什么 system + user 分两个 message：
          让 LLM 清楚区分"你是谁"（system）和"现在发生了什么"（user）。
          混在一个 message 里会降低角色一致性。
        """
        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ]

        kwargs = {"model": self.model, "messages": messages}
        if response_format:
            kwargs["response_format"] = response_format

        response = await self.client.chat.completions.create(**kwargs)
        content = response.choices[0].message.content

        # ★ LLM 返回的是 JSON 字符串 → 解析为 Python 字典
        # 为什么要 try-except：LLM 偶尔会输出非标准 JSON（如 Markdown 包裹）
        try:
            return json.loads(content)
        except json.JSONDecodeError:
            # 兜底：尝试从 Markdown 代码块中提取
            if "```json" in content:
                start = content.index("```json") + 7
                end = content.index("```", start)
                return json.loads(content[start:end])
            raise
