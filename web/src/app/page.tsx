"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { formatMoney, labelCategory, monthRange } from "@/lib/money";
import { getCurrency } from "@/lib/session";
import type { ReceiptListItem, Summary } from "@/lib/types";

export default function HomePage() {
  const range = monthRange();
  const [summary, setSummary] = useState<Summary | null>(null);
  const [receipts, setReceipts] = useState<ReceiptListItem[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const currency = receipts[0]?.currency ?? getCurrency();

  useEffect(() => {
    let cancelled = false;
    Promise.all([api.summary(range.from, range.to), api.listReceipts(range.from, range.to)])
      .then(([nextSummary, nextReceipts]) => {
        if (cancelled) return;
        setSummary(nextSummary);
        setReceipts(nextReceipts);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(err instanceof Error ? err.message : "Could not load spendings.");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [range.from, range.to]);

  if (loading) return <p className="text-[var(--muted)]">Loading this month…</p>;
  if (error) {
    return (
      <div className="card p-6">
        <p className="text-red-700">{error}</p>
        <p className="mt-2 text-sm text-[var(--muted)]">
          Is the API running at localhost:8000? Check Settings if you changed the URL.
        </p>
        <Link href="/settings" className="mt-4 inline-block text-sm text-[var(--terracotta)]">
          Open settings
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <section className="card p-6">
        <p className="text-sm text-[var(--muted)]">{range.label}</p>
        <p className="font-display text-4xl tracking-tight">
          {formatMoney(summary?.total ?? "0", currency)}
        </p>
        <p className="mt-1 text-sm text-[var(--muted)]">
          {summary?.receipt_count ?? 0} receipts this month
        </p>
        {summary?.by_category && summary.by_category.length > 0 ? (
          <ul className="mt-4 space-y-1 text-sm text-[var(--muted)]">
            {summary.by_category.map((row) => (
              <li key={row.category_id ?? "none"} className="flex justify-between">
                <span>{labelCategory(row.name)}</span>
                <span>{formatMoney(row.total, currency)}</span>
              </li>
            ))}
          </ul>
        ) : null}
      </section>

      {receipts.length === 0 ? (
        <div className="card p-8 text-center">
          <p className="font-display text-xl">No spendings yet</p>
          <p className="mt-2 text-sm text-[var(--muted)]">
            Add a receipt and type each item by hand.
          </p>
          <Link href="/receipts/new" className="btn-primary mt-5">
            Add receipt
          </Link>
        </div>
      ) : (
        <ul className="space-y-3">
          {receipts.map((receipt) => (
            <li key={receipt.id}>
              <Link
                href={`/receipts/${receipt.id}`}
                className="card flex items-center justify-between gap-4 p-4 hover:border-[var(--terracotta)]"
              >
                <div>
                  <p className="font-medium">{receipt.merchant_name}</p>
                  <p className="text-sm text-[var(--muted)]">
                    {receipt.purchased_at} · {receipt.item_count} items
                    {receipt.category ? ` · ${labelCategory(receipt.category.name)}` : ""}
                  </p>
                </div>
                <p className="font-display text-lg">
                  {formatMoney(receipt.total, receipt.currency)}
                </p>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
