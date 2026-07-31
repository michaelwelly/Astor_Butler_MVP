"use client";

import { useAuth } from "@/hooks/useAuth";
import { writeLocalSession } from "@/lib/local-session";
import { portfolioCases, type PortfolioCase } from "@/lib/portfolio";

/* ─────────────────────────────────────────────────────────────────────────
   Editor cabinet — session
   Real auth comes from useAuth() (GET /api/auth/me, cookie-based, backend-owned).
   Until Codex ships editor roles, a local demo session lets the team open and
   use the cabinet. Everything else (upload target, chat list) is demo/mock and
   flips to real endpoints without UI changes.
   ───────────────────────────────────────────────────────────────────────── */

export type StudioUser = { name: string; role: string; demo: boolean };

export function useStudioSession() {
  const { user: authUser, loading, isLocal, logout } = useAuth();
  const user: StudioUser | null = authUser
    ? {
        name: authUser.claims?.name || authUser.claims?.email || "Монтажёр",
        role: authUser.roles?.[0] ?? "editor",
        demo: isLocal,
      }
    : null;
  const enterDemo = () => writeLocalSession("editor@c3flex.local");
  const signOut = async () => {
    await logout();
  };
  return { user, loading, enterDemo, signOut };
}

/* ── Render-TZ reference data (§3 / §4.3 / §8 / §10) ─────────────────────── */

// §4.3 target weights, MB [min, max].
export const WEIGHT_TARGETS: Record<"hero" | "catalog", Record<number, [number, number]>> = {
  hero: { 1080: [6, 12], 720: [3, 6] },
  catalog: { 1080: [15, 40], 720: [8, 20] },
};
export const POSTER_MAX_KB = 250; // §7 hero poster ceiling (catalog preview is 150)

export const TZ_RESOLUTIONS = [
  { type: "Hero — desktop", res: "1920×1080", sound: "нет", note: "фон, луп 10–25 сек" },
  { type: "Hero — mobile", res: "1280×720", sound: "нет", note: "облегчённый фон" },
  { type: "Каталог — desktop", res: "1920×1080", sound: "да", note: "просмотр по клику" },
  { type: "Каталог — mobile", res: "1280×720", sound: "да", note: "мобильная версия" },
  { type: "Постер", res: "1920×1080 · 1280×720", sound: "—", note: "WebP + JPG, первый кадр" },
];

export const TZ_NAMING = "<проект>-<название>-<тип>-<разрешение>.<ext>";

export const TZ_ACCEPTANCE = [
  "Пиксельный формат yuv420p (8-bit)",
  "+faststart включён (moov в начале)",
  "Hero — без звука; каталог — AAC 160k / Opus 128k",
  "Сданы обе веб-версии: MP4 + WebM",
  "Сданы desktop (1080p) и mobile (720p)",
  "Нет видимого бандинга в тёмных сценах",
  "Hero-луп бесшовный, 10–25 сек",
  "Постеры WebP + JPG, первый кадр, в пределах веса",
  "Именование по схеме (латиница, без пробелов)",
  "Вес файлов в целевых пределах (§4.3)",
];

/* ── Editor tasks (deterministic mock over the portfolio) ───────────────── */

export type TaskStatus = "assigned" | "rendering" | "review" | "accepted";

export const STATUS_LABEL: Record<TaskStatus, string> = {
  assigned: "Назначено",
  rendering: "Рендер",
  review: "На приёмке",
  accepted: "Принято",
};

export type EditorTask = { case: PortfolioCase; status: TaskStatus };

// Fixed pattern → no randomness, stable across renders.
const STATUS_CYCLE: TaskStatus[] = [
  "accepted",
  "accepted",
  "review",
  "rendering",
  "rendering",
  "assigned",
  "assigned",
  "assigned",
];

export function editorTasks(): EditorTask[] {
  return portfolioCases.slice(0, 8).map((c, i) => ({
    case: c,
    status: STATUS_CYCLE[i % STATUS_CYCLE.length],
  }));
}

/* ── Mock client chats (until GET /api/chats ships) ─────────────────────── */

export type MockMessage = { from: "client" | "studio"; text: string; at: string };
export type MockChat = {
  id: string;
  name: string;
  page: string;
  video: string | null;
  updated: string;
  unread: number;
  messages: MockMessage[];
};

export const MOCK_CHATS: MockChat[] = [
  {
    id: "chat-01",
    name: "Ресторан Segreto",
    page: "/#catalog",
    video: "Segreto",
    updated: "12:40",
    unread: 2,
    messages: [
      { from: "client", text: "Нужен ролик атмосферы зала, как в кейсе Segreto. Сроки — 2 недели.", at: "12:31" },
      { from: "studio", text: "Здравствуйте! Отличная референс-работа. Уточните хронометраж и площадку.", at: "12:34" },
      { from: "client", text: "60 секунд, вертикаль для Reels + горизонталь на сайт.", at: "12:40" },
    ],
  },
  {
    id: "chat-02",
    name: "Cristal / бренд",
    page: "/#services",
    video: "Cristal",
    updated: "Вчера",
    unread: 0,
    messages: [
      { from: "client", text: "Интересует пакет 10 Reels для продуктовой линейки.", at: "18:02" },
      { from: "studio", text: "Пакет Reels A — 85 000 ₽: идея, съёмка, монтаж, одна правка. Подходит?", at: "18:20" },
    ],
  },
  {
    id: "chat-03",
    name: "Night Drive",
    page: "/#about",
    video: null,
    updated: "2 дня",
    unread: 0,
    messages: [
      { from: "client", text: "Хотим кинематографичный рекламный фильм, референс — ваш Night Drive.", at: "10:05" },
    ],
  },
];
