from tests.conftest import auth_header, register

RECEIPT = {
    "merchant_name": "Onigiri Coffee",
    "purchased_at": "2026-09-19",
    "items": [{"name": "Latte", "amount": "500"}],
}


async def test_saving_receipt_stores_merchant_once(client):
    session = await register(client)
    headers = auth_header(session["access_token"])

    await client.post("/receipts", json=RECEIPT, headers=headers)
    await client.post(
        "/receipts",
        json={**RECEIPT, "merchant_name": "onigiri coffee"},
        headers=headers,
    )

    listed = await client.get("/merchants", headers=headers)
    assert listed.status_code == 200
    names = [row["name"] for row in listed.json()]
    assert names == ["onigiri coffee"]


async def test_merchant_isolation(client):
    alice = await register(client, "alice@example.com")
    bob = await register(client, "bob@example.com")
    await client.post("/receipts", json=RECEIPT, headers=auth_header(alice["access_token"]))

    bob_list = await client.get("/merchants", headers=auth_header(bob["access_token"]))
    assert bob_list.json() == []
