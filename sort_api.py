from bubble_sort import bubble_sort


def sort_data(data: list):
    """包装冒泡排序为接口调用"""
    original = list(data)
    sorted_data = bubble_sort(list(data))
    return {
        "original": original,
        "sorted": sorted_data,
        "algorithm": "bubble_sort"
    }