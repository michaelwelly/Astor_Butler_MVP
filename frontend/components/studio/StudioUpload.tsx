"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { AlertTriangle, Check, Trash2, UploadCloud, X } from "lucide-react";
import {
  autoChecklist,
  completeness,
  parseFilename,
  type ParsedFile,
} from "@/lib/deliverables";
import { POSTER_MAX_KB, WEIGHT_TARGETS } from "@/lib/studio";

type Dropped = {
  id: number;
  file: File;
  url: string | null; // object URL for mp4/webm preview
  parsed: ParsedFile;
  weightOk: boolean | null; // null → not weight-checked
  weightNote: string;
};

// §4.3 / §7 weight check from the real File size — no backend needed.
function weightCheck(parsed: ParsedFile, bytes: number): { ok: boolean | null; note: string } {
  const mb = bytes / (1024 * 1024);
  const kb = bytes / 1024;
  if (parsed.type === "poster") {
    return { ok: kb <= POSTER_MAX_KB, note: `${kb.toFixed(0)} КБ · лимит ~${POSTER_MAX_KB} КБ` };
  }
  if ((parsed.type === "hero" || parsed.type === "catalog") && parsed.resolution) {
    const target = WEIGHT_TARGETS[parsed.type]?.[parsed.resolution];
    if (!target) return { ok: null, note: `${mb.toFixed(1)} МБ` };
    const [min, max] = target;
    return { ok: mb <= max, note: `${mb.toFixed(1)} МБ · цель ${min}–${max} МБ` };
  }
  return { ok: null, note: `${mb.toFixed(1)} МБ` };
}

let idSeq = 0;

