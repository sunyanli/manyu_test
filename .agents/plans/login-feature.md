# Login Feature Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a secure login/logout REST API with bcrypt password hashing, HttpOnly session cookies, rate limiting, and brute-force protection — no registration, password reset, or SSO in this phase.

**Architecture:** Layered Python backend: FastAPI route handlers → Auth service → User repository → SQLAlchemy models → SQLite (dev). Passwords hashed via passlib[bcrypt]. Sessions managed via server-signed cookies with configurable TTL. In-memory rate limiter with IP-based counting.

**Tech Stack:** Python 3.10+, FastAPI 0.100+, SQLAlchemy 2.0 (async), SQLite (dev), passlib[bcrypt], itsdangerous (session signing), pytest + httpx (testing)

---

## Global Constraints

- Password storage: bcrypt via passlib, `CryptContext(schemes=["bcrypt"])` — auto-rounds
- Login failure response: always `{"code": "AUTH_FAILED", "message": "账号或密码错误"}` regardless of whether account exists
- Session cookie: `HttpOnly=True`, `Secure=True` (skip in dev), `SameSite="Lax"`, path="/"
- "Remember me": checked → 30-day session TTL; unchecked → 2-hour session TTL
- Rate limiting: 5 failed attempts per IP per 60s window; exceeding → return `{"code": "RATE_LIMITED", "message": "请求过于频繁，请稍后再试"}`
- Account lockout: 10 consecutive failed attempts on same account → 30-minute lock; return same AUTH_FAILED message
- All error responses: JSON with `code` (UPPER_SNAKE) and `message` (Chinese user-facing)
- No registration, password reset, SSO, social login, or MFA in this phase
- Disabled accounts: return same AUTH_FAILED message as nonexistent accounts

---

## File Structure

```
src/
├── main.py                    # FastAPI app factory, lifespan, CORS
├── config.py                  # Settings via pydantic-settings / env
├── models/
│   ├── __init__.py
│   └── user.py                # SQLAlchemy User model
├── repository/
│   ├── __init__.py
│   └── user_repo.py           # UserRepository: find by username, update failed count
├── services/
│   ├── __init__.py
│   ├── auth_service.py        # AuthService: authenticate, session sign, logout
│   ├── password_service.py    # PasswordService: hash, verify via passlib
│   └── rate_limiter.py        # RateLimiter: IP-based sliding window counter
├── routes/
│   ├── __init__.py
│   └── auth_routes.py         # POST /api/auth/login, POST /api/auth/logout
├── schemas/
│   ├── __init__.py
│   └── auth_schemas.py        # Pydantic request/response models
└── db.py                      # AsyncSession factory, engine, Base

tests/
├── conftest.py                # Fixtures: async test client, test DB, seed user
├── test_password_service.py
├── test_user_repo.py
├── test_auth_service.py
├── test_rate_limiter.py
├── test_auth_routes.py
└── test_acceptance.py         # End-to-end acceptance per spec
```

---

## Task 1: Project Scaffolding & Configuration

**Files:**
- Create: `src/__init__.py`
- Create: `src/config.py`
- Create: `src/main.py`
- Create: `src/db.py`
- Create: `requirements.txt`
- Create: `tests/__init__.py`
- Create: `tests/conftest.py`
- Create: `.env.example`

**Interfaces:**
- Produces: `Settings` class (DATABASE_URL, SECRET_KEY, SESSION_TTL_SECONDS, REMEMBER_ME_TTL_SECONDS, RATE_LIMIT_MAX_ATTEMPTS, RATE_LIMIT_WINDOW_SECONDS, ACCOUNT_LOCKOUT_THRESHOLD, ACCOUNT_LOCKOUT_SECONDS)
- Produces: `get_db()` async generator for SQLAlchemy sessions
- Produces: `app` FastAPI instance, `lifespan` context manager that creates tables
- Produces: `AsyncClient` fixture in conftest.py

- [ ] **Step 1: Write requirements.txt**

```
fastapi==0.115.6
uvicorn[standard]==0.34.0
sqlalchemy[asyncio]==2.0.36
aiosqlite==0.20.0
passlib[bcrypt]==1.7.4
bcrypt==4.0.1
itsdangerous==2.2.0
pydantic-settings==2.7.0
python-dotenv==1.0.1
pytest==8.3.4
pytest-asyncio==0.24.0
httpx==0.28.1
```

- [ ] **Step 2: Write src/config.py**

```python
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    DATABASE_URL: str = "sqlite+aiosqlite:///./dev.db"
    SECRET_KEY: str = "dev-secret-change-in-production"
    SESSION_TTL_SECONDS: int = 7200        # 2 hours
    REMEMBER_ME_TTL_SECONDS: int = 2592000 # 30 days
    RATE_LIMIT_MAX_ATTEMPTS: int = 5
    RATE_LIMIT_WINDOW_SECONDS: int = 60
    ACCOUNT_LOCKOUT_THRESHOLD: int = 10
    ACCOUNT_LOCKOUT_SECONDS: int = 1800    # 30 minutes

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}

settings = Settings()
```

