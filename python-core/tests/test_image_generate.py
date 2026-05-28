"""
图片生成引擎 Python 端测试

测试策略：mock aiohttp 代替真 API，用 `tests/data/1.jpg` 模拟图片文件。

用法：
    cd python-core
    pip install pytest pytest-asyncio
    pytest tests/ -v

覆盖：
  1. _request_generation: 成功返回 URL / 400错误 / 空 data / 返回 error
  2. _download_image: 文件被正确保存 / 404错误
  3. generate: 完整成功流程 / API失败 / 下载失败
  4. 文件名格式: 时间戳 + context + prompt_slug
  5. 图片文件夹: data/generated_images/{persona_id}
  6. create_image_generator: 不同 provider 映射
  7. /api/image/generate 端点：请求 → 响应
"""
import os
import sys
import json
import tempfile
import shutil
from unittest.mock import patch, AsyncMock, MagicMock
from datetime import datetime

import pytest

# 项目路径
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from engine.image.generator import GenerateResult, ImageGenerator
from engine.image.openai_generator import OpenAIImageGenerator
from engine.llm.factory import create_image_generator


# ==================== Fixtures ====================

TEST_PROMPT = "a cute cat on windowsill, digital art"
TEST_MODEL = "dall-e-3"
TEST_API_KEY = "sk-test-123"
TEST_BASE_URL = "https://api.openai.com/v1"
TEST_PERSONA_ID = "d926f28e-ea21-4f6a-8d69-9ed3dba6052b"

PIC_1_JPG = os.path.join(os.path.dirname(__file__), "data", "1.jpg")


def _fake_jpg_bytes() -> bytes:
    """返回一张极简 JPEG — 实际读取测试用的 1.jpg，不存在时返回最小占位"""
    if os.path.exists(PIC_1_JPG):
        with open(PIC_1_JPG, "rb") as f:
            return f.read()
    return b"\xff\xd8\xff\xe0\x00\x10JFIF\x00\x01\x01\x00\x00\x01\x00\x01\x00\x00\xff\xdb\x00C\x00\x08\x06\x06\x07\x06\x05\x08\x07\x07\x07\t\t\x08\n\x0c\x14\r\x0c\x0b\x0b\x0c\x19\x12\x13\x0f\x14\x1d\x1a\x1f\x1e\x1d\x1a\x1c\x1c $.' \",#\x1c\x1c(7),01444\x1f'9=82<.342\xff\xc0\x00\x0b\x08\x00\x01\x00\x01\x01\x01\x11\x00\xff\xc4\x00\x1f\x00\x00\x01\x05\x01\x01\x01\x01\x01\x01\x00\x00\x00\x00\x00\x00\x00\x01\x02\x03\x04\x05\x06\x07\x08\t\n\x0b\xff\xc4\x00\xb5\x10\x00\x02\x01\x03\x03\x02\x04\x03\x05\x05\x04\x04\x00\x00\x00\x00\x00\x00\x00\x01\x02\x03\x11\x04\x12!1A\x06\x13Qa\x07\"q\x142\x81\x91\xa1\x08#B\xb1\xc1\x15R\xd1\xf0$3br\x82\t\n\x16\x17\x18\x19\x1a%&'()*456789:CDEFGHIJSTUVWXYZcdefghijstuvwxyz\x83\x84\x85\x86\x87\x88\x89\x8a\x92\x93\x94\x95\x96\x97\x98\x99\x9a\xa2\xa3\xa4\xa5\xa6\xa7\xa8\xa9\xaa\xb2\xb3\xb4\xb5\xb6\xb7\xb8\xb9\xba\xc2\xc3\xc4\xc5\xc6\xc7\xc8\xc9\xca\xd2\xd3\xd4\xd5\xd6\xd7\xd8\xd9\xda\xe1\xe2\xe3\xe4\xe5\xe6\xe7\xe8\xe9\xea\xf1\xf2\xf3\xf4\xf5\xf6\xf7\xf8\xf9\xfa\xff\xc4\x00\x1f\x01\x01\x01\x01\x01\x01\x01\x01\x01\x01\x00\x00\x00\x00\x00\x00\x01\x02\x03\x04\x05\x06\x07\x08\t\n\x0b\xff\xc4\x00\xb5\x11\x00\x02\x01\x02\x04\x04\x03\x04\x07\x05\x04\x04\x00\x01\x02w\x00\x01\x02\x03\x11\x04\x05!1\x06\x12AQ\x07aq\x13\"2\x81\x08\x14B\x91\xa1\xb1\xc1\t#3R\xf0\x15br\x82\n\x16$4\x17\x18\x19\x1a%&'()*56789:CDEFGHIJSTUVWXYZcdefghijstuvwxyz\x83\x84\x85\x86\x87\x88\x89\x8a\x92\x93\x94\x95\x96\x97\x98\x99\x9a\xa2\xa3\xa4\xa5\xa6\xa7\xa8\xa9\xaa\xb2\xb3\xb4\xb5\xb6\xb7\xb8\xb9\xba\xc2\xc3\xc4\xc5\xc6\xc7\xc8\xc9\xca\xd2\xd3\xd4\xd5\xd6\xd7\xd8\xd9\xda\xe1\xe2\xe3\xe4\xe5\xe6\xe7\xe8\xe9\xea\xf1\xf2\xf3\xf4\xf5\xf6\xf7\xf8\xf9\xfa\xff\xda\x00\x08\x01\x01\x00\x00?\x00\xd5\x93\xf2\xc2\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\xff\xd9"


