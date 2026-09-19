from datetime import UTC, datetime
from uuid import UUID, uuid4

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models import Merchant


def normalize_merchant_name(name: str) -> tuple[str, str]:
    display = " ".join(name.strip().split())
    return display, display.lower()


async def upsert_merchant(db: AsyncSession, user_id: UUID, raw_name: str) -> Merchant | None:
    display, name_key = normalize_merchant_name(raw_name)
    if not name_key:
        return None
    merchant = await db.scalar(
        select(Merchant).where(Merchant.user_id == user_id, Merchant.name_key == name_key)
    )
    now = datetime.now(UTC)
    if merchant is None:
        merchant = Merchant(
            id=uuid4(),
            user_id=user_id,
            name=display,
            name_key=name_key,
            last_used_at=now,
            created_at=now,
        )
        db.add(merchant)
    else:
        merchant.name = display
        merchant.last_used_at = now
    await db.flush()
    return merchant
