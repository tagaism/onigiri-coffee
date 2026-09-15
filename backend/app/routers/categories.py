from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import func, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.ext.asyncio import AsyncSession

from app.categories import ensure_default_categories, normalize_name, require_owned_category
from app.db import get_db
from app.deps import get_current_user
from app.models import Category, User
from app.schemas import CategoryIn, CategoryOut

router = APIRouter(prefix="/categories", tags=["categories"])


@router.get("", response_model=list[CategoryOut])
async def list_categories(
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> list[CategoryOut]:
    categories = await ensure_default_categories(db, user.id)
    await db.commit()
    return [CategoryOut.model_validate(category) for category in categories]


@router.post("", response_model=CategoryOut, status_code=status.HTTP_201_CREATED)
async def create_category(
    body: CategoryIn,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> CategoryOut:
    await ensure_default_categories(db, user.id)
    name = " ".join(body.name.strip().split())
    name_key = normalize_name(name)
    if not name_key:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail="Name is required")
    max_order = await db.scalar(
        select(func.max(Category.sort_order)).where(Category.user_id == user.id)
    )
    category = Category(
        user_id=user.id,
        name=name,
        name_key=name_key,
        is_default=False,
        sort_order=(max_order or 0) + 1,
    )
    db.add(category)
    try:
        await db.commit()
    except IntegrityError as exc:
        await db.rollback()
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT, detail="Category already exists"
        ) from exc
    await db.refresh(category)
    return CategoryOut.model_validate(category)


@router.delete("/{category_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_category(
    category_id: UUID,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> None:
    category = await require_owned_category(db, user.id, category_id)
    if category.is_default:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST, detail="Default categories cannot be deleted"
        )
    await db.delete(category)
    await db.commit()
