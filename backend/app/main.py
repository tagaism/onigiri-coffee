import logging

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.routers import auth, categories, receipts, stats

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")

app = FastAPI(title="Onigiri Spend", version="0.1.0")

origins = (
    ["*"]
    if settings.cors_origins.strip() == "*"
    else [origin.strip() for origin in settings.cors_origins.split(",") if origin.strip()]
)
app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth.router)
app.include_router(categories.router)
app.include_router(receipts.router)
app.include_router(stats.router)


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok"}
