"""Flask 主应用入口"""

import logging

from flask import Flask, send_from_directory
from flask_cors import CORS

from config import Config
from database import init_db
from api.hello import hello_bp
from api.hash import hash_bp
from api.sort import sort_bp
from api.export import export_bp
from api.stats import stats_bp
from middleware.tracking import register_tracking

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger(__name__)


def create_app():
    """创建 Flask 应用实例"""
    app = Flask(__name__, static_folder="static", static_url_path="")
    app.config.from_object(Config)

    # 允许跨域（前端开发时可能跨域访问）
    CORS(app)

    # 初始化数据库
    init_db(app)

    # 注册蓝图
    app.register_blueprint(hello_bp)
    app.register_blueprint(hash_bp)
    app.register_blueprint(sort_bp)
    app.register_blueprint(export_bp)
    app.register_blueprint(stats_bp)

    # 注册埋点切面
    register_tracking(app)

    # 提供前端静态页面
    @app.route("/")
    def index():
        return send_from_directory(app.static_folder, "index.html")

    return app


if __name__ == "__main__":
    app = create_app()
    logger.info("Starting server on http://0.0.0.0:5000")
    app.run(host="0.0.0.0", port=5000, debug=True)