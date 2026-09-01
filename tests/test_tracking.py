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


def _seed_data(client):
    for i in range(3):
        client.get("/api/helloworld", headers={
            "X-User-Name": f"user{i}",
            "X-User-Type": "正式员工",
            "X-User-Level": "中级",
            "X-User-Dept": "技术部",
        })
    for i in range(2):
        client.get("/api/helloworld", headers={
            "X-User-Name": f"ext{i}",
            "X-User-Type": "外包",
            "X-User-Level": "初级",
            "X-User-Dept": "运营部",
        })


def test_tracking_by_type(client):
    _seed_data(client)
    resp = client.get("/api/tracking?dimension=type")
    assert resp.status_code == 200
    data = resp.get_json()
    assert data["dimension"] == "type"
    assert data["summary"]["total"] == 5
    types = {item["key"]: item["count"] for item in data["data"]}
    assert types["正式员工"] == 3
    assert types["外包"] == 2


def test_tracking_by_level(client):
    _seed_data(client)
    resp = client.get("/api/tracking?dimension=level")
    assert resp.status_code == 200
    data = resp.get_json()
    levels = {item["key"]: item["count"] for item in data["data"]}
    assert levels["中级"] == 3
    assert levels["初级"] == 2


def test_tracking_by_dept(client):
    _seed_data(client)
    resp = client.get("/api/tracking?dimension=dept")
    assert resp.status_code == 200
    data = resp.get_json()
    depts = {item["key"]: item["count"] for item in data["data"]}
    assert depts["技术部"] == 3
    assert depts["运营部"] == 2


def test_tracking_by_time(client):
    _seed_data(client)
    resp = client.get("/api/tracking?dimension=time")
    assert resp.status_code == 200
    data = resp.get_json()
    assert data["dimension"] == "time"
    assert data["summary"]["total"] == 5
    assert len(data["data"]) >= 1
    for item in data["data"]:
        assert "time" in item
        assert "count" in item


def test_tracking_default_dimension(client):
    _seed_data(client)
    resp = client.get("/api/tracking")
    assert resp.status_code == 200
    data = resp.get_json()
    assert data["dimension"] == "type"