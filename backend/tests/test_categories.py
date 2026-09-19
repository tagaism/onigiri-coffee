from tests.conftest import auth_header, register

DEFAULTS = ["food", "restaurants", "amusement", "education", "transport"]


async def test_register_seeds_default_categories(client):
    session = await register(client)
    headers = auth_header(session["access_token"])
    response = await client.get("/categories", headers=headers)
    assert response.status_code == 200
    names = [row["name"] for row in response.json()]
    assert names[:5] == DEFAULTS
    assert all(row["is_default"] for row in response.json() if row["name"] in DEFAULTS)


async def test_create_custom_category_and_attach_to_receipt(client):
    session = await register(client)
    headers = auth_header(session["access_token"])
    created = await client.post("/categories", json={"name": "Pets"}, headers=headers)
    assert created.status_code == 201, created.text
    category_id = created.json()["id"]
    assert created.json()["is_default"] is False

    listed = await client.get("/categories", headers=headers)
    names = [row["name"] for row in listed.json()]
    assert "Pets" in names

    receipt = await client.post(
        "/receipts",
        json={
            "merchant_name": "Pet shop",
            "purchased_at": "2026-09-15",
            "category_id": category_id,
            "items": [{"name": "Treats", "amount": "500"}],
        },
        headers=headers,
    )
    assert receipt.status_code == 201, receipt.text
    assert receipt.json()["category"]["name"] == "Pets"

    summary = await client.get(
        "/stats/summary",
        params={"from": "2026-09-01", "to": "2026-09-30"},
        headers=headers,
    )
    by_category = {row["name"]: row["total"] for row in summary.json()["by_category"]}
    assert by_category["Pets"] == "500.00"


async def test_cannot_delete_default_or_duplicate_names(client):
    session = await register(client)
    headers = auth_header(session["access_token"])
    defaults = (await client.get("/categories", headers=headers)).json()
    food_id = next(row["id"] for row in defaults if row["name"] == "food")
    denied = await client.delete(f"/categories/{food_id}", headers=headers)
    assert denied.status_code == 400

    dup = await client.post("/categories", json={"name": "Food"}, headers=headers)
    assert dup.status_code == 409

    custom = await client.post("/categories", json={"name": "Gifts"}, headers=headers)
    deleted = await client.delete(f"/categories/{custom.json()['id']}", headers=headers)
    assert deleted.status_code == 204


async def test_category_isolation(client):
    alice = await register(client, "alice@example.com")
    bob = await register(client, "bob@example.com")
    alice_h = auth_header(alice["access_token"])
    bob_h = auth_header(bob["access_token"])
    custom = await client.post("/categories", json={"name": "Alice only"}, headers=alice_h)
    category_id = custom.json()["id"]
    stolen = await client.post(
        "/receipts",
        json={
            "merchant_name": "Shop",
            "purchased_at": "2026-09-15",
            "category_id": category_id,
            "items": [{"name": "X", "amount": "1"}],
        },
        headers=bob_h,
    )
    assert stolen.status_code == 404
