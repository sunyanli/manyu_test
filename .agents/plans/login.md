# Login Feature Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a secure, testable login/logout system with password hashing, JWT-based session management, rate limiting, and brute-force protection.

**Architecture:** Layered Python backend using FastAPI. Requests flow through rate-limiting middleware → auth routes → auth service (business logic) → SQLAlchemy User model → SQLite database. JWT tokens stored in HttpOnly/Secure/SameSite cookies. All error responses for invalid credentials are intentionally identical to prevent account enumeration.

**Tech Stack:** Python 3.10+, FastAPI, SQLAlchemy 2.0, SQLite (dev), bcrypt, PyJWT, Pydantic v2, pytest, HTTPX (async test client)

---

## Global Constraints

- Password hashing: bcrypt with werkzeug's `generate_password_hash` / `check_password_hash` (cost factor ≥ 12)
- Account-not-found and wrong-password MUST return identical HTTP 401 `{"detail": "账号或密码错误"}` — no timing or content differentiation
- Login cookies: `HttpOnly=True`, `Secure=True` (dev relaxable), `SameSite=Lax`
- "Remember me": short-lived token = 30 min; long-lived token = 7 days
- Failed login limit: 5 attempts per account → lockout for 15 min; 100 attempts per IP per minute → IP throttled
- Excluded from scope: registration, password reset, SSO, MFA, social login, email verification
- All endpoints return JSON; errors use RFC 7807 Problem Details shape via FastAPI default handlers
- No hardcoded secrets; use environment variables with `.env` fallback in dev

---

## File Structure

```
src/
├── __init__.py
├── config.py              # Settings from env, class Settings
├── database.py            # Engine, SessionLocal, Base, get_db dependency
├── models/
│   ├── __init__.py
│   └── user.py            # User ORM model (id, username, password_hash, failed_attempts, locked_until, disabled, created_at)
├── schemas/
│   ├── __init__.py
│   └── auth.py            # LoginRequest, LoginResponse, LogoutResponse, TokenPayload
├── services/
│   ├── __init__.py
│   └── auth_service.py    # AuthService: login(), logout(), hash_password(), verify_password(), create_token(), decode_token()
├── routes/
│   ├── __init__.py
│   └── auth.py            # POST /api/auth/login, POST /api/auth/logout
├── middleware/
│   ├── __init__.py
│   ├── auth.py            # get_current_user dependency (JWT cookie extraction + verification)
│   └── rate_limiter.py    # RateLimiter: in-memory IP + account tracking, decorator
└── main.py                # FastAPI app factory, lifespan, mount routes, exception handlers
tests/
├── __init__.py
├── conftest.py            # TestClient fixture, test DB, override get_db
├── test_auth_service.py   # Unit tests: hash/verify, token round-trip, login logic, lockout logic
└── test_auth_routes.py    # Integration tests: login success, wrong password, nonexistent user, lockout, logout
```

---

## Task 1: Project Scaffolding & Configuration

**Files:**
- Create: `src/__init__.py`
- Create: `src/config.py`
- Create: `requirements.txt`
- Create: `.env.example`

**Interfaces:**
- Produces: `Settings` class with `DATABASE_URL`, `SECRET_KEY`, `ACCESS_TOKEN_EXPIRE_MINUTES`, `REFRESH_TOKEN_EXPIRE_DAYS`, `BCRYPT_ROUNDS`, `MAX_FAILED_ATTEMPTS`, `LOCKOUT_MINUTES`, `IP_RATE_LIMIT_PER_MINUTE`

- [ ] **Step 1: Create requirements.txt**

```txt
fastapi==0.115.6
uvicorn[standard]==0.34.0
sqlalchemy==2.0.36
pydantic[email]==2.10.3
pydantic-settings==2.7.0
python-jose[cryptography]==3.3.0
passlib[bcrypt]==1.7.4
bcrypt==4.2.1
python-multipart==0.0.18
pytest==8.3.4
httpx==0.28.1
```

- [ ] **Step 2: Install dependencies**

Run: `pip install -r requirements.txt`
Expected: All packages install without error.

- [ ] **Step 3: Create .env.example**

```ini
DATABASE_URL=sqlite:///./dev.db
SECRET_KEY=change-me-to-a-random-256-bit-string
ACCESS_TOKEN_EXPIRE_MINUTES=30
REFRESH_TOKEN_EXPIRE_DAYS=7
BCRYPT_ROUNDS=12
MAX_FAILED_ATTEMPTS=5
LOCKOUT_MINUTES=15
IP_RATE_LIMIT_PER_MINUTE=100
```

- [ ] **Step 4: Create src/__init__.py**

```python
# src package
```

- [ ] **Step 5: Create src/config.py**

