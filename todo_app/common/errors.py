"""异常与错误码定义

对应系分方案 §4 全局约定：错误码格式 {MODULE}_{SEQ}，模块前缀 TODO。
"""


class TodoErrorCode:
    """待办事项模块错误码常量。"""

    SUCCESS: str = "TODO_000"
    NAME_EMPTY: str = "TODO_001"
    NAME_TOO_LONG: str = "TODO_002"
    DESCRIPTION_TOO_LONG: str = "TODO_003"
    SYSTEM_ERROR: str = "TODO_999"


class TodoErrorMessage:
    """错误码对应提示信息。"""

    SUCCESS: str = "SUCCESS"
    NAME_EMPTY: str = "事项名称不能为空"
    NAME_TOO_LONG: str = "事项名称长度不能超过100字符"
    DESCRIPTION_TOO_LONG: str = "事项描述长度不能超过500字符"
    SYSTEM_ERROR: str = "系统异常，请稍后重试"


class TodoBizException(Exception):
    """业务异常，携带错误码与提示信息。"""

    def __init__(self, code: str, msg: str) -> None:
        super().__init__(msg)
        self.code: str = code
        self.msg: str = msg
