# Onigiri web

Primary client for the spending tracker. Talks to the FastAPI backend.

```bash
# from repo root
docker compose up

cd web
cp .env.example .env.local
npm install
npm run dev
```

Open http://localhost:3000 (or another port if 3000 is busy: `npm run dev -- -p 3001`).

Default API URL is `http://localhost:8000`. Change it in Settings or `.env.local`.
