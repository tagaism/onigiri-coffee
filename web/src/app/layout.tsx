import type { Metadata } from "next";
import { Figtree, Fraunces } from "next/font/google";
import { AuthGuard } from "@/components/AuthGuard";
import { Header } from "@/components/Header";
import "./globals.css";

const figtree = Figtree({
  variable: "--font-figtree",
  subsets: ["latin"],
});

const fraunces = Fraunces({
  variable: "--font-fraunces",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Onigiri",
  description: "Track personal spending from receipts you enter by hand.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en" className={`${figtree.variable} ${fraunces.variable} h-full antialiased`}>
      <body className="min-h-full">
        <AuthGuard>
          <Header />
          <main className="mx-auto w-full max-w-3xl px-4 py-8">{children}</main>
        </AuthGuard>
      </body>
    </html>
  );
}