export function StudioUpload() {
  const [items, setItems] = useState<Dropped[]>([]);
  const [dragOver, setDragOver] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  // Revoke every object URL on unmount.
  useEffect(() => {
    return () => {
      setItems((cur) => {
        cur.forEach((i) => i.url && URL.revokeObjectURL(i.url));
        return cur;
      });
    };
  }, []);

  const addFiles = (files: FileList | File[]) => {
    const next: Dropped[] = Array.from(files).map((file) => {
      const parsed = parseFilename(file.name);
      const isVideo = /\.(mp4|webm)$/i.test(file.name);
      const { ok, note } = weightCheck(parsed, file.size);
      return {
        id: idSeq++,
        file,
        url: isVideo ? URL.createObjectURL(file) : null,
        parsed,
        weightOk: ok,
        weightNote: note,
      };
    });
    setSubmitted(false);
    setItems((prev) => [...prev, ...next]);
  };

  const removeItem = (id: number) => {
    setItems((prev) => {
      const it = prev.find((p) => p.id === id);
      if (it?.url) URL.revokeObjectURL(it.url);
      return prev.filter((p) => p.id !== id);
    });
  };

  const clearAll = () => {
    items.forEach((i) => i.url && URL.revokeObjectURL(i.url));
    setItems([]);
    setSubmitted(false);
  };

  const parsedList = useMemo(() => items.map((i) => i.parsed), [items]);
  const reports = useMemo(() => completeness(parsedList), [parsedList]);
  const auto = useMemo(() => autoChecklist(parsedList, reports), [parsedList, reports]);

  const validNames = items.filter((i) => i.parsed.ok).length;
  const weightWarnings = items.filter((i) => i.weightOk === false).length;
  const readyToSubmit = items.length > 0 && validNames === items.length && weightWarnings === 0;

  return (
    <div className="cab-panel">
      <div className="cab-panel-head">
        <div>
          <h1 className="cab-h1">Загрузка рендеров</h1>
          <p className="cab-sub">
            Перетащите готовые файлы — проверю имена (§8), формат, разрешение и вес (§4.3)
            локально, до отправки.
          </p>
        </div>
        {items.length > 0 && (
          <button type="button" className="cab-ghost-btn" onClick={clearAll}>
            <Trash2 size={15} /> Очистить
          </button>
        )}
      </div>

      <div
        className={`cab-drop${dragOver ? " is-over" : ""}`}
        onDragOver={(e) => {
          e.preventDefault();
          setDragOver(true);
        }}
        onDragLeave={() => setDragOver(false)}
        onDrop={(e) => {
          e.preventDefault();
          setDragOver(false);
          if (e.dataTransfer.files.length) addFiles(e.dataTransfer.files);
        }}
        onClick={() => inputRef.current?.click()}
        role="button"
        tabIndex={0}
        onKeyDown={(e) => (e.key === "Enter" || e.key === " ") && inputRef.current?.click()}
      >
        <UploadCloud size={30} />
        <p>Перетащите файлы сюда или нажмите, чтобы выбрать</p>
        <span>MP4 · WebM · WebP · JPG — по схеме {`<проект>-<название>-<тип>-<разрешение>`}</span>
        <input
          ref={inputRef}
          type="file"
          multiple
          accept=".mp4,.webm,.webp,.jpg,.jpeg,video/mp4,video/webm,image/webp,image/jpeg"
          hidden
          onChange={(e) => {
            if (e.target.files?.length) addFiles(e.target.files);
            e.target.value = "";
          }}
        />
      </div>

      {items.length > 0 && (
        <>
          <div className="cab-upload-grid">
            {items.map((it) => {
              const bad = !it.parsed.ok || it.weightOk === false;
              return (
                <div key={it.id} className={`cab-file${bad ? " is-bad" : " is-ok"}`}>
                  <div className="cab-file-media">
                    {it.url ? (
                      <video src={it.url} muted playsInline preload="metadata" />
                    ) : (
                      <div className="cab-file-poster">{it.parsed.ext?.toUpperCase() ?? "?"}</div>
                    )}
                    <button
                      type="button"
                      className="cab-file-remove"
                      onClick={() => removeItem(it.id)}
                      aria-label="Убрать файл"
                    >
                      <X size={14} />
                    </button>
                  </div>
                  <code className="cab-file-name">{it.file.name}</code>
                  <div className="cab-file-tags">
                    <span className={it.parsed.ok ? "tag-ok" : "tag-bad"}>
                      {it.parsed.ok ? <Check size={12} /> : <X size={12} />}
                      {it.parsed.ok ? "имя по схеме" : "имя не по схеме"}
                    </span>
                    <span
                      className={
                        it.weightOk === false ? "tag-bad" : it.weightOk ? "tag-ok" : "tag-mute"
                      }
                    >
                      {it.weightNote}
                    </span>
                  </div>
                  {!it.parsed.ok && (
                    <p className="cab-file-issues">{it.parsed.issues.join(" · ")}</p>
                  )}
                </div>
              );
            })}
          </div>

          <div className="cab-upload-summary">
            <div className="cab-slug-list">
              <h3>Комплектность (§2–§3)</h3>
              {reports.length === 0 && <p className="cab-mute">Нет валидных файлов.</p>}
              {reports.map((r) => (
                <div key={r.slug} className={`cab-slug${r.complete ? " is-ok" : ""}`}>
                  <div className="cab-slug-head">
                    <strong>{r.slug}</strong>
                    <span>{r.complete ? "полный комплект" : `не хватает ${r.missing.length}`}</span>
                  </div>
                  {r.missing.length > 0 && (
                    <ul>
                      {r.missing.map((m) => (
                        <li key={m}>
                          <AlertTriangle size={11} /> {m}
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              ))}
            </div>

            <div className="cab-check-list">
              <h3>Чек-лист приёмки (§10)</h3>
              {[
                ["Именование по схеме", auto.allNamed],
                ["Обе веб-версии MP4 + WebM", auto.bothFormats],
                ["Desktop 1080p + mobile 720p", auto.bothRes],
                ["Постеры WebP + JPG", auto.posterPair],
                ["Вес в целевых пределах (§4.3)", weightWarnings === 0 && items.length > 0],
              ].map(([label, ok]) => (
                <div key={label as string} className={ok ? "auto-ok" : "auto-no"}>
                  {ok ? <Check size={14} /> : <X size={14} />} {label}
                </div>
              ))}
            </div>
          </div>

          <div className="cab-submit-row">
            <button
              type="button"
              className="cab-primary-btn"
              disabled={!readyToSubmit}
              onClick={() => setSubmitted(true)}
            >
              Отправить на приёмку ({items.length})
            </button>
            {submitted && (
              <p className="cab-submit-note">
                Demo: {items.length} файлов проверены и готовы. Реальную загрузку в хранилище
                (POST /api/studio/uploads) поднимет бэкенд.
              </p>
            )}
            {!readyToSubmit && !submitted && (
              <p className="cab-mute">Исправьте имена / вес — тогда можно отправлять.</p>
            )}
          </div>
        </>
      )}
    </div>
  );
}
