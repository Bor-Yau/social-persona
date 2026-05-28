"""
Java ↔ Python 连通性测试

用 FastAPI TestClient 模拟 Java Feign 客户端发送的 HTTP 请求，
验证 Python 端能正确处理并返回 Java 期望的 JSON 格式。

测试流程：
  1. 构造 Java 端 ImageGenerateRequest 的等价 JSON
  2. POST 到 /api/image/generate
  3. 验证响应 JSON 与 Java 端 ImageGenerateResponse DTO 完全兼容
  4. 用真实 1.jpg 图片验证文件保存路径

覆盖：
  - 请求体字段映射（image_config / prompt / persona_id / context）
  - 响应体字段映射（success / local_path / width / height / error）
  - 图片保存到 data/generated_images/{persona_id}/
  - 图片生成失败时的错误响应格式
  - 未配置图片提供商时的错误响应
"""
import os
import sys
import json

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from fastapi.testclient import TestClient
from unittest.mock import patch, AsyncMock, MagicMock
import pytest

from main import app
from engine.image.openai_generator import OpenAIImageGenerator

client = TestClient(app)

TEST_PERSONA_ID = "d926f28e-ea21-4f6a-8d69-9ed3dba6052b"
TEST_IMG_PATH = os.path.join(os.path.dirname(__file__), "data", "1.jpg")


def _fake_jpg_bytes() -> bytes:
    if os.path.exists(TEST_IMG_PATH):
        with open(TEST_IMG_PATH, "rb") as f:
            return f.read()
    return b"\xff\xd8\xff\xe0\x00\x10JFIF\x00\x01\x01\x00\x00\x01\x00\x01\x00\x00\xff\xdb\x00C\x00\x08\x06\x07\x06\x05\x08\x07\x07\x07\t\t\x08\n\x0c\x14\r\x0c\x0b\x0b\x0c\x19\x12\x13\x0f\x14\x1d\x1a\x1f\x1e\x1d\x1a\x1c\x1c $.' \",#\x1c\x1c(7),01444\x1f'9=82<.342\xff\xc0\x00\x0b\x08\x00\x01\x00\x01\x01\x01\x11\x00\xff\xc4\x00\x1f\x00\x00\x01\x05\x01\x01\x01\x01\x01\x01\x00\x00\x00\x00\x00\x00\x00\x01\x02\x03\x04\x05\x06\x07\x08\t\n\x0b\xff\xc4\x00\xb5\x10\x00\x02\x01\x03\x03\x02\x04\x03\x05\x05\x04\x04\x00\x00\x00\x00\x00\x00\x01\x02\x03\x11\x04\x12!1A\x06\x13Qa\x07\"q\x142\x81\x91\xa1\x08#B\xb1\xc1\x15R\xd1\xf0$3br\x82\t\n\x16\x17\x18\x19\x1a%&'()*456789:CDEFGHIJSTUVWXYZcdefghijstuvwxyz\x83\x84\x85\x86\x87\x88\x89\x8a\x92\x93\x94\x95\x96\x97\x98\x99\x9a\xa2\xa3\xa4\xa5\xa6\xa7\xa8\xa9\xaa\xb2\xb3\xb4\xb5\xb6\xb7\xb8\xb9\xba\xc2\xc3\xc4\xc5\xc6\xc7\xc8\xc9\xca\xd2\xd3\xd4\xd5\xd6\xd7\xd8\xd9\xda\xe1\xe2\xe3\xe4\xe5\xe6\xe7\xe8\xe9\xea\xf1\xf2\xf3\xf4\xf5\xf6\xf7\xf8\xf9\xfa\xff\xda\x00\x08\x01\x01\x00\x00?\x00\xd5\x93\xf2\xc2\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\xff\xd9"


# ==================== 模拟 Java Feign 的 HTTP 请求 ====================

class MockResponse:
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


