from fastapi import APIRouter

router = APIRouter()


@router.get("/helloworld")
def helloworld():
    """GET /api/helloworld — 返回 Hello World 消息"""
    return {"message": "Hello World!"}