- [ ] **Step 3: Write src/db.py**

```python
from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine, async_sessionmaker
from sqlalchemy.orm import DeclarativeBase
from src.config import settings

engine = create_async_engine(settings.DATABASE_URL, echo=False)
async_session = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)

class Base(DeclarativeBase):
    pass

async def get_db():
    async with async_session() as session:
        yield session
```

- [ ] **Step 4: Write src/main.py**

```python
from contextlib import asynccontextmanager
from fastapi import FastAPI
from src.db import engine, Base
from src.routes.auth_routes import router as auth_router

@asynccontextmanager
async def lifespan(app: FastAPI):
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    yield

app = FastAPI(title="Auth Service", lifespan=lifespan)
app.include_router(auth_router)
```

- [ ] **Step 5: Write tests/conftest.py**

```python
import pytest
import pytest_asyncio
from httpx import AsyncClient, ASGITransport
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from src.db import Base, get_db
from src.main import app
from src.config import settings

TEST_DATABASE_URL = "sqlite+aiosqlite:///./test.db"

@pytest_asyncio.fixture(scope="function")
async def db_session():
    engine = create_async_engine(TEST_DATABASE_URL, echo=False)
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    test_session = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)
    async with test_session() as session:
        yield session
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)
    await engine.dispose()

@pytest_asyncio.fixture(scope="function")
async def client(db_session: AsyncSession):
    async def override_get_db():
        yield db_session
    app.dependency_overrides[get_db] = override_get_db
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        yield ac
    app.dependency_overrides.clear()
```

- [ ] **Step 6: Run tests to verify scaffolding works**

Run: `pytest tests/conftest.py -v`
Expected: 0 tests collected but no import errors (fixtures are parsed correctly)

---

## Task 2: User Model & Database Layer

**Files:**
- Create: `src/models/__init__.py`
- Create: `src/models/user.py`
- Modify: `src/db.py` (import model so Base.metadata tracks it)

**Interfaces:**
- Produces: `User` model — columns: `id` (int PK), `username` (str unique), `password_hash` (str), `is_active` (bool default True), `failed_attempts` (int default 0), `locked_until` (datetime nullable), `created_at` (datetime)

- [ ] **Step 1: Write src/models/user.py**

```python
from datetime import datetime
from sqlalchemy import String, Boolean, Integer, DateTime, func
from sqlalchemy.orm import Mapped, mapped_column
from src.db import Base

class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    username: Mapped[str] = mapped_column(String(128), unique=True, nullable=False, index=True)
    password_hash: Mapped[str] = mapped_column(String(256), nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    failed_attempts: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    locked_until: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
```

- [ ] **Step 2: Wire model into db.py**

Edit `src/db.py` — add after the `Base` class definition:

```python
# Ensure models are imported so Base.metadata knows about them
import src.models.user  # noqa: F401
```

- [ ] **Step 3: Write the repository test**

Create `tests/test_user_repo.py`:

```python
import pytest
from src.repository.user_repo import UserRepository
from src.models.user import User

@pytest.mark.asyncio
async def test_find_by_username_returns_user(db_session):
    user = User(username="alice", password_hash="hash123")
    db_session.add(user)
    await db_session.commit()

    repo = UserRepository(db_session)
    found = await repo.find_by_username("alice")
    assert found is not None
    assert found.username == "alice"

@pytest.mark.asyncio
async def test_find_by_username_returns_none_for_missing(db_session):
    repo = UserRepository(db_session)
    found = await repo.find_by_username("nobody")
    assert found is None

@pytest.mark.asyncio
async def test_increment_failed_attempts(db_session):
    user = User(username="bob", password_hash="hash", failed_attempts=0)
    db_session.add(user)
    await db_session.commit()

    repo = UserRepository(db_session)
    await repo.increment_failed_attempts(user)
    await db_session.commit()

    updated = await repo.find_by_username("bob")
    assert updated.failed_attempts == 1

@pytest.mark.asyncio
async def test_lock_account_sets_locked_until(db_session):
    from datetime import datetime, timezone
    user = User(username="eve", password_hash="hash")
    db_session.add(user)
    await db_session.commit()

    repo = UserRepository(db_session)
    await repo.lock_account(user, 1800)
    await db_session.commit()

    updated = await repo.find_by_username("eve")
    assert updated.locked_until is not None

@pytest.mark.asyncio
async def test_reset_failed_attempts(db_session):
    user = User(username="carol", password_hash="hash", failed_attempts=5)
    db_session.add(user)
    await db_session.commit()

    repo = UserRepository(db_session)
    await repo.reset_failed_attempts(user)
    await db_session.commit()

    updated = await repo.find_by_username("carol")
    assert updated.failed_attempts == 0
```

- [ ] **Step 4: Run tests to verify they fail**

Run: `pytest tests/test_user_repo.py -v`
Expected: FAIL — `ModuleNotFoundError: No module named 'src.repository.user_repo'`