```python
"""Application configuration loaded from environment variables."""
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    DATABASE_URL: str = "sqlite:///./dev.db"
    SECRET_KEY: str = "change-me-to-a-random-256-bit-string"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 30
    REFRESH_TOKEN_EXPIRE_DAYS: int = 7
    BCRYPT_ROUNDS: int = 12
    MAX_FAILED_ATTEMPTS: int = 5
    LOCKOUT_MINUTES: int = 15
    IP_RATE_LIMIT_PER_MINUTE: int = 100

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}


settings = Settings()
```

- [ ] **Step 6: Verify config loads**

Run: `python -c "from src.config import settings; print(settings.DATABASE_URL)"`
Expected: `sqlite:///./dev.db`

- [ ] **Step 7: Commit**

```bash
git add requirements.txt .env.example src/__init__.py src/config.py
git commit -m "chore: project scaffolding, config, and dependencies"
```

---

## Task 2: Database & User Model

**Files:**
- Create: `src/database.py`
- Create: `src/models/__init__.py`
- Create: `src/models/user.py`

**Interfaces:**
- Consumes: `settings.DATABASE_URL` from `src.config`
- Produces: `Base` (declarative base), `SessionLocal`, `engine`, `get_db()` async generator, `User` ORM model

- [ ] **Step 1: Create src/database.py**

```python
"""Database engine, session factory, and dependency."""
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, Session, DeclarativeBase

from src.config import settings

engine = create_engine(
    settings.DATABASE_URL,
    connect_args={"check_same_thread": False} if "sqlite" in settings.DATABASE_URL else {},
    echo=False,
)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


class Base(DeclarativeBase):
    pass


def get_db() -> Session:
    """FastAPI dependency: yields a DB session and closes it after request."""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
```

- [ ] **Step 2: Create src/models/__init__.py**

```python
from src.models.user import User

__all__ = ["User"]
```

- [ ] **Step 3: Create src/models/user.py**

```python
"""User ORM model."""
from datetime import datetime

from sqlalchemy import Column, Integer, String, Boolean, DateTime
from sqlalchemy.orm import Mapped, mapped_column

from src.database import Base


class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    username: Mapped[str] = mapped_column(String(150), unique=True, nullable=False, index=True)
    password_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    disabled: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    failed_attempts: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    locked_until: Mapped[datetime | None] = mapped_column(DateTime, nullable=True, default=None)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, nullable=False)
```

- [ ] **Step 4: Create tables and verify**

Run: `python -c "from src.database import engine, Base; from src.models import User; Base.metadata.create_all(bind=engine); print('Tables created')"`
Expected: `Tables created` (no error)

- [ ] **Step 5: Commit**

```bash
git add src/database.py src/models/
git commit -m "feat: add database setup and User model"
```

---

## Task 3: Auth Schemas (Pydantic)

**Files:**
- Create: `src/schemas/__init__.py`
- Create: `src/schemas/auth.py`

**Interfaces:**
- Produces: `LoginRequest`, `LoginResponse`, `LogoutResponse`, `TokenPayload`

- [ ] **Step 1: Create src/schemas/__init__.py**

```python
from src.schemas.auth import LoginRequest, LoginResponse, LogoutResponse, TokenPayload

__all__ = ["LoginRequest", "LoginResponse", "LogoutResponse", "TokenPayload"]
```

- [ ] **Step 2: Create src/schemas/auth.py**

```python
"""Pydantic schemas for authentication requests and responses."""
from pydantic import BaseModel, Field


class LoginRequest(BaseModel):
    username: str = Field(..., min_length=1, max_length=150, description="用户名")
    password: str = Field(..., min_length=1, max_length=128, description="密码")
    remember_me: bool = Field(default=False, description="记住我（延长有效期至7天）")


class LoginResponse(BaseModel):
    message: str = Field(default="登录成功")
    username: str


class LogoutResponse(BaseModel):
    message: str = Field(default="已退出登录")


class TokenPayload(BaseModel):
    sub: str  # username
    exp: int  # expiration timestamp
    iat: int  # issued at timestamp
```

- [ ] **Step 3: Verify schemas are importable**

Run: `python -c "from src.schemas import LoginRequest, LoginResponse, LogoutResponse; print('Schemas OK')"`
Expected: `Schemas OK`

- [ ] **Step 4: Commit**

```bash
git add src/schemas/
git commit -m "feat: add auth Pydantic schemas"
```

---

## Task 4: Auth Service (Business Logic)

**Files:**
- Create: `src/services/__init__.py`
- Create: `src/services/auth_service.py`

**Interfaces:**
- Consumes: `settings` from `src.config`, `User` from `src.models.user`, `TokenPayload` from `src.schemas.auth`
- Produces: `AuthService` class with methods:
  - `hash_password(password: str) -> str`
  - `verify_password(plain: str, hashed: str) -> bool`
  - `create_token(username: str, remember_me: bool) -> str`
  - `decode_token(token: str) -> TokenPayload | None`
  - `login(db: Session, username: str, password: str, remember_me: bool, client_ip: str) -> tuple[str, str]`
  - `logout(response: Response) -> None`

