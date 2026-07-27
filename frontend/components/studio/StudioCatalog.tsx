"use client";

import { useMemo, useState } from "react";
import {
  Check,
  Copy,
  Pencil,
  Play,
  Plus,
  RotateCcw,
  Search,
  Trash2,
  Upload,
  X,
} from "lucide-react";
import { directions, type DirectionId } from "@/lib/portfolio";
import { resolveMediaRef } from "@/lib/media-ref";
import type { VideoRecord } from "@/lib/video-db";
import { emptyRecord, useVideoDbDraft } from "@/lib/studio-catalog";

const DIR_LABEL: Record<DirectionId, string> = {
  events: "Ивенты",
  reels: "Рилсы",
  commercials: "Реклама",
};

type StatusFilter = "all" | "hosted" | "pending";
const mb = (bytes: number) => (bytes ? `${(bytes / 1048576).toFixed(0)} МБ` : "");

export function StudioCatalog() {
  const { records, hydrated, hasDraft, updateRecord, addRecord, removeRecord, resetDraft, stats, json } =
    useVideoDbDraft();

  const [q, setQ] = useState("");
  const [dir, setDir] = useState<"all" | DirectionId>("all");
  const [status, setStatus] = useState<StatusFilter>("all");
  const [editing, setEditing] = useState<VideoRecord | null>(null);
  const [isNew, setIsNew] = useState(false);
  const [preview, setPreview] = useState<VideoRecord | null>(null);
  const [exportOpen, setExportOpen] = useState(false);
  const [copied, setCopied] = useState(false);

  const filtered = useMemo(() => {
    const needle = q.trim().toLowerCase();
    return records.filter((r) => {
      if (dir !== "all" && r.direction !== dir) return false;
      if (status === "hosted" && !r.src.trim()) return false;
      if (status === "pending" && r.src.trim()) return false;
      if (needle && !(`${r.title} ${r.category}`.toLowerCase().includes(needle))) return false;
      return true;
    });
  }, [records, q, dir, status]);

  const openAdd = () => {
    setEditing(emptyRecord());
    setIsNew(true);
  };
  const openEdit = (r: VideoRecord) => {
    setEditing({ ...r });
    setIsNew(false);
  };
  const closeForm = () => {
    setEditing(null);
    setIsNew(false);
  };
  const saveForm = () => {
    if (!editing || !editing.title.trim()) return;
    if (isNew) addRecord(editing);
    else updateRecord(editing.id, editing);
    closeForm();
  };
  const patch = (p: Partial<VideoRecord>) => setEditing((e) => (e ? { ...e, ...p } : e));

  const copyJson = async () => {
    try {
      await navigator.clipboard.writeText(json);
      setCopied(true);
      setTimeout(() => setCopied(false), 1800);
    } catch {
      /* clipboard blocked — textarea is selectable */
    }
  };

  return (
    <div className="cab-panel">
      <div className="sc-head">
        <div>
          <h3 className="cab-h3">База видео</h3>
          <p className="sc-sub">
            <b className="sc-stat">{stats.total}</b> всего ·{" "}
            <b className="sc-stat sc-stat--ok">{stats.hosted}</b> захостено ·{" "}
            <b className="sc-stat sc-stat--wait">{stats.pending}</b> без хостинга
          </p>
        </div>
        <div className="sc-head-actions">
          <button type="button" className="cab-primary-btn" onClick={openAdd}>
            <Plus size={16} /> Добавить видео
          </button>
          <button type="button" className="sc-ghost-btn" onClick={() => setExportOpen(true)}>
            <Upload size={15} /> Экспорт videos.json
          </button>
          {hasDraft && (
            <button type="button" className="sc-ghost-btn" onClick={resetDraft} title="Вернуть к тому, что в базе">
              <RotateCcw size={15} /> Сбросить
            </button>
          )}
        </div>
      </div>

      <p className="sc-note">
        Правки — <b>локально в этом браузере</b>. Вставьте ссылку хостинга в поле у ролика (или
        откройте «карандаш»), и он появится в каталоге. Чтобы опубликовать для всех: «Экспорт
        videos.json» → заменить <code>frontend/data/videos.json</code> → коммит.
      </p>

      <div className="sc-filters">
        <label className="sc-search">
          <Search size={15} />
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Поиск по названию / папке" />
        </label>
        <select value={dir} onChange={(e) => setDir(e.target.value as "all" | DirectionId)}>
          <option value="all">Все направления</option>
          {directions.map((d) => (
            <option key={d.id} value={d.id}>
              {DIR_LABEL[d.id]}
            </option>
          ))}
        </select>
        <select value={status} onChange={(e) => setStatus(e.target.value as StatusFilter)}>
          <option value="all">Любой статус</option>
          <option value="hosted">Захостено</option>
          <option value="pending">Без хостинга</option>
        </select>
        <span className="sc-count">{filtered.length}</span>
      </div>

      {!hydrated ? (
        <p className="sc-sub">Загрузка базы…</p>
      ) : (
        <div className="sc-list">
          {filtered.map((r) => {
            const hosted = !!r.src.trim();
            const poster = resolveMediaRef(r.poster);
            return (
              <div className="sc-row" key={r.id}>
                <button
                  type="button"
                  className="sc-thumb"
                  onClick={() => hosted && setPreview(r)}
                  disabled={!hosted}
                  aria-label={hosted ? `Смотреть ${r.title}` : "Нет хостинга"}
                >
                  {poster ? (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img src={poster} alt="" loading="lazy" />
                  ) : (
                    <span className="sc-thumb-empty" />
                  )}
                  {hosted && (
                    <span className="sc-thumb-play">
                      <Play size={16} />
                    </span>
                  )}
                </button>

                <div className="sc-row-meta">
                  <strong>{r.title || "— без названия —"}</strong>
                  <span className="sc-row-sub">
                    <span className="sc-badge">{DIR_LABEL[r.direction]}</span>
                    <span className={`sc-status ${hosted ? "is-ok" : "is-wait"}`}>
                      {hosted ? "захостено" : "нет хостинга"}
                    </span>
                    <span className="sc-dim">
                      {r.category}
                      {mb(r.sizeBytes) ? ` · ${mb(r.sizeBytes)}` : ""}
                    </span>
                  </span>
                  <input
                    className="sc-src-input"
                    key={`${r.id}:${r.src}`}
                    defaultValue={r.src}
                    placeholder="Ссылка хостинга (URL или имя файла в public/portfolio)…"
                    onBlur={(e) => {
                      const v = e.target.value.trim();
                      if (v !== r.src) updateRecord(r.id, { src: v });
                    }}
                  />
                </div>

                <div className="sc-row-actions">
                  <button type="button" onClick={() => openEdit(r)} aria-label="Редактировать">
                    <Pencil size={16} />
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      if (window.confirm(`Удалить «${r.title}» из базы?`)) removeRecord(r.id);
                    }}
                    aria-label="Удалить"
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              </div>
            );
          })}
          {filtered.length === 0 && <p className="sc-sub">Ничего не найдено по фильтрам.</p>}
        </div>
      )}

      {/* ── Add / edit ──────────────────────────────────────────────────── */}
      {editing && (
        <div className="sc-modal-backdrop" onClick={closeForm}>
          <div className="sc-modal" onClick={(e) => e.stopPropagation()} role="dialog" aria-modal="true">
            <div className="sc-modal-head">
              <h3>{isNew ? "Новое видео" : "Редактирование"}</h3>
              <button type="button" onClick={closeForm} aria-label="Закрыть">
                <X size={18} />
              </button>
            </div>
            <div className="sc-form">
              <label className="sc-field sc-field--wide">
                <span>Название *</span>
                <input value={editing.title} onChange={(e) => patch({ title: e.target.value })} placeholder="Segreto" />
              </label>
              <label className="sc-field">
                <span>Направление</span>
                <select value={editing.direction} onChange={(e) => patch({ direction: e.target.value as DirectionId })}>
                  {directions.map((d) => (
                    <option key={d.id} value={d.id}>
                      {DIR_LABEL[d.id]}
                    </option>
                  ))}
                </select>
              </label>
              <label className="sc-field">
                <span>Хронометраж</span>
                <input value={editing.duration} onChange={(e) => patch({ duration: e.target.value })} placeholder="01:40" />
              </label>
              <label className="sc-field sc-field--wide">
                <span>Ссылка хостинга (src)</span>
                <input value={editing.src} onChange={(e) => patch({ src: e.target.value })} placeholder="https://…  ·  reel.mp4" />
              </label>
              <label className="sc-field sc-field--wide">
                <span>Постер</span>
                <input value={editing.poster} onChange={(e) => patch({ poster: e.target.value })} placeholder="https://…  ·  reel.jpg" />
              </label>
              <label className="sc-field">
                <span>Ориентация</span>
                <select
                  value={editing.orientation}
                  onChange={(e) => patch({ orientation: e.target.value as VideoRecord["orientation"] })}
                >
                  <option value="">авто (по направлению)</option>
                  <option value="portrait">вертикаль</option>
                  <option value="landscape">горизонталь</option>
                </select>
              </label>
              <label className="sc-check">
                <input type="checkbox" checked={editing.featured} onChange={(e) => patch({ featured: e.target.checked })} />
                <span>Избранное</span>
              </label>
              {!isNew && editing.sourcePath && (
                <p className="sc-field--wide sc-dim sc-srcpath">Архив: {editing.sourcePath}</p>
              )}
            </div>
            <div className="sc-modal-foot">
              <button type="button" className="sc-ghost-btn" onClick={closeForm}>
                Отмена
              </button>
              <button type="button" className="cab-primary-btn" onClick={saveForm} disabled={!editing.title.trim()}>
                <Check size={16} /> {isNew ? "Добавить" : "Сохранить"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── Preview ─────────────────────────────────────────────────────── */}
      {preview && (
        <div className="sc-preview-backdrop" onClick={() => setPreview(null)} role="dialog" aria-modal="true">
          <button type="button" className="sc-preview-close" onClick={() => setPreview(null)} aria-label="Закрыть">
            <X size={24} />
          </button>
          <div className={`sc-preview sc-preview--${preview.orientation || "landscape"}`} onClick={(e) => e.stopPropagation()}>
            <video src={resolveMediaRef(preview.src)} poster={resolveMediaRef(preview.poster)} controls autoPlay loop playsInline />
            <div className="sc-preview-cap">
              <strong>{preview.title}</strong>
              <span>
                {DIR_LABEL[preview.direction]}
                {preview.duration ? ` · ${preview.duration}` : ""}
              </span>
            </div>
          </div>
        </div>
      )}

      {/* ── Export ──────────────────────────────────────────────────────── */}
      {exportOpen && (
        <div className="sc-modal-backdrop" onClick={() => setExportOpen(false)}>
          <div className="sc-modal sc-modal--export" onClick={(e) => e.stopPropagation()} role="dialog" aria-modal="true">
            <div className="sc-modal-head">
              <h3>Экспорт videos.json</h3>
              <button type="button" onClick={() => setExportOpen(false)} aria-label="Закрыть">
                <X size={18} />
              </button>
            </div>
            <p className="sc-sub">
              Скопируйте и замените содержимое <code>frontend/data/videos.json</code>, затем закоммитьте —
              тогда захостенные ролики увидят все.
            </p>
            <textarea className="sc-export" readOnly value={json} onFocus={(e) => e.target.select()} />
            <div className="sc-modal-foot">
              <button type="button" className="cab-primary-btn" onClick={copyJson}>
                {copied ? <Check size={16} /> : <Copy size={16} />} {copied ? "Скопировано" : "Скопировать"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
