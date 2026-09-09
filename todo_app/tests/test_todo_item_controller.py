"""TodoItemController 集成测试（HTTP 端到端冒烟）"""

import json
import threading
import unittest
import urllib.request
import urllib.error

from todo_app.controller.todo_item_controller import create_server
from todo_app.common.errors import TodoErrorCode
from todo_app.repository.todo_item_repository import TodoItemRepository
from todo_app.service.todo_item_service import TodoItemService


class TestTodoItemController(unittest.TestCase):
    """POST /api/todos 端到端测试。"""

    @classmethod
    def setUpClass(cls):
        service = TodoItemService(TodoItemRepository())
        cls.server = create_server(service, host="127.0.0.1", port=0)
        cls.port = cls.server.server_address[1]
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.server.server_close()
        cls.thread.join(timeout=2)

    def _post(self, body: dict):
        payload = json.dumps(body).encode("utf-8")
        req = urllib.request.Request(
            f"http://127.0.0.1:{self.port}/api/todos",
            data=payload,
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        try:
            with urllib.request.urlopen(req, timeout=5) as resp:
                return resp.status, json.loads(resp.read().decode("utf-8"))
        except urllib.error.HTTPError as e:
            return e.code, json.loads(e.read().decode("utf-8"))

    def test_should_create_todo_via_http(self):
        status, body = self._post({"name": "完成日报", "description": "日报"})
        self.assertEqual(status, 200)
        self.assertEqual(body["code"], TodoErrorCode.SUCCESS)
        self.assertEqual(body["data"]["name"], "完成日报")
        self.assertIn("id", body["data"])

    def test_should_return_400_when_name_missing(self):
        status, body = self._post({"description": "no name"})
        self.assertEqual(status, 400)
        self.assertEqual(body["code"], TodoErrorCode.NAME_EMPTY)

    def test_should_return_400_when_name_too_long(self):
        status, body = self._post({"name": "a" * 101})
        self.assertEqual(status, 400)
        self.assertEqual(body["code"], TodoErrorCode.NAME_TOO_LONG)


if __name__ == "__main__":
    unittest.main()
