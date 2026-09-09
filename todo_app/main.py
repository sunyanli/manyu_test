"""应用入口：启动待办事项 HTTP 服务。

用法：
    python -m todo_app.main
"""

import logging

from todo_app.controller.todo_item_controller import (
    DEFAULT_HOST,
    DEFAULT_PORT,
    create_server,
)
from todo_app.repository.todo_item_repository import TodoItemRepository
from todo_app.service.todo_item_service import TodoItemService

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
)
logger = logging.getLogger(__name__)


def main() -> None:
    """组装依赖并启动 HTTP 服务。"""
    repository = TodoItemRepository()
    service = TodoItemService(repository)
    server = create_server(service, host=DEFAULT_HOST, port=DEFAULT_PORT)
    logger.info("todo server listening on %s:%s", DEFAULT_HOST, DEFAULT_PORT)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        logger.info("shutting down")
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
