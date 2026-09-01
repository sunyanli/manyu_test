# Project Review Profile — 三接口演示平台

## Project Type
Python 3 + FastAPI backend (manyu_test), vanilla HTML/JS + ECharts frontend (manyu_test1), SQLite storage.

## Key Gates

### Backend (manyu_test)
- All API routes must follow the contract defined in `.agents/20260901-分别写三个接口helloworld_哈希/design.md` §4.
- SQL queries must use parameterized queries; f-string interpolation into SQL is forbidden even with whitelist validation.
- Middleware must not block the response path; async writes must be safe for SQLite concurrency.
- `sys.path.insert` hacks for cross-module imports are not allowed; use proper package structure.
- All new API endpoints must have corresponding pytest tests with `TestClient`.

### Frontend (manyu_test1)
- All user-facing errors must be caught and displayed; no unhandled promise rejections.
- Event listeners must be cleaned up to avoid memory leaks (especially ECharts resize).
- BASE_URL must be configurable, not hardcoded to localhost.

### Cross-Repo
- Header names (X-User-*) must be consistent between frontend and backend.
- API path names and dimension enum values must be identical across repos.
- Export format (CSV columns) must match the design doc exactly.