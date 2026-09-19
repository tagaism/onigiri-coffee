"""saved merchants

Revision ID: 0004
Revises: 0003
Create Date: 2026-09-19

"""

from typing import Sequence, Union
from uuid import uuid4

import sqlalchemy as sa
from alembic import op

revision: str = "0004"
down_revision: Union[str, None] = "0003"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "merchants",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("user_id", sa.Uuid(), nullable=False),
        sa.Column("name", sa.String(length=255), nullable=False),
        sa.Column("name_key", sa.String(length=255), nullable=False),
        sa.Column(
            "last_used_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("user_id", "name_key", name="uq_merchants_user_name"),
    )
    op.create_index("ix_merchants_user_id", "merchants", ["user_id"])

    connection = op.get_bind()
    rows = connection.execute(
        sa.text(
            """
            SELECT user_id, merchant_name, created_at
            FROM receipts
            WHERE btrim(merchant_name) <> ''
            ORDER BY created_at DESC
            """
        )
    ).mappings()
    seen: set[tuple] = set()
    for row in rows:
        name = " ".join(str(row["merchant_name"]).split())
        name_key = name.lower()
        key = (row["user_id"], name_key)
        if not name_key or key in seen:
            continue
        seen.add(key)
        connection.execute(
            sa.text(
                """
                INSERT INTO merchants (id, user_id, name, name_key, last_used_at, created_at)
                VALUES (:id, :user_id, :name, :name_key, :used, :used)
                """
            ),
            {
                "id": uuid4(),
                "user_id": row["user_id"],
                "name": name,
                "name_key": name_key,
                "used": row["created_at"],
            },
        )


def downgrade() -> None:
    op.drop_index("ix_merchants_user_id", table_name="merchants")
    op.drop_table("merchants")
