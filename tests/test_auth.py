"""登录/登出接口测试：真实 HTTP 端到端 + 关键单元回归。"""

from __future__ import annotations

import json
import os
import shutil
import tempfile
import threading
import time
import unittest
from http.client import HTTPConnection
from typing import Any, Optional

from src import db as database
from src.auth_service import AUTH_FAILED, AuthService
from src.config import Config
from src.security import (
    RateLimiter,
    hash_password,
    sign_session,
    verify_password,
    verify_session,
)


class HTTPTestCase(unittest.TestCase):
    """每个用例独立临时库 + 独立服务进程内实例。"""

    def setUp(self) -> None:
        self.tmpdir = tempfile.mkdtemp(prefix="auth-test-")
        self.db_path = os.path.join(self.tmpdir, "test.db")
        database.init_db(self.db_path)
        self.config = Config(db_path=self.db_path, secret_key="test-secret")
        from src.app import create_server

        AuthService(self.config).reset_revoked()
        self.server = create_server(self.config, host="127.0.0.1", port=0)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        self.port = self.server.server_address[1]

    def tearDown(self) -> None:
        self.server.shutdown()
        self.server.server_close()
        self.thread.join(timeout=5)
        shutil.rmtree(self.tmpdir, ignore_errors=True)

    # ---------------------------------------------------------------- helpers
    def add_user(
        self, username: str, password: str, is_active: bool = True
    ) -> int:
        return database.create_user(self.db_path, username, hash_password(password), is_active)

    def post(
        self,
        path: str,
        payload: Any = None,
        headers: Optional[dict[str, str]] = None,
        raw_body: Optional[bytes] = None,
    ) -> tuple[int, dict[str, Any], dict[str, str]]:
        conn = HTTPConnection("127.0.0.1", self.port, timeout=10)
        body = raw_body if raw_body is not None else (
            json.dumps(payload).encode("utf-8") if payload is not None else None
        )
        request_headers = {"Content-Type": "application/json"}
        request_headers.update(headers or {})
        conn.request("POST", path, body=body, headers=request_headers)
        response = conn.getresponse()
        raw = response.read()
        header_map = {name.lower(): value for name, value in response.getheaders()}
        conn.close()
        return response.status, (json.loads(raw) if raw else {}), header_map

    def login(
        self,
        username: str,
        password: str,
        remember_me: Optional[bool] = None,
        ip: Optional[str] = None,
    ) -> tuple[int, dict[str, Any], dict[str, str]]:
        payload: dict[str, Any] = {"username": username, "password": password}
        if remember_me is not None:
            payload["remember_me"] = remember_me
        headers = {"X-Forwarded-For": ip} if ip else None
        return self.post("/api/auth/login", payload, headers=headers)


class LoginAcceptanceTest(HTTPTestCase):
    def test_01_login_success_sets_httponly_session_cookie(self) -> None:
        self.add_user("accept1", "Pass1234")

        status, data, headers = self.login("accept1", "Pass1234")

        self.assertEqual(status, 200)
        self.assertEqual(data["code"], "SUCCESS")
        cookie = headers["set-cookie"]
        self.assertIn("session=", cookie)
        self.assertIn("Max-Age=7200", cookie)
        self.assertIn("HttpOnly", cookie)
        self.assertIn("SameSite=Lax", cookie)
        self.assertIn("Path=/", cookie)

    def test_02_wrong_password_returns_unified_error(self) -> None:
        self.add_user("accept2", "correct")

        status, data, _ = self.login("accept2", "wrong")

        self.assertEqual(status, 401)
        self.assertEqual(data["code"], AUTH_FAILED)
        self.assertEqual(data["message"], "账号或密码错误")

    def test_03_unknown_account_returns_identical_error(self) -> None:
        status, data, _ = self.login("no_such_user_42", "anything")

        self.assertEqual(status, 401)
        self.assertEqual(data["code"], AUTH_FAILED)
        self.assertEqual(data["message"], "账号或密码错误")

    def test_04_disabled_account_is_rejected_with_same_message(self) -> None:
        self.add_user("accept4", "pass", is_active=False)

        status, data, _ = self.login("accept4", "pass")

        self.assertEqual(status, 401)
        self.assertEqual(data["code"], AUTH_FAILED)
        self.assertEqual(data["message"], "账号或密码错误")

    def test_05_rate_limited_after_five_failures_per_ip(self) -> None:
        for index in range(5):
            status, data, _ = self.login(f"ghost_{index}", "wrong", ip="203.0.113.7")
            self.assertEqual(status, 401)
            self.assertEqual(data["code"], AUTH_FAILED)

        status, data, _ = self.login("ghost_x", "wrong", ip="203.0.113.7")

        self.assertEqual(status, 429)
        self.assertEqual(data["code"], "RATE_LIMITED")
        self.assertEqual(data["message"], "请求过于频繁，请稍后再试")

    def test_05b_other_ip_not_rate_limited(self) -> None:
        for index in range(5):
            self.login(f"ghost_{index}", "wrong", ip="198.51.100.1")

        status, _, _ = self.login("ghost_x", "wrong", ip="198.51.100.2")
        self.assertEqual(status, 401)

    def test_06_account_locked_after_ten_failures(self) -> None:
        user_id = self.add_user("accept6", "realpass")
        service = AuthService(self.config)

        for index in range(10):
            user, error = service.authenticate("accept6", "wrong", f"10.9.{index}.1")
            self.assertIsNone(user)
            self.assertEqual(error, AUTH_FAILED)

        row = database.find_by_id(self.db_path, user_id)
        self.assertEqual(row["failed_attempts"], 10)
        self.assertIsNotNone(row["locked_until"])
        self.assertGreater(row["locked_until"], time.time())

        # 锁定后即使密码正确也必须返回统一错误
        status, data, _ = self.login("accept6", "realpass", ip="10.9.99.1")
        self.assertEqual(status, 401)
        self.assertEqual(data["message"], "账号或密码错误")

    def test_06b_successful_login_resets_failure_counter(self) -> None:
        user_id = self.add_user("reset_user", "goodpass")
        service = AuthService(self.config)

        for index in range(3):
            self.assertEqual(
                service.authenticate("reset_user", "bad", f"10.8.{index}.1")[1], AUTH_FAILED
            )
        user, error = service.authenticate("reset_user", "goodpass", "10.8.99.1")

        self.assertEqual(error, "")
        self.assertIsNotNone(user)
        row = database.find_by_id(self.db_path, user_id)
        self.assertEqual(row["failed_attempts"], 0)

    def test_07_remember_me_sets_thirty_day_cookie(self) -> None:
        self.add_user("accept7", "pass")

        status, _, headers = self.login("accept7", "pass", remember_me=True)

        self.assertEqual(status, 200)
        self.assertIn("Max-Age=2592000", headers["set-cookie"])

    def test_08_logout_clears_cookie_and_revokes_token(self) -> None:
        self.add_user("accept8", "pass")
        _, _, login_headers = self.login("accept8", "pass")
        token = login_headers["set-cookie"].split("session=", 1)[1].split(";", 1)[0]
        service = AuthService(self.config)
        self.assertIsNotNone(service.verify_session(token))

        status, data, headers = self.post(
            "/api/auth/logout", headers={"Cookie": f"session={token}"}
        )

        self.assertEqual(status, 200)
        self.assertEqual(data["code"], "SUCCESS")
        self.assertIn("session=;", headers["set-cookie"])
        self.assertIn("Max-Age=0", headers["set-cookie"])
        self.assertIsNone(service.verify_session(token))