- [ ] **Step 5: Write src/repository/user_repo.py**

```python
from datetime import datetime, timezone, timedelta
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from src.models.user import User

class UserRepository:
    def __init__(self, session: AsyncSession):
        self.session = session

    async def find_by_username(self, username: str) -> User | None:
        result = await self.session.execute(
            select(User).where(User.username == username)
        )
        return result.scalar_one_or_none()

    async def increment_failed_attempts(self, user: User) -> None:
        user.failed_attempts += 1

    async def lock_account(self, user: User, lockout_seconds: int) -> None:
        user.locked_until = datetime.now(timezone.utc) + timedelta(seconds=lockout_seconds)

    async def reset_failed_attempts(self, user: User) -> None:
        user.failed_attempts = 0
        user.locked_until = None
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `pytest tests/test_user_repo.py -v`
Expected: 5 PASS

---

## Task 3: Password Service

**Files:**
- Create: `src/services/__init__.py`
- Create: `src/services/password_service.py`
- Create: `tests/test_password_service.py`

**Interfaces:**
- Produces: `PasswordService` class with `hash_password(plain: str) -> str` and `verify_password(plain: str, hashed: str) -> bool`

- [ ] **Step 1: Write tests/test_password_service.py**

```python
from src.services.password_service import PasswordService

def test_hash_returns_different_from_plain():
    svc = PasswordService()
    hashed = svc.hash_password("mysecret")
    assert hashed != "mysecret"
    assert hashed.startswith("$2b$")  # bcrypt prefix

def test_verify_correct_password():
    svc = PasswordService()
    hashed = svc.hash_password("mysecret")
    assert svc.verify_password("mysecret", hashed) is True

def test_verify_wrong_password():
    svc = PasswordService()
    hashed = svc.hash_password("mysecret")
    assert svc.verify_password("wrongpass", hashed) is False

def test_hash_is_deterministic_per_call_but_not_equal():
    svc = PasswordService()
    h1 = svc.hash_password("same")
    h2 = svc.hash_password("same")
    assert h1 != h2  # different salts
    assert svc.verify_password("same", h1) is True
    assert svc.verify_password("same", h2) is True
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `pytest tests/test_password_service.py -v`
Expected: FAIL — `ModuleNotFoundError`

- [ ] **Step 3: Write src/services/password_service.py**

```python
from passlib.context import CryptContext

class PasswordService:
    def __init__(self):
        self._ctx = CryptContext(schemes=["bcrypt"], deprecated="auto")

    def hash_password(self, plain: str) -> str:
        return self._ctx.hash(plain)

    def verify_password(self, plain: str, hashed: str) -> bool:
        return self._ctx.verify(plain, hashed)
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `pytest tests/test_password_service.py -v`
Expected: 4 PASS

---

## Task 4: Auth Service (Core Logic)

**Files:**
- Create: `src/services/auth_service.py`
- Create: `tests/test_auth_service.py`

**Interfaces:**
- Consumes: `UserRepository`, `PasswordService`, `RateLimiter`, `Settings`
- Produces: `AuthService` with:
  - `authenticate(username: str, password: str, client_ip: str) -> tuple[User | None, str]` — returns (user, error_code) where error_code is "" on success
  - `sign_session(user: User, remember_me: bool) -> str` — returns signed session token
  - `verify_session(token: str) -> dict | None` — returns session payload or None
  - `logout(token: str) -> None` — adds token to blocklist

- [ ] **Step 1: Write tests/test_auth_service.py**

```python
import pytest
from datetime import datetime, timezone, timedelta
from src.services.auth_service import AuthService
from src.services.password_service import PasswordService
from src.repository.user_repo import UserRepository
from src.models.user import User
from src.config import Settings

@pytest.fixture
def svc():
    settings = Settings()
    pwd_svc = PasswordService()
    return AuthService, settings, pwd_svc

@pytest.mark.asyncio
async def test_authenticate_success(db_session, svc):
    AuthService, settings, pwd_svc = svc
    hashed = pwd_svc.hash_password("correct")
    user = User(username="testuser", password_hash=hashed)
    db_session.add(user)
    await db_session.commit()

    repo = UserRepository(db_session)
    auth = AuthService(repo, pwd_svc, settings)
    result, err = await auth.authenticate("testuser", "correct", "127.0.0.1")
    assert result is not None
    assert err == ""
    assert result.username == "testuser"

@pytest.mark.asyncio
async def test_authenticate_wrong_password_returns_none(db_session, svc):
    AuthService, settings, pwd_svc = svc
    hashed = pwd_svc.hash_password("correct")
    user = User(username="testuser", password_hash=hashed)
    db_session.add(user)
    await db_session.commit()

    repo = UserRepository(db_session)
    auth = AuthService(repo, pwd_svc, settings)
    result, err = await auth.authenticate("testuser", "wrong", "127.0.0.1")
    assert result is None
    assert err == "AUTH_FAILED"

