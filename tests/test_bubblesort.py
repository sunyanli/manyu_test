import pytest
from app import create_app


@pytest.fixture
def client():
    app = create_app()
    app.config["TESTING"] = True
    return app.test_client()


def test_bubblesort_sorts_array(client):
    resp = client.post("/api/bubblesort", json={"array": [5, 3, 8, 4, 2]})
    assert resp.status_code == 200
    data = resp.get_json()
    assert data["input"] == [5, 3, 8, 4, 2]
    assert data["sorted"] == [2, 3, 4, 5, 8]
    assert data["steps"] > 0


def test_bubblesort_empty_array(client):
    resp = client.post("/api/bubblesort", json={"array": []})
    assert resp.status_code == 200
    data = resp.get_json()
    assert data["sorted"] == []
    assert data["steps"] == 0


def test_bubblesort_invalid_input(client):
    resp = client.post("/api/bubblesort", json={"array": "not_an_array"})
    assert resp.status_code == 400
    data = resp.get_json()
    assert "error" in data