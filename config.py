"""应用配置"""

import os


class Config:
    """应用配置类"""
    SECRET_KEY = os.environ.get("SECRET_KEY", "dev-secret-key")
    SQLALCHEMY_DATABASE_URI = os.environ.get(
        "DATABASE_URL",
        "sqlite:///invoke_log.db"  # 默认使用 SQLite，生产环境可切换 MySQL
    )
    SQLALCHEMY_TRACK_MODIFICATIONS = False
    # 埋点开关
    ENABLE_TRACKING = os.environ.get("ENABLE_TRACKING", "true").lower() == "true"