class ValidationAndProtocolTest(HTTPTestCase):
    def test_empty_username_and_password_rejected(self) -> None:
        self.assertEqual(self.login("", "something")[0], 422)
        self.assertEqual(self.login("someone", "")[0], 422)

    def test_malformed_json_rejected(self) -> None:
        status, data, _ = self.post("/api/auth/login", raw_body=b"{not json")

        self.assertEqual(status, 422)
        self.assertEqual(data["code"], "VALIDATION_ERROR")

    def test_unknown_route_and_wrong_method(self) -> None:
        self.assertEqual(self.post("/api/auth/unknown", {"a": 1})[0], 404)

        conn = HTTPConnection("127.0.0.1", self.port, timeout=10)
        conn.request("GET", "/api/auth/login")
        response = conn.getresponse()
        body = json.loads(response.read())
        conn.close()
        self.assertEqual(response.status, 405)
        self.assertEqual(body["code"], "METHOD_NOT_ALLOWED")

    def test_password_error_message_does_not_leak_account_existence(self) -> None:
        self.add_user("leaky", "right")

        _, existing, _ = self.login("leaky", "nope", ip="192.0.2.1")
        _, missing, _ = self.login("absent_user", "nope", ip="192.0.2.2")

        self.assertEqual(existing, missing)


class SecurityUnitTest(unittest.TestCase):
    def test_hash_and_verify(self) -> None:
        hashed = hash_password("mysecret")
        self.assertNotEqual(hashed, "mysecret")
        self.assertTrue(hashed.startswith("pbkdf2_sha256$"))
        self.assertTrue(verify_password("mysecret", hashed))
        self.assertFalse(verify_password("wrong", hashed))
        self.assertFalse(verify_password("mysecret", "garbage"))

    def test_hash_uses_unique_salt(self) -> None:
        first = hash_password("same")
        second = hash_password("same")
        self.assertNotEqual(first, second)
        self.assertTrue(verify_password("same", first))
        self.assertTrue(verify_password("same", second))

    def test_session_token_roundtrip_and_tampering(self) -> None:
        token = sign_session({"user_id": 7, "username": "u"}, 60, "secret")
        payload = verify_session(token, "secret")
        self.assertEqual(payload["user_id"], 7)
        self.assertEqual(payload["username"], "u")
        self.assertIsNone(verify_session(token, "other-secret"))
        self.assertIsNone(verify_session("garbage", "secret"))
        self.assertIsNone(verify_session(token + "x", "secret"))

    def test_expired_session_token_rejected(self) -> None:
        token = sign_session({"user_id": 1}, -1, "secret")
        self.assertIsNone(verify_session(token, "secret"))

    def test_short_ttl_token_expires(self) -> None:
        token = sign_session({"user_id": 1}, 0, "secret")
        time.sleep(1.05)
        self.assertIsNone(verify_session(token, "secret"))

    def test_rate_limiter_window_expiry(self) -> None:
        limiter = RateLimiter(max_attempts=2, window_seconds=0.3)
        limiter.record_attempt("1.2.3.4")
        limiter.record_attempt("1.2.3.4")
        self.assertTrue(limiter.is_rate_limited("1.2.3.4"))
        self.assertFalse(limiter.is_rate_limited("5.6.7.8"))
        time.sleep(0.35)
        self.assertFalse(limiter.is_rate_limited("1.2.3.4"))


if __name__ == "__main__":
    unittest.main()

