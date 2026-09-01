"""manyu_test Flask 后端主入口"""

from flask import Flask
from flask_cors import CORS


def create_app() -> Flask:
    app = Flask(__name__)
    CORS(app)

    from middleware.tracking import init_tracking
    init_tracking(app)

    # 注册路由
    from routes.helloworld import helloworld_bp
    from routes.hash import hash_bp
    from routes.bubblesort import bubblesort_bp
    from routes.export import export_bp
    from routes.tracking import tracking_bp

    app.register_blueprint(helloworld_bp)
    app.register_blueprint(hash_bp)
    app.register_blueprint(bubblesort_bp)
    app.register_blueprint(export_bp)
    app.register_blueprint(tracking_bp)

    return app


app = create_app()

if __name__ == "__main__":
    app.run(debug=True, port=5000)