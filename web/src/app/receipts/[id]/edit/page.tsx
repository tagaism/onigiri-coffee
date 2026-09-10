"use client";

import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { ReceiptForm } from "@/components/ReceiptForm";
import { api } from "@/lib/api";
import type { Receipt } from "@/lib/types";

export default function EditReceiptPage() {
  const params = useParams<{ id: string }>();
  const [receipt, setReceipt] = useState<Receipt | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .getReceipt(params.id)
      .then(setReceipt)
      .catch((err: unknown) => setError(err instanceof Error ? err.message : "Not found"));
  }, [params.id]);

  if (error) return <p className="text-red-700">{error}</p>;
  if (!receipt) return <p className="text-[var(--muted)]">Loading receipt…</p>;

  return (
    <div>
      <h1 className="mb-6 font-display text-3xl">Edit receipt</h1>
      <ReceiptForm receipt={receipt} />
    </div>
  );
}
