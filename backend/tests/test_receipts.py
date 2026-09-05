from tests.conftest import auth_header, register

RECEIPT = {
    "merchant_name": "Onigiri Coffee",
    "purchased_at": "2026-09-05",
    "currency": "JPY",
    "tax": "80",
    "items": [
        {"name": "Latte", "quantity": "1", "unit_price": "500", "amount": "500"},
        {"name": "Onigiri", "quantity": "2", "unit_price": "180", "amount": "360"},
    ],
}


async def test_create_lists_and_computes_total(client):
    session = await register(client)
    headers = auth_header(session["access_token"])

    created = await client.post("/receipts", json=RECEIPT, headers=headers)
    assert created.status_code == 201, created.text
    body = created.json()
    assert body["total"] == "940.00"
    assert body["computed_total"] == "940.00"
    assert body["total_mismatch"] is False
    assert len(body["items"]) == 2

    listed = await client.get("/receipts", headers=headers)
    assert listed.status_code == 200
    assert len(listed.json()) == 1
    assert listed.json()[0]["item_count"] == 2

    detail = await client.get(f"/receipts/{body['id']}", headers=headers)
    assert detail.status_code == 200
    assert detail.json()["merchant_name"] == "Onigiri Coffee"


async def test_explicit_total_mismatch(client):
    session = await register(client)
    headers = auth_header(session["access_token"])
    payload = {**RECEIPT, "total": "1000"}
    created = await client.post("/receipts", json=payload, headers=headers)
    body = created.json()
    assert body["total"] == "1000.00"
    assert body["computed_total"] == "940.00"
    assert body["total_mismatch"] is True


async def test_requires_at_least_one_item(client):
    session = await register(client)
    headers = auth_header(session["access_token"])
    payload = {**RECEIPT, "items": []}
    response = await client.post("/receipts", json=payload, headers=headers)
    assert response.status_code == 422


async def test_amount_from_unit_price(client):
    session = await register(client)
    headers = auth_header(session["access_token"])
    payload = {
        "merchant_name": "Shop",
        "purchased_at": "2026-09-01",
        "items": [{"name": "Tea", "quantity": "2", "unit_price": "150"}],
    }
    created = await client.post("/receipts", json=payload, headers=headers)
    assert created.status_code == 201, created.text
    assert created.json()["items"][0]["amount"] == "300.00"
    assert created.json()["total"] == "300.00"


async def test_patch_replaces_items(client):
    session = await register(client)
    headers = auth_header(session["access_token"])
    created = await client.post("/receipts", json=RECEIPT, headers=headers)
    receipt_id = created.json()["id"]

    patched = await client.patch(
        f"/receipts/{receipt_id}",
        json={
            "merchant_name": "New Shop",
            "tax": "0",
            "items": [{"name": "Water", "amount": "120"}],
        },
        headers=headers,
    )
    assert patched.status_code == 200, patched.text
    body = patched.json()
    assert body["merchant_name"] == "New Shop"
    assert len(body["items"]) == 1
    assert body["items"][0]["name"] == "Water"
    assert body["total"] == "120.00"


async def test_delete_and_user_isolation(client):
    alice = await register(client, "alice@example.com")
    bob = await register(client, "bob@example.com")
    alice_h = auth_header(alice["access_token"])
    bob_h = auth_header(bob["access_token"])

    created = await client.post("/receipts", json=RECEIPT, headers=alice_h)
    receipt_id = created.json()["id"]

    bob_get = await client.get(f"/receipts/{receipt_id}", headers=bob_h)
    assert bob_get.status_code == 404

    bob_list = await client.get("/receipts", headers=bob_h)
    assert bob_list.json() == []

    bob_delete = await client.delete(f"/receipts/{receipt_id}", headers=bob_h)
    assert bob_delete.status_code == 404

    deleted = await client.delete(f"/receipts/{receipt_id}", headers=alice_h)
    assert deleted.status_code == 204
    missing = await client.get(f"/receipts/{receipt_id}", headers=alice_h)
    assert missing.status_code == 404
