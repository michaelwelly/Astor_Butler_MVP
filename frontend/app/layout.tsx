import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "C3FLEX.com | Production stories",
  description: "Video-first production portfolio for stories built to be felt.",
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
