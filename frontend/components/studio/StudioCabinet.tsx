"use client";

import { useState } from "react";
import Link from "next/link";
import {
  ArrowLeft,
  ArrowRight,
  FileText,
  Film,
  LayoutDashboard,
  LogOut,
  Play,
  UploadCloud,
} from "lucide-react";
import { ReelsPlayer } from "@/components/ui/ReelsPlayer";
import { portfolioCases } from "@/lib/portfolio";
import {
  editorTasks,
  TZ_ACCEPTANCE,
  TZ_NAMING,
  TZ_RESOLUTIONS,
  useStudioSession,
  WEIGHT_TARGETS,
  type StudioUser,
} from "@/lib/studio";
import { StudioUpload } from "./StudioUpload";
import { StudioCatalog } from "./StudioCatalog";
import { ChatDock } from "./ChatDock";

type TabId = "overview" | "catalog" | "upload";

const TABS: { id: TabId; label: string; icon: typeof LayoutDashboard }[] = [
  { id: "overview", label: "Обзор", icon: LayoutDashboard },
  { id: "catalog", label: "Каталог", icon: Film },
  { id: "upload", label: "Загрузка", icon: UploadCloud },
];

export function StudioCabinet() {
  const { user, loading, enterDemo, signOut } = useStudioSession();
  const [tab, setTab] = useState<TabId>("overview");

  if (loading) return <div className="cab-loading">Загрузка кабинета…</div>;
  if (!user) return <StudioGate onDemo={enterDemo} />;

  return (
    <div className="cab">
      <header className="cab-top">
        <span className="cab-top-brand">
          C3 <b>Studio</b>
        </span>
        <nav className="cab-top-nav">
          {TABS.map((t) => {
            const Icon = t.icon;
            return (
              <button
                key={t.id}
                type="button"
                className={`cab-tab${tab === t.id ? " is-active" : ""}`}
                onClick={() => setTab(t.id)}
              >
                <Icon size={16} /> {t.label}
              </button>
            );
          })}
        </nav>
        <div className="cab-top-actions">
          <Link href="/" className="cab-top-link">
            <ArrowLeft size={14} /> На сайт
          </Link>
          <button type="button" className="cab-top-signout" onClick={() => void signOut()} aria-label="Выйти">
            <LogOut size={16} />
          </button>
        </div>
      </header>

      <main className="cab-main">
        {tab === "overview" && <Overview user={user} onUpload={() => setTab("upload")} />}
        {tab === "catalog" && <StudioCatalog />}
        {tab === "upload" && <UploadTab />}
      </main>

      <ChatDock />
    </div>
  );
}

/* ── Gate ───────────────────────────────────────────────────────────────── */
function StudioGate({ onDemo }: { onDemo: () => void }) {
  return (
    <div className="cab-gate">
      <Link href="/" className="studio-back">
        <ArrowLeft size={16} /> На сайт
      </Link>
      <div className="cab-gate-card">
        <p className="section-label">C3 Studio</p>
        <h1 className="cab-gate-title">
          Вход для
          <br />
          <i>команды.</i>
        </h1>
        <p className="cab-gate-lede">
          Доступ к C3 Studio выдаётся по роли монтажёра. Пока бэкенд авторизации
          поднимается, кабинет открыт в demo-режиме.
        </p>
        <div className="cab-gate-demo">
          <button type="button" onClick={onDemo}>
            Войти в demo-режиме <ArrowRight size={14} />
          </button>
        </div>
      </div>
    </div>
  );
}

