"""
图片生成端点 —— POST /api/image/generate

由 Java MessageService 在 dispatchReply 处理 type=image item 时调用。
不同 api_format 通过 factory.create_image_generator() 分派。
"""
from fastapi import APIRouter
from api.models import ImageGenerateRequest, ImageGenerateResponse
from engine.llm.factory import create_image_generator

router = APIRouter()


@router.post("/api/image/generate", response_model=ImageGenerateResponse)
async def generate_image(request: ImageGenerateRequest):
    try:
        generator = create_image_generator(request.image_config)
        dir_id = request.persona_dir_name or request.persona_id
        result = await generator.generate(
            prompt=request.prompt,
            persona_id=dir_id,
            context=request.context,
        )
        return ImageGenerateResponse(
            success=result.success,
            local_path=result.local_path,
            width=result.width,
            height=result.height,
            error=result.error,
        )
    except Exception as e:
        return ImageGenerateResponse(success=False, error=str(e))
