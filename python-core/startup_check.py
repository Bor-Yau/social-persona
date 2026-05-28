"""
启动预检 —— 验证（必要时自动下载）本地嵌入模型 BAAI/bge-small-zh-v1.5

- 模型存在于 python-core/data/models/ 则直接通过
- 不存在则通过 sentence-transformers 自动从 HuggingFace 下载（~47MB）
- 下载全过程带进度提示，不会静默挂起
"""
import sys
import os
import traceback

MODEL_NAME = "BAAI/bge-small-zh-v1.5"
MODEL_DIR = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "data", "models", MODEL_NAME.replace("/", "--")
)
CACHE_ROOT = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "data", "models"
)


def _check_model_local() -> bool:
    if not os.path.isdir(MODEL_DIR):
        return False
    key_files = ["config.json", "tokenizer.json", "modules.json", "model.safetensors"]
    for f in key_files:
        if not os.path.isfile(os.path.join(MODEL_DIR, f)):
            return False
    return True


def _download_model() -> bool:
    print()
    print("  [INFO] 正在从 HuggingFace 下载嵌入模型（约 47MB）...")
    print(f"  [INFO] 模型: {MODEL_NAME}")
    print(f"  [INFO] 目标: {MODEL_DIR}")
    print("  [进度] ", end="", flush=True)

    try:
        from sentence_transformers import SentenceTransformer

        print("正在连接...", end="", flush=True)
        SentenceTransformer(
            MODEL_NAME,
            cache_folder=CACHE_ROOT,
            trust_remote_code=False,
        )
        print(" ✓ 下载完成")
        return True
    except ImportError:
        print()
        print()
        print("  [ERROR] sentence-transformers 未安装")
        print("  请手动执行: pip install sentence-transformers")
        print("  然后重新运行启动脚本。")
        return False
    except Exception as e:
        print()
        print()
        print(f"  [ERROR] 模型下载失败: {e}")
        print()
        lines = traceback.format_exc().splitlines()
        for line in lines[-5:]:
            print(f"    {line}")
        print()
        print("  可能的原因:")
        print("    · 网络连不上 HuggingFace（尝试挂代理或使用镜像）")
        print("    · 磁盘空间不足")
        print()
        print("  手动下载方案:")
        print("    1. git lfs install")
        print(f"    2. cd {CACHE_ROOT}")
        print(f"    3. git clone https://huggingface.co/{MODEL_NAME}")
        print()
        return False


def main():
    print()
    print(f"  ═══════════════════════════════════")
    print(f"   预检：嵌入模型 {MODEL_NAME}")
    print(f"  ═══════════════════════════════════")
    print()

    if _check_model_local():
        print("  ✓ 模型已就绪（跳过下载）")
        return 0

    print("  → 模型未找到，开始自动下载...")

    if _download_model():
        print()
        print("  ✓ 模型准备完成")
        return 0

    return 1


if __name__ == "__main__":
    sys.exit(main())