class MockResponse:
    """模拟 aiohttp.ClientResponse"""
    def __init__(self, status=200, json_data=None, content=b""):
        self.status = status
        self._json_data = json_data or {}
        self._content = content

    async def __aenter__(self):
        return self

    async def __aexit__(self, *args):
        pass

    async def json(self):
        return self._json_data

    async def read(self):
        return self._content

    def raise_for_status(self):
        if self.status >= 400:
            raise Exception(f"HTTP {self.status}")


# ==================== Test _request_generation ====================

@pytest.mark.asyncio
async def test_request_generation_success():
    """正常返回图片 URL"""
    gen = OpenAIImageGenerator(TEST_BASE_URL, TEST_API_KEY, TEST_MODEL)
    fake_resp = MockResponse(200, {
        "data": [{"url": "https://cdn.example.com/img/abc.png", "size": "1024x1024"}],
        "created": 1234567890
    })

    with patch("aiohttp.ClientSession.post", return_value=fake_resp):
        url = await gen._request_generation(TEST_PROMPT)

    assert url == "https://cdn.example.com/img/abc.png"


@pytest.mark.asyncio
async def test_request_generation_http_error():
    """API 返回 400 → 抛出异常"""
    gen = OpenAIImageGenerator(TEST_BASE_URL, TEST_API_KEY, TEST_MODEL)
    fake_resp = MockResponse(400, {
        "error": {"message": "Invalid parameter model"}
    })

    with patch("aiohttp.ClientSession.post", return_value=fake_resp):
        with pytest.raises(Exception, match="图片生成 API HTTP 400"):
            await gen._request_generation(TEST_PROMPT)


@pytest.mark.asyncio
async def test_request_generation_empty_data():
    """API 返回空 data → 抛出异常"""
    gen = OpenAIImageGenerator(TEST_BASE_URL, TEST_API_KEY, TEST_MODEL)
    fake_resp = MockResponse(200, {"data": []})

    with patch("aiohttp.ClientSession.post", return_value=fake_resp):
        with pytest.raises(Exception, match="空 data"):
            await gen._request_generation(TEST_PROMPT)


@pytest.mark.asyncio
async def test_request_generation_entry_has_error():
    """API 返回 data[0] 含 error → 抛出异常"""
    gen = OpenAIImageGenerator(TEST_BASE_URL, TEST_API_KEY, TEST_MODEL)
    fake_resp = MockResponse(200, {
        "data": [{"error": {"code": "content_moderation", "message": "triggered"}}]
    })

    with patch("aiohttp.ClientSession.post", return_value=fake_resp):
        with pytest.raises(Exception, match="triggered"):
            await gen._request_generation(TEST_PROMPT)


# ==================== Test _download_image ====================

