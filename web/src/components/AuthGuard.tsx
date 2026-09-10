"use client";

import { useEffect, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import { getToken } from "@/lib/session";

export function AuthGuard({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const [ready, setReady] = useState(false);
  const publicRoute = pathname === "/login" || pathname === "/settings";

  useEffect(() => {
    const token = getToken();
    if (!token && !publicRoute) {
      router.replace("/login");
      return;
    }
    if (token && pathname === "/login") {
      router.replace("/");
      return;
    }
    setReady(true);
  }, [pathname, publicRoute, router]);

  if (!ready) {
    return (
      <div className="flex min-h-screen items-center justify-center text-sm text-[var(--muted)]">
        Loading…
      </div>
    );
  }

  return <>{children}</>;
}
