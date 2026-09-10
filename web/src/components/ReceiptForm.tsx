"use client";

import { FormEvent, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { formatMoney, lineAmount, parseMoney, todayIso } from "@/lib/money";
import { getCurrency } from "@/lib/session";
import type { Receipt } from "@/lib/types";

type ItemDraft = {
  key: string;
  name: string;
  quantity: string;
  unitPrice: string;
  amount: string;
};

function blankItem(): ItemDraft {
  return {
    key: crypto.randomUUID(),
    name: "",
    quantity: "1",
    unitPrice: "",
    amount: "",
  };
}

function fromReceipt(receipt: Receipt): ItemDraft[] {
  return receipt.items.map((item) => ({
    key: item.id,
    name: item.name,
    quantity: item.quantity,
    unitPrice: item.unit_price ?? "",
    amount: item.amount,
  }));
}

export function ReceiptForm({ receipt }: { receipt?: Receipt }) {
  const router = useRouter();
  const [merchant, setMerchant] = useState(receipt?.merchant_name ?? "");
  const [purchasedAt, setPurchasedAt] = useState(receipt?.purchased_at ?? todayIso());
  const [tax, setTax] = useState(receipt?.tax ?? "0");
  const [notes, setNotes] = useState(receipt?.notes ?? "");
  const [items, setItems] = useState<ItemDraft[]>(
    receipt ? fromReceipt(receipt) : [blankItem()],
  );
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const currency = receipt?.currency ?? getCurrency();

  const total = useMemo(() => {
    const itemsTotal = items.reduce(
      (sum, item) => sum + lineAmount(item.quantity, item.unitPrice, item.amount),
      0,
    );
    return itemsTotal + (parseMoney(tax) ?? 0);
  }, [items, tax]);

  function updateItem(key: string, patch: Partial<ItemDraft>) {
    setItems((current) => current.map((item) => (item.key === key ? { ...item, ...patch } : item)));
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    if (!merchant.trim()) {
      setError("Merchant is required.");
      return;
    }
    const payloadItems = items
      .filter((item) => item.name.trim())
      .map((item) => ({
        name: item.name.trim(),
        quantity: item.quantity.trim() || "1",
        unit_price: item.unitPrice.trim() || null,
        amount: String(lineAmount(item.quantity, item.unitPrice, item.amount)),
      }));
    if (payloadItems.length === 0) {
      setError("Add at least one item with a name.");
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const body = {
        merchant_name: merchant.trim(),
        purchased_at: purchasedAt,
        currency,
        tax: String(parseMoney(tax) ?? 0),
        notes: notes.trim() || null,
        items: payloadItems,
      };
      const saved = receipt
        ? await api.updateReceipt(receipt.id, body)
        : await api.createReceipt(body);
      router.replace(`/receipts/${saved.id}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not save receipt.");
      setSaving(false);
    }
  }

  return (
    <form onSubmit={onSubmit} className="space-y-5">
      <label className="block">
        <span className="mb-1 block text-sm text-[var(--muted)]">Merchant</span>
        <input
          value={merchant}
          onChange={(e) => setMerchant(e.target.value)}
          className="field"
          placeholder="Onigiri Coffee"
          autoComplete="organization"
        />
      </label>
      <div className="grid gap-4 sm:grid-cols-2">
        <label className="block">
          <span className="mb-1 block text-sm text-[var(--muted)]">Date</span>
          <input
            type="date"
            value={purchasedAt}
            onChange={(e) => setPurchasedAt(e.target.value)}
            className="field"
          />
        </label>
        <label className="block">
          <span className="mb-1 block text-sm text-[var(--muted)]">Tax</span>
          <input
            value={tax}
            onChange={(e) => setTax(e.target.value)}
            className="field"
            inputMode="decimal"
          />
        </label>
      </div>
      <label className="block">
        <span className="mb-1 block text-sm text-[var(--muted)]">Notes</span>
        <input
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          className="field"
          placeholder="Optional"
        />
      </label>

      <div className="flex items-center justify-between">
        <h2 className="font-display text-lg">Items</h2>
        <button
          type="button"
          className="text-sm font-medium text-[var(--terracotta)]"
          onClick={() => setItems((current) => [...current, blankItem()])}
        >
          + Add item
        </button>
      </div>

      <div className="space-y-3">
        {items.map((item) => (
          <div key={item.key} className="card grid gap-2 p-3 sm:grid-cols-12 sm:items-end">
            <label className="sm:col-span-5">
              <span className="mb-1 block text-xs text-[var(--muted)]">Name</span>
              <input
                value={item.name}
                onChange={(e) => updateItem(item.key, { name: e.target.value })}
                className="field"
                placeholder="Latte"
              />
            </label>
            <label className="sm:col-span-2">
              <span className="mb-1 block text-xs text-[var(--muted)]">Qty</span>
              <input
                value={item.quantity}
                onChange={(e) => updateItem(item.key, { quantity: e.target.value })}
                className="field"
                inputMode="decimal"
              />
            </label>
            <label className="sm:col-span-2">
              <span className="mb-1 block text-xs text-[var(--muted)]">Unit</span>
              <input
                value={item.unitPrice}
                onChange={(e) => updateItem(item.key, { unitPrice: e.target.value })}
                className="field"
                inputMode="decimal"
              />
            </label>
            <label className="sm:col-span-2">
              <span className="mb-1 block text-xs text-[var(--muted)]">Amount</span>
              <input
                value={item.amount}
                onChange={(e) => updateItem(item.key, { amount: e.target.value })}
                className="field"
                inputMode="decimal"
                placeholder={String(lineAmount(item.quantity, item.unitPrice, item.amount) || "")}
              />
            </label>
            <button
              type="button"
              className="text-sm text-[var(--muted)] hover:text-[var(--ink)] sm:col-span-1 sm:mb-2"
              onClick={() =>
                setItems((current) =>
                  current.length === 1 ? [blankItem()] : current.filter((row) => row.key !== item.key),
                )
              }
              aria-label="Remove item"
            >
              ✕
            </button>
          </div>
        ))}
      </div>

      <p className="font-display text-2xl">{formatMoney(total, currency)}</p>
      {error ? <p className="text-sm text-red-700">{error}</p> : null}
      <button type="submit" className="btn-primary w-full sm:w-auto" disabled={saving}>
        {saving ? "Saving…" : "Save receipt"}
      </button>
    </form>
  );
}
