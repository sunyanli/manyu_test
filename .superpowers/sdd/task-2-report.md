# Task 2 Report: Implement TodoManager with add + persistence

## Status: DONE

## Files Changed

| File | Change |
|------|--------|
| `scripts/todo.py` | Appended `DEFAULT_STORAGE_PATH` constant + `TodoManager` class (38 lines) after `TodoItem` |

## Changes Summary

在 `scripts/todo.py` 末尾追加了以下内容：

- **`DEFAULT_STORAGE_PATH`**: 默认 JSON 存储路径 `data/todos.json`
- **`TodoManager` 类**:
  - `__init__(file_path)`: 接受可选路径，默认 `DEFAULT_STORAGE_PATH`
  - `_load()`: 从 JSON 文件加载数据，文件不存在返回 `[]`
  - `_save(items)`: 自动创建父目录并写入 JSON（`ensure_ascii=False, indent=2`）
  - `add(name, description)`: 创建 `TodoItem`，追加到持久化列表，返回 `TodoItem` 实例

原有 `TodoItem` 代码完整保留，所有依赖 (`json`, `Path`, `Optional`) 在文件顶部已导入。

## Verification

**Smoke test — PASSED (exit 0)**:
```
Created: id=6e2d3037-02fd-4626-bdec-23db7f2e7dee, name=买菜, desc=去超市买蔬菜和水果
OK - persistence verified
```

验证内容：
- ✅ `TodoManager.add()` 正确创建 `TodoItem` 并返回
- ✅ JSON 文件正确写入临时目录
- ✅ 文件内容包含 1 条记录，`name` 字段为 `买菜`
- ✅ 无外部依赖，仅 Python stdlib

## Concerns

无。Commit 步骤按约束跳过（Git 只读）。