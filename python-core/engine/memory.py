"""
Mem0 记忆系统集成服务

★ 为什么需要记忆：
  AI 当前每次对话都是 "失忆" —— 不记得三天前聊过什么。
  Mem0 提供轻量级语义记忆：add(对话摘要) → ChromaDB向量存储 → search(关键词) → 召回相关记忆

★ 使用流程：
  1. Java 调 Python 端点 → 端点调 memory_service.search(query, persona_id)
  2. 提取相关记忆 → 注入 System Prompt
  3. LLM 回复后 → 如果 inner_thought.should_remember=true → memory_service.add(memorable_point, persona_id)

★ 多角色记忆隔离：
  每个 persona_id 独立一个 user_id → 不同 AI 的记忆互不污染
"""
import os
import logging
from typing import List, Dict, Any, Optional
from mem0 import Memory

logger = logging.getLogger(__name__)

# 国内用户如需 HuggingFace 加速，请在启动前设置环境变量：
# set HF_ENDPOINT=https://hf-mirror.com
# Windows 下 hugggingface_hub symlink 警告（不影响功能）
os.environ["HF_HUB_DISABLE_SYMLINKS_WARNING"] = "1"

# ChromaDB 持久化路径（项目根目录下）
CHROMA_PERSIST_DIR = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "data", "chromadb"
)

# 本地嵌入模型路径（预先下载好，无需联网）
LOCAL_MODEL_PATH = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "data", "models", "BAAI--bge-small-zh-v1.5"
)


