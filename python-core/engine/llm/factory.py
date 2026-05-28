"""
LLM Provider 工厂

职责：根据请求中的 api_config 创建对应的 LLMProvider 实例。

为什么用工厂而非每个端点自己写 if/else：
  6 个端点都需要创建 Provider，统一工厂避免重复代码。
  未来加新 Provider（如 Claude 原生 SDK）也只需改工厂。
"""
from .provider import LLMProvider
from .openai_provider import OpenAICompatibleProvider


def create_provider(api_config: dict) -> LLMProvider:
    """
    根据 API 配置创建 LLM Provider

    Args:
        api_config: Java 传来的 api_config 对象
            {
                "provider": "deepseek" | "openai" | "anthropic",
                "api_key": "sk-xxx",       ← Java 已解密，明文
                "base_url": "https://api.deepseek.com/v1",
                "model": "deepseek-chat"
            }

    Returns:
        LLMProvider 实例

    ★ 为什么返回抽象类型 LLMProvider：
      调用方不关心具体是 DeepSeek 还是 OpenAI，只调用 provider.chat()。
    """
    provider_name = api_config.get("provider", "deepseek").lower()
    api_key = api_config.get("api_key", "")
    base_url = api_config.get("base_url", "https://api.deepseek.com/v1")
    model = api_config.get("model", "deepseek-chat")

    if provider_name == "openai":
        return OpenAICompatibleProvider(
            base_url=base_url or "https://api.openai.com/v1",
            api_key=api_key,
            model=model or "gpt-4o",
        )

    if provider_name == "deepseek":
        return OpenAICompatibleProvider(
            base_url=base_url or "https://api.deepseek.com/v1",
            api_key=api_key,
            model=model or "deepseek-chat",
        )

    # anthropic 也走 OpenAI 兼容接口（Anthropic 已支持）
    if provider_name == "anthropic":
        return OpenAICompatibleProvider(
            base_url=base_url or "https://api.anthropic.com/v1",
            api_key=api_key,
            model=model or "claude-3.5-sonnet",
        )

    raise ValueError(f"不支持的 LLM Provider: {provider_name}")


def create_image_generator(image_config: dict):
    from engine.image.generator import create_image_generator as _create
    return _create(image_config)