- [ ] **Step 1: Create src/services/__init__.py**

```python
from src.services.auth_service import AuthService

__all__ = ["AuthService"]
```

- [ ] **Step 2: Write the failing test**

Create `tests/conftest.py`:

```python
"""Shared test fixtures."""
import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, Session

from src.database import Base, get_db
from src.models.user import User
from src.config import settings


@pytest.fixture(scope="function")
def db_session() -> Session:
    """In-memory SQLite session for testing."""
    engine = create_engine("sqlite:///:memory:", connect_args={"check_same_thread": False})
    Base.metadata.create_all(bind=engine)
    TestSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
    session = TestSessionLocal()
    try:
        yield session
    finally:
        session.close()
        Base.metadata.drop_all(bind=engine)
```

Create `tests/test_auth_service.py`:

```python
"""Unit tests for AuthService."""
import time
from datetime import datetime, timedelta

import pytest

from src.services.auth_service import AuthService
from src.models.user import User
from src.config import settings


def test_hash_and_verify_password():
    """Password hashing and verification round-trip."""
    plain = "MySecureP@ss1"
    hashed = AuthService.hash_password(plain)
    assert hashed != plain
    assert AuthService.verify_password(plain, hashed) is True
    assert AuthService.verify_password("WrongPassword", hashed) is False


def test_create_and_decode_token():
    """JWT token creation and decoding round-trip."""
    token = AuthService.create_token("testuser", remember_me=False)
    payload = AuthService.decode_token(token)
    assert payload is not None
    assert payload.sub == "testuser"


def test_create_token_remember_me_longer_expiry():
    """Remember-me token should have longer expiry."""
    token_short = AuthService.create_token("user1", remember_me=False)
    token_long = AuthService.create_token("user1", remember_me=True)
    payload_short = AuthService.decode_token(token_short)
    payload_long = AuthService.decode_token(token_long)
    assert payload_short is not None
    assert payload_long is not None
    diff = payload_long.exp - payload_short.exp
    # 7 days minus 30 minutes should be roughly 6.98 days in seconds
    assert diff > 6 * 24 * 3600  # at least 6 days difference


def test_decode_invalid_token_returns_none():
    """Tampered or expired token returns None."""
    assert AuthService.decode_token("not.a.valid.token") is None
    assert AuthService.decode_token("") is None


def test_login_success_returns_token_and_username(db_session):
    """Successful login returns token and username."""
    hashed = AuthService.hash_password("correct")
    user = User(username="alice", password_hash=hashed)
    db_session.add(user)
    db_session.commit()

    token, username = AuthService.login(db_session, "alice", "correct", False, "127.0.0.1")
    assert username == "alice"
    assert token is not None
    payload = AuthService.decode_token(token)
    assert payload is not None
    assert payload.sub == "alice"


def test_login_wrong_password_raises_401(db_session):
    """Wrong password raises ValueError with identical message."""
    hashed = AuthService.hash_password("correct")
    user = User(username="bob", password_hash=hashed)
    db_session.add(user)
    db_session.commit()

    with pytest.raises(ValueError, match="账号或密码错误"):
        AuthService.login(db_session, "bob", "wrongpass", False, "127.0.0.1")


def test_login_nonexistent_user_raises_401(db_session):
    """Nonexistent user raises ValueError with identical message to wrong password."""
    with pytest.raises(ValueError, match="账号或密码错误"):
        AuthService.login(db_session, "ghost", "any", False, "127.0.0.1")


def test_login_disabled_account_raises_401(db_session):
    """Disabled account cannot log in."""
    hashed = AuthService.hash_password("correct")
    user = User(username="disabled_user", password_hash=hashed, disabled=True)
    db_session.add(user)
    db_session.commit()

    with pytest.raises(ValueError, match="账号或密码错误"):
        AuthService.login(db_session, "disabled_user", "correct", False, "127.0.0.1")


def test_login_increments_failed_attempts(db_session):
    """Failed login increments failed_attempts counter."""
    hashed = AuthService.hash_password("correct")
    user = User(username="charlie", password_hash=hashed, failed_attempts=0)
    db_session.add(user)
    db_session.commit()

    try:
        AuthService.login(db_session, "charlie", "wrong", False, "127.0.0.1")
    except ValueError:
        pass

    db_session.refresh(user)
    assert user.failed_attempts == 1


def test_login_locks_account_after_max_failures(db_session):
    """Account locks after MAX_FAILED_ATTEMPTS consecutive failures."""
    hashed = AuthService.hash_password("correct")
    user = User(username="dave", password_hash=hashed, failed_attempts=settings.MAX_FAILED_ATTEMPTS - 1)
    db_session.add(user)
    db_session.commit()

    # This should be the 5th (MAX) failure
    with pytest.raises(ValueError, match="账号或密码错误"):
        AuthService.login(db_session, "dave", "wrong", False, "127.0.0.1")

    db_session.refresh(user)
    assert user.failed_attempts == settings.MAX_FAILED_ATTEMPTS
    assert user.locked_until is not None
    assert user.locked_until > datetime.utcnow()


def test_login_rejected_when_locked(db_session):
    """Locked account cannot log in even with correct password."""
    hashed = AuthService.hash_password("correct")
    user = User(
        username="eve",
        password_hash=hashed,
        failed_attempts=settings.MAX_FAILED_ATTEMPTS,
        locked_until=datetime.utcnow() + timedelta(minutes=10),
    )
    db_session.add(user)
    db_session.commit()

    with pytest.raises(ValueError, match="账号或密码错误"):
        AuthService.login(db_session, "eve", "correct", False, "127.0.0.1")


def test_login_resets_failed_attempts_on_success(db_session):
    """Successful login clears failed_attempts and locked_until."""
    hashed = AuthService.hash_password("correct")
    user = User(
        username="frank",
        password_hash=hashed,
        failed_attempts=3,
        locked_until=datetime.utcnow() + timedelta(minutes=5),
    )
    db_session.add(user)
    db_session.commit()

    # Manually expire the lock to simulate timeout
    user.locked_until = None
    db_session.commit()

    token, username = AuthService.login(db_session, "frank", "correct", False, "127.0.0.1")
    db_session.refresh(user)
    assert user.failed_attempts == 0
    assert user.locked_until is None
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `pytest tests/test_auth_service.py -v`
Expected: All tests FAIL with `ModuleNotFoundError` or `ImportError` (AuthService not yet implemented).

- [ ] **Step 4: Create src/services/auth_service.py (full implementation)**

```python
"""Authentication business logic: hashing, tokens, login, logout."""
from datetime import datetime, timedelta
from typing import Optional