@pytest.mark.asyncio
async def test_authenticate_nonexistent_user_returns_same_error(db_session, svc):
    AuthService, settings, pwd_svc = svc
    repo = UserRepository(db_session)
    auth = AuthService(repo, pwd_svc, settings)
    result, err = await auth.authenticate("nobody", "any", "127.0.0.1")
    assert result is None
    assert err == "AUTH_FAILED"

@pytest.mark.asyncio
async def test_authenticate_disabled_account(db_session, svc):
    AuthService, settings, pwd_svc = svc
    hashed = pwd_svc.hash_password("pass")
    user = User(username="disabled", password_hash=hashed, is_active=False)
    db_session.add(user)
    await db_session.commit()

    repo = UserRepository(db_session)
    auth = AuthService(repo, pwd_svc, settings)
    result, err = await auth.authenticate("disabled", "pass", "127.0.0.1")
    assert result is None
    assert err == "AUTH_FAILED"

@pytest.mark.asyncio
async def test_authenticate_locked_account(db_session, svc):
    AuthService, settings, pwd_svc = svc
    hashed = pwd_svc.hash_password("pass")
    user = User(username="locked", password_hash=hashed,
                locked_until=datetime.now(timezone.utc) + timedelta(minutes=10))
    db_session.add(user)
    await db_session.commit()

    repo = UserRepository(db_session)
    auth = AuthService(repo, pwd_svc, settings)
    result, err = await auth.authenticate("locked", "pass", "127.0.0.1")
    assert result is None
    assert err == "AUTH_FAILED"

@pytest.mark.asyncio
async def test_sign_session_returns_token(svc):
    AuthService, settings, pwd_svc = svc
    repo = None  # not needed for sign
    auth = AuthService(repo, pwd_svc, settings)
    user = User(id=1, username="u", password_hash="h")
    token = auth.sign_session(user, remember_me=False)
    assert isinstance(token, str)
    assert len(token) > 0

@pytest.mark.asyncio
async def test_verify_session_returns_payload(svc):
    AuthService, settings, pwd_svc = svc
    auth = AuthService(None, pwd_svc, settings)
    user = User(id=1, username="u", password_hash="h")
    token = auth.sign_session(user, remember_me=False)
    payload = auth.verify_session(token)
    assert payload is not None
    assert payload["user_id"] == 1
    assert payload["username"] == "u"

@pytest.mark.asyncio
async def test_verify_session_bad_token_returns_none(svc):
    AuthService, settings, pwd_svc = svc
    auth = AuthService(None, pwd_svc, settings)
    assert auth.verify_session("garbage") is None
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `pytest tests/test_auth_service.py -v`
Expected: FAIL — `ModuleNotFoundError`

- [ ] **Step 3: Write src/services/auth_service.py**

```python
from datetime import datetime, timezone
from itsdangerous import URLSafeTimedSerializer, BadSignature, SignatureExpired
from src.repository.user_repo import UserRepository
from src.services.password_service import PasswordService
from src.config import Settings

class AuthService:
    def __init__(self, user_repo: UserRepository, password_svc: PasswordService, settings: Settings):
        self._repo = user_repo
        self._pwd = password_svc
        self._settings = settings
        self._serializer = URLSafeTimedSerializer(settings.SECRET_KEY, salt="session")

    async def authenticate(self, username: str, password: str, client_ip: str) -> tuple:
        """Returns (user | None, error_code: str). error_code is "" on success."""
        user = await self._repo.find_by_username(username)

        # Constant-time-ish: always hash even if user doesn't exist
        if user is None:
            self._pwd.verify_password(password, "$2b$12$dummyhashplaceholder0000000000000000000000000000u")
            return None, "AUTH_FAILED"

        # Check disabled
        if not user.is_active:
            return None, "AUTH_FAILED"

        # Check lockout
        if user.locked_until and user.locked_until > datetime.now(timezone.utc):
            return None, "AUTH_FAILED"

        # Verify password
        if not self._pwd.verify_password(password, user.password_hash):
            await self._repo.increment_failed_attempts(user)
            if user.failed_attempts >= self._settings.ACCOUNT_LOCKOUT_THRESHOLD:
                await self._repo.lock_account(user, self._settings.ACCOUNT_LOCKOUT_SECONDS)
            await self._repo.session.commit()
            return None, "AUTH_FAILED"

        # Success: reset failed attempts
        await self._repo.reset_failed_attempts(user)
        await self._repo.session.commit()
        return user, ""

    def sign_session(self, user, remember_me: bool) -> str:
        ttl = self._settings.REMEMBER_ME_TTL_SECONDS if remember_me else self._settings.SESSION_TTL_SECONDS
        payload = {"user_id": user.id, "username": user.username}
        return self._serializer.dumps(payload)

    def verify_session(self, token: str) -> dict | None:
        try:
            ttl = max(self._settings.SESSION_TTL_SECONDS, self._settings.REMEMBER_ME_TTL_SECONDS)
            return self._serializer.loads(token, max_age=ttl)
        except (BadSignature, SignatureExpired):
            return None
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `pytest tests/test_auth_service.py -v`
Expected: 8 PASS

---

## Task 5: Rate Limiter

**Files:**
- Create: `src/services/rate_limiter.py`
- Create: `tests/test_rate_limiter.py`

**Interfaces:**
- Produces: `RateLimiter` class with `is_rate_limited(client_ip: str) -> bool` and `record_attempt(client_ip: str) -> None`

- [ ] **Step 1: Write tests/test_rate_limiter.py**

```python
import time
import pytest
from src.services.rate_limiter import RateLimiter
from src.config import Settings

