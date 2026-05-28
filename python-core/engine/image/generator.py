import os
import re
import aiohttp
import aiofiles
from datetime import datetime
from abc import ABC, abstractmethod


class GenerateResult:
    def __init__(self, success: bool, local_path: str = "",
                 width: int = 0, height: int = 0, error: str = ""):
        self.success = success
        self.local_path = local_path
        self.width = width
        self.height = height
        self.error = error


class ImageGenerator(ABC):
    """图片生成器抽象基类
    子类必须在 __init__ 中设置 self.base_url, self.api_key, self.model"""

    def __init__(self):
        self.base_url = ""
        self.api_key = ""
        self.model = ""

    @abstractmethod
    async def generate(self, prompt: str, persona_id: str = "",
                       context: str = "reply") -> GenerateResult:
        pass

    @abstractmethod
    def get_endpoint(self) -> str:
        """返回 API 端点，如 '/images/generations'"""
        pass

    def build_request_body(self, prompt: str) -> dict:
        """构建请求体。子类可重写以添加 provider 特有参数"""
        return {"model": self.model, "prompt": prompt}

    def build_headers(self) -> dict:
        """构建 HTTP 请求头。子类可重写以修改认证方式"""
        return {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {self.api_key}",
        }

    def parse_response(self, data: dict) -> str:
        """从 API 响应中提取图片 URL。子类可重写"""
        entries = data.get("data", [])
        if not entries:
            raise Exception("API 返回空 data 数组")
        first = entries[0]
        if "error" in first:
            raise Exception(f"图片生成失败: {first['error']}")
        return first.get("url", "")

    def _ensure_dir(self, persona_id: str) -> str:
        base = os.path.dirname(os.path.dirname(
            os.path.dirname(os.path.abspath(__file__))))
        img_dir = os.environ.get(
            "IMAGE_OUTPUT_DIR",
            os.path.normpath(os.path.join(
                base, "..", "java-manager", "data", "generated_images"))
        )
        os.makedirs(img_dir, exist_ok=True)
        persona_dir = os.path.join(img_dir, persona_id) if persona_id else img_dir
        os.makedirs(persona_dir, exist_ok=True)
        return persona_dir

    def _make_filename(self, prompt: str, context: str) -> str:
        ts = datetime.now().strftime("%Y%m%d_%H%M%S")
        slug = re.sub(r'[\\/*?:"<>|]', '', prompt)[:30]
        return f"{ts}_{context}_{slug}.png"


# ===== 具体实现 =====

class OpenAIImageGenerator(ImageGenerator):
    """OpenAI DALL-E / 任何标准 OpenAI 兼容图片 API"""

    def __init__(self, base_url: str, api_key: str, model: str = "dall-e-3"):
        self.base_url = base_url.rstrip("/")
        self.api_key = api_key
        self.model = model

    def get_endpoint(self) -> str:
        return "/images/generations"

    async def generate(self, prompt: str, persona_id: str = "",
                       context: str = "reply") -> GenerateResult:
        directory = self._ensure_dir(persona_id)
        filename = self._make_filename(prompt, context)
        filepath = os.path.join(directory, filename)

        try:
            image_url = await self._request_generation(prompt)
            if not image_url:
                return GenerateResult(False, error="API 返回空图片 URL")

            await self._download_image(image_url, filepath)

            return GenerateResult(True, local_path=os.path.abspath(filepath))
        except Exception as e:
            return GenerateResult(False, error=str(e))

    async def _request_generation(self, prompt: str) -> str:
        url = f"{self.base_url}{self.get_endpoint()}"
        body = self.build_request_body(prompt)
        headers = self.build_headers()

        async with aiohttp.ClientSession() as session:
            async with session.post(url, json=body, headers=headers,
                                    timeout=aiohttp.ClientTimeout(total=60)) as resp:
                data = await resp.json()
                if resp.status != 200:
                    err = data.get("error", {}).get("message", str(data))
                    raise Exception(f"图片生成 API HTTP {resp.status}: {err}")

                return self.parse_response(data)

    async def _download_image(self, image_url: str, filepath: str):
        async with aiohttp.ClientSession() as session:
            async with session.get(image_url,
                                   timeout=aiohttp.ClientTimeout(total=120)) as resp:
                if resp.status != 200:
                    raise Exception(f"下载图片失败 HTTP {resp.status}")
                async with aiofiles.open(filepath, "wb") as f:
                    await f.write(await resp.read())


class VolcengineImageGenerator(OpenAIImageGenerator):
    """火山引擎 Seedream —— OpenAI 兼容但有额外参数"""

    def build_request_body(self, prompt: str) -> dict:
        return {
            "model": self.model,
            "prompt": prompt,
            "size": "2K",
            "response_format": "url",
            "watermark": False,
        }


# ===== Provider 注册表 =====

PROVIDER_REGISTRY = {
    "openai_image":    OpenAIImageGenerator,
    "custom_image":    OpenAIImageGenerator,
    "volcengine_image": VolcengineImageGenerator,
}


def create_image_generator(image_config: dict) -> ImageGenerator:
    """工厂方法：按 provider 字段选择生成器类"""
    provider = image_config.get("provider", "openai_image")
    cls = PROVIDER_REGISTRY.get(provider)
    if cls is None:
        raise ValueError(f"不支持的图片 provider: {provider}")
    return cls(
        base_url=image_config.get("base_url") or "https://api.openai.com/v1",
        api_key=image_config.get("api_key", ""),
        model=image_config.get("model") or "dall-e-3",
    )
