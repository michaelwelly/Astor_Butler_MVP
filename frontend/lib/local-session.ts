"use client";

/**
 * Local email session — a working sign-in until the real OAuth backend lands.
 * The user enters an email on the site; we persist it and the whole app (header
 * auth + editor cabinet) reads the same session. When Codex / the Next auth
 * backend ships, `useAuth` will prefer the backend user and this becomes a
 * fallback only.
 */

export type LocalSession = { email: string; name: string };

const KEY = "c3flex.session";
const EVENT = "c3flex-session-change";

export function readLocalSession(): LocalSession | null {
  try {
    const raw = localStorage.getItem(KEY);
    return raw ? (JSON.parse(raw) as LocalSession) : null;
  } catch {
    return null;
  }
}

export function writeLocalSession(email: string): LocalSession {
  const clean = email.trim();
  const name = clean.split("@")[0] || clean;
  const session: LocalSession = { email: clean, name };
  try {
    localStorage.setItem(KEY, JSON.stringify(session));
    window.dispatchEvent(new Event(EVENT));
  } catch {
    /* ignore */
  }
  return session;
}

export function clearLocalSession(): void {
  try {
    localStorage.removeItem(KEY);
    window.dispatchEvent(new Event(EVENT));
  } catch {
    /* ignore */
  }
}

/** Subscribe to session changes (same tab via custom event, other tabs via storage). */
export function onLocalSessionChange(cb: () => void): () => void {
  window.addEventListener(EVENT, cb);
  window.addEventListener("storage", cb);
  return () => {
    window.removeEventListener(EVENT, cb);
    window.removeEventListener("storage", cb);
  };
}