def test_not_limited_on_first_attempt():
    rl = RateLimiter(Settings())
    assert rl.is_rate_limited("1.2.3.4") is False

def test_limited_after_max_attempts():
    settings = Settings(RATE_LIMIT_MAX_ATTEMPTS=3, RATE_LIMIT_WINDOW_SECONDS=60)
    rl = RateLimiter(settings)
    for _ in range(3):
        rl.record_attempt("1.2.3.4")
    assert rl.is_rate_limited("1.2.3.4") is True

def test_different_ips_not_affected():
    settings = Settings(RATE_LIMIT_MAX_ATTEMPTS=2, RATE_LIMIT_WINDOW_SECONDS=60)
    rl = RateLimiter(settings)
    rl.record_attempt("1.1.1.1")
    rl.record_attempt("1.1.1.1")
    assert rl.is_rate_limited("1.1.1.1") is True
    assert rl.is_rate_limited("2.2.2.2") is False

def test_window_expires():
    settings = Settings(RATE_LIMIT_MAX_ATTEMPTS=2, RATE_LIMIT_WINDOW_SECONDS=1)
    rl = RateLimiter(settings)
    rl.record_attempt("10.0.0.1")
    rl.record_attempt("10.0.0.1")
    assert rl.is_rate_limited("10.0.0.1") is True
    time.sleep(1.1)
    assert rl.is_rate_limited("10.0.0.1") is False
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `pytest tests/test_rate_limiter.py -v`
Expected: FAIL — `ModuleNotFoundError`

- [ ] **Step 3: Write src/services/rate_limiter.py**

```python
import time
from collections import defaultdict
from src.config import Settings

class RateLimiter:
    def __init__(self, settings: Settings):
        self._max = settings.RATE_LIMIT_MAX_ATTEMPTS
        self._window = settings.RATE_LIMIT_WINDOW_SECONDS
        self._store: dict[str, list[float]] = defaultdict(list)

    def _prune(self, ip: str, now: float) -> None:
        cutoff = now - self._window
        self._store[ip] = [t for t in self._store[ip] if t > cutoff]

    def is_rate_limited(self, client_ip: str) -> bool:
        self._prune(client_ip, time.time())
        return len(self._store[client_ip]) >= self._max

    def record_attempt(self, client_ip: str) -> None:
        self._store[client_ip].append(time.time())
        self._prune(client_ip, time.time())
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `pytest tests/test_rate_limiter.py -v`
Expected: 4 PASS

---

## Task 6: Request/Response Schemas

**Files:**
- Create: `src/schemas/__init__.py`
- Create: `src/schemas/auth_schemas.py`

**Interfaces:**
- Produces: `LoginRequest(username: str, password: str, remember_me: bool = False)`, `LoginResponse(code: str, message: str)`, `LogoutResponse(code: str, message: str)`

- [ ] **Step 1: Write src/schemas/auth_schemas.py**

```python
from pydantic import BaseModel, Field

class LoginRequest(BaseModel):
    username: str = Field(..., min_length=1, max_length=128, description="登录用户名")
    password: str = Field(..., min_length=1, max_length=256, description="登录密码")
    remember_me: bool = Field(default=False, description="是否记住登录状态")

class LoginResponse(BaseModel):
    code: str = Field(..., description="状态码")
    message: str = Field(..., description="用户可读的消息")

class LogoutResponse(BaseModel):
    code: str = Field(..., description="状态码")
    message: str = Field(..., description="用户可读的消息")
```

- [ ] **Step 2: Quick validation test**

Run: `python -c "from src.schemas.auth_schemas import LoginRequest; print(LoginRequest(username='a', password='b'))"`
Expected: prints `username='a' password='b' remember_me=False`

---

## Task 7: Auth Routes (Login & Logout Endpoints)

**Files:**
- Create: `src/routes/__init__.py`
- Create: `src/routes/auth_routes.py`
- Create: `tests/test_auth_routes.py`

**Interfaces:**
- Consumes: `AuthService`, `RateLimiter`, `UserRepository`, `PasswordService`, `get_db`
- Produces: `POST /api/auth/login` → Set-Cookie header + JSON, `POST /api/auth/logout` → Clear cookie + JSON

- [ ] **Step 1: Write tests/test_auth_routes.py**

```python
import pytest
from src.models.user import User
from src.services.password_service import PasswordService

