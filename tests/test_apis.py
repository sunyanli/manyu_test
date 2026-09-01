import pytest
from fastapi.testclient import TestClient
from main import app
from models.tracking import init_db, get_db_path
import os
import sqlite3
import tempfile

client = TestClient(app)


@pytest.fixture(autouse=True)
def setup_db():
    """每个测试前重置数据库"""
    import models.tracking as mtrack
    fd, tmp = tempfile.mkstemp(suffix=".db", prefix="tracking_test_")
    os.close(fd)
    mtrack.DB_PATH = tmp
    init_db(tmp)
    yield
    if os.path.exists(tmp):
        os.remove(tmp)


class TestHelloworld:
    def test_returns_greeting(self):
        resp = client.post("/api/helloworld")
        assert resp.status_code == 200
        data = resp.json()
        assert data["message"] == "Hello, World!"
        assert "timestamp" in data


class TestHash:
    def test_valid_hash(self):
        resp = client.post("/api/hash", json={"text": "abc"})
        assert resp.status_code == 200
        data = resp.json()
        assert data["algorithm"] == "SHA256"
        assert data["input"] == "abc"
        assert data["hash"] == "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"

    def test_empty_text_rejected(self):
        resp = client.post("/api/hash", json={"text": ""})
        assert resp.status_code == 422

    def test_missing_text_rejected(self):
        resp = client.post("/api/hash", json={})
        assert resp.status_code == 422


class TestBubbleSort:
    def test_sort_numbers(self):
        resp = client.post("/api/bubble-sort", json={"numbers": [5, 3, 8, 4, 2]})
        assert resp.status_code == 200
        data = resp.json()
        assert data["original"] == [5, 3, 8, 4, 2]
        assert data["sorted"] == [2, 3, 4, 5, 8]
        assert data["algorithm"] == "bubble_sort"

    def test_empty_array_rejected(self):
        resp = client.post("/api/bubble-sort", json={"numbers": []})
        assert resp.status_code == 422

    def test_missing_numbers_rejected(self):
        resp = client.post("/api/bubble-sort", json={})
        assert resp.status_code == 422


class TestExport:
    def test_export_helloworld_csv(self):
        client.post("/api/helloworld", headers={"X-User-Name": "ZhangSan"})
        resp = client.get("/api/export/helloworld")
        assert resp.status_code == 200
        assert "text/csv" in resp.headers["content-type"]
        assert "helloworld_export.csv" in resp.headers["content-disposition"]

    def test_export_invalid_type(self):
        resp = client.get("/api/export/invalid")
        assert resp.status_code == 400


class TestAnalytics:
    def test_analytics_by_dept(self):
        client.post("/api/helloworld", headers={"X-User-Dept": "Tech"})
        client.post("/api/hash", json={"text": "x"}, headers={"X-User-Dept": "Tech"})
        client.post("/api/bubble-sort", json={"numbers": [1]}, headers={"X-User-Dept": "Product"})

        resp = client.get("/api/analytics?dimension=dept")
        assert resp.status_code == 200
        data = resp.json()
        assert data["dimension"] == "dept"
        assert len(data["data"]) == 2

    def test_analytics_invalid_dimension(self):
        resp = client.get("/api/analytics?dimension=invalid")
        assert resp.status_code == 400


class TestTracking:
    def test_tracking_inserts_log(self):
        import time

        client.post("/api/helloworld", headers={
            "X-User-Id": "u001",
            "X-User-Name": "ZhangSan",
            "X-User-Dept": "Tech",
            "X-User-Level": "P6",
            "X-User-Type": "FTE",
        })
        time.sleep(0.3)  # 等待异步写入

        conn = sqlite3.connect(get_db_path())
        row = conn.execute("SELECT * FROM api_call_logs").fetchone()
        conn.close()
        assert row is not None
        assert row[1] == "helloworld"
        assert row[3] == "ZhangSan"

    def test_export_analytics_not_tracked(self):
        import time

        client.get("/api/export/helloworld")
        client.get("/api/analytics?dimension=dept")
        time.sleep(0.3)

        conn = sqlite3.connect(get_db_path())
        count = conn.execute("SELECT COUNT(*) FROM api_call_logs").fetchone()[0]
        conn.close()
        assert count == 0