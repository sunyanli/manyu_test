# Fix mypy --strict test errors — Report

## Summary

Fixed all `no-untyped-def` errors reported by `mypy --strict tests/` (15 original errors, plus 2 uncovered `setUp` methods).

## Files modified

### `tests/test_hello_world.py`
- `setUp(self)` → `setUp(self) -> None`
- `test_doctests(self)` → `test_doctests(self) -> None`
- `test_greet(self)` → `test_greet(self) -> None`
- `test_greet_many(self)` → `test_greet_many(self) -> None`
- `test_greet_many_empty(self)` → `test_greet_many_empty(self) -> None`
- `test_protocol_compatibility(self)` → `test_protocol_compatibility(self) -> None`

### `tests/test_hash_algo.py`
- `setUp(self)` → `setUp(self) -> None`
- `test_doctests(self)` → `test_doctests(self) -> None`
- `test_algorithm_name(self)` → `test_algorithm_name(self) -> None`
- `test_hash_known_value(self)` → `test_hash_known_value(self) -> None`
- `test_hash_empty_bytes(self)` → `test_hash_empty_bytes(self) -> None`
- `test_hash_unicode(self)` → `test_hash_unicode(self) -> None`
- `test_hash_file(self)` → `test_hash_file(self) -> None`
- `test_hash_file_large_chunk(self)` → `test_hash_file_large_chunk(self) -> None`
- `test_protocol_compatibility(self)` → `test_protocol_compatibility(self) -> None`

## Scope note

The original task listed 13 methods (5 + 8). Two additional `setUp` methods were also missing return types and were caught by mypy on re-run, so they were fixed as well (total 15 annotations added).

## Verification

```bash
$ mypy --strict tests/
Success: no issues found in 3 source files
```