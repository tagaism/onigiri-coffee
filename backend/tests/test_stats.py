from tests.conftest import auth_header, register


async def test_summary_defaults_to_current_month_and_isolates_users(client):
    alice = await register(client, "alice@example.com")
    bob = await register(client, "bob@example.com")
    alice_h = auth_header(alice["access_token"])
    bob_h = auth_header(bob["access_token"])

    await client.post(
        "/receipts",
        json={
            "merchant_name": "Cafe",
            "purchased_at": "2026-09-05",
            "tax": "0",
            "items": [{"name": "Coffee", "amount": "400"}],
        },
        headers=alice_h,
    )
    await client.post(
        "/receipts",
        json={
            "merchant_name": "Market",
            "purchased_at": "2026-09-05",
            "tax": "10",
            "items": [{"name": "Rice", "amount": "200"}],
        },
        headers=alice_h,
    )
    await client.post(
        "/receipts",
        json={
            "merchant_name": "Bob Shop",
            "purchased_at": "2026-09-05",
            "items": [{"name": "Tea", "amount": "9999"}],
        },
        headers=bob_h,
    )

    summary = await client.get(
        "/stats/summary",
        params={"from": "2026-09-01", "to": "2026-09-30"},
        headers=alice_h,
    )
    assert summary.status_code == 200, summary.text
    body = summary.json()
    assert body["from"] == "2026-09-01"
    assert body["to"] == "2026-09-30"
    assert body["receipt_count"] == 2
    assert body["total"] == "610.00"
    assert body["by_day"] == [{"date": "2026-09-05", "count": 2, "total": "610.00"}]