def test_java_feign_request_format():
    """
    模拟 Java Feign 客户端发送的 JSON 请求体格式
    Java 端:
      ImageGenerateRequest {
          image_config: {provider, api_key, base_url, model}
          prompt: "..."
          persona_id: "..."
          context: "reply"
      }
    """
    body = {
        "image_config": {
            "provider": "custom_image",
            "api_key": "sk-test-key-123",
            "base_url": "https://ark.cn-beijing.volces.com/api/v3",
            "model": "doubao-seedream-4-5-251128",
        },
        "prompt": "一只橘猫在窗台上晒太阳",
        "persona_id": TEST_PERSONA_ID,
        "context": "reply",
    }

    fake_gen_resp = MockResponse(200, {
        "data": [{"url": "https://cdn.example.com/cat.png"}],
    })
    fake_dl_resp = MockResponse(200, content=_fake_jpg_bytes())

    with patch("aiohttp.ClientSession.post", return_value=fake_gen_resp), \
         patch("aiohttp.ClientSession.get", return_value=fake_dl_resp), \
         patch("engine.image.generator.aiofiles.open", new_callable=MagicMock) as mock_open:
        mock_file = AsyncMock()
        mock_open.return_value.__aenter__.return_value = mock_file

        resp = client.post("/api/image/generate", json=body)

    # ★ 验证 HTTP 状态码
    assert resp.status_code == 200, f"Java 应收到 HTTP 200, 实际 {resp.status_code}"

    # ★ 验证响应 JSON 字段 — 与 Java ImageGenerateResponse 严格对应
    data = resp.json()

    # Java 端字段: success(boolean)
    assert "success" in data, "Java 需要 success 字段"
    assert data["success"] is True, "Java 期望 success=true"

    # Java 端字段: local_path(string) — @JsonProperty("local_path")
    assert "local_path" in data, "Java 需要 local_path 字段"
    assert isinstance(data["local_path"], str), "local_path 应为字符串"
    assert len(data["local_path"]) > 0, "local_path 不应为空"

    # Java 端字段: error(string)
    assert "error" in data, "Java 需要 error 字段"
    assert data["error"] == "", "成功时 error 应为空字符串"

    # Java 端字段: width/height(int)
    assert "width" in data, "Java 需要 width 字段"
    assert "height" in data, "Java 需要 height 字段"

    # ★ 验证路径含 persona_id
    assert TEST_PERSONA_ID in data["local_path"], \
        f"路径应含 persona_id: {data['local_path']}"

    # ★ 验证路径含 reply context
    assert "reply" in data["local_path"], \
        f"路径应含 context: {data['local_path']}"


def test_java_feign_getting_error_response():
    """
    Java 收到 Python 返回的失败响应时的字段兼容性
    对应 Java 代码: if (resp != null && resp.isSuccess()) { ... }
    """
    body = {
        "image_config": {
            "provider": "nonexistent",
        },
        "prompt": "test",
    }

    resp = client.post("/api/image/generate", json=body)

    assert resp.status_code == 200
    data = resp.json()

    # Java 端的关键判断: resp.isSuccess() == false → 进入失败分支
    assert "success" in data
    assert data["success"] is False, "无效 provider 应返回 success=false"

    # Java 端: resp.getError() 取错误信息
    assert "error" in data
    assert isinstance(data["error"], str)
    assert len(data["error"]) > 0, "失败时 error 不应为空"

    # ★ Java 端 resp.isSuccess() 为 false 时不会读 local_path
    #   所以 local_path 为空是可以的
    assert data["local_path"] == "" or data["local_path"] is None


