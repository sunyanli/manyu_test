# Todo App Code Review Profile

## Project: 待办事项应用 (todo-app)

- **Stack**: Java 21, Spring Boot 3.2.0, MyBatis 3.0.3, MySQL 8.0+
- **Build**: Maven
- **Test**: JUnit 5 + Mockito + AssertJ
- **Architecture**: Controller → Service → Mapper → DB, with DO/VO/Request DTOs

## Project-Specific Gates

1. **Service method signature must match design doc** — the design document is the contract; drift must be documented or fixed.
2. **BusinessException carries errorCode separately from message** — tests must assert `getErrorCode()`, not `getMessage()`.
3. **tenant_id must not be hardcoded** — must come from auth context in production; hardcoded placeholder is acceptable only with explicit TODO.
4. **All validation errors must use designated error codes (TODO_001–TODO_004)** per design spec.
5. **Controller catches BusinessException and returns structured JSON** `{code, msg, data}`.
6. **SQL uses parameterized `#{}` placeholders** — no string concatenation.
7. **Test coverage**: normal path, null/empty/blank, boundary (exact max), exceeded max, DB failure.