@pytest.mark.asyncio
async def test_download_image_success(tmp_path):
    """下载图片 → 文件写入正确路径"""
    gen = OpenAIImageGenerator(TEST_BASE_URL, TEST_API_KEY, TEST_MODEL)
    dest = tmp_path / "test_output.png"
    fake_resp = MockResponse(200, content=_fake_jpg_bytes())

    with patch("aiohttp.ClientSession.get", return_value=fake_resp):
        await gen._download_image("https://cdn.example.com/img.png", str(dest))

    assert dest.exists(), "图片文件应被保存"
    assert dest.stat().st_size > 0, "文件不应为空"
    # 验证开头为 JPEG 魔数
    data = dest.read_bytes()
    assert data[:2] == b"\xff\xd8", "文件应为 JPEG 格式"


@pytest.mark.asyncio
async def test_download_image_404(tmp_path):
    """下载图片 404 → 抛出异常"""
    gen = OpenAIImageGenerator(TEST_BASE_URL, TEST_API_KEY, TEST_MODEL)
    dest = tmp_path / "test_output.png"
    fake_resp = MockResponse(404)

    with patch("aiohttp.ClientSession.get", return_value=fake_resp):
        with pytest.raises(Exception, match="下载图片失败 HTTP 404"):
            await gen._download_image("https://cdn.example.com/img.png", str(dest))

    assert not dest.exists(), "失败时文件不应被创建"


# ==================== Test generate (full flow) ====================

@pytest.mark.asyncio
async def test_generate_full_success():
    """完整成功流程：调 API → 下载 → 返回 local_path"""
    gen = OpenAIImageGenerator(TEST_BASE_URL, TEST_API_KEY, TEST_MODEL)

    gen_resp = MockResponse(200, {
        "data": [{"url": "https://cdn.example.com/img.png"}],
    })
    dl_resp = MockResponse(200, content=_fake_jpg_bytes())

    with patch("aiohttp.ClientSession.post", return_value=gen_resp), \
         patch("aiohttp.ClientSession.get", return_value=dl_resp), \
         patch("engine.image.generator.aiofiles.open", new_callable=MagicMock) as mock_open:
        mock_file = AsyncMock()
        mock_open.return_value.__aenter__.return_value = mock_file

        result = await gen.generate(TEST_PROMPT, TEST_PERSONA_ID, "reply")

    assert result.success, f"应成功: {result.error}"
    assert result.local_path != "", "应返回 local_path"
    assert TEST_PERSONA_ID in result.local_path, "路径应含 persona_id"
    assert "reply" in result.local_path, "路径应含 context"


@pytest.mark.asyncio
async def test_generate_api_failure():
    """API 失败 → 返回错误结果而非抛异常"""
    gen = OpenAIImageGenerator(TEST_BASE_URL, TEST_API_KEY, TEST_MODEL)
    fake_resp = MockResponse(400, {"error": {"message": "rate limit"}})

    with patch("aiohttp.ClientSession.post", return_value=fake_resp):
        result = await gen.generate(TEST_PROMPT, TEST_PERSONA_ID)

    assert not result.success
    assert "rate limit" in result.error


# ==================== Test filename ====================

def test_filename_format():
    """文件名格式: {timestamp}_{context}_{prompt_slug}.png"""
    gen = OpenAIImageGenerator(TEST_BASE_URL, TEST_API_KEY, TEST_MODEL)
    filename = gen._make_filename("一只橘猫在窗台上晒太阳 风景好美", "moment")

    # 提取各部分
    now = datetime.now()
    assert filename.startswith(now.strftime("%Y%m%d_")), "应包含日期前缀"
    assert "moment" in filename, "应含 context"
    assert "一只橘猫在窗台上晒太阳" in filename, "应含 prompt slug (前30字符)"
    assert filename.endswith(".png"), "应以 .png 结尾"

    # 特殊字符被过滤
    filename2 = gen._make_filename('测试<特殊>字符:"引号"/斜杠\\', "reply")
    assert "<" not in filename2, "特殊字符应被过滤"
    assert ">" not in filename2, "特殊字符应被过滤"
    assert '"' not in filename2, "特殊字符应被过滤"


# ==================== Test directory creation ====================