/* ── Overview: splash + editor profile + clip rectangles + reels ────────── */
function Overview({ user, onUpload }: { user: StudioUser; onUpload: () => void }) {
  const [reelsStart, setReelsStart] = useState<number | null>(null);
  const tasks = editorTasks();
  const inWork = tasks.filter((t) => t.status !== "accepted").length;
  const accepted = tasks.filter((t) => t.status === "accepted").length;
  const firstName = user.name.split(" ")[0];

  return (
    <div className="cab-panel">
      <section className="cab-splash">
        <div className="cab-splash-copy">
          <p className="section-label">C3 Studio</p>
          <h1 className="cab-splash-title">
            Здравствуйте,
            <br />
            {firstName}.
          </h1>
          <p className="cab-splash-sub">Ваши ролики, приёмка, загрузки и чаты клиентов — в одном месте.</p>
          <button type="button" className="cab-primary-btn cab-primary-btn--lg" onClick={onUpload}>
            <UploadCloud size={18} /> Загрузить рендеры
          </button>
        </div>
        <aside className="cab-editor-card">
          <div className="cab-avatar cab-avatar--lg">{user.name.slice(0, 1).toUpperCase()}</div>
          <strong className="cab-editor-name">{user.name}</strong>
          <span className="cab-editor-role">
            {user.role === "editor" ? "Монтажёр" : user.role}
            {user.demo ? " · demo" : ""}
          </span>
          <div className="cab-editor-stats">
            <div>
              <b>{inWork}</b>
              <span>в работе</span>
            </div>
            <div>
              <b>{accepted}</b>
              <span>принято</span>
            </div>
          </div>
        </aside>
      </section>

      <h3 className="cab-h3">Все ролики</h3>
      <div className="cab-rects">
        {portfolioCases.map((c, i) => (
          <button key={c.id} type="button" className="cab-rect" onClick={() => setReelsStart(i)}>
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src={c.image} alt={c.title} loading="lazy" />
            <span className="cab-rect-play">
              <Play size={20} />
            </span>
            <span className="cab-rect-meta">
              <strong>{c.title}</strong>
              <span>
                {c.category} · {c.duration}
              </span>
            </span>
          </button>
        ))}
      </div>

      {reelsStart !== null && (
        <ReelsPlayer items={portfolioCases} startIndex={reelsStart} onClose={() => setReelsStart(null)} />
      )}
    </div>
  );
}

/* ── Upload tab = uploader + collapsible TZ cheatsheet ──────────────────── */
function UploadTab() {
  return (
    <div className="cab-panel">
      <StudioUpload />
      <details className="cab-tz-details">
        <summary>
          <FileText size={15} /> Памятка по ТЗ — разрешения, вес, именование
        </summary>
        <TzCheatsheet />
      </details>
    </div>
  );
}

function TzCheatsheet() {
  return (
    <div className="cab-tz-body">
      <h3 className="cab-h3">Разрешения и типы (§3)</h3>
      <div className="cab-table-wrap">
        <table className="cab-table">
          <thead>
            <tr>
              <th>Тип</th>
              <th>Разрешение</th>
              <th>Звук</th>
              <th>Примечание</th>
            </tr>
          </thead>
          <tbody>
            {TZ_RESOLUTIONS.map((r) => (
              <tr key={r.type}>
                <td>{r.type}</td>
                <td>{r.res}</td>
                <td>{r.sound}</td>
                <td className="cab-mute">{r.note}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="cab-tz-cols">
        <div>
          <h3 className="cab-h3">Целевой вес (§4.3)</h3>
          <ul className="cab-weights">
            <li>Hero desktop 1080p — {WEIGHT_TARGETS.hero[1080].join("–")} МБ</li>
            <li>Hero mobile 720p — {WEIGHT_TARGETS.hero[720].join("–")} МБ</li>
            <li>Каталог desktop 1080p — {WEIGHT_TARGETS.catalog[1080].join("–")} МБ</li>
            <li>Каталог mobile 720p — {WEIGHT_TARGETS.catalog[720].join("–")} МБ</li>
          </ul>
          <h3 className="cab-h3">Именование (§8)</h3>
          <code className="cab-code-block">{TZ_NAMING}</code>
        </div>
        <div>
          <h3 className="cab-h3">Чек-лист приёмки (§10)</h3>
          <ul className="cab-tz-check">
            {TZ_ACCEPTANCE.map((c) => (
              <li key={c}>{c}</li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  );
}
