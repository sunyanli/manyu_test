import pytest
from app import create_app


@pytest.fixture
def client():
    app = create_app()
    app.config["TESTING"] = True
    return app.test_client()


def test_helloworld_returns_hello_world(client):
    resp = client.get("/api/helloworld")
    assert resp.status_code == 200
    data = resp.get_json()
    assert data["result"] == "Hello, World!"