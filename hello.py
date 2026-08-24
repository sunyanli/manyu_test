"""Hello World module — upstream definition for cross-repo collaboration demo."""


def greet(name: str = "World") -> str:
    """Return a greeting string.

    Args:
        name: The entity to greet. Defaults to "World".

    Returns:
        A greeting message.
    """
    return f"Hello, {name}!"


if __name__ == "__main__":
    print(greet())