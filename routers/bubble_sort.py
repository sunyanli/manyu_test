import sys
import os
from typing import List, Literal

from fastapi import APIRouter
from pydantic import BaseModel, Field

# 确保能导入根目录的 bubble_sort.py
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from bubble_sort import bubble_sort_optimized, bubble_sort_descending

router = APIRouter()


class BubbleSortRequest(BaseModel):
    array: List[int] = Field(..., description="待排序的整数数组")
    order: Literal["asc", "desc"] = Field(..., description="排序方向")


class BubbleSortResponse(BaseModel):
    original: List[int]
    sorted: List[int]
    order: str


@router.post("/bubble_sort")
def sort_array(req: BubbleSortRequest):
    """POST /api/bubble_sort — 对整数数组进行冒泡排序"""
    original = req.array.copy()
    if req.order == "asc":
        result = bubble_sort_optimized(req.array.copy())
    else:
        result = bubble_sort_descending(req.array.copy())

    return BubbleSortResponse(original=original, sorted=result, order=req.order)