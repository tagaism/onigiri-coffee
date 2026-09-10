"use client";

import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import {
  DEFAULT_API_URL,
  clearSession,
  getApiUrl,
  getCurrency,
  getToken,
  setApiUrl,
  setCurrency,
} from "@/lib/session";

export default function SettingsPage() {
  const router = useRouter();
  const [loggedIn, setLoggedIn] = useState(false);
  const [apiUrl, setApiUrlField] = useState(DEFAULT_API_URL);
  const [currency, setCurrencyField] = useState("JPY");
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    setLoggedIn(Boolean(getToken()));
    setApiUrlField(getApiUrl());
    setCurrencyField(getCurrency());
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
