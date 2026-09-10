import type { User } from "./types";

const TOKEN_KEY = "onigiri_token";
const USER_KEY = "onigiri_user";
const API_URL_KEY = "onigiri_api_url";
const CURRENCY_KEY = "onigiri_currency";

export const DEFAULT_API_URL = "http://localhost:8000";
export const DEFAULT_CURRENCY = "JPY";

function read(key: string): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(key);
}

function write(key: string, value: string | null) {
  if (typeof window === "undefined") return;
  if (value === null) window.localStorage.removeItem(key);
  else window.localStorage.setItem(key, value);
}

export function getToken(): string | null {
  return read(TOKEN_KEY);
}

export function getUser(): User | null {
  const raw = read(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as User;
  } catch {
    return null;
  }
}

export function getApiUrl(): string {
  return (read(API_URL_KEY) || process.env.NEXT_PUBLIC_API_URL || DEFAULT_API_URL).replace(
    /\/$/,
    "",
  );
}

export function getCurrency(): string {
  return (read(CURRENCY_KEY) || getUser()?.default_currency || DEFAULT_CURRENCY).toUpperCase();
}

export function setSession(token: string, user: User) {
  write(TOKEN_KEY, token);
  write(USER_KEY, JSON.stringify(user));
  write(CURRENCY_KEY, user.default_currency);
}

export function clearSession() {
  write(TOKEN_KEY, null);
  write(USER_KEY, null);
}

export function setApiUrl(url: string) {
  write(API_URL_KEY, url.trim().replace(/\/$/, ""));
}

export function setCurrency(code: string) {
  write(CURRENCY_KEY, code.trim().toUpperCase());
}
