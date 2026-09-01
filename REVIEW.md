# Project Review Profile

## Project
- **Name**: algorithm-demo (算法展示与监控系统)
- **Stack**: Java 8 + Spring Boot 2.7 + MyBatis-Plus + MySQL / React + ECharts
- **Architecture**: 前后端分离，RESTful JSON API

## Review Gates

### Cross-repo Contract
- Backend (manyu_test) ↔ Frontend (manyu_test1): API path, request/response JSON shape must match
- CORS must be configured for cross-origin frontend requests
- User identity header (`X-User-Id`) contract must be honored by both sides

### Data Integrity
- All tracking/埋点 data must be persisted to database (api_call_log table)
- Report queries must read from database, not mock data
- Export must read from database, not mock data

### Error Handling
- Business exceptions → `BusinessException` with error code
- Global exception handler must catch all unhandled exceptions
- 埋点 failure must not affect main business flow (R08)

### Testing
- Unit tests required for all Service implementations
- Tests must assert behavior, not just logs or counters