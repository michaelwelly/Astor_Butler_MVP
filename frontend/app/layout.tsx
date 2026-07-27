import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";

// One family for the whole site — headings differ by weight and tracking,
// not by typeface. See the "Minimal skin" block in globals.css.
const inter = Inter({
  subsets: ["cyrillic", "latin"],
  weight: ["300", "400", "500", "600"],
  variable: "--font-body",
  display: "swap",
});

const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? "https://frontend-eight-tau-82.vercel.app";
const SITE_TITLE = "C3FLEX.com — видео-продакшн с характером";
const SITE_DESCRIPTION =
  "Видео-портфолио независимой продакшн-студии C3FLEX: ивенты, рилсы и коммерческая реклама. Кинематографичная съёмка и монтаж под ключ. Astor Butler подберёт формат и команду под вашу задачу.";

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  title: SITE_TITLE,
  description: SITE_DESCRIPTION,
  keywords: [
    "видеопродакшн",
    "видеосъёмка",
    "ивенты",
    "рилсы",
    "реклама",
    "C3FLEX",
    "монтаж",
  ],
  openGraph: {
    type: "website",
    locale: "ru_RU",
    url: SITE_URL,
    siteName: "C3FLEX",
    title: SITE_TITLE,
    description: SITE_DESCRIPTION,
    images: [
      {
        url: "/og-image.png",
        width: 1200,
        height: 630,
        alt: "C3FLEX — видео-продакшн с характером",
      },
    ],
  },
  twitter: {
    card: "summary_large_image",
    title: SITE_TITLE,
    description: SITE_DESCRIPTION,
    images: ["/og-image.png"],
  },
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="ru" className={inter.variable}>
      <body>{children}</body>
    </html>
  );
}