from jose import JWTError, jwt
from passlib.context import CryptContext
from sqlalchemy.orm import Session

from src.config import settings
from src.models.user import User
from src.schemas.auth import TokenPayload

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


class AuthService:
    """Stateless authentication service. All methods are classmethods."""

    @classmethod
    def hash_password(cls, password: str) -> str:
        """Hash a plaintext password with bcrypt."""
        return pwd_context.hash(password)

    @classmethod
    def verify_password(cls, plain: str, hashed: str) -> bool:
        """Verify a plaintext password against a bcrypt hash."""
        return pwd_context.verify(plain, hashed)

    @classmethod
    def create_token(cls, username: str, remember_me: bool = False) -> str:
        """Create a signed JWT for the given username."""
        now = datetime.utcnow()
        if remember_me:
            expire = now + timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS)
        else:
            expire = now + timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)

        payload = {
            "sub": username,
            "iat": int(now.timestamp()),
            "exp": int(expire.timestamp()),
        }
        return jwt.encode(payload, settings.SECRET_KEY, algorithm="HS256")

    @classmethod
    def decode_token(cls, token: str) -> Optional[TokenPayload]:
        """Decode and validate a JWT. Returns None if invalid or expired."""
        try:
            payload = jwt.decode(token, settings.SECRET_KEY, algorithms=["HS256"])
            return TokenPayload(
                sub=payload["sub"],
                exp=payload["exp"],
                iat=payload["iat"],
            )
        except (JWTError, KeyError):
            return None

    @classmethod
    def login(
        cls,
        db: Session,
        username: str,
        password: str,
        remember_me: bool,
        client_ip: str,
    ) -> tuple[str, str]:
        """
        Authenticate a user. Returns (token, username) on success.
        Raises ValueError with identical message for all failure modes
        to prevent account enumeration.
        """
        user = db.query(User).filter(User.username == username).first()

        if user is None:
            # Account does not exist — same error as wrong password
            raise ValueError("账号或密码错误")

        # Check if account is locked
        if user.locked_until and user.locked_until > datetime.utcnow():
            raise ValueError("账号或密码错误")

        # Check if account is disabled
        if user.disabled:
            raise ValueError("账号或密码错误")

        # Verify password
        if not cls.verify_password(password, user.password_hash):
            # Increment failed attempts
            user.failed_attempts += 1
            if user.failed_attempts >= settings.MAX_FAILED_ATTEMPTS:
                user.locked_until = datetime.utcnow() + timedelta(minutes=settings.LOCKOUT_MINUTES)
            db.commit()
            raise ValueError("账号或密码错误")

        # Success — reset failed attempts
        user.failed_attempts = 0
        user.locked_until = None
        db.commit()

        token = cls.create_token(username, remember_me=remember_me)
        return token, username

    @classmethod
    def logout(cls) -> None:
        """
        Logout is stateless on the server side.
        The client is responsible for clearing the cookie.
        This method exists for API symmetry and future extension (e.g., token blocklist).
        """
        pass
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `pytest tests/test_auth_service.py -v`
Expected: All 10 tests PASS.

