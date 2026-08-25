"""自定义异常 + 全局异常处理器。"""

from fastapi import Request
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError


class AppException(Exception):
    """业务异常基类。"""

    def __init__(self, code: int, msg: str, status_code: int = 400):
        self.code = code
        self.msg = msg
        self.status_code = status_code


class NotFoundException(AppException):
    def __init__(self, msg: str = "资源不存在"):
        super().__init__(code=404, msg=msg, status_code=404)


class ConflictException(AppException):
    def __init__(self, msg: str = "数据冲突"):
        super().__init__(code=409, msg=msg, status_code=409)


class ForbiddenException(AppException):
    def __init__(self, msg: str = "无权访问"):
        super().__init__(code=403, msg=msg, status_code=403)


class BadRequestException(AppException):
    def __init__(self, msg: str = "请求参数错误"):
        super().__init__(code=400, msg=msg, status_code=400)


async def app_exception_handler(request: Request, exc: AppException) -> JSONResponse:
    return JSONResponse(
        status_code=exc.status_code,
        content={"code": exc.code, "data": None, "msg": exc.msg},
    )


async def validation_exception_handler(
    request: Request, exc: RequestValidationError
) -> JSONResponse:
    errors = exc.errors()
    detail = "; ".join(
        f"{'.'.join(str(loc) for loc in e['loc'])}: {e['msg']}" for e in errors
    )
    return JSONResponse(
        status_code=422,
        content={"code": 422, "data": None, "msg": detail},
    )