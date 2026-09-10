"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { clearSession, getUser } from "@/lib/session";
import type { User } from "@/lib/types";

export function Header() {
  const pathname = usePathname();
  const router = useRouter();
  const [user, setUser] = useState<User | null>(null);

  useEffect(() => {
    setUser(getUser());
  }, [pathname]);

  if (pathname === "/login" || !user) return null;

  return (
    <header className="border-b border-[var(--line)] bg-[var(--paper)]/80 backdrop-blur">
      <div className="mx-auto flex max-w-3xl items-center justify-between gap-4 px-4 py-3">
        <Link href="/" className="font-display text-xl tracking-tight text-[var(--ink)]">
          Onigiri
        </Link>
        <nav className="flex items-center gap-3 text-sm">
          <Link
            href="/receipts/new"
            className="rounded-full bg-[var(--terracotta)] px-3 py-1.5 font-medium text-white hover:bg-[var(--terracotta-dark)]"
          >
            Add receipt
          </Link>
          <Link href="/settings" className="text-[var(--muted)] hover:text-[var(--ink)]">
            Settings
          </Link>
          <button
            type="button"
            className="text-[var(--muted)] hover:text-[var(--ink)]"
            onClick={() => {
              clearSession();
              setUser(null);
              router.replace("/login");
            }}
          >
            Log out
          </button>
          <span className="hidden text-[var(--muted)] sm:inline">{user.email}</span>
        </nav>
      </div>
    </header>
  );
}
