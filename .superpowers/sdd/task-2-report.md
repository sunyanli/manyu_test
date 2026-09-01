# Task 2 Report: hash_algo package

## Status
DONE

## Commits

- `feat: add hash_algo package (HashAlgorithmInterface Protocol + SHA256Hasher implementation)` — created `hash_algo/__init__.py`, `hash_algo/_interface.py`, `hash_algo/sha256_impl.py`, and `tests/test_hash_algo.py`

## Files Created

| File | Description |
|------|-------------|
| `hash_algo/__init__.py` | Package init, exports `HashAlgorithmInterface` and `SHA256Hasher` |
| `hash_algo/_interface.py` | `HashAlgorithmInterface` Protocol with `hash()`, `hash_file()`, `algorithm_name` |
| `hash_algo/sha256_impl.py` | `SHA256Hasher` implementation using `hashlib.sha256` |
| `tests/test_hash_algo.py` | Unit tests: doctest verification, known hash values, empty bytes, unicode, file hashing, large chunk consistency, protocol compatibility |

## Test Results

**Command:** `python3 -m pytest tests/test_hash_algo.py -v`

**Result:** 8 passed in 0.03s

| Test | Status |
|------|--------|
| TestHashAlgoDoctest::test_doctests | PASSED |
| TestSHA256Hasher::test_algorithm_name | PASSED |
| TestSHA256Hasher::test_hash_empty_bytes | PASSED |
| TestSHA256Hasher::test_hash_file | PASSED |
| TestSHA256Hasher::test_hash_file_large_chunk | PASSED |
| TestSHA256Hasher::test_hash_known_value | PASSED |
| TestSHA256Hasher::test_hash_unicode | PASSED |
| TestSHA256Hasher::test_protocol_compatibility | PASSED |

## Concerns / Observations

- Existing `bubble_sort.py` was not modified.
- `hello_world` package (Task 1) remains intact.
- Python 3.12, pytest 9.1.1 used.