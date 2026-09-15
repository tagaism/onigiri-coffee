import { getApiUrl, getToken } from "./session";
import type {
  Category,
  Receipt,
  ReceiptIn,
  ReceiptListItem,
  Summary,
  TokenResponse,
  User,
} from "./types";

type Detail = { detail?: string | { msg?: string }[] };

async function request<T>(path: string, init: RequestInit = {}, auth = true): Promise<T> {
  const headers = new Headers(init.headers);
  if (!headers.has("Content-Type") && init.body) {
    headers.set("Content-Type", "application/json");
  }
  if (auth) {
    const token = getToken();
    if (token) headers.set("Authorization", `Bearer ${token}`);
  }
  let response: Response;
  try {
    response = await fetch(`${getApiUrl()}${path}`, { ...init, headers });
  } catch {
    throw new Error("Cannot reach the API. Start it with docker compose up and check the URL in Settings.");
  }
  if (response.status === 204) return undefined as T;
  const text = await response.text();
  const data = text ? (JSON.parse(text) as Detail & T) : undefined;
  if (!response.ok) {
    throw new Error(formatDetail(data?.detail) || `Request failed (${response.status})`);
  }
  return data as T;
}

function formatDetail(detail: Detail["detail"]): string | undefined {
  if (!detail) return undefined;
  if (typeof detail === "string") return detail;
  if (Array.isArray(detail)) {
    return detail.map((item) => item.msg).filter(Boolean).join(" ");
  }
  return undefined;
}

export const api = {
  register: (email: string, password: string) =>
    request<TokenResponse>("/auth/register", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    }, false),
  login: (email: string, password: string) =>
    request<TokenResponse>("/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    }, false),
  me: () => request<User>("/auth/me"),
  listReceipts: (from: string, to: string) =>
    request<ReceiptListItem[]>(`/receipts?from=${from}&to=${to}`),
  getReceipt: (id: string) => request<Receipt>(`/receipts/${id}`),
  createReceipt: (body: ReceiptIn) =>
    request<Receipt>("/receipts", { method: "POST", body: JSON.stringify(body) }),
  updateReceipt: (id: string, body: ReceiptIn) =>
    request<Receipt>(`/receipts/${id}`, { method: "PATCH", body: JSON.stringify(body) }),
  deleteReceipt: (id: string) =>
    request<void>(`/receipts/${id}`, { method: "DELETE" }),
  summary: (from: string, to: string) =>
    request<Summary>(`/stats/summary?from=${from}&to=${to}`),
  listCategories: () => request<Category[]>("/categories"),
  createCategory: (name: string) =>
    request<Category>("/categories", { method: "POST", body: JSON.stringify({ name }) }),
  deleteCategory: (id: string) => request<void>(`/categories/${id}`, { method: "DELETE" }),
};
