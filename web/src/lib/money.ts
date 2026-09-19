export function parseMoney(raw: string): number | null {
  const trimmed = raw.trim().replace(/,/g, "");
  if (!trimmed) return null;
  const value = Number(trimmed);
  return Number.isFinite(value) ? value : null;
}

export function formatMoney(amount: string | number, currency: string): string {
  const value = typeof amount === "number" ? amount : parseMoney(amount);
  if (value === null) return String(amount);
  try {
    return new Intl.NumberFormat(undefined, {
      style: "currency",
      currency,
      maximumFractionDigits: currency === "JPY" ? 0 : 2,
      minimumFractionDigits: currency === "JPY" ? 0 : 2,
    }).format(value);
  } catch {
    return `${value} ${currency}`;
  }
}

export function lineAmount(quantity: string, unitPrice: string, amount: string): number {
  const explicit = parseMoney(amount);
  if (explicit !== null) return Math.round(explicit * 100) / 100;
  const qty = parseMoney(quantity) ?? 1;
  const price = parseMoney(unitPrice) ?? 0;
  return Math.round(qty * price * 100) / 100;
}

export function monthRange(date = new Date()): { from: string; to: string; label: string } {
  const year = date.getFullYear();
  const month = date.getMonth();
  const from = new Date(year, month, 1);
  const to = new Date(year, month + 1, 0);
  const iso = (d: Date) =>
    `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
  const label = from.toLocaleDateString(undefined, { month: "long", year: "numeric" });
  return { from: iso(from), to: iso(to), label };
}

export function labelCategory(name: string): string {
  if (!name) return name;
  return name.charAt(0).toUpperCase() + name.slice(1);
}

export function todayIso(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}