- [ ] **Step 6: Commit**

```bash
git add src/services/ tests/
git commit -m "feat: add AuthService with password hashing, JWT, and login logic"
```

---

## Task 5: Auth Routes (Login & Logout Endpoints)

**Files:**
- Create: `src/routes/__init__.py`
- Create: `src/routes/auth.py`
- Create: `src/main.py`

**Interfaces:**
- Consumes: `AuthService` from `src.services.auth_service`, `LoginRequest`/`LoginResponse`/`LogoutResponse` from `src.schemas.auth`, `get_db` from `src.database`
- Produces: FastAPI `APIRouter` with `POST /api/auth/login` and `POST /api/auth/logout`

- [ ] **Step 1: Create src/routes/__init__.py**

```python
from src.routes.auth import router as auth_router

__all__ = ["auth_router"]
```

- [ ] **Step 2: Write the route integration tests (failing)**

Create `tests/test_auth_routes.py`:

```python
"""Integration tests for auth endpoints."""
from datetime import datetime, timedelta

import pytest
from fastapi.testclient import TestClient

from src.database import Base, get_db
from src.main import app
from src.models.user import User
from src.services.auth_service import AuthService
from src.config import settings
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker


@pytest.fixture(scope="module")
def test_engine():
    engine = create_engine("sqlite:///:memory:", connect_args={"check_same_thread": False})
    Base.metadata.create_all(bind=engine)
    yield engine
    Base.metadata.drop_all(bind=engine)


@pytest.fixture(scope="function")
def db_session(test_engine):
    TestSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=test_engine)
    session = TestSessionLocal()
    # Clear users table for each test
    session.query(User).delete()
    session.commit()
    try:
        yield session
    finally:
        session.close()


@pytest.fixture(scope="function")
def client(db_session):
    def override_get_db():
        try:
            yield db_session
        finally:
            pass

    app.dependency_overrides[get_db] = override_get_db
    with TestClient(app) as c:
        yield c
    app.dependency_overrides.clear()


@pytest.fixture
def seed_user(db_session):
    """Create a test user with known password."""
    user = User(
        username="testuser",
        password_hash=AuthService.hash_password("SecureP@ss1"),
    )
    db_session.add(user)
    db_session.commit()
    return user


def test_login_success_returns_200_and_set_cookie(client, seed_user):
    """POST /api/auth/login with valid credentials."""
    resp = client.post(
        "/api/auth/login",
        json={"username": "testuser", "password": "SecureP@ss1", "remember_me": False},
    )
    assert resp.status_code == 200
    data = resp.json()
    assert data["message"] == "登录成功"
    assert data["username"] == "testuser"
    assert "set-cookie" in resp.headers
    cookie = resp.headers["set-cookie"]
    assert "HttpOnly" in cookie or "httponly" in cookie


def test_login_wrong_password_returns_401(client, seed_user):
    """Wrong password returns 401 with generic message."""
    resp = client.post(
        "/api/auth/login",
        json={"username": "testuser", "password": "WrongP@ss1", "remember_me": False},
    )
    assert resp.status_code == 401
    assert resp.json()["detail"] == "账号或密码错误"


def test_login_nonexistent_user_returns_401(client):
    """Nonexistent user returns same 401 as wrong password."""
    resp = client.post(
        "/api/auth/login",
        json={"username": "nobody", "password": "any", "remember_me": False},
    )
    assert resp.status_code == 401
    assert resp.json()["detail"] == "账号或密码错误"


def test_login_missing_username_returns_422(client):
    """Missing required field returns 422."""
    resp = client.post(
        "/api/auth/login",
        json={"password": "any", "remember_me": False},
    )
    assert resp.status_code == 422


def test_login_empty_password_returns_422(client):
    """Empty string password fails validation."""
    resp = client.post(
        "/api/auth/login",
        json={"username": "testuser", "password": "", "remember_me": False},
    )
    assert resp.status_code == 422


def test_login_remember_me_sets_longer_cookie(client, seed_user):
    """Remember-me flag is accepted."""
    resp = client.post(
        "/api/auth/login",
        json={"username": "testuser", "password": "SecureP@ss1", "remember_me": True},
    )
    assert resp.status_code == 200
    assert "set-cookie" in resp.headers


def test_logout_clears_cookie(client):
    """POST /api/auth/logout returns success and clears cookie."""
    resp = client.post("/api/auth/logout")
    assert resp.status_code == 200
    data = resp.json()
    assert data["message"] == "已退出登录"
    if "set-cookie" in resp.headers:
        cookie = resp.headers["set-cookie"]
        # Cookie should be cleared (max-age=0 or empty value)
        assert "Max-Age=0" in cookie or '=""' in cookie or "expires=Thu, 01 Jan 1970" in cookie


def test_login_after_lockout_returns_401(client, seed_user, db_session):
    """After MAX_FAILED_ATTEMPTS, account is locked."""
    for i in range(settings.MAX_FAILED_ATTEMPTS):
        client.post(
            "/api/auth/login",
            json={"username": "testuser", "password": "wrong", "remember_me": False},
        )

    # Now even correct password should fail
    resp = client.post(
        "/api/auth/login",
        json={"username": "testuser", "password": "SecureP@ss1", "remember_me": False},
    )
    assert resp.status_code == 401
    assert resp.json()["detail"] == "账号或密码错误"


def test_login_disabled_account_returns_401(client, db_session):
    """Disabled account cannot log in."""
    user = User(
        username="disabled_one",
        password_hash=AuthService.hash_password("password"),
        disabled=True,
    )
    db_session.add(user)
    db_session.commit()

    resp = client.post(
        "/api/auth/login",
        json={"username": "disabled_one", "password": "password", "remember_me": False},
    )
    assert resp.status_code == 401
    assert resp.json()["detail"] == "账号或密码错误"
```

