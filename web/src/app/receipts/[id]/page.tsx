"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { formatMoney, labelCategory } from "@/lib/money";
import type { Receipt } from "@/lib/types";

export default function ReceiptDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const [receipt, setReceipt] = useState<Receipt | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [confirm, setConfirm] = useState(false);

  useEffect(() => {
    api
      .getReceipt(params.id)
      .then(setReceipt)
      .catch((err: unknown) => setError(err instanceof Error ? err.message : "Not found"));
  }, [params.id]);

  async function onDelete() {
    try {
      await api.deleteReceipt(params.id);
      router.replace("/");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not delete.");
    }
  }

  if (error && !receipt) return <p className="text-red-700">{error}</p>;
  if (!receipt) return <p className="text-[var(--muted)]">Loading receipt…</p>;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="font-display text-3xl">{receipt.merchant_name}</h1>
          <p className="text-[var(--muted)]">
            {receipt.purchased_at}
            {receipt.category ? ` · ${labelCategory(receipt.category.name)}` : ""}
          </p>
        </div>
        <div className="flex gap-2">
          <Link href={`/receipts/${receipt.id}/edit`} className="btn-primary">
            Edit
          </Link>
          <button type="button" className="rounded-full border border-[var(--line)] px-4 py-2" onClick={() => setConfirm(true)}>
            Delete
          </button>
        </div>
      </div>

      <p className="font-display text-4xl">{formatMoney(receipt.total, receipt.currency)}</p>
      {receipt.total_mismatch ? (
        <p className="text-sm text-red-700">
          Stored total differs from items + tax ({formatMoney(receipt.computed_total, receipt.currency)}).
        </p>
      ) : null}
      {receipt.notes ? <p>{receipt.notes}</p> : null}

      <ul className="space-y-2">
        {receipt.items.map((item) => (
          <li key={item.id} className="card flex items-center justify-between p-4">
            <div>
              <p className="font-medium">{item.name}</p>
              <p className="text-sm text-[var(--muted)]">Qty {item.quantity}</p>
            </div>
            <p>{formatMoney(item.amount, receipt.currency)}</p>
          </li>
        ))}
      </ul>
      <p className="text-sm text-[var(--muted)]">Tax {formatMoney(receipt.tax, receipt.currency)}</p>
      {error ? <p className="text-sm text-red-700">{error}</p> : null}

      {confirm ? (
        <div className="card p-4">
          <p className="font-medium">Delete this receipt?</p>
          <div className="mt-3 flex gap-3">
            <button type="button" className="btn-primary" onClick={onDelete}>
              Delete
            </button>
            <button type="button" onClick={() => setConfirm(false)}>
              Cancel
            </button>
          </div>
        </div>
      ) : null}
    </div>
  );
}
