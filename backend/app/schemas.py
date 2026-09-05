from __future__ import annotations

from datetime import date, datetime
from decimal import Decimal
from typing import Self
from uuid import UUID

from pydantic import BaseModel, EmailStr, Field, field_validator, model_validator

from app.money import Money, Quantity, money, parse_decimal, qty


class UserOut(BaseModel):
    id: UUID
    email: str
    default_currency: str
    created_at: datetime

    model_config = {"from_attributes": True}


class RegisterIn(BaseModel):
    email: EmailStr
    password: str = Field(min_length=8, max_length=72)
    default_currency: str | None = None

    @field_validator("email")
    @classmethod
    def normalize_email(cls, value: str) -> str:
        return value.lower().strip()

    @field_validator("default_currency")
    @classmethod
    def currency_code(cls, value: str | None) -> str | None:
        if value is None:
            return value
        value = value.upper()
        if len(value) != 3 or not value.isalpha():
            raise ValueError("currency must be a 3-letter ISO code")
        return value


class LoginIn(BaseModel):
    email: EmailStr
    password: str

    @field_validator("email")
    @classmethod
    def normalize_email(cls, value: str) -> str:
        return value.lower().strip()


class TokenOut(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: UserOut


class LineItemIn(BaseModel):
    name: str = Field(min_length=1, max_length=500)
    quantity: Quantity = Decimal("1")
    unit_price: Money | None = None
    amount: Money | None = None

    @model_validator(mode="after")
    def fill_amount(self) -> Self:
        if self.amount is None:
            if self.unit_price is None:
                raise ValueError("amount or unit_price is required")
            object.__setattr__(
                self,
                "amount",
                money(qty(self.quantity) * money(self.unit_price)),
            )
        if self.quantity <= 0:
            raise ValueError("quantity must be greater than 0")
        if self.amount < 0:
            raise ValueError("amount must be >= 0")
        return self


class LineItemOut(BaseModel):
    id: UUID
    name: str
    quantity: Quantity
    unit_price: Money | None
    amount: Money
    sort_order: int

    model_config = {"from_attributes": True}


class ReceiptIn(BaseModel):
    merchant_name: str = Field(min_length=1, max_length=255)
    purchased_at: date
    currency: str | None = None
    tax: Money = Decimal("0")
    total: Money | None = None
    notes: str | None = None
    items: list[LineItemIn] = Field(min_length=1)

    @field_validator("currency")
    @classmethod
    def currency_code(cls, value: str | None) -> str | None:
        if value is None:
            return value
        value = value.upper()
        if len(value) != 3 or not value.isalpha():
            raise ValueError("currency must be a 3-letter ISO code")
        return value

    @field_validator("tax")
    @classmethod
    def tax_non_negative(cls, value: Decimal) -> Decimal:
        if parse_decimal(value) < 0:
            raise ValueError("tax must be >= 0")
        return value


class ReceiptPatchIn(BaseModel):
    merchant_name: str | None = Field(default=None, min_length=1, max_length=255)
    purchased_at: date | None = None
    currency: str | None = None
    tax: Money | None = None
    total: Money | None = None
    notes: str | None = None
    items: list[LineItemIn] | None = Field(default=None, min_length=1)

    @field_validator("currency")
    @classmethod
    def currency_code(cls, value: str | None) -> str | None:
        if value is None:
            return value
        value = value.upper()
        if len(value) != 3 or not value.isalpha():
            raise ValueError("currency must be a 3-letter ISO code")
        return value


class ReceiptOut(BaseModel):
    id: UUID
    merchant_name: str
    purchased_at: date
    currency: str
    tax: Money
    total: Money
    computed_total: Money
    total_mismatch: bool
    notes: str | None
    items: list[LineItemOut]
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}


class ReceiptListOut(BaseModel):
    id: UUID
    merchant_name: str
    purchased_at: date
    currency: str
    tax: Money
    total: Money
    item_count: int
    created_at: datetime

    model_config = {"from_attributes": True}


class DayTotalOut(BaseModel):
    date: date
    count: int
    total: Money


class SummaryOut(BaseModel):
    from_date: date = Field(serialization_alias="from")
    to_date: date = Field(serialization_alias="to")
    receipt_count: int
    total: Money
    by_day: list[DayTotalOut]

    model_config = {"populate_by_name": True}