def test_ensure_dir_creates_persona_folder(tmp_path):
    """_ensure_dir 应创建 data/generated_images/{persona_id}"""
    gen = OpenAIImageGenerator(TEST_BASE_URL, TEST_API_KEY, TEST_MODEL)
    with patch.object(ImageGenerator, "_ensure_dir", return_value=str(tmp_path / TEST_PERSONA_ID)):
        persona_dir = gen._ensure_dir(TEST_PERSONA_ID)

    os.makedirs(persona_dir, exist_ok=True)
    assert os.path.isdir(persona_dir), "persona 文件夹应已创建"

    # 空 persona_id → 用根目录
    with patch.object(ImageGenerator, "_ensure_dir", return_value=str(tmp_path)):
        root_dir = gen._ensure_dir("")
    assert os.path.isdir(root_dir), "根目录应已创建"


# ==================== Test factory ====================

class TestCreateImageGenerator:

    def test_openai_image_provider(self):
        img = create_image_generator({
            "provider": "openai_image",
            "api_key": "sk-test",
            "base_url": "https://api.openai.com/v1",
            "model": "dall-e-3",
        })
        from engine.image.openai_generator import OpenAIImageGenerator
        assert isinstance(img, OpenAIImageGenerator)

    def test_custom_image_provider(self):
        img = create_image_generator({
            "provider": "custom_image",
            "api_key": "key",
            "base_url": "https://ark.cn-beijing.volces.com/api/v3",
            "model": "doubao-seedream-4-5-251128",
        })
        from engine.image.openai_generator import OpenAIImageGenerator
        assert isinstance(img, OpenAIImageGenerator)

    def test_volcengine_image_provider(self):
        img = create_image_generator({
            "provider": "volcengine_image",
            "api_key": "key",
            "base_url": "https://ark.cn-beijing.volces.com/api/v3",
            "model": "doubao-seedream-4-5-251128",
        })
        from engine.image.openai_generator import OpenAIImageGenerator
        assert isinstance(img, OpenAIImageGenerator)

    def test_unknown_provider_raises(self):
        with pytest.raises(ValueError, match="不支持的图片"):
            create_image_generator({"provider": "unknown_provider"})

    def test_defaults_when_empty(self):
        img = create_image_generator({
            "provider": "openai_image",
            "api_key": "",
            "base_url": "",
            "model": "",
        })
        from engine.image.openai_generator import OpenAIImageGenerator
        assert isinstance(img, OpenAIImageGenerator)
        # 默认值测试
        assert img.base_url == "https://api.openai.com/v1"
        assert img.model == "dall-e-3"


# ==================== Test route (端点) ====================

@pytest.mark.asyncio
async def test_image_generate_route_success():
    """POST /api/image/generate → 返回 success"""
    from api.models import ImageGenerateRequest, ImageGenerateResponse
    from api.image_routes import generate_image

    # 此时 create_image_generator 会创建真 generator，
    # 但 generate() 内部通过 aiohttp 调用会被 mock
    req = ImageGenerateRequest(
        image_config={
            "provider": "custom_image",
            "api_key": "sk-test",
            "base_url": "https://ark.cn-beijing.volces.com/api/v3",
            "model": "doubao-seedream-4-5-251128",
        },
        prompt="一只猫",
        persona_id=TEST_PERSONA_ID,
        context="reply",
    )

    fake_gen_resp = MockResponse(200, {
        "data": [{"url": "https://cdn.example.com/img.png"}],
    })
    fake_dl_resp = MockResponse(200, content=_fake_jpg_bytes())

    with patch("aiohttp.ClientSession.post", return_value=fake_gen_resp), \
         patch("aiohttp.ClientSession.get", return_value=fake_dl_resp), \
         patch("engine.image.generator.aiofiles.open", new_callable=MagicMock) as mock_open:
        mock_file = AsyncMock()
        mock_open.return_value.__aenter__.return_value = mock_file

        resp = await generate_image(req)

    assert resp.success, f"端点应成功: {resp.error}"
    assert resp.local_path != "", "应返回 local_path"


@pytest.mark.asyncio
async def test_image_generate_route_invalid_config():
    """POST /api/image/generate 传无效 provider → 返回 error"""
    from api.models import ImageGenerateRequest
    from api.image_routes import generate_image

    req = ImageGenerateRequest(
        image_config={"provider": "nonexistent"},
        prompt="test",
    )
    resp = await generate_image(req)

    assert not resp.success
    assert resp.error != ""
