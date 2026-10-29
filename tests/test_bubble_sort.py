"""Characterization tests for the existing bubble sort implementations."""

import random

import pytest

from bubble_sort import (
    bubble_sort,
    bubble_sort_descending,
    bubble_sort_optimized,
)

# Boundary datasets: empty, single element, all-equal, already ascending,
# already descending, negatives/zero, floats, strings.
DATASETS = [
    pytest.param([], id="empty"),
    pytest.param([7], id="single"),
    pytest.param([4, 4, 4, 4], id="all-equal"),
    pytest.param([1, 2, 3, 4, 5], id="ascending"),
    pytest.param([5, 4, 3, 2, 1], id="descending"),
    pytest.param([9, -3, 0, 7, -1, 0], id="negatives-and-zero"),
    pytest.param([2.5, -1.5, 0.0, 3.25, 2.5], id="floats"),
    pytest.param(["pear", "apple", "fig", "apple"], id="strings"),
]


def random_ints(length):
    """Deterministic pseudo-random integers (fixed seed, no global state)."""
    rng = random.Random(20261029)
    return [rng.randint(-50, 50) for _ in range(length)]


def assert_sorts_in_place(func, data, expected):
    """Assert func mutates and returns the very same list, in expected order."""
    original = list(data)
    result = func(original)
    assert result is original
    assert original == expected


# ---------------------------------------------------------------- bubble_sort


@pytest.mark.parametrize("data", DATASETS)
def test_bubble_sort_sorts_ascending_in_place(data):
    assert_sorts_in_place(bubble_sort, data, sorted(data))


@pytest.mark.parametrize("length", [2, 17, 40])
def test_bubble_sort_sorts_deterministic_random_data(length):
    data = random_ints(length)
    assert_sorts_in_place(bubble_sort, data, sorted(data))


# ------------------------------------------------------ bubble_sort_optimized


@pytest.mark.parametrize("data", DATASETS)
def test_bubble_sort_optimized_sorts_ascending_in_place(data):
    assert_sorts_in_place(bubble_sort_optimized, data, sorted(data))


@pytest.mark.parametrize("length", [2, 17, 40])
def test_bubble_sort_optimized_sorts_deterministic_random_data(length):
    data = random_ints(length)
    assert_sorts_in_place(bubble_sort_optimized, data, sorted(data))


# ------------------------------------------------------ bubble_sort_descending


@pytest.mark.parametrize("data", DATASETS)
def test_bubble_sort_descending_sorts_descending_in_place(data):
    assert_sorts_in_place(bubble_sort_descending, data, sorted(data, reverse=True))


@pytest.mark.parametrize("length", [2, 17, 40])
def test_bubble_sort_descending_sorts_deterministic_random_data(length):
    data = random_ints(length)
    assert_sorts_in_place(
        bubble_sort_descending, data, sorted(data, reverse=True)
    )
