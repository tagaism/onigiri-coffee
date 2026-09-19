"use client";

import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { labelCategory } from "@/lib/money";
import {
  DEFAULT_API_URL,
  clearSession,
  getApiUrl,
  getCurrency,
  getToken,
  setApiUrl,
  setCurrency,
} from "@/lib/session";
import type { Category } from "@/lib/types";

export default function SettingsPage() {
  const router = useRouter();
  const [loggedIn, setLoggedIn] = useState(false);
  const [apiUrl, setApiUrlField] = useState(DEFAULT_API_URL);
  const [currency, setCurrencyField] = useState("JPY");
  const [saved, setSaved] = useState(false);
  const [categories, setCategories] = useState<Category[]>([]);
  const [newCategory, setNewCategory] = useState("");
  const [categoryError, setCategoryError] = useState<string | null>(null);

  useEffect(() => {
    const token = Boolean(getToken());
    setLoggedIn(token);
    setApiUrlField(getApiUrl());
    setCurrencyField(getCurrency());
    if (token) {
      api.listCategories().then(setCategories).catch(() => setCategories([]));
    }
  }, []);

  function onSave(event: FormEvent) {
    event.preventDefault();
    setApiUrl(apiUrl || DEFAULT_API_URL);
    setCurrency(currency || "JPY");
    setSaved(true);
  }

  return (
    <div className="max-w-lg space-y-6">
      <h1 className="font-display text-3xl">Settings</h1>
      <form onSubmit={onSave} className="card space-y-4 p-6">
        <label className="block">
          <span className="mb-1 block text-sm text-[var(--muted)]">API base URL</span>
          <input
            value={apiUrl}
            onChange={(e) => {
              setApiUrlField(e.target.value);
              setSaved(false);
            }}
            className="field"
          />
          <span className="mt-1 block text-xs text-[var(--muted)]">
            Local default: {DEFAULT_API_URL}
          </span>
        </label>
        <label className="block">
          <span className="mb-1 block text-sm text-[var(--muted)]">Default currency</span>
          <input
            value={currency}
            onChange={(e) => {
              setCurrencyField(e.target.value.toUpperCase());
              setSaved(false);
            }}
            className="field"
            maxLength={3}
          />
        </label>
        <button type="submit" className="btn-primary">
          {saved ? "Saved" : "Save"}
        </button>
      </form>
      {loggedIn ? (
        <section className="card space-y-3 p-6">
          <h2 className="font-display text-xl">Categories</h2>
          <ul className="space-y-2">
            {categories.map((category) => (
              <li key={category.id} className="flex items-center justify-between gap-3 text-sm">
                <span>
                  {labelCategory(category.name)}
                  {category.is_default ? (
                    <span className="ml-2 text-xs text-[var(--muted)]">default</span>
                  ) : null}
                </span>
                {category.is_default ? null : (
                  <button
                    type="button"
                    className="text-[var(--muted)] hover:text-[var(--ink)]"
                    onClick={async () => {
                      setCategoryError(null);
                      try {
                        await api.deleteCategory(category.id);
                        setCategories((current) => current.filter((row) => row.id !== category.id));
                      } catch (err) {
                        setCategoryError(err instanceof Error ? err.message : "Could not delete.");
                      }
                    }}
                  >
                    Remove
                  </button>
                )}
              </li>
            ))}
          </ul>
          <div className="flex gap-2">
            <input
              value={newCategory}
              onChange={(e) => setNewCategory(e.target.value)}
              className="field"
              placeholder="Custom category"
            />
            <button
              type="button"
              className="btn-primary shrink-0"
              disabled={!newCategory.trim()}
              onClick={async () => {
                setCategoryError(null);
                try {
                  const created = await api.createCategory(newCategory.trim());
                  setCategories((current) => [...current, created]);
                  setNewCategory("");
                } catch (err) {
                  setCategoryError(err instanceof Error ? err.message : "Could not add.");
                }
              }}
            >
              Add
            </button>
          </div>
          {categoryError ? <p className="text-sm text-red-700">{categoryError}</p> : null}
        </section>
      ) : null}
      {loggedIn ? (
        <button
          type="button"
          className="text-sm text-[var(--muted)]"
          onClick={() => {
            clearSession();
            router.replace("/login");
          }}
        >
          Log out
        </button>
      ) : null}
    </div>
  );
}
