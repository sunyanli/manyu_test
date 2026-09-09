"""待办事项 CLI 入口

支持命令行新增待办事项。
用法：
    python app.py add "事项名称" "事项描述"
    python app.py add "事项名称"
"""
import sys
from todo_service import TodoService


def print_usage():
    """打印使用说明"""
    print("用法:")
    print("  python app.py add <事项名称> [事项描述]")
    print("示例:")
    print('  python app.py add "买 groceries" "买牛奶和面包"')
    print('  python app.py add "开会"')


def main():
    """主入口"""
    if len(sys.argv) < 2:
        print_usage()
        sys.exit(1)

    command = sys.argv[1]

    if command == "add":
        if len(sys.argv) < 3:
            print("错误: 缺少事项名称")
            print_usage()
            sys.exit(1)

        name = sys.argv[2]
        description = sys.argv[3] if len(sys.argv) > 3 else ""

        service = TodoService()
        try:
            item = service.add_todo(name, description)
            print(f"✅ 已创建待办事项:")
            print(f"   ID: {item.id}")
            print(f"   名称: {item.name}")
            if item.description:
                print(f"   描述: {item.description}")
            print(f"   创建时间: {item.created_at}")
        except ValueError as e:
            print(f"❌ 创建失败: {e}")
            sys.exit(1)
    else:
        print(f"错误: 未知命令 '{command}'")
        print_usage()
        sys.exit(1)


if __name__ == "__main__":
    main()