import hashlib
from typing import Literal

from fastapi import APIRouter
from pydantic import BaseModel, Field

router = APIRouter()


class HashRequest(BaseModel):
    text: str = Field(..., description="待哈希的文本")
    algorithm: Literal["sha256", "md5"] = Field(..., description="哈希算法")


class HashResponse(BaseModel):
    algorithm: str
    input: str
    hash: str


@router.post("/hash")
def compute_hash(req: HashRequest):
    """POST /api/hash — 计算文本的 SHA-256 或 MD5 哈希"""
    if req.algorithm == "sha256":
        h = hashlib.sha256(req.text.encode()).hexdigest()
    else:
        h = hashlib.md5(req.text.encode()).hexdigest()

    return HashResponse(algorithm=req.algorithm, input=req.text, hash=h)