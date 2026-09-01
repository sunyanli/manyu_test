import pytest
from app import create_app


@pytest.fixture
def client():
    app = create_app()
    app.config["TESTING"] = True
    return app.test_client()


def test_hash_sha256(client):
    resp = client.post("/api/hash", json={"algorithm": "sha256", "text": "hello"})
    assert resp.status_code == 200
    data = resp.get_json()
    assert data["algorithm"] == "sha256"
    assert data["input"] == "hello"
    assert len(data["hash"]) == 64


def test_hash_md5(client):
    resp = client.post("/api/hash", json={"algorithm": "md5", "text": "hello"})
    assert resp.status_code == 200
    data = resp.get_json()
    assert len(data["hash"]) == 32


def test_hash_sha1(client):
    resp = client.post("/api/hash", json={"algorithm": "sha1", "text": "hello"})
    assert resp.status_code == 200
    data = resp.get_json()
    assert len(data["hash"]) == 40


def test_hash_invalid_algorithm(client):
    resp = client.post("/api/hash", json={"algorithm": "crc32", "text": "hello"})
    assert resp.status_code == 400
    data = resp.get_json()
    assert "error" in data