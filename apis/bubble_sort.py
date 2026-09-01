import sys
import os

sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))
from bubble_sort import bubble_sort as bs

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

router = APIRouter()


class SortRequest(BaseModel):
    numbers: list[int | float] = Field(..., min_length=1, description="待排序的数字数组")


class SortResponse(BaseModel):
    original: list[int | float]
    sorted: list[int | float]
    algorithm: str


@router.post("/api/bubble-sort", response_model=SortResponse)
async def bubble_sort_api(req: SortRequest):
    original = list(req.numbers)
    return SortResponse(
        original=original,
        sorted=bs(original.copy()),
        algorithm="bubble_sort",
    )