# Onigiri

Personal spending tracker. Next.js web app + Android app + Python API + Postgres.

MVP is **manual entry only**: you type the merchant, date, tax, and each line item. No OCR yet. Use the **web app** as the main client.

## Database (Postgres.app)

Local development uses **[Postgres.app](https://postgresapp.com/)** as the database, not Docker Postgres. Open Postgres.app and wait until it is running (menu-bar elephant).

Create the app databases once (this is already done on this machine):

```bash
psql -d postgres -f backend/scripts/setup_postgres_app.sql
```

That creates user `onigiri` / password `onigiri` and databases `onigiri` and `onigiri_test`.

Copy `.env.example` to `.env`. The default URL talks to Postgres.app on localhost:

```env
DATABASE_URL=postgresql+asyncpg://onigiri:onigiri@localhost:5432/onigiri
```

Do **not** run Docker’s Postgres at the same time — both use port `5432`.

```bash
docker compose stop db
```

## Run the API

Run the API **on your Mac** so it can reach Postgres.app on `localhost`:

```bash
cd backend
../.venv/bin/alembic upgrade head
../.venv/bin/uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

API: [http://localhost:8000](http://localhost:8000)  
Docs: [http://localhost:8000/docs](http://localhost:8000/docs)

If port 8000 is already in use, use `--port 8001` and point the web app at `http://localhost:8001` (Settings, or `web/.env.local`).

Copy `.env.example` to `.env` if you want to override `JWT_SECRET`.

A Dockerized API cannot see Postgres.app (it only listens on your Mac’s localhost). Keep Docker Postgres optional with `docker compose --profile docker-db up` — do not use it while Postgres.app is running.

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

## Web app

With the API running:

```bash
cd web
cp .env.example .env.local   # optional; default is http://localhost:8000
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000). Create an account, add a receipt, and type each item.

Change the API URL under **Settings** if the backend is not on localhost:8000.

## Android

Open `android/` in Android Studio and run the `app` configuration.

| Where the app runs | API base URL |
|---|---|
| Emulator | `http://10.0.2.2:8000` (default) |
| Physical phone on the same LAN | `http://<your-computer-ip>:8000` |

Change the URL under **API settings** on the login screen.

HTTP cleartext is allowed in this MVP so the emulator can talk to Docker on your machine. Use HTTPS if you ever expose the API beyond localhost.

## Tests

Postgres.app must be running, with databases `onigiri` and `onigiri_test`.

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
web/          Next.js app (primary client)
backend/     FastAPI, Alembic, tests
android/      Kotlin + Jetpack Compose
docker-compose.yml
```

## Later

Receipt photos, OCR, categories, charts, budgets, cloud deploy.