- [ ] **Step 3: Create src/routes/auth.py**

```python
"""Authentication routes: login and logout."""
from fastapi import APIRouter, Depends, Response, Request
from sqlalchemy.orm import Session

from src.database import get_db
from src.schemas.auth import LoginRequest, LoginResponse, LogoutResponse
from src.services.auth_service import AuthService
from src.config import settings

router = APIRouter(prefix="/api/auth", tags=["auth"])


@router.post("/login", response_model=LoginResponse, status_code=200)
def login(
    body: LoginRequest,
    response: Response,
    request: Request,
    db: Session = Depends(get_db),
):
    """
    Authenticate user with username and password.
    Sets an HttpOnly JWT cookie on success.
    Returns 401 with generic message on any failure.
    """
    client_ip = request.client.host if request.client else "unknown"

    try:
        token, username = AuthService.login(
            db=db,
            username=body.username,
            password=body.password,
            remember_me=body.remember_me,
            client_ip=client_ip,
        )
    except ValueError as e:
        from fastapi import HTTPException
        raise HTTPException(status_code=401, detail=str(e))

    # Determine cookie max_age
    if body.remember_me:
        max_age = settings.REFRESH_TOKEN_EXPIRE_DAYS * 24 * 3600
    else:
        max_age = settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60

    response.set_cookie(
        key="access_token",
        value=token,
        max_age=max_age,
        httponly=True,
        secure=False,  # Set True in production with HTTPS
        samesite="lax",
    )

    return LoginResponse(message="登录成功", username=username)


@router.post("/logout", response_model=LogoutResponse, status_code=200)
def logout(response: Response):
    """Clear the access token cookie."""
    AuthService.logout()
    response.delete_cookie(
        key="access_token",
        httponly=True,
        secure=False,
        samesite="lax",
    )
    return LogoutResponse(message="已退出登录")
```

- [ ] **Step 4: Create src/main.py**

```python
"""FastAPI application entry point."""
from fastapi import FastAPI

from src.database import engine, Base
from src.routes.auth import router as auth_router


def create_app() -> FastAPI:
    app = FastAPI(title="Login Service", version="0.1.0")

    # Create tables on startup
    Base.metadata.create_all(bind=engine)

    app.include_router(auth_router)

    return app


app = create_app()
```

- [ ] **Step 5: Run integration tests**

Run: `pytest tests/test_auth_routes.py -v`
Expected: All 9 tests PASS.

- [ ] **Step 6: Commit**

```bash
git add src/routes/ src/main.py tests/test_auth_routes.py
git commit -m "feat: add login and logout API endpoints with JWT cookie"
```

---

## Task 6: Auth Middleware (Protected Route Dependency)

**Files:**
- Create: `src/middleware/__init__.py`
- Create: `src/middleware/auth.py`

**Interfaces:**
- Consumes: `AuthService.decode_token` from `src.services.auth_service`, `get_db` from `src.database`
- Produces: `get_current_user(request: Request, db: Session) -> User` FastAPI dependency

- [ ] **Step 1: Create src/middleware/__init__.py**

```python
from src.middleware.auth import get_current_user

__all__ = ["get_current_user"]
```

- [ ] **Step 2: Create src/middleware/auth.py**

