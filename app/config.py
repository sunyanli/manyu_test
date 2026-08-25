"""配置管理 — 从环境变量读取 DB/Redis/审批回调 URL 等配置。"""

from pydantic_settings import BaseSettings
from typing import Optional


class Settings(BaseSettings):
    # 数据库
    db_host: str = "localhost"
    db_port: int = 3306
    db_user: str = "root"
    db_password: str = ""
    db_name: str = "org_structure"

    # Redis（可选）
    redis_url: Optional[str] = None

    # 审批系统回调
    approval_service_url: Optional[str] = None

    # 部门树最大深度
    max_dept_level: int = 10

    @property
    def database_url(self) -> str:
        return (
            f"mysql+aiomysql://{self.db_user}:{self.db_password}"
            f"@{self.db_host}:{self.db_port}/{self.db_name}"
        )

    model_config = {"env_prefix": "ORG_", "env_file": ".env", "extra": "ignore"}


settings = Settings()