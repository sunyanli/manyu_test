import pytest
from app import create_app
from middleware.tracking import tracking_store


@pytest.fixture
def client():
    app = create_app()
    app.config["TESTING"] = True
    return app.test_client()


@pytest.fixture(autouse=True)
def clear_store():
    tracking_store.clear()


def test_export_returns_csv(client):
    client.get("/api/helloworld", headers={
        "X-User-Name": "testuser",
        "X-User-Type": "正式员工",
        "X-User-Level": "中级",
        "X-User-Dept": "技术部",
    })
    resp = client.get("/api/export?type=helloworld")
    assert resp.status_code == 200
    assert "text/csv" in resp.content_type
    assert "attachment" in resp.headers.get("Content-Disposition", "")
    body = resp.data.decode("utf-8")
    assert "testuser" in body
    assert "正式员工" in body


def test_export_empty_when_no_matching_type(client):
    resp = client.get("/api/export?type=helloworld")
    assert resp.status_code == 200
    body = resp.data.decode("utf-8")
    lines = body.strip().split("\n")
    assert len(lines) == 1  # header only