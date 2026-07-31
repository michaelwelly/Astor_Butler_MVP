"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { videoDb, type VideoRecord } from "@/lib/video-db";

/* ─────────────────────────────────────────────────────────────────────────
   Studio video-DB draft.

   The public site reads the committed data/videos.json (via video-db.ts). The
   browser can't write that file, so the studio edits a DRAFT in localStorage —
   per-browser, not published. "Publish" = export the draft as videos.json,
   paste it in, commit (or POST to the backend once it exists). Seeded from the
   committed DB, so the editor always opens on the real 197-record inventory.
   ───────────────────────────────────────────────────────────────────────── */

const STORAGE_KEY = "c3flex.studio.videodb.draft.v1";

/** Fresh copy so edits never mutate the imported JSON module. */
function seed(): VideoRecord[] {
  return videoDb.map((r) => ({ ...r }));
}

function readStored(): VideoRecord[] | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? (parsed as VideoRecord[]) : null;
  } catch {
    return null;
  }
}

function writeStored(records: VideoRecord[]): void {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(records));
  } catch {
    /* quota / private mode — draft just won't persist */
  }
}

let seq = 0;
/** A blank record for a manually-added video (not from the archive). */
export function emptyRecord(): VideoRecord {
  seq += 1;
  return {
    id: `manual-${seq}-${(typeof performance !== "undefined" ? performance.now() : 0) | 0}`,
    title: "",
    category: "Добавлено вручную",
    direction: "reels",
    sourcePath: "",
    sizeBytes: 0,
    src: "",
    poster: "",
    duration: "",
    featured: false,
    orientation: "",
  };
}

/** Pretty JSON ready to replace frontend/data/videos.json. */
export function toJson(records: VideoRecord[]): string {
  return JSON.stringify(records, null, 2);
}

/**
 * DB draft state + mutators. Persists on each mutation only (NOT via a records
 * effect) so resetDraft can truly clear back to "follow the committed DB".
 */
export function useVideoDbDraft() {
  const [records, setRecords] = useState<VideoRecord[]>(seed);
  const [hydrated, setHydrated] = useState(false);
  const [hasDraft, setHasDraft] = useState(false);

  useEffect(() => {
    const stored = readStored();
    if (stored) {
      setRecords(stored);
      setHasDraft(true);
    }
    setHydrated(true);
  }, []);

  const commit = useCallback((update: (cur: VideoRecord[]) => VideoRecord[]) => {
    setRecords((cur) => {
      const next = update(cur);
      writeStored(next);
      return next;
    });
    setHasDraft(true);
  }, []);

  const updateRecord = useCallback(
    (id: string, patch: Partial<VideoRecord>) =>
      commit((cur) => cur.map((r) => (r.id === id ? { ...r, ...patch } : r))),
    [commit],
  );
  const addRecord = useCallback((r: VideoRecord) => commit((cur) => [r, ...cur]), [commit]);
  const removeRecord = useCallback((id: string) => commit((cur) => cur.filter((r) => r.id !== id)), [commit]);
  const resetDraft = useCallback(() => {
    if (typeof window !== "undefined") window.localStorage.removeItem(STORAGE_KEY);
    setRecords(seed());
    setHasDraft(false);
  }, []);

  const stats = useMemo(() => {
    const hosted = records.filter((r) => r.src.trim()).length;
    return { total: records.length, hosted, pending: records.length - hosted };
  }, [records]);

  const json = useMemo(() => toJson(records), [records]);

  return { records, hydrated, hasDraft, updateRecord, addRecord, removeRecord, resetDraft, stats, json };
}
