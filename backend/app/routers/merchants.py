from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db import get_db
from app.deps import get_current_user
from app.models import Merchant, User
from app.schemas import MerchantOut

router = APIRouter(prefix="/merchants", tags=["merchants"])


@router.get("", response_model=list[MerchantOut])
async def list_merchants(
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> list[MerchantOut]:
    rows = (
        await db.scalars(
            select(Merchant)
            .where(Merchant.user_id == user.id)
            .order_by(Merchant.last_used_at.desc(), Merchant.name.asc())
        )
    ).all()
    return [MerchantOut.model_validate(row) for row in rows]