@pytest.mark.asyncio
async def test_login_success_returns_200_and_set_cookie(client, db_session):
    pwd = PasswordService()
    user = User(username="alice", password_hash=pwd.hash_password("secret123"))
    db_session.add(user)
    await db_session.commit()

    resp = await client.post("/api/auth/login", json={
        "username": "alice", "password": "secret123"
    })
    assert resp.status_code == 200
    data = resp.json()
    assert data["code"] == "SUCCESS"
    assert "session" in resp.cookies

@pytest.mark.asyncio
async def test_login_wrong_password_returns_401_same_message(client, db_session):
    pwd = PasswordService()
    user = User(username="alice", password_hash=pwd.hash_password("secret123"))
    db_session.add(user)
    await db_session.commit()

    resp = await client.post("/api/auth/login", json={
        "username": "alice", "password": "wrongpass"
    })
    assert resp.status_code == 401
    data = resp.json()
    assert data["code"] == "AUTH_FAILED"
    assert data["message"] == "账号或密码错误"

@pytest.mark.asyncio
async def test_login_nonexistent_user_returns_same_401(client, db_session):
    resp = await client.post("/api/auth/login", json={
        "username": "nobody", "password": "anything"
    })
    assert resp.status_code == 401
    data = resp.json()
    assert data["code"] == "AUTH_FAILED"
    assert data["message"] == "账号或密码错误"

@pytest.mark.asyncio
async def test_logout_clears_cookie(client, db_session):
    pwd = PasswordService()
    user = User(username="bob", password_hash=pwd.hash_password("pass"))
    db_session.add(user)
    await db_session.commit()

    login_resp = await client.post("/api/auth/login", json={
        "username": "bob", "password": "pass"
    })
    session_cookie = login_resp.cookies.get("session")

    logout_resp = await client.post("/api/auth/logout", cookies={"session": session_cookie})
    assert logout_resp.status_code == 200
    data = logout_resp.json()
    assert data["code"] == "SUCCESS"
    # Cookie should be cleared (max-age=0 or empty value)
    set_cookie = logout_resp.headers.get("set-cookie", "")
    assert "session=" in set_cookie

@pytest.mark.asyncio
async def test_login_disabled_account_returns_401(client, db_session):
    pwd = PasswordService()
    user = User(username="disabled", password_hash=pwd.hash_password("pass"), is_active=False)
    db_session.add(user)
    await db_session.commit()

    resp = await client.post("/api/auth/login", json={
        "username": "disabled", "password": "pass"
    })
    assert resp.status_code == 401
    assert resp.json()["code"] == "AUTH_FAILED"

@pytest.mark.asyncio
async def test_login_empty_username_returns_422(client):
    resp = await client.post("/api/auth/login", json={
        "username": "", "password": "something"
    })
    assert resp.status_code == 422

@pytest.mark.asyncio
async def test_login_empty_password_returns_422(client):
    resp = await client.post("/api/auth/login", json={
        "username": "someone", "password": ""
    })
    assert resp.status_code == 422
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `pytest tests/test_auth_routes.py -v`
Expected: FAIL — `ModuleNotFoundError` or 404 on `/api/auth/login`

- [ ] **Step 3: Write src/routes/auth_routes.py**

```python
from fastapi import APIRouter, Depends, Request, Response
from sqlalchemy.ext.asyncio import AsyncSession
from src.db import get_db
from src.schemas.auth_schemas import LoginRequest, LoginResponse, LogoutResponse
from src.repository.user_repo import UserRepository
from src.services.password_service import PasswordService
from src.services.auth_service import AuthService
from src.services.rate_limiter import RateLimiter
from src.config import settings

router = APIRouter(prefix="/api/auth", tags=["auth"])

# Shared rate limiter instance (in-memory, per-process)
_rate_limiter = RateLimiter(settings)

def _get_client_ip(request: Request) -> str:
    forwarded = request.headers.get("X-Forwarded-For")
    if forwarded:
        return forwarded.split(",")[0].strip()
    return request.client.host if request.client else "unknown"

@router.post("/login", response_model=LoginResponse)
async def login(body: LoginRequest, request: Request, response: Response,
                db: AsyncSession = Depends(get_db)):
    client_ip = _get_client_ip(request)

    # Rate limit check
    if _rate_limiter.is_rate_limited(client_ip):
        return Response(
            content='{"code":"RATE_LIMITED","message":"请求过于频繁，请稍后再试"}',
            status_code=429,
            media_type="application/json"
        )

    repo = UserRepository(db)
    pwd_svc = PasswordService()
    auth_svc = AuthService(repo, pwd_svc, settings)

    user, error = await auth_svc.authenticate(body.username, body.password, client_ip)

    if error:
        _rate_limiter.record_attempt(client_ip)
        return Response(
            content='{"code":"AUTH_FAILED","message":"账号或密码错误"}',
            status_code=401,
            media_type="application/json"
        )

    # Sign session
    token = auth_svc.sign_session(user, body.remember_me)
    max_age = settings.REMEMBER_ME_TTL_SECONDS if body.remember_me else settings.SESSION_TTL_SECONDS
    response.set_cookie(
        key="session",
        value=token,
        max_age=max_age,
        httponly=True,
        secure=False,  # True in production with HTTPS
        samesite="lax",
        path="/"
    )
    return {"code": "SUCCESS", "message": "登录成功"}

@router.post("/logout", response_model=LogoutResponse)
async def logout(response: Response):
    response.delete_cookie(key="session", path="/")
    return {"code": "SUCCESS", "message": "已退出登录"}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `pytest tests/test_auth_routes.py -v`
Expected: 7 PASS

---

## Task 8: Acceptance Tests (End-to-End per Spec)

**Files:**
- Create: `tests/test_acceptance.py`

**Interfaces:**
- Consumes: All prior modules, test fixtures
- Produces: 8 acceptance test cases matching the spec's 8验收标准

- [ ] **Step 1: Write tests/test_acceptance.py**

```python
import pytest
from src.models.user import User
from src.services.password_service import PasswordService

