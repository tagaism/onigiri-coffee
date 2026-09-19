from uuid import UUID, uuid4

from fastapi import HTTPException, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models import Category

DEFAULT_CATEGORY_NAMES = (
    "food",
    "restaurants",
    "amusement",
    "education",
    "transport",
)


def normalize_name(name: str) -> str:
    return " ".join(name.strip().split()).lower()


async def ensure_default_categories(db: AsyncSession, user_id: UUID) -> list[Category]:
    existing = list(
        (await db.scalars(select(Category).where(Category.user_id == user_id))).all()
    )
    have = {category.name_key for category in existing}
    added = False
    for index, name in enumerate(DEFAULT_CATEGORY_NAMES):
        if name in have:
            continue
        db.add(
            Category(
                id=uuid4(),
                user_id=user_id,
                name=name,
                name_key=name,
                is_default=True,
                sort_order=index,
            )
        )
        added = True
    if added:
        await db.flush()
        existing = list(
            (await db.scalars(select(Category).where(Category.user_id == user_id))).all()
        )
    existing.sort(key=lambda category: (not category.is_default, category.sort_order, category.name))
    return existing


async def get_owned_category(
    db: AsyncSession, user_id: UUID, category_id: UUID
) -> Category | None:
    return await db.scalar(
        select(Category).where(Category.id == category_id, Category.user_id == user_id)
    )


async def require_owned_category(
    db: AsyncSession, user_id: UUID, category_id: UUID
) -> Category:
    category = await get_owned_category(db, user_id, category_id)
    if category is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Category not found")
    return category
