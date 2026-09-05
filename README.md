# Onigiri

Personal spending tracker. Android app + Python API + Postgres.

MVP is **manual entry only**: you type the merchant, date, tax, and each line item. No OCR yet.

## Run the API

```bash
docker compose up --build
```

API: [http://localhost:8000](http://localhost:8000)  
Docs: [http://localhost:8000/docs](http://localhost:8000/docs)

Copy `.env.example` to `.env` if you want to override `JWT_SECRET`.

### Example

```bash
# register
TOKEN=$(curl -s http://localhost:8000/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"you@example.com","password":"password123"}' \
  | python3 -c 'import sys,json; print(json.load(sys.stdin)["access_token"])')

# add a receipt
curl -s http://localhost:8000/receipts \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "merchant_name": "Onigiri Coffee",
    "purchased_at": "2026-09-05",
    "currency": "JPY",
    "tax": "80",
    "items": [
      {"name": "Latte", "quantity": "1", "unit_price": "500", "amount": "500"},
      {"name": "Onigiri", "quantity": "2", "unit_price": "180", "amount": "360"}
    ]
  }'

# month totals
curl -s "http://localhost:8000/stats/summary?from=2026-09-01&to=2026-09-30" \
  -H "Authorization: Bearer $TOKEN"
```

Money fields are JSON **strings** (for example `"500.00"`) so amounts stay exact.

## Android

Open `android/` in Android Studio and run the `app` configuration.

| Where the app runs | API base URL |
|---|---|
| Emulator | `http://10.0.2.2:8000` (default) |
| Physical phone on the same LAN | `http://<your-computer-ip>:8000` |

Change the URL under **API settings** on the login screen.

HTTP cleartext is allowed in this MVP so the emulator can talk to Docker on your machine. Use HTTPS if you ever expose the API beyond localhost.

## Tests

Postgres must be running (`docker compose up -d db`).

```bash
cd backend
../.venv/bin/python -m pytest
```

Create the venv once with:

```bash
python3.13 -m venv .venv
.venv/bin/pip install -r backend/requirements.txt
```

## Layout

```
backend/     FastAPI, Alembic, tests
android/      Kotlin + Jetpack Compose
docker-compose.yml
```

## Later

Receipt photos, OCR, categories, charts, budgets, cloud deploy.