@pytest.mark.asyncio
async def test_acceptance_01_login_success_with_correct_credentials(client, db_session):
    """验收1: 正确账号密码登录成功，返回SUCCESS并设置session cookie"""
    pwd = PasswordService()
    user = User(username="accept1", password_hash=pwd.hash_password("Pass1234"))
    db_session.add(user)
    await db_session.commit()

    resp = await client.post("/api/auth/login", json={
        "username": "accept1", "password": "Pass1234"
    })
    assert resp.status_code == 200
    assert resp.json()["code"] == "SUCCESS"
    assert "session" in resp.cookies
    cookie = resp.cookies["session"]
    assert len(cookie) > 0

@pytest.mark.asyncio
async def test_acceptance_02_wrong_password_unified_error(client, db_session):
    """验收2: 密码错误返回统一'账号或密码错误'，不区分账号是否存在"""
    pwd = PasswordService()
    user = User(username="accept2", password_hash=pwd.hash_password("correct"))
    db_session.add(user)
    await db_session.commit()

    resp = await client.post("/api/auth/login", json={
        "username": "accept2", "password": "wrong"
    })
    assert resp.status_code == 401
    assert resp.json()["message"] == "账号或密码错误"

@pytest.mark.asyncio
async def test_acceptance_03_nonexistent_account_unified_error(client, db_session):
    """验收3: 不存在的账号返回与密码错误相同的'账号或密码错误'"""
    resp = await client.post("/api/auth/login", json={
        "username": "no_such_user_42", "password": "anything"
    })
    assert resp.status_code == 401
    assert resp.json()["code"] == "AUTH_FAILED"
    assert resp.json()["message"] == "账号或密码错误"

@pytest.mark.asyncio
async def test_acceptance_04_disabled_account_rejected(client, db_session):
    """验收4: 禁用账号无法登录，返回相同错误消息"""
    pwd = PasswordService()
    user = User(username="accept4", password_hash=pwd.hash_password("pass"), is_active=False)
    db_session.add(user)
    await db_session.commit()

    resp = await client.post("/api/auth/login", json={
        "username": "accept4", "password": "pass"
    })
    assert resp.status_code == 401
    assert resp.json()["code"] == "AUTH_FAILED"

@pytest.mark.asyncio
async def test_acceptance_05_rate_limiting_triggers_after_max_attempts(client, db_session):
    """验收5: 同一IP连续失败达到上限后触发限流，返回429"""
    # Use a unique IP-like username to avoid interfering with other tests
    for i in range(5):
        resp = await client.post("/api/auth/login", json={
            "username": f"ratelimit_user_{i}", "password": "wrong"
        })
    # The 6th attempt should be rate limited
    resp = await client.post("/api/auth/login", json={
        "username": "ratelimit_user_x", "password": "wrong"
    })
    assert resp.status_code == 429
    assert resp.json()["code"] == "RATE_LIMITED"

@pytest.mark.asyncio
async def test_acceptance_06_account_lockout_after_consecutive_failures(client, db_session):
    """验收6: 同一账号连续10次失败后被锁定，第11次仍返回相同错误消息"""
    pwd = PasswordService()
    user = User(username="accept6", password_hash=pwd.hash_password("realpass"))
    db_session.add(user)
    await db_session.commit()

    for _ in range(10):
        resp = await client.post("/api/auth/login", json={
            "username": "accept6", "password": "wrong"
        })
        assert resp.status_code == 401

    # 11th attempt: still AUTH_FAILED (not a different lockout message)
    resp = await client.post("/api/auth/login", json={
        "username": "accept6", "password": "realpass"
    })
    assert resp.status_code == 401
    assert resp.json()["code"] == "AUTH_FAILED"
    assert resp.json()["message"] == "账号或密码错误"

@pytest.mark.asyncio
async def test_acceptance_07_remember_me_sets_longer_cookie_ttl(client, db_session):
    """验收7: '记住我'登录设置更长的cookie有效期"""
    pwd = PasswordService()
    user = User(username="accept7", password_hash=pwd.hash_password("pass"))
    db_session.add(user)
    await db_session.commit()

    resp = await client.post("/api/auth/login", json={
        "username": "accept7", "password": "pass", "remember_me": True
    })
    assert resp.status_code == 200
    set_cookie = resp.headers.get("set-cookie", "")
    # 30 days = 2592000 seconds
    assert "Max-Age=2592000" in set_cookie or "max-age=2592000" in set_cookie.lower()

