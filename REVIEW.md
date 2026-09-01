# manyu_test Review Profile

## Project
- Flask REST API backend with in-memory tracking middleware
- Frontend companion: manyu_test1 (single-page HTML + Chart.js)

## Entry Points
- `app.py` — Flask app factory (`create_app()`), registers blueprints + middleware
- `routes/*.py` — Blueprint-based route modules
- `middleware/tracking.py` — `before_request` hook, global `tracking_store` list

## Review Gates
- All public API paths must be under `/api/` prefix
- Middleware must exclude `/api/tracking` from tracking
- Tests use pytest with Flask test client; each route module has a corresponding `tests/test_*.py`
- CSV export must include header row + data rows with consistent columns
- Tracking aggregation must support `type`, `level`, `dept`, `time` dimensions
- `bubble_sort.py` is a pre-existing module; do not modify it

## Tech Stack
- Python 3.12+, Flask >=3.0, flask-cors >=4.0
- pytest for testing
- Frontend: vanilla HTML/JS + Chart.js 4.x CDN (separate repo)