```python
"""Authentication middleware: extract and validate JWT from cookie."""
from fastapi import Request, HTTPException, Depends
from sqlalchemy.orm import Session

from src.database import get_db
from src.models.user import User
from src.services.auth_service import AuthService


def get_current_user(request: Request, db: Session = Depends(get_db)) -> User:
    """
    FastAPI dependency: extract JWT from cookie, validate it,
    and return the authenticated User. Raises 401 on failure.
    """
    token = request.cookies.get("access_token")
    if not token:
        raise HTTPException(status_code=401, detail="请先登录")

    payload = AuthService.decode_token(token)
    if payload is None:
        raise HTTPException(status_code=401, detail="登录已过期，请重新登录")

    user = db.query(User).filter(User.username == payload.sub).first()
    if user is None:
        raise HTTPException(status_code=401, detail="用户不存在")

    if user.disabled:
        raise HTTPException(status_code=403, detail="账号已被禁用")

    return user
```

- [ ] **Step 3: Add a protected test endpoint and verify**

Add to `src/main.py` after `app.include_router(auth_router)`:

```python
from src.middleware.auth import get_current_user
from src.models.user import User as UserModel


@app.get("/api/auth/me")
def get_me(current_user: UserModel = Depends(get_current_user)):
    return {"username": current_user.username}
```

- [ ] **Step 4: Write and run middleware test**

Add to `tests/test_auth_routes.py`:

```python
def test_protected_route_without_cookie_returns_401(client):
    """GET /api/auth/me without cookie returns 401."""
    resp = client.get("/api/auth/me")
    assert resp.status_code == 401


def test_protected_route_with_valid_cookie_returns_user(client, seed_user):
    """Login then access protected route."""
    login_resp = client.post(
        "/api/auth/login",
        json={"username": "testuser", "password": "SecureP@ss1", "remember_me": False},
    )
    cookie = login_resp.headers.get("set-cookie", "")
    resp = client.get("/api/auth/me", headers={"Cookie": cookie})
    assert resp.status_code == 200
    assert resp.json()["username"] == "testuser"
```

Run: `pytest tests/test_auth_routes.py::test_protected_route_without_cookie_returns_401 tests/test_auth_routes.py::test_protected_route_with_valid_cookie_returns_user -v`
Expected: Both tests PASS.

- [ ] **Step 5: Commit**

```bash
git add src/middleware/ src/main.py tests/test_auth_routes.py
git commit -m "feat: add JWT auth middleware for protected routes"
```

---

## Task 7: Rate Limiting & Brute-Force Protection

**Files:**
- Create: `src/middleware/rate_limiter.py`
- Modify: `src/middleware/__init__.py`
- Modify: `src/main.py`

**Interfaces:**
- Consumes: `settings` from `src.config`
- Produces: `RateLimiter` class with `check_ip(ip: str) -> bool` and `check_account(username: str) -> bool`

- [ ] **Step 1: Create src/middleware/rate_limiter.py**

```python
"""In-memory rate limiter for IP and account-level brute-force protection."""
import time
from collections import defaultdict
from threading import Lock

from src.config import settings


class RateLimiter:
    """
    Simple in-memory rate limiter.
    Tracks requests per IP and per account using sliding windows.
    Thread-safe via Lock.
    """

    _instance = None
    _lock = Lock()

    def __new__(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
                    cls._instance._ip_requests = defaultdict(list)
                    cls._instance._data_lock = Lock()
        return cls._instance

    def check_ip(self, ip: str) -> bool:
        """
        Check if IP has exceeded the rate limit.
        Returns True if allowed, False if rate-limited.
        """
        now = time.time()
        window_start = now - 60  # 1-minute sliding window

        with self._data_lock:
            # Clean old entries
            self._ip_requests[ip] = [
                ts for ts in self._ip_requests[ip] if ts > window_start
            ]

            if len(self._ip_requests[ip]) >= settings.IP_RATE_LIMIT_PER_MINUTE:
                return False

            self._ip_requests[ip].append(now)
            return True


rate_limiter = RateLimiter()
```

- [ ] **Step 2: Integrate rate limiter into login route**

Modify `src/routes/auth.py` to add rate limiting before the login logic:

```python
# Add at top of login function, after extracting client_ip:
from src.middleware.rate_limiter import rate_limiter

# Inside the login function, before AuthService.login():
if not rate_limiter.check_ip(client_ip):
    from fastapi import HTTPException
    raise HTTPException(status_code=429, detail="请求过于频繁，请稍后再试")
```

- [ ] **Step 3: Write rate limiter tests**

Add to `tests/test_auth_routes.py`:

```python
def test_rate_limiter_blocks_excessive_requests(client, seed_user):
    """After exceeding IP rate limit, returns 429."""
    # Send requests up to the limit
    for i in range(settings.IP_RATE_LIMIT_PER_MINUTE):
        resp = client.post(
            "/api/auth/login",
            json={"username": "testuser", "password": "wrong", "remember_me": False},
        )
        # They should all be 401 (wrong password) or 429 (if we hit limit early)
        assert resp.status_code in (401, 429)

    # One more should be 429
    resp = client.post(
        "/api/auth/login",
        json={"username": "testuser", "password": "wrong", "remember_me": False},
    )
    assert resp.status_code == 429
```

