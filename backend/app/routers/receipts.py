from datetime import date
from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession
from app.db import get_db
from app.deps import get_current_user
from app.models import LineItem, Receipt, User
from app.receipts import apply_patch, create_receipt, get_owned_receipt, to_receipt_out
from app.schemas import ReceiptIn, ReceiptListOut, ReceiptOut, ReceiptPatchIn

router = APIRouter(prefix="/receipts", tags=["receipts"])


@router.post("", response_model=ReceiptOut, status_code=status.HTTP_201_CREATED)
async def create(
    body: ReceiptIn,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> ReceiptOut:
    receipt = create_receipt(user, body)
    db.add(receipt)
    await db.commit()
    created = await get_owned_receipt(db, user.id, receipt.id)
    assert created is not None
    return to_receipt_out(created)


@router.get("", response_model=list[ReceiptListOut])
async def list_receipts(
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
    from_date: Annotated[date | None, Query(alias="from")] = None,
    to_date: Annotated[date | None, Query(alias="to")] = None,
    limit: Annotated[int, Query(ge=1, le=200)] = 50,
    offset: Annotated[int, Query(ge=0)] = 0,
) -> list[ReceiptListOut]:
    item_count = (
        select(func.count(LineItem.id))
        .where(LineItem.receipt_id == Receipt.id)
        .correlate(Receipt)
        .scalar_subquery()
    )
    stmt = select(Receipt, item_count).where(Receipt.user_id == user.id)
    if from_date is not None:
        stmt = stmt.where(Receipt.purchased_at >= from_date)
    if to_date is not None:
        stmt = stmt.where(Receipt.purchased_at <= to_date)
    stmt = stmt.order_by(Receipt.purchased_at.desc(), Receipt.created_at.desc())
    stmt = stmt.limit(limit).offset(offset)
    rows = (await db.execute(stmt)).all()
    return [
        ReceiptListOut(
            id=receipt.id,
            merchant_name=receipt.merchant_name,
            purchased_at=receipt.purchased_at,
            currency=receipt.currency,
            tax=receipt.tax,
            total=receipt.total,
            item_count=count or 0,
            created_at=receipt.created_at,
        )
        for receipt, count in rows
    ]


@router.get("/{receipt_id}", response_model=ReceiptOut)
async def get_receipt(
    receipt_id: UUID,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> ReceiptOut:
    receipt = await get_owned_receipt(db, user.id, receipt_id)
    if receipt is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Receipt not found")
    return to_receipt_out(receipt)


@router.patch("/{receipt_id}", response_model=ReceiptOut)
async def patch_receipt(
    receipt_id: UUID,
    body: ReceiptPatchIn,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> ReceiptOut:
    receipt = await get_owned_receipt(db, user.id, receipt_id)
    if receipt is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Receipt not found")
    await apply_patch(db, receipt, body)
    await db.commit()
    updated = await get_owned_receipt(db, user.id, receipt_id)
    assert updated is not None
    return to_receipt_out(updated)


@router.delete("/{receipt_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_receipt(
    receipt_id: UUID,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> None:
    receipt = await db.scalar(
        select(Receipt).where(Receipt.id == receipt_id, Receipt.user_id == user.id)
    )
    if receipt is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Receipt not found")
    await db.delete(receipt)
    await db.commit()
