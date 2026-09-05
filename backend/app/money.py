from decimal import ROUND_HALF_UP, Decimal
from typing import Annotated

from pydantic import BeforeValidator, PlainSerializer

TWOPLACE = Decimal("0.01")
THREEPLACE = Decimal("0.001")


def parse_decimal(value: object) -> Decimal:
    if isinstance(value, Decimal):
        return value
    if isinstance(value, (int, float, str)):
        return Decimal(str(value))
    raise TypeError(f"Cannot parse decimal from {type(value)}")


def money(value: Decimal) -> Decimal:
    return parse_decimal(value).quantize(TWOPLACE, rounding=ROUND_HALF_UP)


def qty(value: Decimal) -> Decimal:
    return parse_decimal(value).quantize(THREEPLACE, rounding=ROUND_HALF_UP)


Money = Annotated[
    Decimal,
    BeforeValidator(parse_decimal),
    PlainSerializer(lambda v: format(money(v), "f"), return_type=str),
]

Quantity = Annotated[
    Decimal,
    BeforeValidator(parse_decimal),
    PlainSerializer(lambda v: format(qty(v), "f"), return_type=str),
]
