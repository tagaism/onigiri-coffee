from datetime import UTC, datetime
from decimal import Decimal
from uuid import UUID, uuid4

from sqlalchemy import delete, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.models import Category, LineItem, Receipt, User
from app.money import money, qty
from app.schemas import CategoryOut, LineItemIn, ReceiptIn, ReceiptOut, ReceiptPatchIn


def items_sum(items: list[LineItem]) -> Decimal:
    return money(sum((item.amount for item in items), Decimal("0")))


def computed_total(receipt: Receipt) -> Decimal:
    return money(items_sum(list(receipt.items)) + money(receipt.tax))


def to_receipt_out(receipt: Receipt) -> ReceiptOut:
    computed = computed_total(receipt)
    stored = money(receipt.total)
    return ReceiptOut(
        id=receipt.id,
        merchant_name=receipt.merchant_name,
        purchased_at=receipt.purchased_at,
        currency=receipt.currency,
        tax=money(receipt.tax),
        total=stored,
        computed_total=computed,
        total_mismatch=stored != computed,
        notes=receipt.notes,
        category=CategoryOut.model_validate(receipt.category) if receipt.category else None,
        items=receipt.items,
        created_at=receipt.created_at,
        updated_at=receipt.updated_at,
    )


def build_line_items(items: list[LineItemIn]) -> list[LineItem]:
    result: list[LineItem] = []
    for index, item in enumerate(items):
        unit = money(item.unit_price) if item.unit_price is not None else None
        result.append(
            LineItem(
                id=uuid4(),
                name=item.name.strip(),
                quantity=qty(item.quantity),
                unit_price=unit,
                amount=money(item.amount),  # type: ignore[arg-type]
                sort_order=index,
            )
        )
    return result


def resolve_total(
    items: list[LineItem], tax: Decimal, explicit: Decimal | None
) -> Decimal:
    computed = money(items_sum(items) + money(tax))
    if explicit is None:
        return computed
    return money(explicit)


async def get_owned_receipt(db: AsyncSession, user_id: UUID, receipt_id: UUID) -> Receipt | None:
    stmt = (
        select(Receipt)
        .where(Receipt.id == receipt_id, Receipt.user_id == user_id)
        .options(selectinload(Receipt.items), selectinload(Receipt.category))
    )
    return await db.scalar(stmt)


def create_receipt(user: User, body: ReceiptIn, category: Category | None = None) -> Receipt:
    items = build_line_items(body.items)
    tax = money(body.tax)
    currency = (body.currency or user.default_currency).upper()
    now = datetime.now(UTC)
    return Receipt(
        id=uuid4(),
        user_id=user.id,
        merchant_name=body.merchant_name.strip(),
        purchased_at=body.purchased_at,
        currency=currency,
        tax=tax,
        total=resolve_total(items, tax, body.total),
        notes=body.notes,
        category_id=category.id if category else None,
        items=items,
        created_at=now,
        updated_at=now,
    )


async def apply_patch(db: AsyncSession, receipt: Receipt, body: ReceiptPatchIn) -> Receipt:
    if body.merchant_name is not None:
        receipt.merchant_name = body.merchant_name.strip()
    if body.purchased_at is not None:
        receipt.purchased_at = body.purchased_at
    if body.currency is not None:
        receipt.currency = body.currency
    if body.notes is not None:
        receipt.notes = body.notes
    if "category_id" in body.model_fields_set:
        receipt.category_id = body.category_id
    if body.tax is not None:
        receipt.tax = money(body.tax)
    if body.items is not None:
        await db.execute(delete(LineItem).where(LineItem.receipt_id == receipt.id))
        receipt.items = build_line_items(body.items)
    if body.total is not None or body.items is not None or body.tax is not None:
        receipt.total = resolve_total(list(receipt.items), receipt.tax, body.total)
    receipt.updated_at = datetime.now(UTC)
    return receipt
