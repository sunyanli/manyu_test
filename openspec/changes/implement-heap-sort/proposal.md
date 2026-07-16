# Proposal: 实现堆排序算法

## 概述
实现一个标准的堆排序（Heap Sort）算法，提供原地排序能力，时间复杂度 O(n log n)。

## 目标
- 实现最大堆的构建（heapify）操作
- 实现堆排序主流程
- 支持泛型比较（任意可比较类型）
- 提供完整的单元测试覆盖

## 范围
- 单个 Python 模块 `heap_sort.py`
- 对应的测试文件 `test_heap_sort.py`

## 技术决策
- 语言：Python 3
- 排序方式：原地排序（in-place）
- 堆类型：最大堆
- 升序输出