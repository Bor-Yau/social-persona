"""OpenAI 兼容图片生成器 —— 重新导出，保持向后兼容"""
from .generator import (
    OpenAIImageGenerator,
    VolcengineImageGenerator,
    create_image_generator,
    PROVIDER_REGISTRY,
    ImageGenerator,
    GenerateResult,
)
