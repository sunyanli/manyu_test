"""Tests for AuthService — AAA pattern (Arrange, Act, Assert)."""

import pytest
from datetime import datetime, timedelta

from src.service.auth_service import AuthService
from src.model.entity.User import User


class TestAuthServiceLogin:
    """Test AuthService.login method."""

    def test_should_return_token_when_valid_credentials(self):
        """正常路径：正确的用户名和密码，应返回 Token。"""
        # Arrange
        user = User(id=1, username="zhangsan", password_hash="hashed_password")
        service = AuthService()
        service._users = {"zhangsan": user}

        # Act
        result = service.login(username="zhangsan", password="correct_password")

        # Assert
        assert result is not None
        assert "token" in result
        assert result["username"] == "zhangsan"

    def test_should_raise_when_username_not_found(self):
        """异常路径：不存在的用户名，应抛出 ValueError。"""
        # Arrange
        service = AuthService()
        service._users = {}

        # Act & Assert
        with pytest.raises(ValueError, match="用户名或密码错误"):
            service.login(username="unknown", password="any")

    def test_should_raise_when_password_incorrect(self):
        """异常路径：密码错误，应抛出 ValueError。"""
        # Arrange
        user = User(id=1, username="zhangsan", password_hash="hashed_password")
        service = AuthService()
        service._users = {"zhangsan": user}

        # Act & Assert
        with pytest.raises(ValueError, match="用户名或密码错误"):
            service.login(username="zhangsan", password="wrong_password")

    def test_should_return_different_tokens_for_different_users(self):
        """正常路径：不同用户登录应返回不同的 Token。"""
        # Arrange
        user1 = User(id=1, username="alice", password_hash="hash1")
        user2 = User(id=2, username="bob", password_hash="hash2")
        service = AuthService()
        service._users = {"alice": user1, "bob": user2}

        # Act
        result1 = service.login(username="alice", password="password1")
        result2 = service.login(username="bob", password="password2")

        # Assert
        assert result1["token"] != result2["token"]

    def test_token_should_be_valid_for_24_hours(self):
        """正常路径：Token 有效期应为 24 小时。"""
        # Arrange
        user = User(id=1, username="zhangsan", password_hash="hashed_password")
        service = AuthService()
        service._users = {"zhangsan": user}

        # Act
        result = service.login(username="zhangsan", password="correct_password")

        # Assert
        assert result is not None
        assert "expires_in" in result
        assert result["expires_in"] == 86400  # 24 hours in seconds


class TestAuthServiceVerifyToken:
    """Test AuthService.verify_token method."""

    def test_should_return_user_when_valid_token(self):
        """正常路径：有效的 Token，应返回用户信息。"""
        # Arrange
        user = User(id=1, username="zhangsan", password_hash="hashed_password")
        service = AuthService()
        service._users = {"zhangsan": user}
        login_result = service.login(username="zhangsan", password="correct_password")
        token = login_result["token"]

        # Act
        result = service.verify_token(token)

        # Assert
        assert result is not None
        assert result.username == "zhangsan"

    def test_should_raise_when_token_expired(self):
        """异常路径：过期的 Token，应抛出 ValueError。"""
        # Arrange
        user = User(id=1, username="zhangsan", password_hash="hashed_password")
        service = AuthService()
        service._users = {"zhangsan": user}
        login_result = service.login(username="zhangsan", password="correct_password")
        token = login_result["token"]

        # Act & Assert
        with pytest.raises(ValueError, match="Token 已过期"):
            service.verify_token(token, expired=True)

    def test_should_raise_when_token_invalid(self):
        """异常路径：无效的 Token，应抛出 ValueError。"""
        # Arrange
        service = AuthService()

        # Act & Assert
        with pytest.raises(ValueError, match="Token 无效"):
            service.verify_token("invalid_token")

    def test_should_raise_when_token_is_empty(self):
        """异常路径：空 Token，应抛出 ValueError。"""
        # Arrange
        service = AuthService()

        # Act & Assert
        with pytest.raises(ValueError, match="Token 无效"):
            service.verify_token("")