class MemoryService:
    """
    Mem0 记忆服务封装

    ★ 为什么用单例模式（模块级实例）：
      ChromaDB 在同一进程内只需初始化一次。
      用模块变量而非类实例减少 ChromaDB 重复连接的资源开销。
    """

    def __init__(self):
        self._memory: Optional[Memory] = None
        self._initialized = False

    def initialize(self, api_config: dict) -> bool:
        """
        初始化 Mem0 实例

        ★ 调用时机：首次调用 search() 或 add() 时延迟初始化

        ★ 为什么延迟初始化而非 FastAPI startup 事件：
          Mem0 初始化需要 API Key（由 Java 每次请求传入），
          FastAPI startup 时还没有 API Key——只能在首次调用时初始化。

        ★ 嵌入模型与对话 LLM 分离：
          DeepSeek 不支持 /embeddings 端点，所以需要独立的 embedder provider。
          在 system_config.json 中配置 embedderBaseUrl + embedderApiKeyEncrypted，
          Java 透传到 api_config 的 embedder_base_url / embedder_api_key 字段。
          推荐：SiliconFlow（免费），模型 BAAI/bge-large-zh-v1.5。
          如果未配置 embedder → 初始化失败，记忆降级为空（不阻塞主流程）。
        """
        if self._initialized:
            return True

        try:
            api_key = api_config.get("api_key", "")
            base_url = api_config.get("base_url", "https://api.deepseek.com/v1")
            model = api_config.get("model", "deepseek-chat")

            # Mem0 SDK 在 openai_base_url 的基础上自动拼接 /v1/chat/completions
            # 如果传入的 base_url 已含 /v1，会导致路径变为 /v1/v1/chat/completions → 404
            mem0_base_url = base_url.rstrip("/")
            if mem0_base_url.endswith("/v1"):
                mem0_base_url = mem0_base_url[:-3].rstrip("/")

            # 嵌入模型配置
            # 默认使用本地 HuggingFace sentence-transformers（零 API Key，零注册）
            # 如果 system_config.json 配置了 embedderBaseUrl，则使用 API 模式
            embedder_key = api_config.get("embedder_api_key", "")
            embedder_url = api_config.get("embedder_base_url", "")

            config = {
                "llm": {
                    "provider": "openai",
                    "config": {
                        "api_key": api_key,
                        "model": model,
                        "openai_base_url": mem0_base_url,
                    }
                },
                "embedder": {
                    "provider": "huggingface",
                    "config": {
                        "model": LOCAL_MODEL_PATH,
                    }
                },
                "vector_store": {
                    "provider": "chroma",
                    "config": {
                        "collection_name": "social_persona_memories",
                        "path": CHROMA_PERSIST_DIR,
                    }
                },
                "history_db_path": os.path.join(
                    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "data", "mem0_history.db"
                )
            }

            # 如果显式配置了 embedderBaseUrl，切换到 API 模式（覆盖默认）
            if embedder_url:
                config["embedder"] = {
                    "provider": "openai",
                    "config": {
                        "api_key": embedder_key or api_key,
                        "model": "BAAI/bge-large-zh-v1.5",
                        "openai_base_url": embedder_url.rstrip("/"),
                    }
                }

            self._memory = Memory.from_config(config)
            self._initialized = True
            embedder_label = embedder_url if embedder_url else "local (huggingface)"
            logger.info(f"Mem0 初始化成功（llm={model}，embedder={embedder_label}）")
            return True

        except Exception as e:
            logger.warning(f"Mem0 初始化失败，记忆功能降级为空: {e}")
            logger.warning("提示：如报错信息包含 'sentence_transformers' 或 'huggingface'，请运行: pip install sentence-transformers")
            self._memory = None
            self._initialized = False
            return False

    def search(self, query: str, persona_id: str, limit: int = 5, api_config: Optional[dict] = None) -> List[str]:
        """
        语义检索相关记忆

        ★ 为什么返回 List[str] 而非 List[dict]：
          端点只需要记忆内容文本注入 Prompt，不需要 metadata。

        ★ limit=5 的设计依据：
          记忆太多会稀释 Prompt 质量。5 条最相关的足够提供上下文。

        Args:
            query: 搜索关键词（如用户消息 + 事件描述）
            persona_id: 人格标识（Mem0 user_id）
            limit: 返回最大条数
            api_config: API 配置（首次调用时用于延迟初始化）

        Returns:
            记忆文本列表，初始化失败返回空列表
        """
        # 自动延迟初始化
        if not self._memory and api_config:
            self.initialize(api_config)
        if not self._memory:
            return []
        try:
            results = self._memory.search(query, user_id=persona_id, limit=limit)
            memories = []
            for r in results:
                memory_text = r.get("memory", "") if isinstance(r, dict) else str(r)
                if memory_text:
                    memories.append(memory_text)
            return memories
        except Exception as e:
            logger.warning(f"Mem0 检索失败（不阻塞主流程）: {e}")
            return []

    def add(self, content: str, persona_id: str, api_config: Optional[dict] = None):
        """
        写入记忆

        ★ 调用时机：LLM 回复后，inner_thought.should_remember=true

        ★ 为什么是 fire-and-forget：
          记忆写入不应阻塞消息回复。
          用户不应该等 "记忆保存中..." —— 这没有任何意义。

        ★ Mem0 内部流程：
          1. LLM 从 content 中提取关键信息（实体/关系/事件）
          2. Embedding 模型将提取的信息向量化
          3. 向量存入 ChromaDB

        Args:
            content: 要记住的内容（如 "用户被老板骂了，心情很差"）
            persona_id: 人格标识
            api_config: API 配置（首次调用时用于延迟初始化）
        """
        # 自动延迟初始化
        if not self._memory and api_config:
            self.initialize(api_config)
        if not self._memory:
            return
        try:
            # Mem0.add() 传入消息列表自动提取记忆
            self._memory.add(content, user_id=persona_id)
            logger.debug(f"Mem0 写入成功: persona={persona_id}, content={content[:50]}...")
        except Exception as e:
            logger.warning(f"Mem0 写入失败（不阻塞主流程）: {e}")

    def add_batch(self, items: list, persona_id: str, api_config: Optional[dict] = None):
        """
        批量写入记忆（今日反思后 key_memories 批量持久化）

        ★ 为什么单独 add_batch 而非循环调 add：
          Mem0.add() 每条记忆都调一次 LLM 提取 → N 次提取 = N 次 LLM 调用。
          add_batch 传列表 → Mem0 内部合并提取 → 节省 LLM 调用次数。

        Args:
            items: 记忆项列表（如 [{"content":"...","importance":8}]）
            persona_id: 人格标识
            api_config: API 配置（首次调用时用于延迟初始化）
        """
        # 自动延迟初始化
        if not self._memory and api_config:
            self.initialize(api_config)
        if not self._memory:
            return
        try:
            # 只存 importance >= 7 的高价值记忆
            high_value = [item["content"] for item in items
                          if item.get("importance", 0) >= 7
                          and item.get("content")]
            if high_value:
                for content in high_value:
                    self._memory.add(content, user_id=persona_id)
                logger.info(f"Mem0 批量写入 {len(high_value)} 条关键记忆: persona={persona_id}")
        except Exception as e:
            logger.warning(f"Mem0 批量写入失败: {e}")


# 模块级单例
memory_service = MemoryService()