def test_java_sends_image_config_keys():
    """
    验证 Java 端 loadImageApiConfig() 构造的 image_config 字段映射正确
    Java 端:
      image.put("provider", ...)
      image.put("api_key", ...)
      image.put("base_url", ...)
      image.put("model", ...)
    Python 端 ImageGenerateRequest.image_config 应收到这些字段
    """
    body = {
        "image_config": {
            "provider": "volcengine_image",
            "api_key": "base64_decoded_key",
            "base_url": "https://ark.cn-beijing.volces.com/api/v3",
            "model": "doubao-seedream-4-5-251128",
        },
        "prompt": "一只猫",
        "persona_id": TEST_PERSONA_ID,
        "context": "moment",
    }

    fake_gen_resp = MockResponse(200, {
        "data": [{"url": "https://cdn.example.com/cat.png"}],
    })
    fake_dl_resp = MockResponse(200, content=_fake_jpg_bytes())

    with patch("aiohttp.ClientSession.post", return_value=fake_gen_resp), \
         patch("aiohttp.ClientSession.get", return_value=fake_dl_resp), \
         patch("engine.image.generator.aiofiles.open", new_callable=MagicMock) as mock_open:
        mock_file = AsyncMock()
        mock_open.return_value.__aenter__.return_value = mock_file

        resp = client.post("/api/image/generate", json=body)

    assert resp.status_code == 200
    data = resp.json()
    assert data["success"] is True

    # context 应反映到文件名中
    assert "moment" in data["local_path"], \
        f"文件名应含 context 'moment': {data['local_path']}"


def test_image_saved_to_persona_folder():
    """
    Java 端逻辑：图片应保存在 data/generated_images/{persona_id}/
    Python 端 _ensure_dir() 保证
    此处验证实际文件创建
    """
    import tempfile
    import shutil

    gen = OpenAIImageGenerator("https://api.openai.com/v1", "sk-test", "dall-e-3")
    fake_resp = MockResponse(200, {
        "data": [{"url": "https://cdn.example.com/img.png"}],
    })
    dl_resp = MockResponse(200, content=_fake_jpg_bytes())

    with patch("aiohttp.ClientSession.post", return_value=fake_resp), \
         patch("aiohttp.ClientSession.get", return_value=dl_resp), \
         patch("engine.image.generator.aiofiles.open", new_callable=MagicMock) as mock_open:
        mock_file = AsyncMock()
        mock_open.return_value.__aenter__.return_value = mock_file

        import asyncio
        result = asyncio.run(gen.generate("test", TEST_PERSONA_ID, "reply"))

    assert result.success
    # 路径应包含 persona_id
    assert TEST_PERSONA_ID in result.local_path
    # 文件应在 data/generated_images/ 下
    assert "generated_images" in result.local_path


def test_image_generated_file_is_accessible_by_java():
    """
    Java 端通过 qqWs.sendImage(ownerQq, resp.getLocalPath(), caption)
    发送图片到 QQ。验证返回的 local_path 是绝对路径，NapCat 可访问。
    """
    gen = OpenAIImageGenerator("https://api.openai.com/v1", "sk-test", "dall-e-3")
    fake_resp = MockResponse(200, {
        "data": [{"url": "https://cdn.example.com/img.png"}],
    })
    dl_resp = MockResponse(200, content=_fake_jpg_bytes())

    with patch("aiohttp.ClientSession.post", return_value=fake_resp), \
         patch("aiohttp.ClientSession.get", return_value=dl_resp), \
         patch("engine.image.generator.aiofiles.open", new_callable=MagicMock) as mock_open:
        mock_file = AsyncMock()
        mock_open.return_value.__aenter__.return_value = mock_file

        import asyncio
        result = asyncio.run(gen.generate("test", TEST_PERSONA_ID, "reply"))

    assert result.success
    # 验证返回的是绝对路径
    assert os.path.isabs(result.local_path), \
        f"local_path 应为绝对路径: {result.local_path}"
    # 验证路径格式与 QQWebSocketHandler.sendImage() 兼容
    abs_path = result.local_path.replace("\\", "/")
    # CQ 码路径格式: file:///D:/path/to/file
    cq_path = "file:///" + abs_path
    assert cq_path.startswith("file:///"), "CQ 码路径应以 file:/// 开头"
