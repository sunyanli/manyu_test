### Task 1: 定义 TodoItem 数据类

**Files:**
- Create: `scripts/todo.py`

**Interfaces:**
- Consumes: 无（第一个任务）
- Produces: `TodoItem` dataclass，字段 `id: str`, `name: str`, `description: str`, `created_at: str`

- [ ] **Step 1: 写入 TodoItem 数据类定义**

```python
"""TODO 待办事项管理模块 — 仅支持新增操作（最小闭环）."""
import json
import uuid
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Optional


@dataclass
class TodoItem:
    """一条待办事项."""

    name: str
    description: str
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    created_at: str = field(
        default_factory=lambda: datetime.now(timezone.utc).isoformat()
    )

    def to_dict(self) -> dict:
        """序列化为字典."""
        return {
            "id": self.id,
            "name": self.name,
            "description": self.description,
            "created_at": self.created_at,
        }

    @classmethod
    def from_dict(cls, data: dict) -> "TodoItem":
        """从字典反序列化."""
        return cls(
            id=data["id"],
            name=data["name"],
            description=data["description"],
            created_at=data["created_at"],
        )
```

- [ ] **Step 2: 验证模块可导入**

Run: `python -c "from scripts.todo import TodoItem; t = TodoItem('测试', '描述'); print(t.to_dict())"`

Expected: 输出包含 `name`、`description`、`id`、`created_at` 的字典，退出码 0。

- [ ] **Step 3: Commit**

```bash
git add scripts/todo.py
git commit -m "feat: add TodoItem dataclass to todo module"
```

---

