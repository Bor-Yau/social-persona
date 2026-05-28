"""
LlamaIndex Provider — 为 Mem0 提供 LLM 后端

Day 6 Mem0 集成时需要这个适配器，Day 5 先写好避免后续编译阻断。
"""
from typing import Optional, List, Dict, Any
from openai import AsyncOpenAI


class LlamaIndexProvider:
    """
    适配 mem0 所需的 LLM 接口

    mem0 要求传入一个符合 LlamaIndex LLM 接口的对象。
    这个适配器把 AsyncOpenAI 包装成 Mem0 期望的接口。
    """

    def __init__(self, client: AsyncOpenAI, model: str):
        self.client = client
        self.model = model
        self._metadata = None
    
    @property
    def metadata(self):
        if self._metadata is None:
            self._metadata = type('Meta', (), {
                'model_name': self.model,
                'is_chat_model': True,
                'context_window': 131072,
                'is_function_calling_model': True,
            })()
        return self._metadata
    
    async def acomplete(self, prompt: str, **kwargs) -> Any:
        """非聊天式补全（mem0 某些场景调用）"""
        response = await self.client.chat.completions.create(
            model=self.model,
            messages=[{"role": "user", "content": prompt}],
            **kwargs
        )
        return response.choices[0].message.content
    
    async def achat(self, messages: List[Dict[str, str]], **kwargs) -> Any:
        """聊天式调用"""
        response = await self.client.chat.completions.create(
            model=self.model,
            messages=messages,
            **kwargs
        )
        return type('ChatResponse', (), {
            'message': type('Message', (), {
                'content': response.choices[0].message.content
            })()
        })()
