"""todo_item - 待办事项模块（内存存储实现）

提供最小闭环能力：新增待办事项。
分层：
- entity: TodoItemDO 数据对象
- repository: TodoItemRepository 内存存储
- service: TodoItemService 业务逻辑
- controller: TodoItemController HTTP 入口（基于标准库 http.server）
"""
