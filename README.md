# todo — 内部待办记录工具

帮助内部用户记录日常待办事项。本期最小闭环：仅支持新增待办。

## 使用

```bash
python3 todo.py add <事项名称> [事项描述]
```

示例：

```bash
python3 todo.py add "购买服务器" "为内部工具准备"
```

- 事项名称必填（不能为空白）；事项描述可选。
- 待办默认保存在当前目录的 `todos.json`；用 `--file <路径>` 可指定其他存储文件。
- 运行环境：Python 3.12+，无第三方依赖。

## 开发

```bash
python3 -m unittest discover -v
```
