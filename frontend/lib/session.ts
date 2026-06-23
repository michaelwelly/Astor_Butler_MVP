/**
 * Anonymous web session identity.
 *
 * Per FRONTEND_BACKEND_CONTRACTS.md §4: a web user can chat without OAuth once
 * privacy consent is accepted. `chatId` is a temporary local numeric id until
 * the backend issues stable web sessions; `sessionId` is a UUID kept in local
 * storage/cookie after consent. Nothing here is FSM state.
 */

const SESSION_KEY = "c3flex.sessionId";
const CHAT_ID_KEY = "c3flex.chatId";

function uuid(): string {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  // RFC4122-ish fallback for older runtimes.
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === "x" ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

function safeGet(key: string): string | null {
  try {
    return typeof window !== "undefined" ? window.localStorage.getItem(key) : null;
  } catch {
    return null;
  }
}

function safeSet(key: string, value: string): void {
  try {
    if (typeof window !== "undefined") window.localStorage.setItem(key, value);
  } catch {
    /* storage unavailable (private mode) — degrade to in-memory id */
  }
}

let memorySessionId: string | null = null;
let memoryChatId: number | null = null;

/** Stable anonymous session id (persisted after consent, memory-only before). */
export function getSessionId(): string {
  const stored = safeGet(SESSION_KEY);
  if (stored) return stored;
  if (!memorySessionId) memorySessionId = uuid();
  return memorySessionId;
}

/** Persist the session id once the user has accepted consent. */
export function persistSessionId(sessionId: string): void {
  safeSet(SESSION_KEY, sessionId);
}

/** externalUserId for the Web channel, e.g. "web:anon:<uuid>". */
export function getExternalUserId(): string {
  return `web:anon:${getSessionId()}`;
}

/**
 * Temporary numeric chatId for anonymous web users. Stable within a session so
 * the backend admin projection can group messages. Replaced once the backend
 * issues real web sessions.
 */
export function getTempChatId(): number {
  const stored = safeGet(CHAT_ID_KEY);
  if (stored) {
    const n = parseInt(stored, 10);
    if (!Number.isNaN(n)) return n;
  }
  if (memoryChatId == null) {
    memoryChatId = 900_000_000 + Math.floor(Math.random() * 1_000_000);
  }
  return memoryChatId;
}

export function persistChatId(chatId: number): void {
  safeSet(CHAT_ID_KEY, String(chatId));
}

/** correlationId format from the contract: web-<iso>-<short-session>. */
export function buildCorrelationId(sessionId: string): string {
  const shortId = sessionId.slice(0, 8);
  return `web-${new Date().toISOString()}-${shortId}`;
}
