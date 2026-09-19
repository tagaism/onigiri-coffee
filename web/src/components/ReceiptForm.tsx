"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import {
  TAX_RATES,
  formatMoney,
  inferTaxRate,
  labelCategory,
  lineAmount,
  parseMoney,
  taxFromRate,
  todayIso,
  type TaxRate,
} from "@/lib/money";
import { getCurrency } from "@/lib/session";
import type { Category, Merchant, Receipt } from "@/lib/types";

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
  const [notes, setNotes] = useState(receipt?.notes ?? "");
  const [items, setItems] = useState<ItemDraft[]>(
    receipt ? fromReceipt(receipt) : [blankItem()],
  );
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [categories, setCategories] = useState<Category[]>([]);
  const [merchants, setMerchants] = useState<Merchant[]>([]);
  const [categoryId, setCategoryId] = useState(receipt?.category?.id ?? "");
  const [newCategory, setNewCategory] = useState("");
  const [addingCategory, setAddingCategory] = useState(false);
  const currency = receipt?.currency ?? getCurrency();
  const initialSubtotal = receipt
    ? receipt.items.reduce((sum, item) => sum + (parseMoney(item.amount) ?? 0), 0)
    : 0;
  const [taxRate, setTaxRate] = useState<TaxRate>(
    receipt?.tax_rate === 8 || receipt?.tax_rate === 10
      ? receipt.tax_rate
      : receipt
        ? inferTaxRate(receipt.tax, initialSubtotal)
        : 10,
  );

  useEffect(() => {
    api.listCategories().then(setCategories).catch(() => setCategories([]));
    api.listMerchants().then(setMerchants).catch(() => setMerchants([]));
  }, []);

  const itemsTotal = useMemo(
    () =>
      items.reduce(
        (sum, item) => sum + lineAmount(item.quantity, item.unitPrice, item.amount),
        0,
      ),
    [items],
  );
  const taxAmount = taxFromRate(itemsTotal, taxRate);
  const total = itemsTotal + taxAmount;

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
        tax_rate: taxRate,
        notes: notes.trim() || null,
        category_id: categoryId || null,
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
          autoComplete="off"
          list="saved-merchants"
        />
        <datalist id="saved-merchants">
          {merchants.map((saved) => (
            <option key={saved.id} value={saved.name} />
          ))}
        </datalist>
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
          <select
            value={taxRate}
            onChange={(e) => setTaxRate(Number(e.target.value) as TaxRate)}
            className="field"
          >
            {TAX_RATES.map((rate) => (
              <option key={rate} value={rate}>
                {rate}%
              </option>
            ))}
          </select>
          <span className="mt-1 block text-xs text-[var(--muted)]">
            {formatMoney(taxAmount, currency)}
          </span>
        </label>
      </div>
      <label className="block">
        <span className="mb-1 block text-sm text-[var(--muted)]">Category</span>
        <select
          value={categoryId}
          onChange={(e) => setCategoryId(e.target.value)}
          className="field"
        >
          <option value="">Uncategorized</option>
          {categories.map((category) => (
            <option key={category.id} value={category.id}>
              {labelCategory(category.name)}
            </option>
          ))}
        </select>
      </label>
      <div className="flex gap-2">
        <input
          value={newCategory}
          onChange={(e) => setNewCategory(e.target.value)}
          className="field"
          placeholder="New custom category"
        />
        <button
          type="button"
          className="shrink-0 rounded-full border border-[var(--line)] px-4 py-2 text-sm"
          disabled={addingCategory || !newCategory.trim()}
          onClick={async () => {
            setAddingCategory(true);
            setError(null);
            try {
              const created = await api.createCategory(newCategory.trim());
              setCategories((current) => [...current, created]);
              setCategoryId(created.id);
              setNewCategory("");
            } catch (err) {
              setError(err instanceof Error ? err.message : "Could not add category.");
            } finally {
              setAddingCategory(false);
            }
          }}
        >
          {addingCategory ? "Adding…" : "Add"}
        </button>
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
