### Task 2: 实现 TodoManager 新增 + 持久化

**Files:**
- Modify: `scripts/todo.py` — 追加 `TodoManager` 类

**Interfaces:**
- Consumes: `TodoItem`（Task 1 定义）
- Produces: `TodoManager(file_path: Path)` with `add(name: str, description: str) -> TodoItem`, `_load() -> list[dict]`, `_save(items: list[dict]) -> None`

- [ ] **Step 1: 追加 TodoManager 类到 scripts/todo.py**

在文件末尾追加以下代码（紧接 `TodoItem` 类之后）：

```python
DEFAULT_STORAGE_PATH = Path("data/todos.json")


class TodoManager:
    """管理待办事项的持久化与操作."""

    def __init__(self, file_path: Optional[Path] = None) -> None:
        self._file_path = file_path or DEFAULT_STORAGE_PATH

    def _load(self) -> list[dict]:
        """从 JSON 文件加载所有事项."""
        if not self._file_path.exists():
            return []
        with open(self._file_path, "r", encoding="utf-8") as f:
            return json.load(f)

    def _save(self, items: list[dict]) -> None:
        """将所有事项写入 JSON 文件."""
        self._file_path.parent.mkdir(parents=True, exist_ok=True)
        with open(self._file_path, "w", encoding="utf-8") as f:
            json.dump(items, f, ensure_ascii=False, indent=2)

    def add(self, name: str, description: str) -> TodoItem:
        """新增一条待办事项并持久化.

        Args:
            name: 事项名称（必填）.
            description: 事项描述（必填）.

        Returns:
            创建的 TodoItem 实例.
        """
        item = TodoItem(name=name, description=description)
        current = self._load()
        current.append(item.to_dict())
        self._save(current)
        return item
```

- [ ] **Step 2: 手动冒烟验证**

Run: `python -c "
from pathlib import Path
from scripts.todo import TodoManager
import tempfile, os

tmp = Path(tempfile.mkdtemp()) / 'todos.json'
mgr = TodoManager(tmp)
item = mgr.add('买菜', '去超市买蔬菜和水果')
print(f'Created: id={item.id}, name={item.name}, desc={item.description}')
assert tmp.exists()
import json
data = json.loads(tmp.read_text('utf-8'))
assert len(data) == 1
assert data[0]['name'] == '买菜'
print('OK - persistence verified')
"`

Expected: 输出 `OK - persistence verified`，退出码 0。

- [ ] **Step 3: Commit**

```bash
git add scripts/todo.py
git commit -m "feat: add TodoManager with JSON file persistence"
```

---