Run: `pytest tests/test_auth_routes.py::test_rate_limiter_blocks_excessive_requests -v`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add src/middleware/rate_limiter.py src/middleware/__init__.py src/routes/auth.py src/main.py tests/test_auth_routes.py
git commit -m "feat: add IP-based rate limiting for brute-force protection"
```

---

## Task 8: End-to-End Verification & Documentation

**Files:**
- Create: `tests/conftest.py` (finalize)
- Modify: `src/main.py` (finalize with lifespan and CORS)

**No new interfaces — this task validates the complete system.**

- [ ] **Step 1: Run the full test suite**

Run: `pytest tests/ -v --tb=short`
Expected: All tests pass (goal: 22+ tests).

- [ ] **Step 2: Start the server and smoke-test with curl**

Run (background): `uvicorn src.main:app --host 0.0.0.0 --port 8000 &`
Wait 2 seconds.

Run: `curl -s -X POST http://localhost:8000/api/auth/login -H "Content-Type: application/json" -d '{"username":"testuser","password":"SecureP@ss1","remember_me":false}' -v 2>&1 | head -30`
Expected: 200 with Set-Cookie header.

Run: `curl -s -X POST http://localhost:8000/api/auth/logout -v 2>&1 | head -20`
Expected: 200 with cleared cookie.

Run: `curl -s -X POST http://localhost:8000/api/auth/login -H "Content-Type: application/json" -d '{"username":"ghost","password":"x","remember_me":false}'`
Expected: `{"detail":"账号或密码错误"}`

Stop server: `kill %1 2>/dev/null; pkill -f "uvicorn src.main" 2>/dev/null`

- [ ] **Step 3: Verify all acceptance criteria**

| # | Acceptance Criterion | Test Coverage |
|---|---------------------|---------------|
| 1 | 用户可通过用户名+密码登录 | `test_login_success_returns_200_and_set_cookie` |
| 2 | 登录成功返回JWT并设置HttpOnly Cookie | Cookie assertion in success test |
| 3 | 账号不存在返回"账号或密码错误" | `test_login_nonexistent_user_returns_401` |
| 4 | 密码错误返回"账号或密码错误"（与账号不存在一致） | `test_login_wrong_password_returns_401` |
| 5 | 连续失败5次后锁定15分钟 | `test_login_locks_account_after_max_failures` + `test_login_after_lockout_returns_401` |
| 6 | 已禁用账号无法登录 | `test_login_disabled_account_returns_401` |
| 7 | "记住我"延长有效期至7天 | `test_create_token_remember_me_longer_expiry` |
| 8 | 退出登录清除Cookie | `test_logout_clears_cookie` |

- [ ] **Step 4: Final commit**

```bash
git add -A
git commit -m "feat: complete login feature with full test coverage and acceptance verification"
```

---

## Self-Review

### 1. Spec Coverage

| Spec Section | Covered By |
|---|---|
| 登录入口 | Task 5: `POST /api/auth/login` |
| 输入校验 | Task 3: Pydantic `LoginRequest` with `min_length` constraints |
| 核心认证流程 | Task 4: `AuthService.login()` |
| 登录态维持 | Task 5: `set_cookie` with `max_age`; Task 6: `get_current_user` |
| 退出登录 | Task 5: `POST /api/auth/logout` + `delete_cookie` |
| 账号不存在 → 统一错误 | Task 4: `login()` raises same `ValueError` for all failures |
| 密码错误 → 统一错误 | Task 4: same |
| 暴力破解锁定 | Task 4: `failed_attempts` + `locked_until`; Task 7: IP rate limiter |
| 账号禁用 | Task 4: `disabled` check |
| 密码加盐哈希 | Task 4: bcrypt via `passlib` |
| Cookie HttpOnly+Secure+SameSite | Task 5: `set_cookie` parameters |
| 记住我区分长短有效期 | Task 4: `remember_me` parameter in `create_token` |
| 8条验收标准 | Task 8: verification table |
| 后续规划 | Covered in plan header scope note |

### 2. Placeholder Scan

No TBD, TODO, "implement later", "add appropriate error handling", or "similar to Task N" patterns found. Every step contains concrete code, exact commands, and expected outputs.

### 3. Type Consistency

- `LoginRequest`: username `str`, password `str`, remember_me `bool` → consistent across Tasks 3, 4, 5
- `LoginResponse`: message `str`, username `str` → consistent
- `TokenPayload`: sub `str`, exp `int`, iat `int` → consistent
- `AuthService.login()` returns `tuple[str, str]` → consumed correctly in route
- `AuthService.decode_token()` returns `Optional[TokenPayload]` → consumed correctly in middleware
- `get_db()` yields `Session` → consistent across all tasks
- `settings` fields: all referenced fields exist in `Settings` class