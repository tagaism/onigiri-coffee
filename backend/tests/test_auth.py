from tests.conftest import auth_header, register


async def test_register_login_me(client):
    created = await register(client, "Ada@Example.com")
    assert created["token_type"] == "bearer"
    assert created["user"]["email"] == "ada@example.com"
    assert created["user"]["default_currency"] == "JPY"

    login = await client.post(
        "/auth/login",
        json={"email": "ADA@example.com", "password": "password123"},
    )
    assert login.status_code == 200
    token = login.json()["access_token"]

    me = await client.get("/auth/me", headers=auth_header(token))
    assert me.status_code == 200
    assert me.json()["email"] == "ada@example.com"


async def test_duplicate_email(client):
    await register(client, "dup@example.com")
    again = await client.post(
        "/auth/register",
        json={"email": "dup@example.com", "password": "password123"},
    )
    assert again.status_code == 409


async def test_login_wrong_password(client):
    await register(client)
    response = await client.post(
        "/auth/login",
        json={"email": "user@example.com", "password": "wrong-password"},
    )
    assert response.status_code == 401


async def test_me_requires_auth(client):
    response = await client.get("/auth/me")
    assert response.status_code == 401
