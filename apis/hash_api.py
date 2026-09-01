from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field
import hashlib

router = APIRouter()


class HashRequest(BaseModel):
    text: str = Field(..., min_length=1, description="待哈希的文本")


class HashResponse(BaseModel):
    algorithm: str
    input: str
    hash: str


@router.post("/api/hash", response_model=HashResponse)
async def hash_text(req: HashRequest):
    return HashResponse(
        algorithm="SHA256",
        input=req.text,
        hash=hashlib.sha256(req.text.encode()).hexdigest(),
    )