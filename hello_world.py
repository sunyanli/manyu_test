import datetime


def hello_world():
    """HelloWorld 接口逻辑"""
    return {
        "message": "Hello World!",
        "timestamp": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    }