from datetime import date
from decimal import Decimal
from typing import Annotated

from fastapi import APIRouter, Depends, Query
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db import get_db
from app.deps import get_current_user
from app.models import Category, Receipt, User
from app.money import money
from app.schemas import CategoryTotalOut, DayTotalOut, SummaryOut

router = APIRouter(prefix="/stats", tags=["stats"])


def default_month_range(today: date) -> tuple[date, date]:
    start = today.replace(day=1)
    if today.month == 12:
        end = date(today.year, 12, 31)
    else:
        end = date(today.year, today.month + 1, 1)
        end = date.fromordinal(end.toordinal() - 1)
    return start, end


@router.get("/summary", response_model=SummaryOut)
async def summary(
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
    from_date: Annotated[date | None, Query(alias="from")] = None,
    to_date: Annotated[date | None, Query(alias="to")] = None,
) -> SummaryOut:
    if from_date is None or to_date is None:
        month_start, month_end = default_month_range(date.today())
        from_date = from_date or month_start
        to_date = to_date or month_end

    filters = [
        Receipt.user_id == user.id,
        Receipt.purchased_at >= from_date,
        Receipt.purchased_at <= to_date,
    ]

    totals_row = (
        await db.execute(
            select(func.count(Receipt.id), func.coalesce(func.sum(Receipt.total), 0)).where(*filters)
        )
    ).one()
    receipt_count = int(totals_row[0])
    total = money(Decimal(str(totals_row[1])))

    day_rows = (
        await db.execute(
            select(
                Receipt.purchased_at,
                func.count(Receipt.id),
                func.coalesce(func.sum(Receipt.total), 0),
            )
            .where(*filters)
            .group_by(Receipt.purchased_at)
            .order_by(Receipt.purchased_at.asc())
        )
    ).all()

    by_day = [
        DayTotalOut(
            date=day,
            count=int(count),
            total=money(Decimal(str(day_total))),
        )
        for day, count, day_total in day_rows
    ]

    category_rows = (
        await db.execute(
            select(
                Category.id,
                Category.name,
                func.count(Receipt.id),
                func.coalesce(func.sum(Receipt.total), 0),
            )
            .select_from(Receipt)
            .outerjoin(Category, Category.id == Receipt.category_id)
            .where(*filters)
            .group_by(Category.id, Category.name)
            .order_by(func.coalesce(func.sum(Receipt.total), 0).desc())
        )
    ).all()
    by_category = [
        CategoryTotalOut(
            category_id=category_id,
            name=name or "uncategorized",
            count=int(count),
            total=money(Decimal(str(cat_total))),
        )
        for category_id, name, count, cat_total in category_rows
    ]

    return SummaryOut(
        from_date=from_date,
        to_date=to_date,
        receipt_count=receipt_count,
        total=total,
        by_day=by_day,
        by_category=by_category,
    )
