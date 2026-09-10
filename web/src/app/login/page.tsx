"use client";

import Link from "next/link";
import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { setSession } from "@/lib/session";

export default function LoginPage() {
  const router = useRouter();
  const [isRegister, setIsRegister] = useState(false);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    if (!email.trim() || password.length < 8) {
      setError("Email and a password of at least 8 characters are required.");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const result = isRegister
        ? await api.register(email.trim(), password)
        : await api.login(email.trim(), password);
      setSession(result.access_token, result.user);
      router.replace("/");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not sign in.");
      setLoading(false);
    }
  }

  return (
    <div className="mx-auto max-w-md pt-10">
      <p className="font-display text-5xl tracking-tight">Onigiri</p>
      <p className="mt-2 text-[var(--muted)]">Track what you spend. Type the items yourself.</p>
      <form onSubmit={onSubmit} className="card mt-8 space-y-4 p-6">
        <label className="block">
          <span className="mb-1 block text-sm text-[var(--muted)]">Email</span>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="field"
            autoComplete="email"
          />
        </label>
        <label className="block">
          <span className="mb-1 block text-sm text-[var(--muted)]">Password</span>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="field"
            autoComplete={isRegister ? "new-password" : "current-password"}
          />
        </label>
        {error ? <p className="text-sm text-red-700">{error}</p> : null}
        <button type="submit" className="btn-primary w-full" disabled={loading}>
          {loading ? "Working…" : isRegister ? "Create account" : "Log in"}
        </button>
        <button
          type="button"
          className="w-full text-sm text-[var(--muted)]"
          onClick={() => {
            setIsRegister((value) => !value);
            setError(null);
          }}
        >
          {isRegister ? "Already have an account? Log in" : "New here? Create an account"}
        </button>
      </form>
      <p className="mt-4 text-center text-sm text-[var(--muted)]">
        API URL can be changed in{" "}
        <Link href="/settings" className="text-[var(--terracotta)]">
          settings
        </Link>
        .
      </p>
    </div>
  );
}
