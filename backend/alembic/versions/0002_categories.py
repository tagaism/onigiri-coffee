"""categories

Revision ID: 0002
Revises: 0001
Create Date: 2026-09-15

"""

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "0002"
down_revision: Union[str, None] = "0001"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "categories",
        sa.Column("id", sa.Uuid(), nullable=False),
        sa.Column("user_id", sa.Uuid(), nullable=False),
        sa.Column("name", sa.String(length=80), nullable=False),
        sa.Column("name_key", sa.String(length=80), nullable=False),
        sa.Column("is_default", sa.Boolean(), nullable=False),
        sa.Column("sort_order", sa.Integer(), nullable=False),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("user_id", "name_key", name="uq_categories_user_name"),
    )
    op.create_index("ix_categories_user_id", "categories", ["user_id"])
    op.add_column("receipts", sa.Column("category_id", sa.Uuid(), nullable=True))
    op.create_index("ix_receipts_category_id", "receipts", ["category_id"])
    op.create_foreign_key(
        "fk_receipts_category_id",
        "receipts",
        "categories",
        ["category_id"],
        ["id"],
        ondelete="SET NULL",
    )


def downgrade() -> None:
    op.drop_constraint("fk_receipts_category_id", "receipts", type_="foreignkey")
    op.drop_index("ix_receipts_category_id", table_name="receipts")
    op.drop_column("receipts", "category_id")
    op.drop_index("ix_categories_user_id", table_name="categories")
    op.drop_table("categories")