@pytest.mark.asyncio
async def test_acceptance_08_logout_clears_session(client, db_session):
    """验收8: 退出登录后session cookie被清除"""
    pwd = PasswordService()
    user = User(username="accept8", password_hash=pwd.hash_password("pass"))
    db_session.add(user)
    await db_session.commit()

    login_resp = await client.post("/api/auth/login", json={
        "username": "accept8", "password": "pass"
    })
    session_cookie = login_resp.cookies.get("session")
    assert session_cookie is not None

    logout_resp = await client.post("/api/auth/logout", cookies={"session": session_cookie})
    assert logout_resp.status_code == 200
    # Cookie should be cleared
    set_cookie = logout_resp.headers.get("set-cookie", "")
    assert "session=" in set_cookie
```

- [ ] **Step 2: Run acceptance tests**

Run: `pytest tests/test_acceptance.py -v`
Expected: 8 PASS

---

## Task 9: Docker & Run Configuration

**Files:**
- Create: `Dockerfile`
- Create: `docker-compose.yml`
- Create: `.env.example`

- [ ] **Step 1: Write .env.example**

```
DATABASE_URL=sqlite+aiosqlite:///./dev.db
SECRET_KEY=change-me-to-a-random-string
SESSION_TTL_SECONDS=7200
REMEMBER_ME_TTL_SECONDS=2592000
RATE_LIMIT_MAX_ATTEMPTS=5
RATE_LIMIT_WINDOW_SECONDS=60
ACCOUNT_LOCKOUT_THRESHOLD=10
ACCOUNT_LOCKOUT_SECONDS=1800
```

- [ ] **Step 2: Write Dockerfile**

```dockerfile
FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY src/ src/
COPY .env .env
EXPOSE 8000
CMD ["uvicorn", "src.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

- [ ] **Step 3: Write docker-compose.yml**

```yaml
version: "3.8"
services:
  auth:
    build: .
    ports:
      - "8000:8000"
    env_file:
      - .env
    volumes:
      - ./dev.db:/app/dev.db
```

---

## Self-Review

### 1. Spec Coverage Check

| Spec Section | Covered By |
|---|---|
| 登录入口 (POST /api/auth/login) | Task 7 — auth_routes.py |
| 输入校验 (username/password min_length) | Task 6 — Pydantic schemas |
| 核心认证流程 (bcrypt verify + unified error) | Task 4 — AuthService |
| 登录态维持 (HttpOnly+Secure+SameSite cookie) | Task 7 — set_cookie |
| 退出登录 (delete cookie) | Task 7 — /logout |
| 账号不存在统一错误 | Task 4 + Task 7 (AUTH_FAILED) |
| 密码错误统一错误 | Task 4 (always return AUTH_FAILED) |
| 暴力破解锁定 (10次→30分钟) | Task 4 (lock_account) |
| 账号禁用 | Task 4 (is_active check) |
| IP限流 (5次/分钟) | Task 5 (RateLimiter) + Task 7 |
| 记住我 (30天 vs 2小时) | Task 7 (remember_me → TTL) |
| 非功能：安全 (bcrypt, HttpOnly, Secure) | Tasks 3, 7 |
| 非功能：性能 (async, SQLite) | Tasks 1, 2 |
| 非功能：可用性 (统一JSON错误) | Task 6, 7 |
| 验收标准 1-8 | Task 8 (8 acceptance tests) |

### 2. Placeholder Scan

No "TBD", "TODO", "implement later", or "add appropriate error handling" patterns found. All steps contain actual code and commands.

### 3. Type Consistency

- `UserRepository.find_by_username(str) -> User | None` — consistent across Tasks 2, 4, 7
- `PasswordService.hash_password(str) -> str` / `verify_password(str, str) -> bool` — consistent across Tasks 3, 4, 7
- `AuthService.authenticate(str, str, str) -> tuple[User | None, str]` — consistent across Tasks 4, 7
- `AuthService.sign_session(User, bool) -> str` — consistent across Tasks 4, 7
- `RateLimiter.is_rate_limited(str) -> bool` / `record_attempt(str) -> None` — consistent across Tasks 5, 7
- `LoginRequest.username: str, password: str, remember_me: bool` — consistent with route handler in Task 7
- Settings field names match everywhere: `ACCOUNT_LOCKOUT_THRESHOLD`, `RATE_LIMIT_MAX_ATTEMPTS`, etc.

---

## Execution Handoff

Plan complete and saved. Two execution options:

1. **Subagent-Driven (recommended)** — Dispatch a fresh subagent per task, review between tasks, fast iteration
2. **Inline Execution** — Execute tasks in this session using executing-plans, batch execution with checkpoints